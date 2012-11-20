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
package nl.surfnet.bod.nsi.v1sc;

import static nl.surfnet.bod.domain.ReservationStatus.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ogf.schemas.nsi._2011._10.connection.types.ConnectionStateType.AUTO_PROVISION;
import static org.ogf.schemas.nsi._2011._10.connection.types.ConnectionStateType.RESERVED;
import static org.ogf.schemas.nsi._2011._10.connection.types.ConnectionStateType.TERMINATING;
import nl.surfnet.bod.domain.Connection;
import nl.surfnet.bod.domain.NsiRequestDetails;
import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.domain.ReservationStatus;
import nl.surfnet.bod.nsi.v1sc.ConnectionServiceProviderListener;
import nl.surfnet.bod.nsi.v1sc.ConnectionServiceRequesterCallback;
import nl.surfnet.bod.service.ReservationService;
import nl.surfnet.bod.service.ReservationStatusChangeEvent;
import nl.surfnet.bod.support.ConnectionFactory;
import nl.surfnet.bod.support.ReservationFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ogf.schemas.nsi._2011._10.connection.types.ConnectionStateType;

import com.google.common.base.Optional;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionServiceProviderListenerTest {

  @InjectMocks
  private ConnectionServiceProviderListener subject;

  @Mock
  private ConnectionServiceRequesterCallback connectionServiceRequesterMock;

  @Mock
  private ReservationService reservationServiceMock;

  @Test
  public void absentNsiRequestDetailsShouldDoNothing() {
    Reservation reservation = new ReservationFactory().create();

    when(reservationServiceMock.find(anyLong())).thenReturn(reservation);

    ReservationStatusChangeEvent event =
        new ReservationStatusChangeEvent(REQUESTED, reservation, Optional.<NsiRequestDetails>absent());

    subject.onStatusChange(event);
  }

  @Test
  public void reserveFailedWithReason() {
    String failedReason = "No available bandwidth";
    Optional<NsiRequestDetails> requestDetails = Optional.of(new NsiRequestDetails("http://localhost/reply", "123456789"));

    Connection connection = new ConnectionFactory().setCurrentState(ConnectionStateType.RESERVING).create();
    Reservation reservation = new ReservationFactory()
      .setStatus(NOT_ACCEPTED)
      .setConnection(connection)
      .setFailedReason(failedReason).create();
    ReservationStatusChangeEvent event = new ReservationStatusChangeEvent(REQUESTED, reservation, requestDetails);

    when(reservationServiceMock.find(reservation.getId())).thenReturn(reservation);

    subject.onStatusChange(event);

    verify(connectionServiceRequesterMock).reserveFailed(
        connection, requestDetails.get(), Optional.of(failedReason));
  }

  @Test
  public void terminateFailed() {
    Optional<NsiRequestDetails> requestDetails = Optional.of(new NsiRequestDetails("http://localhost/reply", "123456789"));
    Connection connection = new ConnectionFactory().setCurrentState(TERMINATING).create();
    Reservation reservation = new ReservationFactory().setStatus(FAILED).setConnection(connection).create();
    ReservationStatusChangeEvent event = new ReservationStatusChangeEvent(AUTO_START, reservation, requestDetails);

    when(reservationServiceMock.find(reservation.getId())).thenReturn(reservation);

    subject.onStatusChange(event);

    verify(connectionServiceRequesterMock).terminateFailed(connection, requestDetails);
  }

  @Test
  public void terminateSucceed() {
    Optional<NsiRequestDetails> requestDetails = Optional.of(new NsiRequestDetails("http://localhost/reply", "123456789"));
    Connection connection = new ConnectionFactory().setCurrentState(TERMINATING).create();
    Reservation reservation = new ReservationFactory().setStatus(CANCELLED).setConnection(connection).create();
    ReservationStatusChangeEvent event = new ReservationStatusChangeEvent(AUTO_START, reservation, requestDetails);

    when(reservationServiceMock.find(reservation.getId())).thenReturn(reservation);

    subject.onStatusChange(event);

    verify(connectionServiceRequesterMock).terminateConfirmed(connection, requestDetails);
  }

  @Test
  public void provisionSucceeded() {
    Optional<NsiRequestDetails> requestDetails = Optional.of(new NsiRequestDetails("http://localhost/reply", "123456789"));
    Connection connection = new ConnectionFactory().setCurrentState(RESERVED).create();
    Reservation reservation = new ReservationFactory().setStatus(AUTO_START).setConnection(connection).create();
    ReservationStatusChangeEvent event =
        new ReservationStatusChangeEvent(ReservationStatus.RESERVED, reservation, requestDetails);

    when(reservationServiceMock.find(reservation.getId())).thenReturn(reservation);

    subject.onStatusChange(event);

    verify(connectionServiceRequesterMock).provisionSucceeded(connection);
  }

  @Test
  public void reservationStarts() {
    Optional<NsiRequestDetails> requestDetails = Optional.of(new NsiRequestDetails("http://localhost/reply", "123456789"));
    Connection connection = new ConnectionFactory().setCurrentState(AUTO_PROVISION).create();
    Reservation reservation = new ReservationFactory().setStatus(RUNNING).setConnection(connection).create();
    ReservationStatusChangeEvent event =
        new ReservationStatusChangeEvent(AUTO_START, reservation, requestDetails);

    when(reservationServiceMock.find(reservation.getId())).thenReturn(reservation);

    subject.onStatusChange(event);

    verify(connectionServiceRequesterMock).provisionConfirmed(connection, requestDetails.get());
  }
}
