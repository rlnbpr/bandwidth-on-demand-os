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
package nl.surfnet.bod.web.view;

import nl.surfnet.bod.web.noc.LogEventController;
import nl.surfnet.bod.web.noc.PhysicalPortController;
import nl.surfnet.bod.web.noc.ReservationController;

public class NocStatisticsView {

  private final long elapsedReservationsAmount;
  private final long activeReservationsAmount;
  private final long comingReservationsAmount;
  private final long physicalPortsAmount;
  private final long unalignedPhysicalPortsAmount;

  public NocStatisticsView(long countPhysicalPorts, long countElapsedReservations, long countActiveReservations,
      long countComingReservations, long countUnalignedPhyscalPorts) {

    this.physicalPortsAmount = countPhysicalPorts;
    this.elapsedReservationsAmount = countElapsedReservations;
    this.activeReservationsAmount = countActiveReservations;
    this.comingReservationsAmount = countComingReservations;
    this.unalignedPhysicalPortsAmount = countUnalignedPhyscalPorts;
  }

  public long getElapsedReservationsAmount() {
    return elapsedReservationsAmount;
  }

  public long getActiveReservationsAmount() {
    return activeReservationsAmount;
  }

  public long getComingReservationsAmount() {
    return comingReservationsAmount;
  }

  public long getPhysicalPortsAmount() {
    return physicalPortsAmount;
  }

  public String getElapsedReservationsUrl() {
    return ReservationController.ELAPSED_URL;
  }

  public String getActiveReservationsUrl() {
    return ReservationController.ACTIVE_URL;
  }

  public String getComingReservationsUrl() {
    return ReservationController.COMING_URL;
  }

  public long getUnalignedPhysicalPortsAmount() {
    return unalignedPhysicalPortsAmount;
  }

  public String getUnalignedPhysicalPortsUrl() {
    return PhysicalPortController.PAGE_UNALIGNED_URL;
  }

  public String getPpsUrl() {
    return "noc/" + PhysicalPortController.PAGE_URL;
  }

  public String getLogEventsUrl() {
    return "noc/" + LogEventController.PAGE_URL;
  }
}
