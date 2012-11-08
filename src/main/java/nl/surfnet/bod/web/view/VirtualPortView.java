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

import nl.surfnet.bod.domain.VirtualPort;

import com.google.common.base.Optional;

public class VirtualPortView {
  private final Long id;
  private final String managerLabel;
  private final Integer maxBandwidth;
  private final Integer vlanId;
  private final String virtualResourceGroup;
  private final String physicalResourceGroup;
  private final String physicalPort;
  private final String nmsPortId;
  private final String userLabel;
  private final Optional<Long> reservationCounter;
  private final String nsiStpId;

  public VirtualPortView(VirtualPort port) {
    this(port, Optional.<Long> absent());
  }

  public VirtualPortView(VirtualPort port, final Optional<Long> reservationCounter) {
    id = port.getId();
    managerLabel = port.getManagerLabel();
    userLabel = port.getUserLabel();
    maxBandwidth = port.getMaxBandwidth();
    vlanId = port.getVlanId();
    virtualResourceGroup = port.getVirtualResourceGroup().getName();
    physicalResourceGroup = port.getPhysicalResourceGroup().getName();
    physicalPort = port.getPhysicalPort().getManagerLabel();
    nmsPortId = port.getPhysicalPort().getNmsPortId();
    this.reservationCounter = reservationCounter;
    this.nsiStpId = port.getNsiStpId();
  }

  public String getManagerLabel() {
    return managerLabel;
  }

  public Integer getMaxBandwidth() {
    return maxBandwidth;
  }

  public Integer getVlanId() {
    return vlanId;
  }

  public String getVirtualResourceGroup() {
    return virtualResourceGroup;
  }

  public String getPhysicalResourceGroup() {
    return physicalResourceGroup;
  }

  public String getPhysicalPort() {
    return physicalPort;
  }

  public String getNmsPortId() {
    return nmsPortId;
  }

  public Long getId() {
    return id;
  }

  public String getUserLabel() {
    return userLabel;
  }

  public Long getReservationCounter() {
    return reservationCounter.orNull();
  }

  public String getNsiStpId() {
    return nsiStpId;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("VirtualPortView [");
    if (id != null) {
      builder.append("id=");
      builder.append(id);
      builder.append(", ");
    }
    if (managerLabel != null) {
      builder.append("managerLabel=");
      builder.append(managerLabel);
      builder.append(", ");
    }
    if (maxBandwidth != null) {
      builder.append("maxBandwidth=");
      builder.append(maxBandwidth);
      builder.append(", ");
    }
    if (vlanId != null) {
      builder.append("vlanId=");
      builder.append(vlanId);
      builder.append(", ");
    }
    if (virtualResourceGroup != null) {
      builder.append("virtualResourceGroup=");
      builder.append(virtualResourceGroup);
      builder.append(", ");
    }
    if (physicalResourceGroup != null) {
      builder.append("physicalResourceGroup=");
      builder.append(physicalResourceGroup);
      builder.append(", ");
    }
    if (physicalPort != null) {
      builder.append("physicalPort=");
      builder.append(physicalPort);
      builder.append(", ");
    }
    if (nmsPortId != null) {
      builder.append("nmsPortId=");
      builder.append(nmsPortId);
      builder.append(", ");
    }
    if (userLabel != null) {
      builder.append("userLabel=");
      builder.append(userLabel);
      builder.append(", ");
    }
    if (reservationCounter != null) {
      builder.append("reservationCounter=");
      builder.append(reservationCounter.orNull());
      builder.append(", ");
    }
    if (nsiStpId != null) {
      builder.append("nsiStpId=");
      builder.append(nsiStpId);
    }
    builder.append("]");
    return builder.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((managerLabel == null) ? 0 : managerLabel.hashCode());
    result = prime * result + ((maxBandwidth == null) ? 0 : maxBandwidth.hashCode());
    result = prime * result + ((nmsPortId == null) ? 0 : nmsPortId.hashCode());
    result = prime * result + ((nsiStpId == null) ? 0 : nsiStpId.hashCode());
    result = prime * result + ((physicalPort == null) ? 0 : physicalPort.hashCode());
    result = prime * result + ((physicalResourceGroup == null) ? 0 : physicalResourceGroup.hashCode());
    result = prime * result + ((reservationCounter == null) ? 0 : reservationCounter.hashCode());
    result = prime * result + ((userLabel == null) ? 0 : userLabel.hashCode());
    result = prime * result + ((virtualResourceGroup == null) ? 0 : virtualResourceGroup.hashCode());
    result = prime * result + ((vlanId == null) ? 0 : vlanId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    VirtualPortView other = (VirtualPortView) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    }
    else if (!id.equals(other.id)) {
      return false;
    }
    if (managerLabel == null) {
      if (other.managerLabel != null) {
        return false;
      }
    }
    else if (!managerLabel.equals(other.managerLabel)) {
      return false;
    }
    if (maxBandwidth == null) {
      if (other.maxBandwidth != null) {
        return false;
      }
    }
    else if (!maxBandwidth.equals(other.maxBandwidth)) {
      return false;
    }
    if (nmsPortId == null) {
      if (other.nmsPortId != null) {
        return false;
      }
    }
    else if (!nmsPortId.equals(other.nmsPortId)) {
      return false;
    }
    if (nsiStpId == null) {
      if (other.nsiStpId != null) {
        return false;
      }
    }
    else if (!nsiStpId.equals(other.nsiStpId)) {
      return false;
    }
    if (physicalPort == null) {
      if (other.physicalPort != null) {
        return false;
      }
    }
    else if (!physicalPort.equals(other.physicalPort)) {
      return false;
    }
    if (physicalResourceGroup == null) {
      if (other.physicalResourceGroup != null) {
        return false;
      }
    }
    else if (!physicalResourceGroup.equals(other.physicalResourceGroup)) {
      return false;
    }
    if (reservationCounter == null) {
      if (other.reservationCounter != null) {
        return false;
      }
    }
    else if (!reservationCounter.equals(other.reservationCounter)) {
      return false;
    }
    if (userLabel == null) {
      if (other.userLabel != null) {
        return false;
      }
    }
    else if (!userLabel.equals(other.userLabel)) {
      return false;
    }
    if (virtualResourceGroup == null) {
      if (other.virtualResourceGroup != null) {
        return false;
      }
    }
    else if (!virtualResourceGroup.equals(other.virtualResourceGroup)) {
      return false;
    }
    if (vlanId == null) {
      if (other.vlanId != null) {
        return false;
      }
    }
    else if (!vlanId.equals(other.vlanId)) {
      return false;
    }
    return true;
  }

}