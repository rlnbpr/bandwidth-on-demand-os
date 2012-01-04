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
package nl.surfnet.bod.domain.validator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.domain.VirtualPort;
import nl.surfnet.bod.domain.VirtualResourceGroup;
import nl.surfnet.bod.support.ReservationFactory;
import nl.surfnet.bod.support.RichUserDetailsFactory;
import nl.surfnet.bod.support.VirtualPortFactory;
import nl.surfnet.bod.support.VirtualResourceGroupFactory;
import nl.surfnet.bod.web.security.Security;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

public class ReservationValidatorTest {

  private ReservationValidator subject;

  @Before
  public void setUp() {
    subject = new ReservationValidator();
    Security.setUserDetails(new RichUserDetailsFactory().addUserGroup("urn:bandwidth-on-demand").create());
  }

  @Test
  public void shouldSupportReservationClass() {
    assertTrue(subject.supports(Reservation.class));
  }

  @Test
  public void shouldNotSupportObjectClass() {
    assertFalse(subject.supports(Object.class));
  }

  @Test
  public void endDateShouldNotBeBeforeStartDate() {
    LocalDate now = LocalDate.now();
    Reservation reservation = new ReservationFactory().setStartDate(now).setEndDate(now.minusDays(1)).create();
    Errors errors = createErrorObject(reservation);

    subject.validate(reservation, errors);

    assertTrue(errors.hasErrors());
    assertTrue(errors.hasFieldErrors("endDate"));
  }

  @Test
  public void endDateMayBeOnStartDate() {
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    LocalTime noon = new LocalTime(12, 0);

    Reservation reservation = new ReservationFactory().setStartDate(tomorrow).setEndDate(tomorrow).setStartTime(noon)
        .setEndTime(noon.plusMinutes(10)).create();
    Errors errors = createErrorObject(reservation);

    subject.validate(reservation, errors);

    assertFalse(errors.hasErrors());
  }

  @Test
  public void whenEndAndStartDateAreOnTheSameDayStartTimeShouldBeBeforeEndTime() {
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();
    Reservation reservation = new ReservationFactory().setStartDate(today).setEndDate(today).setStartTime(now)
        .setEndTime(now.minusMinutes(1)).create();
    Errors errors = createErrorObject(reservation);

    subject.validate(reservation, errors);

    assertTrue(errors.hasErrors());
    assertTrue(errors.hasFieldErrors("endTime"));
  }

  @Test
  public void whenEndAndStartDateAreOnTheSameDayStartTimeShouldBeBeforeEndTime2() {
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    LocalTime noon = new LocalTime(12, 0);
    Reservation reservation = new ReservationFactory().setStartDate(tomorrow).setEndDate(tomorrow).setStartTime(noon)
        .setEndTime(noon.plusHours(1)).create();
    Errors errors = createErrorObject(reservation);

    subject.validate(reservation, errors);

    assertFalse(errors.hasErrors());
  }

  @Test
  public void reservationShouldHaveDifferentPorts() {
    VirtualPort port = new VirtualPortFactory().create();
    Reservation reservation = new ReservationFactory().setSourcePort(port).setDestinationPort(port).create();
    Errors errors = createErrorObject(reservation);

    subject.validate(reservation, errors);

    assertTrue(errors.hasErrors());
    assertThat(errors.getGlobalError().getCode(), containsString("validation.reservation.sameports"));
  }

  @Test
  public void aReservationShouldNotBeInThePast() {
    LocalDate yesterday = LocalDate.now().minusDays(1);

    Reservation reservation = new ReservationFactory().setStartDate(yesterday).create();
    Errors errors = createErrorObject(reservation);

    subject.validate(reservation, errors);

    assertTrue(errors.hasErrors());
    assertTrue(errors.hasFieldErrors("startDate"));
  }

  @Test
  public void aReservationShouldNotBeInThePast2() {
    LocalDate today = LocalDate.now();
    LocalTime fewHoursAgo = LocalTime.now().minusHours(2);

    Reservation reservation = new ReservationFactory().setStartDate(today).setStartTime(fewHoursAgo).create();
    Errors errors = createErrorObject(reservation);

    subject.validate(reservation, errors);

    assertTrue(errors.hasFieldErrors("startTime"));
  }

  @Test
  public void aReservationShouldNotBeFurtherInTheFuterThanOneYear() {
    Reservation reservation = new ReservationFactory().setStartDate(LocalDate.now().plusYears(1).plusDays(1))
        .setEndDate(LocalDate.now().plusYears(1).plusDays(10)).create();
    Errors errors = createErrorObject(reservation);

    subject.validate(reservation, errors);

    assertTrue(errors.hasFieldErrors("startDate"));
  }

  @Test
  public void reservationShouldBeLongerThan5Minutes() {
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    LocalTime noon = new LocalTime(12, 0);
    Reservation reservation = new ReservationFactory().setStartDate(tomorrow).setEndDate(tomorrow).setStartTime(noon)
        .setEndTime(noon.plusMinutes(3)).create();
    Errors errors = createErrorObject(reservation);

    subject.validate(reservation, errors);

    assertThat(errors.getGlobalErrors(), hasSize(1));
    assertThat(errors.getGlobalError().getCode(), containsString("validation.reservation.duration.tooShort"));
  }

  @Test
  public void reservationShouldBeLongerThan5Minutes2() {
    LocalDate startDate = LocalDate.now().withMonthOfYear(12).withDayOfMonth(31);
    LocalDate endDate = startDate.plusDays(1);
    LocalTime startTime = new LocalTime().withHourOfDay(23).withMinuteOfHour(58);
    LocalTime endTime = new LocalTime().withHourOfDay(0).withMinuteOfHour(1);

    Reservation reservation = new ReservationFactory().setStartDate(startDate).setEndDate(endDate)
        .setStartTime(startTime).setEndTime(endTime).create();
    Errors errors = createErrorObject(reservation);

    subject.validate(reservation, errors);

    assertThat(errors.getGlobalErrors(), hasSize(1));
    assertThat(errors.getGlobalError().getCode(), containsString("validation.reservation.duration.tooShort"));
  }

  @Test
  public void aReservationShouldNotBeLongerThanOneYear() {
    LocalDate startDate = LocalDate.now().plusDays(5);
    LocalDate endDate = startDate.plusYears(1).plusDays(1);

    Reservation reservation = new ReservationFactory().setStartDate(startDate).setEndDate(endDate).create();
    Errors errors = createErrorObject(reservation);

    subject.validate(reservation, errors);

    assertThat(errors.getGlobalErrors(), hasSize(1));
    assertThat(errors.getGlobalError().getCode(), containsString("validation.reservation.duration.tooLong"));
  }

  @Test
  public void vritualGroupNameShouldBeSameAsOfThePorts() {
    VirtualResourceGroup vrg = new VirtualResourceGroupFactory().setSurfConextGroupName("urn:wronggroup").create();
    VirtualPort source = new VirtualPortFactory().setVirtualResourceGroup(vrg).create();

    Reservation reservation = new ReservationFactory().setSourcePort(source).create();
    Errors errors = createErrorObject(reservation);

    subject.validate(reservation, errors);

    assertThat(errors.getGlobalError().getCode(), containsString("validation.reservation.security"));
  }

  @Test
  public void userIsNotAMemberOfTheSurfConextGroup() {
    VirtualResourceGroup vrg = new VirtualResourceGroupFactory().setSurfConextGroupName("urn:wronggroup").create();
    VirtualPort sourcePort = new VirtualPortFactory().setVirtualResourceGroup(vrg).create();
    VirtualPort destPort = new VirtualPortFactory().setVirtualResourceGroup(vrg).create();

    Reservation reservation = new ReservationFactory().setVirtualResourceGroup(vrg).setSourcePort(sourcePort)
        .setDestinationPort(destPort).create();
    Errors errors = createErrorObject(reservation);

    subject.validate(reservation, errors);

    assertThat(errors.getGlobalError().getCode(), containsString("validation.reservation.security"));
  }

  private Errors createErrorObject(Reservation reservation) {
    return new BeanPropertyBindingResult(reservation, "reservation");
  }
}
