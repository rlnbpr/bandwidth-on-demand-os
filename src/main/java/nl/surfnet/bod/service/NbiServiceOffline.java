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

import java.util.List;
import java.util.Random;

import javax.annotation.PostConstruct;

import nl.surfnet.bod.domain.PhysicalPort;
import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.domain.ReservationStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

class NbiServiceOffline implements NbiService {

  private static final Function<NbiPort, PhysicalPort> TRANSFORM_FUNCTION = new Function<NbiPort, PhysicalPort>() {
    @Override
    public PhysicalPort apply(NbiPort nbiPort) {
      PhysicalPort physicalPort = new PhysicalPort();
      physicalPort.setName("Mock_" + nbiPort.getName());
      physicalPort.setNetworkElementPk(nbiPort.getId());

      return physicalPort;
    }
  };

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final Random random = new Random();

  private final List<ReservationStatus> reservationStatuses = ImmutableList.copyOf(ReservationStatus.values());
  private final List<NbiPort> ports = Lists.newArrayList();


  public NbiServiceOffline() {
    ports.add(new NbiPort("Ut002A_OME01_ETH-1-1-4", "00-1B-25-2D-DA-65_ETH-1-1-4"));
    ports.add(new NbiPort("Ut002A_OME01_ETH-1-2-4", "00-1B-25-2D-DA-65_ETH-1-2-4"));
    ports.add(new NbiPort("ETH10G-1-13-1", "00-21-E1-D6-D6-70_ETH10G-1-13-1"));
    ports.add(new NbiPort("ETH10G-1-13-2", "00-21-E1-D6-D6-70_ETH10G-1-13-2"));
    ports.add(new NbiPort("ETH-1-13-4", "00-21-E1-D6-D5-DC_ETH-1-13-4"));
    ports.add(new NbiPort("ETH10G-1-13-2", "00-21-E1-D6-D5-DC_ETH10G-1-13-5"));
    ports.add(new NbiPort("ETH10G-1-5-1", "00-20-D8-DF-33-8B_ETH10G-1-5-1"));
    ports.add(new NbiPort("OME0039_OC12-1-12-1", "00-21-E1-D6-D6-70_OC12-1-12-1"));
    ports.add(new NbiPort("WAN-1-4-102", "00-20-D8-DF-33-86_WAN-1-4-102"));
    ports.add(new NbiPort("ETH-1-3-1", "00-21-E1-D6-D6-70_ETH-1-3-1"));
    ports.add(new NbiPort("ETH-1-1-1", "00-21-E1-D6-D5-DC_ETH-1-1-1"));
    ports.add(new NbiPort("ETH-1-2-3", "00-20-D8-DF-33-8B_ETH-1-2-3"));
    ports.add(new NbiPort("WAN-1-4-101", "00-20-D8-DF-33-86_WAN-1-4-101"));
    ports.add(new NbiPort("ETH-1-1-2", "00-21-E1-D6-D5-DC_ETH-1-1-2"));
    ports.add(new NbiPort("OME0039_OC12-1-12-2", "00-21-E1-D6-D6-70_OC12-1-12-2"));
    ports.add(new NbiPort("ETH-1-13-5", "00-21-E1-D6-D5-DC_ETH-1-13-5"));
    ports.add(new NbiPort("ETH10G-1-13-3", "00-21-E1-D6-D5-DC_ETH10G-1-13-3"));
    ports.add(new NbiPort("Asd001A_OME3T_ETH-1-1-1", "00-20-D8-DF-33-59_ETH-1-1-1"));
  }

  @SuppressWarnings("unused")
  @PostConstruct
  private void init() {
    log.info("USING OFFLINE NBI CLIENT!");
  }

  @Override
  public List<PhysicalPort> findAllPhysicalPorts() {
    return Lists.newArrayList(Lists.transform(ports, TRANSFORM_FUNCTION));
  }

  @Override
  public long getPhysicalPortsCount() {
    return ports.size();
  }

  @Override
  public String createReservation(Reservation reservation) {
    return "SCHEDULE-" + System.currentTimeMillis();
  }

  @Override
  public ReservationStatus getReservationStatus(String scheduleId) {
    return reservationStatuses.get(random.nextInt(reservationStatuses.size()));
  }

  @Override
  public void cancelReservation(String scheduleId) {
    // nothing todo..
  }

  @Override
  public void extendReservation(String scheduleId, int minutes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PhysicalPort findPhysicalPortByNetworkElementId(final String networkElementId) {
    Preconditions.checkNotNull(Strings.emptyToNull(networkElementId));

    return Iterables.getOnlyElement(
        Iterables.transform(
            Iterables.filter(ports, new Predicate<NbiPort>() {
              @Override
              public boolean apply(NbiPort nbiPort) {
                return nbiPort.getId().equals(networkElementId);
              }
            }),
            TRANSFORM_FUNCTION));
  }

  private static final class NbiPort {
    private final String name;
    private final String id;

    public NbiPort(String name, String id) {
      this.name = name;
      this.id = id;
    }

    public String getName() {
      return name;
    }
    public String getId() {
      return id;
    }
  }

}
