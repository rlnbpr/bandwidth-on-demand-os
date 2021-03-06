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

  @Test
  public void succeedShouldTerminate() {
    Optional<NsiRequestDetails> requestDetails = Optional.of(new NsiRequestDetails("http://localhost/reply", "123456789"));
    Connection connection = new ConnectionFactory().setCurrentState(ConnectionStateType.PROVISIONED).create();
    Reservation reservation = new ReservationFactory().setStatus(SUCCEEDED).setConnection(connection).create();
    ReservationStatusChangeEvent event = new ReservationStatusChangeEvent(RUNNING, reservation, requestDetails);

    when(reservationServiceMock.find(reservation.getId())).thenReturn(reservation);

    subject.onStatusChange(event);

    verify(connectionServiceRequesterMock).executionSucceeded(connection);
  }
}
