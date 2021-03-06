/**
 * Copyright (c) 2012, SURFnet BV
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *     disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided with the distribution.
 *   * Neither the name of the SURFnet BV nor the names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nl.surfnet.bod.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import nl.surfnet.bod.domain.PhysicalPort;
import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.domain.VirtualPort;
import nl.surfnet.bod.web.security.Security;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;

@Service
public class NocService {

  private Logger logger = org.slf4j.LoggerFactory.getLogger(NocService.class);

  @Resource
  private ReservationService reservationService;

  @Resource
  private VirtualPortService virtualPortService;

  @Resource
  private PhysicalPortService physicalPortService;

  @PersistenceContext
  private EntityManager entityManager;

  @Transactional
  public Collection<Reservation> movePort(PhysicalPort oldPort, PhysicalPort newPort) {
    Collection<VirtualPort> virtualPorts = virtualPortService.findAllForPhysicalPort(oldPort);
    Collection<Reservation> reservations = getActiveReservations(oldPort);

    logger.info("Move a port with {} reservations.", reservations.size());

    cancelReservationsAndWait(reservations);

    saveNewPort(oldPort, newPort);

    switchVirtualPortsToNewPort(newPort, virtualPorts);

    unallocateOldPort(oldPort);

    Collection<Reservation> newReservations = makeNewReserations(reservations);

    for (Reservation reservation : newReservations) {
      reservationService.create(reservation);
    }

    return newReservations;
  }

  private Collection<Reservation> makeNewReserations(Collection<Reservation> reservations) {
    return Collections2.transform(reservations, new Function<Reservation, Reservation>() {
      @Override
      public Reservation apply(Reservation oldRes) {
        Reservation newRes = new Reservation();
        newRes.setStartDateTime(oldRes.getStartDateTime());
        newRes.setEndDateTime(oldRes.getEndDateTime());
        newRes.setSourcePort(oldRes.getSourcePort());
        newRes.setDestinationPort(oldRes.getDestinationPort());
        newRes.setName(oldRes.getName());
        newRes.setBandwidth(oldRes.getBandwidth());
        newRes.setUserCreated(oldRes.getUserCreated());
        newRes.setProtectionType(oldRes.getProtectionType());

        return newRes;
      }
    });
  }

  private void unallocateOldPort(PhysicalPort oldPort) {
    physicalPortService.delete(oldPort);
  }

  private void switchVirtualPortsToNewPort(PhysicalPort newPort, Collection<VirtualPort> virtualPorts) {
    for (VirtualPort vPort : virtualPorts) {
      vPort.setPhysicalPort(newPort);
      virtualPortService.save(vPort);
    }
  }

  private void saveNewPort(PhysicalPort oldPort, PhysicalPort newPort) {
    newPort.setPhysicalResourceGroup(oldPort.getPhysicalResourceGroup());
    physicalPortService.save(newPort);
  }

  private void cancelReservationsAndWait(Collection<Reservation> reservations) {
    List<Optional<Future<Long>>> futures = new ArrayList<>();

    for (Reservation reservation : reservations) {
      futures.add(reservationService.cancelWithReason(reservation, "A physical port, which the reservation used was moved",
          Security.getUserDetails()));
    }

    for (Optional<Future<Long>> future : futures) {
      if (future.isPresent()) {
        try {
          // waiting for the cancel to complete
          future.get().get();
        }
        catch (InterruptedException | ExecutionException e) {
          logger.error("Failed to wait for a reservation to terminate:", e);
        }
      }
    }

    for (Reservation reservation : reservations) {
      // refresh the reservations, have been changed in different thread
      entityManager.refresh(reservation);
    }

  }

  private Collection<Reservation> getActiveReservations(PhysicalPort port) {
    return reservationService.findActiveByPhysicalPort(port);
  }
}
