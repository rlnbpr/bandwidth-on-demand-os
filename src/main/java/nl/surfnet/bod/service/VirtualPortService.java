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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static nl.surfnet.bod.service.VirtualPortPredicatesAndSpecifications.BY_GROUP_ID_IN_LAST_MONTH_SPEC;
import static nl.surfnet.bod.service.VirtualPortPredicatesAndSpecifications.byPhysicalPortSpec;
import static nl.surfnet.bod.service.VirtualPortPredicatesAndSpecifications.forManagerSpec;
import static nl.surfnet.bod.service.VirtualPortPredicatesAndSpecifications.forUserSpec;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.surfnet.bod.domain.*;
import nl.surfnet.bod.domain.VirtualPortRequestLink.RequestStatus;
import nl.surfnet.bod.nsi.ws.ConnectionServiceProviderConstants;
import nl.surfnet.bod.repo.VirtualPortRepo;
import nl.surfnet.bod.repo.VirtualPortRequestLinkRepo;
import nl.surfnet.bod.repo.VirtualResourceGroupRepo;
import nl.surfnet.bod.web.security.RichUserDetails;
import nl.surfnet.bod.web.security.Security;

import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

@Service
@Transactional
public class VirtualPortService {

  @Autowired
  private VirtualPortRepo virtualPortRepo;
  @Autowired
  private VirtualResourceGroupRepo virtualResourceGroupReppo;
  @Autowired
  private VirtualPortRequestLinkRepo virtualPortRequestLinkRepo;
  @Autowired
  private EmailSender emailSender;

  @Autowired
  private ReservationService reservationService;

  @Autowired
  private LogEventService logEventService;

  public long count() {
    return virtualPortRepo.count();
  }

  public long countForUser(RichUserDetails user) {
    if (user.getUserGroups().isEmpty()) {
      return 0;
    }
    return virtualPortRepo.count(forUserSpec(user));
  }

  public long countForManager(BodRole managerRole) {
    checkArgument(managerRole.isManagerRole());

    return virtualPortRepo.count(forManagerSpec(managerRole));
  }

  public long countForPhysicalPort(PhysicalPort physicalPort) {
    return virtualPortRepo.count(byPhysicalPortSpec(physicalPort));
  }

  public void delete(final VirtualPort virtualPort, RichUserDetails user) {
    final List<Reservation> reservations = reservationService.findBySourcePortOrDestinationPort(virtualPort,
        virtualPort);
    reservationService.cancelAndArchiveReservations(reservations, user);

    logEventService.logDeleteEvent(Security.getUserDetails(), virtualPort);

    virtualPort.getVirtualResourceGroup().removeVirtualPort(virtualPort);
    virtualPortRepo.delete(virtualPort);

    if (virtualPort.getVirtualResourceGroup().getVirtualPortCount() == 0) {
      virtualResourceGroupReppo.delete(virtualPort.getVirtualResourceGroup());
    }
  }

  public VirtualPort find(final Long id) {
    return virtualPortRepo.findOne(id);
  }

  public List<VirtualPort> findAll() {
    return virtualPortRepo.findAll();
  }

  public List<VirtualPort> findAllForUser(final RichUserDetails user) {
    checkNotNull(user);

    if (user.getUserGroups().isEmpty()) {
      return Collections.emptyList();
    }

    return virtualPortRepo.findAll(forUserSpec(user));
  }

  public List<VirtualPort> findEntries(final int firstResult, final int maxResults) {
    checkArgument(maxResults > 0);

    return virtualPortRepo.findAll(new PageRequest(firstResult / maxResults, maxResults)).getContent();
  }

  public List<VirtualPort> findEntriesForUser(final RichUserDetails user, final int firstResult, final int maxResults,
      Sort sort) {
    checkNotNull(user);

    if (user.getUserGroups().isEmpty()) {
      return Collections.emptyList();
    }

    return virtualPortRepo.findAll(forUserSpec(user), new PageRequest(firstResult / maxResults, maxResults, sort))
        .getContent();
  }

  public List<VirtualPort> findEntriesForManager(BodRole managerRole, int firstResult, int maxResults, Sort sort) {
    checkArgument(maxResults > 0);
    checkArgument(managerRole.isManagerRole());

    return virtualPortRepo.findAll(forManagerSpec(managerRole),
        new PageRequest(firstResult / maxResults, maxResults, sort)).getContent();
  }

  public VirtualPort findByManagerLabel(String label) {
    return virtualPortRepo.findByManagerLabel(label);
  }

  public VirtualPort findByUserLabel(String label) {
    return virtualPortRepo.findByUserLabel(label);
  }

  public void save(final VirtualPort virtualPort) {
    logEventService.logCreateEvent(Security.getUserDetails(), virtualPort);
    virtualPortRepo.save(virtualPort);
  }

  public VirtualPort update(final VirtualPort virtualPort) {
    logEventService.logUpdateEvent(Security.getUserDetails(), virtualPort);
    return virtualPortRepo.save(virtualPort);
  }

  public Collection<VirtualPort> findAllForPhysicalPort(PhysicalPort physicalPort) {
    checkNotNull(physicalPort);

    return virtualPortRepo.findAll(byPhysicalPortSpec(physicalPort));
  }

  public void requestNewVirtualPort(RichUserDetails user, VirtualResourceGroup vGroup, PhysicalResourceGroup pGroup,
      String userLabel, Integer minBandwidth, String message) {
    VirtualPortRequestLink link = new VirtualPortRequestLink();
    link.setVirtualResourceGroup(vGroup);
    link.setPhysicalResourceGroup(pGroup);
    link.setUserLabel(userLabel);
    link.setMinBandwidth(minBandwidth);
    link.setMessage(message);
    link.setRequestorEmail(user.getEmail());
    link.setRequestorName(user.getDisplayName());
    link.setRequestorUrn(user.getUsername());
    link.setRequestDateTime(LocalDateTime.now());

    logEventService.logCreateEvent(Security.getUserDetails(), link);
    virtualPortRequestLinkRepo.save(link);
    emailSender.sendVirtualPortRequestMail(user, link);
  }

  public Collection<VirtualPortRequestLink> findPendingRequests(PhysicalResourceGroup prg) {
    return virtualPortRequestLinkRepo.findByPhysicalResourceGroupAndStatus(prg, RequestStatus.PENDING);
  }

  public VirtualPortRequestLink findRequest(String uuid) {
    return virtualPortRequestLinkRepo.findByUuid(uuid);
  }

  public void requestLinkApproved(VirtualPortRequestLink link, VirtualPort port) {
    link.setStatus(RequestStatus.APPROVED);

    logEventService.logUpdateEvent(Security.getUserDetails(), link, "Approved request link");
    virtualPortRequestLinkRepo.save(link);

    emailSender.sendVirtualPortRequestApproveMail(link, port);
  }

  public VirtualPortRequestLink findRequest(Long id) {
    return virtualPortRequestLinkRepo.findOne(id);
  }

  public Collection<VirtualPortRequestLink> findRequestsForLastMonth(Collection<UserGroup> userGroups) {

    Collection<String> groups = Collections2.transform(userGroups, new Function<UserGroup, String>() {
      @Override
      public String apply(UserGroup group) {
        return group.getId();
      }
    });

    return virtualPortRequestLinkRepo.findAll(BY_GROUP_ID_IN_LAST_MONTH_SPEC(groups));
  }

  public void requestLinkDeclined(VirtualPortRequestLink link, String declineMessage) {
    link.setStatus(RequestStatus.DECLINED);

    logEventService.logUpdateEvent(Security.getUserDetails(), link, "Declined request link");
    virtualPortRequestLinkRepo.save(link);

    emailSender.sendVirtualPortRequestDeclineMail(link, declineMessage);
  }

  public VirtualPort findByNsiStpId(String sourceStpId) {
    Pattern pattern = Pattern.compile(ConnectionServiceProviderConstants.URN_STP + ":([0-9]+)");
    Matcher matcher = pattern.matcher(sourceStpId);

    if (!matcher.matches()) {
      return null;
    }

    Long id = Long.valueOf(matcher.group(1));

    return find(id);
  }
}
