/**
 * The owner of the original code is SURFnet BV.
 *
 * Portions created by the original owner are Copyright (C) 2011-2012 the
 * original owner. All Rights Reserved.
 *
 * Portions created by other contributors are Copyright (C) the contributor.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   (Contributors insert name & email here)
 *
 * This file is part of the SURFnet7 Bandwidth on Demand software.
 *
 * The SURFnet7 Bandwidth on Demand software is free software: you can
 * redistribute it and/or modify it under the terms of the BSD license
 * included with this distribution.
 *
 * If the BSD license cannot be found with this distribution, it is available
 * at the following location <http://www.opensource.org/licenses/BSD-3-Clause>
 */
package nl.surfnet.bod.service;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static nl.surfnet.bod.domain.ReservationStatus.PREPARING;
import static nl.surfnet.bod.domain.ReservationStatus.RUNNING;
import static nl.surfnet.bod.domain.ReservationStatus.SCHEDULED;

import java.util.*;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import nl.surfnet.bod.domain.*;
import nl.surfnet.bod.nbi.NbiClient;
import nl.surfnet.bod.repo.ReservationArchiveRepo;
import nl.surfnet.bod.repo.ReservationRepo;
import nl.surfnet.bod.web.security.RichUserDetails;
import nl.surfnet.bod.web.security.Security;
import nl.surfnet.bod.web.view.ElementActionView;
import nl.surfnet.bod.web.view.ReservationFilterView;
import nl.surfnet.bod.web.view.ReservationView;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.*;

@Service
@Transactional
public class ReservationService extends AbstractFullTextSearchService<ReservationView, Reservation> {

  private static final Function<Reservation, ReservationArchive> TO_RESERVATION_ARCHIVE = //
  new Function<Reservation, ReservationArchive>() {
    @Override
    public ReservationArchive apply(Reservation reservation) {
      return new ReservationArchive(reservation);
    }
  };

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Resource
  private ReservationRepo reservationRepo;

  @Resource
  private ReservationArchiveRepo reservationArchiveRepo;

  @Resource
  private NbiClient nbiClient;

  @Resource
  private ReservationToNbi reservationToNbi;

  @PersistenceContext
  private EntityManager entityManager;

  @Resource
  private LogEventService logEventService;

  /**
   * Activates an existing reservation;
   *
   * @param reservation
   *          {@link Reservation} to activate
   * @return true if the reservation was successfully activated, false otherwise
   */
  public void provision(Reservation reservation, Optional<NsiRequestDetails> requestDetails) {
    checkNotNull(reservation);

    reservationToNbi.provision(reservation, requestDetails);
  }

  /**
   * Creates a {@link Reservation} which is auto provisioned
   *
   * @param reservation
   * @See {@link #create(Reservation)}
   */
  public void create(Reservation reservation) {
    create(reservation, true, Optional.<NsiRequestDetails> absent());
  }

  /**
   * Reserves a reservation using the {@link NbiClient} asynchronously.
   *
   * @param reservation
   * @param autoProvision
   *          , indicates if the reservations should be automatically
   *          provisioned
   * @return ReservationId, scheduleId from NMS
   *
   */
  public Reservation create(Reservation reservation, boolean autoProvision,
      Optional<NsiRequestDetails> nsiRequestDetails) {
    checkState(reservation.getSourcePort().getVirtualResourceGroup().equals(reservation.getVirtualResourceGroup()));
    checkState(reservation.getDestinationPort().getVirtualResourceGroup().equals(reservation.getVirtualResourceGroup()));

    fillStartTimeIfEmpty(reservation);
    stripSecondsAndMillis(reservation);

    logEventService.logCreateEvent(Security.getUserDetails(), reservation, reservation.getName());

    reservation = reservationRepo.save(reservation);

    // Async call to nbi
    reservationToNbi.reserve(reservation, autoProvision, nsiRequestDetails);

    return reservation;
  }

  private void fillStartTimeIfEmpty(Reservation reservation) {
    if (reservation.getStartDateTime() == null) {
      reservation.setStartDateTime(LocalDateTime.now().plusMinutes(1));
    }
  }

  private void stripSecondsAndMillis(Reservation reservation) {
    if (reservation.getStartDateTime() != null) {
      reservation.setStartDateTime(reservation.getStartDateTime().withSecondOfMinute(0).withMillisOfSecond(0));
    }
    if (reservation.getEndDateTime() != null) {
      reservation.setEndDateTime(reservation.getEndDateTime().withSecondOfMinute(0).withMillisOfSecond(0));
    }
  }

  public Reservation find(Long id) {
    return reservationRepo.findOne(id);
  }

  public List<Reservation> findEntries(int firstResult, int maxResults, Sort sort) {
    final RichUserDetails user = Security.getUserDetails();

    if (user.getUserGroups().isEmpty()) {
      return Collections.emptyList();
    }

    return reservationRepo.findAll(forCurrentUser(user), new PageRequest(firstResult / maxResults, maxResults, sort))
        .getContent();
  }

  public Collection<Reservation> findByVirtualPort(VirtualPort port) {
    return reservationRepo.findBySourcePortOrDestinationPort(port, port);
  }

  public long countForUser(RichUserDetails user) {
    if (user.getUserGroups().isEmpty()) {
      return 0;
    }

    return reservationRepo.count(forCurrentUser(user));
  }

  public Reservation update(Reservation reservation) {
    checkState(reservation.getSourcePort().getVirtualResourceGroup().equals(reservation.getVirtualResourceGroup()));
    checkState(reservation.getDestinationPort().getVirtualResourceGroup().equals(reservation.getVirtualResourceGroup()));

    log.debug("Updating reservation: {}", reservation.getReservationId());
    logEventService.logUpdateEvent(Security.getUserDetails(), reservation, reservation.getName());
    return reservationRepo.save(reservation);
  }

  /**
   * Cancels a reservation if the current user has the correct role and the
   * reservation is allowed to be deleted depending on its state. Updates the
   * state of the reservation.
   *
   * @param reservation
   *          {@link Reservation} to delete
   * @return true if the reservation was canceled, false otherwise.
   */
  public boolean cancel(Reservation reservation, RichUserDetails user) {
    return cancelWithReason(reservation, "Cancelled by " + user.getDisplayName(), user);
  }

  /**
   * A reservation is allowed to be delete for the following cases:
   * <ul>
   * <li>a manager may delete a reservation to minimal one of his ports</li>
   * <li>or</li>
   * <li>a user may delete a reservation if he is a member of the
   * virtualResourceGroup of the reservation</li>
   * <li>and</li>
   * <li>the current status of the reservation must allow it</li>
   * </ul>
   *
   * @param reservation
   *          {@link Reservation} to check
   * @param role
   *          {@link BodRole} the selected user role
   * @return true if the reservation is allowed to be delete, false otherwise
   */
  public ElementActionView isDeleteAllowed(Reservation reservation, BodRole role) {
    if (!reservation.getStatus().isDeleteAllowed()) {
      return new ElementActionView(false, "reservation_state_transition_not_allowed");
    }

    return isDeleteAllowedForUserOnly(reservation, role);
  }

  private ElementActionView isDeleteAllowedForUserOnly(Reservation reservation, BodRole role) {
    if (role.isNocRole()) {
      return new ElementActionView(true, "label_cancel");
    }
    else if (role.isManagerRole()
        && (reservation.getSourcePort().getPhysicalResourceGroup().getId().equals(role.getPhysicalResourceGroupId())
            || reservation.getDestinationPort().getPhysicalResourceGroup().getId().equals(role.getPhysicalResourceGroupId()))) {
      return new ElementActionView(true, "label_cancel");
    }
    else if (role.isUserRole() && Security.isUserMemberOf(reservation.getVirtualResourceGroup())) {
      return new ElementActionView(true, "label_cancel");
    }

    return new ElementActionView(false, "reservation_cancel_user_has_no_rights");
  }

  public ReservationStatus getStatus(Reservation reservation) {
    return nbiClient.getReservationStatus(reservation.getReservationId());
  }

  /**
   * Finds all reservations which start or ends on the given dateTime and have a
   * status which can still change its status.
   *
   * @param dateTime
   *          {@link LocalDateTime} to search for
   * @return list of found Reservations
   */
  public Collection<Reservation> findReservationsToPoll(LocalDateTime dateTime) {
    Set<Reservation> reservations = Sets.newHashSet();
    reservations.addAll(reservationRepo.findAll(specReservationsThatCouldStart(dateTime)));
    reservations.addAll(findReservationWithStatus(RUNNING, PREPARING));

    return reservations;
  }

  private Specification<Reservation> forVirtualResourceGroup(final VirtualResourceGroup vrg) {
    return new Specification<Reservation>() {
      @Override
      public Predicate toPredicate(Root<Reservation> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return cb.equal(root.get(Reservation_.virtualResourceGroup), vrg);
      }

    };
  }

  private Specification<Reservation> forManager(final RichUserDetails manager) {
    return new Specification<Reservation>() {
      @Override
      public Predicate toPredicate(Root<Reservation> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

        Long prgId = manager.getSelectedRole().getPhysicalResourceGroupId();
        return cb.and(cb.or(
            cb.equal(
                root.get(Reservation_.sourcePort).get(VirtualPort_.physicalPort)
                    .get(PhysicalPort_.physicalResourceGroup).get(PhysicalResourceGroup_.id), prgId),
            cb.equal(
                root.get(Reservation_.destinationPort).get(VirtualPort_.physicalPort)
                    .get(PhysicalPort_.physicalResourceGroup).get(PhysicalResourceGroup_.id), prgId)));
      }
    };
  }

  private Specification<Reservation> forCurrentUser(final RichUserDetails user) {
    return new Specification<Reservation>() {
      @Override
      public Predicate toPredicate(Root<Reservation> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return cb.and(root.get(Reservation_.virtualResourceGroup).get(VirtualResourceGroup_.surfconextGroupId)
            .in(user.getUserGroupIds()));
      }
    };
  }

  private Specification<Reservation> forStatus(final ReservationStatus status) {
    return new Specification<Reservation>() {
      @Override
      public Predicate toPredicate(Root<Reservation> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return cb.equal(root.get(Reservation_.status), status);
      }
    };
  }

  private Specification<Reservation> specReservationsThatCouldStart(final LocalDateTime startDateTime) {
    return new Specification<Reservation>() {
      @Override
      public javax.persistence.criteria.Predicate toPredicate(Root<Reservation> reservation, CriteriaQuery<?> query,
          CriteriaBuilder cb) {

        return cb.and(cb.lessThanOrEqualTo(reservation.get(Reservation_.startDateTime), startDateTime), reservation
            .get(Reservation_.status).in(ImmutableList.of(ReservationStatus.REQUESTED, ReservationStatus.SCHEDULED)));
      }
    };
  }

  private Specification<Reservation> specByPhysicalPort(final PhysicalPort port) {
    return new Specification<Reservation>() {
      @Override
      public Predicate toPredicate(Root<Reservation> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return cb.or(cb.equal(root.get(Reservation_.sourcePort).get(VirtualPort_.physicalPort), port),
            cb.equal(root.get(Reservation_.destinationPort).get(VirtualPort_.physicalPort), port));
      }
    };
  }

  private Specification<Reservation> specActiveReservations() {
    return new Specification<Reservation>() {
      @Override
      public Predicate toPredicate(Root<Reservation> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return root.get(Reservation_.status).in(ReservationStatus.TRANSITION_STATES);
      }
    };
  }

  private Specification<Reservation> specFilteredReservationsForUser(final ReservationFilterView filter,
      final RichUserDetails user) {

    return Specifications.where(specFilteredReservations(filter)).and(forCurrentUser(user));
  }

  private Specification<Reservation> specFilteredReservationsForManager(final ReservationFilterView filter,
      final RichUserDetails manager) {

    return Specifications.where(specFilteredReservations(filter)).and(forManager(manager));
  }

  private Specification<Reservation> specFilteredReservations(final ReservationFilterView filter) {
    Specification<Reservation> filterSpecOnStart = new Specification<Reservation>() {
      @Override
      public Predicate toPredicate(Root<Reservation> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return cb.between(root.get(Reservation_.startDateTime), filter.getStart(), filter.getEnd());
      }
    };

    Specification<Reservation> filterSpecOnEnd = new Specification<Reservation>() {
      @Override
      public Predicate toPredicate(Root<Reservation> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return cb.or(cb.isNull(root.get(Reservation_.endDateTime)),
            cb.between(root.get(Reservation_.endDateTime), filter.getStart(), filter.getEnd()));
      }
    };

    Specification<Reservation> specficiation = null;
    if (filter.isFilterOnStatusOnly()) {
      specficiation = forStatus(filter.getStatus());
    }
    else if (filter.isFilterOnReservationEndOnly()) {
      specficiation = filterSpecOnEnd;
    }
    else {
      specficiation = Specifications.where(filterSpecOnEnd).or(filterSpecOnStart);
    }

    return specficiation;
  }

  public List<Reservation> findEntriesForUserUsingFilter(final RichUserDetails user,
      final ReservationFilterView filter, int firstResult, int maxResults, Sort sort) {

    if (user.getUserGroupIds().isEmpty()) {
      return Collections.emptyList();
    }

    return reservationRepo.findAll(specFilteredReservationsForUser(filter, user),
        new PageRequest(firstResult / maxResults, maxResults, sort)).getContent();
  }

  public List<Reservation> findEntriesForManagerUsingFilter(RichUserDetails manager, ReservationFilterView filter,
      int firstResult, int maxResults, Sort sort) {

    return reservationRepo.findAll(specFilteredReservationsForManager(filter, manager),
        new PageRequest(firstResult / maxResults, maxResults, sort)).getContent();
  }

  public List<Reservation> findAllEntriesUsingFilter(final ReservationFilterView filter, int firstResult,
      int maxResults, Sort sort) {

    return reservationRepo.findAll(specFilteredReservations(filter),
        new PageRequest(firstResult / maxResults, maxResults, sort)).getContent();
  }

  public long countForFilterAndUser(RichUserDetails user, ReservationFilterView filter) {
    if (user.getUserGroupIds().isEmpty()) {
      return 0;
    }

    return reservationRepo.count(specFilteredReservationsForUser(filter, user));
  }

  public long countForFilterAndManager(RichUserDetails manager, ReservationFilterView filter) {
    return reservationRepo.count(specFilteredReservationsForManager(filter, manager));
  }

  public long countAllEntriesUsingFilter(final ReservationFilterView filter) {
    return reservationRepo.count(specFilteredReservations(filter));
  }

  public long countForVirtualResourceGroup(VirtualResourceGroup vrg) {
    return reservationRepo.count(forVirtualResourceGroup(vrg));
  }

  public long countScheduledReservationForVirtualResourceGroup(VirtualResourceGroup vrg) {
    return countForVirtualResourceGroup(vrg, SCHEDULED);
  }

  public long countActiveReservationForVirtualResourceGroup(VirtualResourceGroup vrg) {
    return countForVirtualResourceGroup(vrg, RUNNING);
  }

  private long countForVirtualResourceGroup(VirtualResourceGroup vrg, ReservationStatus status) {
    Specification<Reservation> spec = Specifications.where(forVirtualResourceGroup(vrg)).and(forStatus(status));

    return reservationRepo.count(spec);
  }

  public List<Integer> findUniqueYearsFromReservations() {
    // FIXME Franky add UserDetails to query
    @SuppressWarnings("unchecked")
    List<Double> dbYears = entityManager.createNativeQuery(
        "select distinct extract(year from start_date_time) startYear "
            + "from reservation UNION select distinct extract(year from end_date_time) from reservation")
        .getResultList();

    ImmutableList<Integer> years = FluentIterable.from(dbYears).filter(Predicates.notNull())
        .transform(new Function<Double, Integer>() {
          @Override
          public Integer apply(Double d) {
            return d.intValue();
          }
        }).toImmutableList();

    return Ordering.natural().sortedCopy(years);
  }

  @VisibleForTesting
  Collection<ReservationArchive> transformToReservationArchives(final List<Reservation> reservations) {
    return Collections2.transform(reservations, TO_RESERVATION_ARCHIVE);
  }

  private List<Reservation> findReservationWithStatus(ReservationStatus... states) {
    return reservationRepo.findByStatusIn(Arrays.asList(states));
  }

  public long count() {
    return reservationRepo.count();
  }

  public void cancelAndArchiveReservations(final List<Reservation> reservations, RichUserDetails user) {
    for (final Reservation reservation : reservations) {
      if (isDeleteAllowedForUserOnly(reservation, user.getSelectedRole()).isAllowed()) {
        nbiClient.cancelReservation(reservation.getReservationId());
      }
    }

    Collection<ReservationArchive> reservationArchives = transformToReservationArchives(reservations);
    logEventService.logCreateEvent(Security.getUserDetails(), reservationArchives);
    reservationArchiveRepo.save(reservationArchives);

    logEventService.logDeleteEvent(Security.getUserDetails(), reservations);
    reservationRepo.delete(reservations);
  }

  public List<Reservation> findBySourcePortOrDestinationPort(VirtualPort virtualPortA, VirtualPort virtualPortB) {
    return reservationRepo.findBySourcePortOrDestinationPort(virtualPortA, virtualPortB);
  }

  public List<Reservation> findRunningReservations() {
    return findReservationWithStatus(ReservationStatus.RUNNING);
  }

  public Collection<Reservation> findActiveByPhysicalPort(final PhysicalPort port) {
    return reservationRepo.findAll(Specifications.where(specByPhysicalPort(port)).and(specActiveReservations()));
  }

  public long countActiveForPhysicalPort(final PhysicalPort port) {
    return reservationRepo.count(Specifications.where(specByPhysicalPort(port)).and(specActiveReservations()));
  }

  public long countForPhysicalPort(final PhysicalPort port) {
    return reservationRepo.count(specByPhysicalPort(port));
  }

  public boolean cancelWithReason(Reservation reservation, String cancelReason, RichUserDetails user) {
    return cancelWithReason(reservation, cancelReason, user, Optional.<NsiRequestDetails>absent());
  }

  public boolean cancelWithReason(
      Reservation reservation, String cancelReason, RichUserDetails user, Optional<NsiRequestDetails> requestDetails) {

    if (isDeleteAllowed(reservation, user.getSelectedRole()).isAllowed()) {
      reservationToNbi.terminate(reservation, cancelReason, requestDetails);

      return true;
    }
    else {
      log.info("Not allowed to cancel reservation {}", reservation.getName());
    }
    return false;
  }

  public Reservation findByReservationId(final String reservationId) {
    return reservationRepo.findByReservationId(reservationId);
  }

  @Override
  protected EntityManager getEntityManager() {
    return entityManager;
  }

  /**
   * Transforms the given reservations to {@link ReservationView}s and
   * determines if the reservation is allowed to be delete by the given user.
   *
   * @param reservationsToTransform
   *          {@link Reservation}s to be transformed
   * @param user
   *          {@link RichUserDetails} to check if this user is allowed to delete
   *          the reservation
   * @return {@link List<ReservationView>} transformed reservations
   */
  @Override
  public List<ReservationView> transformToView(List<Reservation> reservationsToTransform, final RichUserDetails user) {

    return Lists.transform(reservationsToTransform, new Function<Reservation, ReservationView>() {
      @Override
      public ReservationView apply(Reservation reservation) {
        return new ReservationView(reservation, isDeleteAllowed(reservation, user.getSelectedRole()));
      }
    });
  }
}
