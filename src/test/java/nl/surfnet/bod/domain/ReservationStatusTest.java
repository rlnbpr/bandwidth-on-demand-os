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
package nl.surfnet.bod.domain;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ReservationStatusTest {

  @Test
  public void someStatesShouldBeTransitionStates() {
    assertThat(ReservationStatus.PREPARING.isTransitionState(), is(true));
    assertThat(ReservationStatus.RUNNING.isTransitionState(), is(true));

    assertThat(ReservationStatus.FAILED.isTransitionState(), is(false));
    assertThat(ReservationStatus.FAILED.isTransitionState(), is(false));
    assertThat(ReservationStatus.CANCELLED.isTransitionState(), is(false));
  }

  @Test
  public void someStatesShouldBeEndStates() {
    assertThat(ReservationStatus.FAILED.isEndState(), is(true));
    assertThat(ReservationStatus.NOT_EXCEPTED.isEndState(), is(true));
    assertThat(ReservationStatus.CANCELLED.isEndState(), is(true));

    assertThat(ReservationStatus.PREPARING.isEndState(), is(false));
    assertThat(ReservationStatus.RUNNING.isEndState(), is(false));
  }

  @Test
  public void forTheseStatesShouldDeletionBeAllowed() {
    assertThat(ReservationStatus.PREPARING.isDeleteAllowed(), is(true));
    assertThat(ReservationStatus.REQUESTED.isDeleteAllowed(), is(true));
    assertThat(ReservationStatus.RUNNING.isDeleteAllowed(), is(true));
    assertThat(ReservationStatus.SCHEDULED.isDeleteAllowed(), is(true));
  }

  @Test
  public void forTheseStateShouldDeletionNotBeAllowed() {
    assertThat(ReservationStatus.CANCELLED.isDeleteAllowed(), is(false));
    assertThat(ReservationStatus.FAILED.isDeleteAllowed(), is(false));
    assertThat(ReservationStatus.NOT_EXCEPTED.isDeleteAllowed(), is(false));
    assertThat(ReservationStatus.SUCCEEDED.isDeleteAllowed(), is(false));
  }

}
