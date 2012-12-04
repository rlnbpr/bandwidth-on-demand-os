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

import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;

import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.domain.ReservationStatus;
import nl.surfnet.bod.repo.ReservationRepo;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

@RunWith(SpringJUnit4ClassRunner.class)
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration(locations = { "/spring/appCtx.xml", "/spring/appCtx-jpa-integration.xml",
    "/spring/appCtx-nbi-client.xml", "/spring/appCtx-idd-client.xml" })
@Transactional
public class ReservationServiceDbTest {

  // override bod.properties to run test and bod server at the same time
  static {
    System.setProperty("snmp.host", "localhost/1622");
  }

  @Resource
  private ReservationService reservationService;

  @Resource
  private ReservationRepo reservationRepo;

  @Resource
  private ReservationServiceDbTestHelper helper;

  private final DateTime rightDateTime = DateTime.now().withTime(0, 0, 0, 0);
  private final DateTime beforeDateTime = rightDateTime.minusMinutes(1);

  private Reservation rightReservationOnStartTime;
  private Reservation rightReservationOnEndTime;

  @Before
  public void setUp() {
    rightReservationOnStartTime = createAndSaveReservation(rightDateTime, beforeDateTime, ReservationStatus.AUTO_START);
    rightReservationOnEndTime = createAndSaveReservation(beforeDateTime, rightDateTime, ReservationStatus.AUTO_START);
    createAndSaveReservation(beforeDateTime.minusMinutes(10), rightDateTime.plusHours(1), ReservationStatus.AUTO_START);
    createAndSaveReservation(rightDateTime, beforeDateTime, ReservationStatus.CANCELLED);
    createAndSaveReservation(rightDateTime, rightDateTime, ReservationStatus.CANCELLED);
    createAndSaveReservation(beforeDateTime, beforeDateTime, ReservationStatus.AUTO_START);
  }

  @AfterTransaction
  public void teardown() {
    System.err.println("Tear down");
    helper.cleanUp();
  }

  @Test
  public void shouldFindAll() {
    List<Reservation> allReservations = reservationRepo.findAll();

    assertThat(allReservations, hasSize(6));
  }

  @Test
  public void shouldFindScheduledReservations() {
    Collection<Reservation> reservations = reservationService.findReservationsToPoll(DateTime.now().withHourOfDay(1));

    assertThat(reservations, hasSize(4));
  }

  @Test
  public void shouldFindReservationsAfterMidnight() {
    DateTimeUtils.setCurrentMillisFixed(DateMidnight.now().getMillis());
    Collection<Reservation> reservations = reservationService.findReservationsToPoll(rightDateTime);

    assertThat(reservations, hasSize(4));
    assertThat(reservations, hasItems(rightReservationOnEndTime, rightReservationOnStartTime));
  }

  private Reservation createAndSaveReservation(DateTime start, DateTime end, ReservationStatus status) {
    Reservation reservation = helper.createReservation(start, end, status);
    return helper.saveReservation(reservation);
  }

}
