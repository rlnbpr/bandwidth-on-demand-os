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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;

import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.domain.ReservationStatus;
import nl.surfnet.bod.domain.VirtualPort;
import nl.surfnet.bod.domain.VirtualResourceGroup;
import nl.surfnet.bod.repo.ReservationRepo;
import nl.surfnet.bod.support.ReservationFactory;
import nl.surfnet.bod.support.RichUserDetailsFactory;
import nl.surfnet.bod.support.VirtualPortFactory;
import nl.surfnet.bod.support.VirtualResourceGroupFactory;
import nl.surfnet.bod.web.security.RichUserDetails;
import nl.surfnet.bod.web.security.Security;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class ReservationServiceTest {

  @InjectMocks
  private ReservationService subject;

  @Mock
  private ReservationRepo reservationRepoMock;

  @Mock
  private ReservationPoller reservationPoller;

  @Mock
  private NbiService nbiPortService;

  @Test
  public void whenTheUserHasNoGroupsTheReservationsShouldBeEmpty() {
    RichUserDetails richUserDetailsWithoutGroups = new RichUserDetailsFactory().create();
    Security.setUserDetails(richUserDetailsWithoutGroups);

    Collection<Reservation> reservations = subject.findEntries(0, 20);

    assertThat(reservations, hasSize(0));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void findEntriesShouldFilterOnUserGroups() {
    RichUserDetails richUserDetailsWithGroups = new RichUserDetailsFactory().addUserGroup("urn:mygroup").create();
    Security.setUserDetails(richUserDetailsWithGroups);

    PageImpl<Reservation> pageResult = new PageImpl<Reservation>(Lists.newArrayList(new ReservationFactory().create()));
    when(reservationRepoMock.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageResult);

    Collection<Reservation> reservations = subject.findEntries(0, 20);

    assertThat(reservations, hasSize(1));
  }

  @Test
  public void whenTheUserHasNoGroupsCountShouldBeZero() {
    RichUserDetails richUserDetailsWithoutGroups = new RichUserDetailsFactory().create();
    Security.setUserDetails(richUserDetailsWithoutGroups);

    long count = subject.count();

    assertThat(count, is(0L));
  }

  @Test
  public void countShouldFilterOnUserGroups() {
    RichUserDetails richUserDetailsWithGroups = new RichUserDetailsFactory().addUserGroup("urn:mygroup").create();
    Security.setUserDetails(richUserDetailsWithGroups);

    when(reservationRepoMock.count(any(Specification.class))).thenReturn(5L);

    long count = subject.count();

    assertThat(count, is(5L));
  }

  @Test(expected = IllegalStateException.class)
  public void reserveDifferentVirtualResrouceGroupsShouldGiveAnIllegalStateException() {
    VirtualResourceGroup vrg1 = new VirtualResourceGroupFactory().create();
    VirtualResourceGroup vrg2 = new VirtualResourceGroupFactory().create();
    VirtualPort source = new VirtualPortFactory().setVirtualResourceGroup(vrg1).create();
    VirtualPort destination = new VirtualPortFactory().setVirtualResourceGroup(vrg2).create();

    Reservation reservation = new ReservationFactory().setVirtualResourceGroup(vrg1).setSourcePort(source)
        .setDestinationPort(destination).create();

    subject.reserve(reservation);
  }

  @Test
  public void reserveSameVirtualResrouceGroupsShouldBeFine() {
    VirtualResourceGroup vrg = new VirtualResourceGroupFactory().create();
    VirtualPort source = new VirtualPortFactory().setVirtualResourceGroup(vrg).create();
    VirtualPort destination = new VirtualPortFactory().setVirtualResourceGroup(vrg).create();

    Reservation reservation = new ReservationFactory().setVirtualResourceGroup(vrg).setSourcePort(source)
        .setDestinationPort(destination).create();

    final String reservationId = "SCHEDULE-" + System.currentTimeMillis();
    when(nbiPortService.createReservation((any(Reservation.class)))).thenReturn(reservationId);

    subject.reserve(reservation);

    assertThat(reservation.getReservationId(), is(reservationId));
    verify(reservationRepoMock).save(reservation);
  }

  @Test(expected = IllegalStateException.class)
  public void updatingDifferentVirtualResrouceGroupsShouldGiveAnIllegalStateException() {
    VirtualResourceGroup vrg1 = new VirtualResourceGroupFactory().create();
    VirtualResourceGroup vrg2 = new VirtualResourceGroupFactory().create();
    VirtualPort source = new VirtualPortFactory().setVirtualResourceGroup(vrg1).create();
    VirtualPort destination = new VirtualPortFactory().setVirtualResourceGroup(vrg2).create();

    Reservation reservation = new ReservationFactory().setVirtualResourceGroup(vrg1).setSourcePort(source)
        .setDestinationPort(destination).create();

    subject.update(reservation);
  }

  @Test
  public void udpateShouldSave() {
    Reservation reservation = new ReservationFactory().create();

    subject.update(reservation);

    verify(reservationRepoMock).save(reservation);
  }

  @Test
  public void cancelAReservationShouldChangeItsStatus() {
    Reservation reservation = new ReservationFactory().setStatus(ReservationStatus.SCHEDULED).create();
    subject.cancel(reservation);
    assertThat(reservation.getStatus(), is(ReservationStatus.CANCELLED));
    verify(reservationRepoMock).save(reservation);
  }

  @Test
  public void shouldCheckAllTransitionStateReservations() {
    Reservation reservationWithTransitionStateOne = new ReservationFactory().setStatus(ReservationStatus.PREPARING)
        .create();
    Reservation reservationWithTransitionStateTwo = new ReservationFactory().setStatus(ReservationStatus.PREPARING)
        .create();

    // Always return endState
    when(nbiPortService.getReservationStatus(any(String.class))).thenReturn(ReservationStatus.SUCCEEDED);

    when(reservationRepoMock.findByStatusIn(ReservationStatus.TRANSITION_STATES)).thenReturn(
        ImmutableList.of(reservationWithTransitionStateOne, reservationWithTransitionStateTwo));

    subject.checkAllReservationsForStatusUpdate();

    assertThat(reservationWithTransitionStateOne.getStatus(), is(ReservationStatus.SUCCEEDED));
    assertThat(reservationWithTransitionStateTwo.getStatus(), is(ReservationStatus.SUCCEEDED));
  }

  @Test
  public void cancelAFailedReservationShouldNotChangeItsStatus() {
    Reservation reservation = new ReservationFactory().setStatus(ReservationStatus.FAILED).create();
    subject.cancel(reservation);
    assertThat(reservation.getStatus(), is(ReservationStatus.FAILED));
    verifyZeroInteractions(reservationRepoMock);
  }

  @Test
  public void shouldBeEndstate() {
    Assert.assertTrue(subject.isEndState(ReservationStatus.CANCELLED));
    Assert.assertTrue(subject.isEndState(ReservationStatus.FAILED));
    Assert.assertTrue(subject.isEndState(ReservationStatus.SUCCEEDED));
  }

  @Test
  public void shouldNotBeTransitionState() {
    Assert.assertFalse(subject.isTransitionState(ReservationStatus.CANCELLED));
    Assert.assertFalse(subject.isTransitionState(ReservationStatus.FAILED));
    Assert.assertFalse(subject.isTransitionState(ReservationStatus.SUCCEEDED));
  }

  @Test
  public void shouldBeTransitionState() {

    Assert.assertTrue(subject.isTransitionState(ReservationStatus.PREPARING));
    Assert.assertTrue(subject.isTransitionState(ReservationStatus.RUNNING));
    Assert.assertTrue(subject.isTransitionState(ReservationStatus.SCHEDULED));
    Assert.assertTrue(subject.isTransitionState(ReservationStatus.SUBMITTED));
  }

  @Test
  public void shouldNotBeEndState() {
    Assert.assertFalse(subject.isEndState(ReservationStatus.PREPARING));
    Assert.assertFalse(subject.isEndState(ReservationStatus.RUNNING));
    Assert.assertFalse(subject.isEndState(ReservationStatus.SCHEDULED));
    Assert.assertFalse(subject.isEndState(ReservationStatus.SUBMITTED));
  }

}
