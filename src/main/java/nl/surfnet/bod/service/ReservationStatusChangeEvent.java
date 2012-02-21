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

import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.domain.ReservationStatus;

import com.google.common.base.Objects;

public class ReservationStatusChangeEvent {

  private ReservationStatus oldStatus;
  private Reservation reservation;

  public ReservationStatusChangeEvent(final ReservationStatus oldStatus, final Reservation reservation) {
    this.oldStatus = oldStatus;
    this.reservation = reservation;
  }

  public ReservationStatus getOldStatus() {
    return oldStatus;
  }

  public Reservation getReservation() {
    return reservation;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("reservationId", reservation.getId()).add("oldStatus", oldStatus)
        .add("newStatus", reservation.getStatus()).toString();
  }

}
