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

import java.util.List;

import javax.annotation.Resource;

import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.domain.ReservationStatus;
import nl.surfnet.bod.nbi.NbiOfflineClient;
import nl.surfnet.bod.repo.ReservationRepo;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static nl.surfnet.bod.domain.ReservationStatus.AUTO_START;
import static nl.surfnet.bod.domain.ReservationStatus.REQUESTED;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration(locations = { "/spring/appCtx.xml", "/spring/appCtx-jpa-integration.xml",
    "/spring/appCtx-nbi-client.xml", "/spring/appCtx-idd-client.xml" })
@Transactional
public class ReportReservationServiceDbTest {
  private final static long AMOUNT_OF_RESERVATIONS = 9;

  // override bod.properties to run test and bod server at the same time
  static {
    System.setProperty("snmp.host", "localhost/1622");
  }

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Resource
  private ReservationServiceDbTestHelper helper;

  @Resource
  private ReservationRepo reservationRepo;

  @Resource
  private NbiOfflineClient nbiClient;

  @Resource
  private ReservationService subject;

  private DateTime periodStart;
  private DateTime periodEnd;

  private Reservation reservationOnStartPeriod;
  private Reservation reservationOnEndPeriod;
  private Reservation reservationBeforeStartAndAfterEndPeriod;
  private Reservation reservationBeforeStartAndOnEndPeriod;
  private Reservation reservationAfterStartAndOnEndPeriod;
  private Reservation reservationAfterStartAndAfterEndPeriod;

  private Reservation reservationBeforePeriod;
  private Reservation reservationInPeriod;
  private Reservation reservationAfterPeriod;

  private List<String> adminGroups;

  @BeforeTransaction
  public void setUp() {
    periodStart = DateTime.now().plusDays(2).plusHours(1);
    periodEnd = periodStart.plusDays(1);

    logger.warn("Start of period [{}], end [{}]", periodStart, periodEnd);
    // Speed up setup time
    nbiClient.setShouldSleep(false);
    helper.cleanUp();

    // Five (5) reservations in reporting period
    reservationOnStartPeriod = createReservation(periodStart, periodEnd.plusDays(1), ReservationStatus.REQUESTED);
    reservationOnEndPeriod = createReservation(periodStart.plusHours(1), periodEnd, ReservationStatus.REQUESTED);
    reservationAfterStartAndOnEndPeriod = createReservation(periodStart.plusHours(1), periodEnd,
        ReservationStatus.REQUESTED);
    reservationAfterStartAndAfterEndPeriod = createReservation(periodStart.plusHours(1), periodEnd.plusDays(1),
        ReservationStatus.REQUESTED);
    reservationInPeriod = createReservation(periodStart.plusHours(1), periodEnd.minusHours(1),
        ReservationStatus.REQUESTED);

    // Two (2) reservations related to reporting period
    reservationBeforeStartAndAfterEndPeriod = createReservation(periodStart.minusHours(1), periodEnd.plusDays(1),
        ReservationStatus.REQUESTED);
    reservationBeforeStartAndOnEndPeriod = createReservation(periodStart.minusHours(1), periodEnd,
        ReservationStatus.REQUESTED);

    // Two (2) reservations not related to reporting period
    reservationBeforePeriod = createReservation(periodStart.minusDays(1), periodStart.minusHours(1),
        ReservationStatus.REQUESTED);
    reservationAfterPeriod = createReservation(periodEnd.plusHours(1), periodEnd.plusDays(1),
        ReservationStatus.REQUESTED);
  }

  @AfterTransaction
  public void teardown() {
    logger.warn("Teardown");
    helper.cleanUp();
  }

  @Test
  public void checkSetup() {
    long amountOfReservations = subject.count();

    assertThat(amountOfReservations, is(AMOUNT_OF_RESERVATIONS));
  }

  @Test
  public void shouldCountExistingStateInPeriod() {
    long count = subject.countReservationsBetweenWhichHadStateInAdminGroups(periodStart, periodEnd, adminGroups,
        ReservationStatus.AUTO_START);

    assertThat(count, is(5L));
  }

  @Test
  public void shouldCountExistingStateBeforePeriodOnCorner() {
    long count = subject.countReservationsBetweenWhichHadStateInAdminGroups(periodStart.minusDays(2), periodStart
        .minusHours(1), adminGroups, ReservationStatus.AUTO_START);

    assertThat(count, is(3L));
  }

  @Test
  public void shouldCountExistingStateBeforePeriod() {
    long count = subject.countReservationsBetweenWhichHadStateInAdminGroups(periodStart.minusDays(2), periodStart
        .minusHours(2), adminGroups, ReservationStatus.AUTO_START);

    assertThat(count, is(1L));
  }

  @Test
  public void shouldCountExistingStateAfterPeriod() {
    long count = subject.countReservationsBetweenWhichHadStateInAdminGroups(periodEnd.plusDays(2), periodEnd
        .plusDays(3), adminGroups, ReservationStatus.REQUESTED);

    assertThat(count, is(0L));
  }

  @Test
  public void shouldCountNonExistingStateInPeriod() {
    long count = subject.countReservationsBetweenWhichHadStateInAdminGroups(periodStart, periodEnd, adminGroups,
        ReservationStatus.RESERVED);

    assertThat(count, is(0L));
  }

  @Test
  public void shouldCountExsitingTransitionInPeriod() {
    long count = subject.countReservationsWhichHadStateTransitionBetweenInAdminGroups(periodStart, periodEnd,
        REQUESTED, AUTO_START, adminGroups);

    assertThat(count, is(5L));
  }

  @Test
  public void shouldCountNonExsitingTransitionInPeriod() {
    long count = subject.countReservationsWhichHadStateTransitionBetweenInAdminGroups(periodStart, periodEnd,
        REQUESTED, ReservationStatus.NOT_ACCEPTED, adminGroups);

    assertThat(count, is(0L));
  }

  @Test
  public void shouldFindActiveReservationsWithState() {
    long count = subject.countActiveReservationsBetweenWithState(periodStart, periodEnd, AUTO_START, adminGroups);

    assertThat(count, is(7L));

    subject.updateStatus(reservationInPeriod, ReservationStatus.SUCCEEDED);
    count = subject.countActiveReservationsBetweenWithState(periodStart, periodEnd, AUTO_START, adminGroups);

    assertThat("Should count one less because of state change", count, is(6L));

    count = subject.countActiveReservationsBetweenWithState(periodStart, periodEnd, AUTO_START, adminGroups);

    assertThat("Should be one more, because search for extra status", count, is(7L));
  }

  @Test
  public void shouldNotFindActiveReservationsBecauseOfState() {
    long count = subject.countActiveReservationsBetweenWithState(periodStart, periodEnd, REQUESTED, adminGroups);

    assertThat(count, is(0L));
  }

  @Test
  public void shouldNotFindActiveReservationsBecauseBeforePeriod() {
    long count = subject.countActiveReservationsBetweenWithState(periodStart.minusDays(3), periodStart.minusDays(2),
        AUTO_START, adminGroups);

    assertThat(count, is(0L));
  }

  private Reservation createReservation(DateTime start, DateTime end, ReservationStatus status) {
    // Make sure all events are created with the time related to the reservation
    DateTimeUtils.setCurrentMillisFixed(start.getMillis());
    try {
      Reservation reservation = helper.createReservation(start, end, status);
      return helper.createThroughService(reservation);
    }
    finally {
      DateTimeUtils.currentTimeMillis();
    }
  }
}