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
package nl.surfnet.bod.nsi.ws.v1sc;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.ogf.schemas.nsi._2011._10.connection.types.ConnectionStateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import nl.surfnet.bod.domain.Connection;
import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.nsi.ws.ConnectionServiceProvider;
import nl.surfnet.bod.repo.ReservationRepo;
import nl.surfnet.bod.service.ReservationEventPublisher;
import nl.surfnet.bod.service.ReservationListener;
import nl.surfnet.bod.service.ReservationStatusChangeEvent;

@Component
public class ConnectionServiceProviderListener implements ReservationListener {

  private final Logger logger = LoggerFactory.getLogger(ConnectionServiceProviderListener.class);

  @Resource
  private ReservationEventPublisher reservationEventPublisher;

  @Resource
  private ConnectionServiceProvider connectionServiceProvider;

  @Resource
  private ReservationRepo reservationRepo;

  @PostConstruct
  public void registerListener() {
    reservationEventPublisher.addListener(this);
  }

  @Override
  public void onStatusChange(ReservationStatusChangeEvent event) {
    if (!event.getNsiRequestDetails().isPresent()) {
      return;
    }
    logger.debug("Got a reservation status change event {}", event);

    Reservation reservation = event.getReservation();
    Connection connection = reservation.getConnection();

    switch (reservation.getStatus()) {
    case RESERVED:
      connectionServiceProvider.reserveConfirmed(connection, event.getNsiRequestDetails().get());
      break;
    case SCHEDULED:
      // no need to send back any confirms
      break;
    case FAILED:
      if (connection.getCurrentState() == ConnectionStateType.AUTO_PROVISION
        || connection.getCurrentState() == ConnectionStateType.SCHEDULED) {
        connectionServiceProvider.provisionFailed(connection, event.getNsiRequestDetails().get());
      }
      else if (connection.getCurrentState() == ConnectionStateType.RESERVING) {
        connectionServiceProvider.reserveFailed(connection, event.getNsiRequestDetails().get());
      }
      break;
    case RUNNING:
      connectionServiceProvider.provisionConfirmed(connection, event.getNsiRequestDetails().get());
      break;
    case CANCELLED:
      // FIXME [AvD] not called yet.. cancel is not async
      connectionServiceProvider.terminateConfirmed(connection, event.getNsiRequestDetails().get());
      break;
    default:
      logger.error("Unhandled status {} of reservation {}", reservation.getStatus(), event.getReservation());
    }

  }

}