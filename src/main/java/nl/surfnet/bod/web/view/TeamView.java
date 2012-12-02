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

import nl.surfnet.bod.domain.UserGroup;
import nl.surfnet.bod.domain.VirtualResourceGroup;

public class TeamView implements Comparable<TeamView> {
  private final Long id;
  private final String name;
  private final long numberOfPorts;
  private final long activeReservations;
  private final long comingReservations;
  private final long elapsedReservations;
  private final String surfconextGroupId;
  private final boolean existing;

  public TeamView(UserGroup group) {
    this.id = null;
    this.name = group.getName();
    this.surfconextGroupId = group.getId();
    this.numberOfPorts = 0;
    this.activeReservations = 0;
    this.comingReservations = 0;
    this.elapsedReservations = 0;
    this.existing = false;
  }

  public TeamView(VirtualResourceGroup group, long activeReservations, long comingReservations, long elapsedReservations) {
    this.id = group.getId();
    this.name = group.getName();
    this.numberOfPorts = group.getVirtualPortCount();
    this.surfconextGroupId = group.getAdminGroup();
    this.activeReservations = activeReservations;
    this.comingReservations = comingReservations;
    this.elapsedReservations = elapsedReservations;
    this.existing = true;
  }

  public String getName() {
    return name;
  }

  public long getNumberOfPorts() {
    return numberOfPorts;
  }

  public String getSurfconextGroupId() {
    return surfconextGroupId;
  }

  public boolean isExisting() {
    return existing;
  }

  public long getActiveReservations() {
    return activeReservations;
  }

  public long getComingReservations() {
    return comingReservations;
  }

  public long getElapsedReservations() {
    return elapsedReservations;
  }

  @Override
  public int compareTo(TeamView other) {
    if (this.equals(other)) {
      return 0;
    }
    else {
      return this.getName().compareTo(other.getName());
    }
  }

  public Long getId() {
    return id;
  }
}
