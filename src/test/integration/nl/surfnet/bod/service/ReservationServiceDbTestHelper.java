/**
 * Copyright (c) 2012, SURFnet BV
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *   * Neither the name of the SURFnet BV nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nl.surfnet.bod.service;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import nl.surfnet.bod.domain.Institute;
import nl.surfnet.bod.domain.PhysicalResourceGroup;
import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.domain.ReservationStatus;
import nl.surfnet.bod.repo.InstituteRepo;
import nl.surfnet.bod.repo.PhysicalPortRepo;
import nl.surfnet.bod.repo.PhysicalResourceGroupRepo;
import nl.surfnet.bod.repo.ReservationRepo;
import nl.surfnet.bod.repo.VirtualPortRepo;
import nl.surfnet.bod.repo.VirtualResourceGroupRepo;
import nl.surfnet.bod.support.ReservationFactory;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import static org.junit.Assert.fail;

@Service
public class ReservationServiceDbTestHelper {

  @Resource
  private ReservationService reservationService;
  @Resource
  private ReservationRepo reservationRepo;
  @Resource
  private VirtualResourceGroupRepo virtualResourceGroupRepo;
  @Resource
  private VirtualPortRepo virtualPortRepo;
  @Resource
  private PhysicalPortRepo physicalPortRepo;
  @Resource
  private InstituteRepo instituteRepo;
  @Resource
  private PhysicalResourceGroupRepo physicalResourceGroupRepo;
  @Resource
  private EntityManagerFactory entityManagerFactory;

  Reservation createReservation(DateTime startDateTime, DateTime endDateTime, ReservationStatus status) {
    Reservation reservation = new ReservationFactory().setStartDateTime(startDateTime).setEndDateTime(endDateTime)
        .setStatus(status).create();

    // Determine institute to use
    final Institute sourceInstitute = findInstituteToPreventUniqueKeyViolationInPhysicalResourceGroup();
    reservation.getSourcePort().getPhysicalResourceGroup().setInstitute(sourceInstitute);
    persistPhysicalResourceGroup(reservation.getSourcePort().getPhysicalResourceGroup());

    final Institute destinationInstitute = findInstituteToPreventUniqueKeyViolationInPhysicalResourceGroup();
    reservation.getDestinationPort().getPhysicalResourceGroup().setInstitute(destinationInstitute);
    persistPhysicalResourceGroup(reservation.getDestinationPort().getPhysicalResourceGroup());

    // Force save of vrg only once, since they all use the same reference
    reservation.getVirtualResourceGroup().setId(null);

    // Source port stuff
    reservation.getSourcePort().getPhysicalPort().setId(null);
    physicalPortRepo.save(reservation.getSourcePort().getPhysicalPort());

    virtualResourceGroupRepo.save(reservation.getSourcePort().getVirtualResourceGroup());

    reservation.getSourcePort().setId(null);
    virtualPortRepo.save(reservation.getSourcePort());

    // Destination port stuff
    reservation.getDestinationPort().getPhysicalPort().setId(null);
    physicalPortRepo.save(reservation.getDestinationPort().getPhysicalPort());

    virtualResourceGroupRepo.save(reservation.getDestinationPort().getVirtualResourceGroup());

    reservation.getDestinationPort().setId(null);
    virtualPortRepo.save(reservation.getDestinationPort());

    reservation.setId(null);
    return reservation;
  }
  
  Reservation saveReservation(Reservation reservation) {

    return reservationRepo.save(reservation);
  }

  Reservation createThroughService(Reservation reservation) {
    Future<Long> future = reservationService.create(reservation);
    Long reservationId = null;

    try {
      reservationId = future.get();
    }
    catch (InterruptedException | ExecutionException e) {
      fail(e.getMessage());
    }
    return reservationService.find(reservationId);
  }


  private Institute findInstituteToPreventUniqueKeyViolationInPhysicalResourceGroup(Long... instituteIdsInUse) {
    Iterator<Institute> instituteIterator = instituteRepo.findAll().iterator();

    Institute institute = null;
    do {
      institute = instituteIterator.next();
    }
    while (physicalResourceGroupRepo.findByInstituteId(institute.getId()) != null);
    return institute;
  }

  private PhysicalResourceGroup persistPhysicalResourceGroup(PhysicalResourceGroup group) {
    group.setId(null);
    return physicalResourceGroupRepo.save(group);
  }


  public void cleanUp() {
    EntityManager em = entityManagerFactory.createEntityManager();
    SQLQuery query = ((Session) em.getDelegate())
        .createSQLQuery("truncate physical_resource_group, virtual_resource_group, connection, log_event cascade;");
    query.executeUpdate();
  }
}