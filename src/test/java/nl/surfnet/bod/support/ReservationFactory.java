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
package nl.surfnet.bod.support;

import java.util.concurrent.atomic.AtomicLong;

import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.domain.ReservationStatus;
import nl.surfnet.bod.domain.VirtualPort;
import nl.surfnet.bod.domain.VirtualResourceGroup;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Months;
import org.joda.time.ReadableDuration;
import org.joda.time.ReadablePeriod;

import com.google.common.base.Strings;

public class ReservationFactory {

  private static final AtomicLong COUNTER = new AtomicLong();

  private static Long id = COUNTER.incrementAndGet();
  private Integer version;
  private VirtualResourceGroup vrGroup = new VirtualResourceGroupFactory().create();
  private ReservationStatus status = ReservationStatus.SCHEDULED;
  private VirtualPort sourcePort;
  private VirtualPort destinationPort;
  private LocalDate startDate = LocalDate.now();
  private LocalDate endDate = LocalDate.now().plusDays(1);
  private LocalTime startTime = new LocalTime(12, 0);
  private LocalTime endTime = new LocalTime(16, 0);
  private String userCreated = "urn:truusvisscher";
  private Integer bandwidth = 10000;
  private String reservationId = "9" + String.valueOf(id);
  private String surfConextGroupName;

  public Reservation create() {
    if (vrGroup != null) {
      sourcePort = sourcePort == null ? new VirtualPortFactory().setVirtualResourceGroup(vrGroup).create() : sourcePort;
      destinationPort = destinationPort == null ? new VirtualPortFactory().setVirtualResourceGroup(vrGroup).create()
          : destinationPort;
    }

    Reservation reservation = new Reservation();
    reservation.setId(id);
    reservation.setVersion(version);
    reservation.setStatus(status);
    reservation.setSourcePort(sourcePort);
    reservation.setDestinationPort(destinationPort);
    reservation.setVirtualResourceGroup(vrGroup);
    reservation.setStartDate(startDate);
    reservation.setStartTime(startTime);
    reservation.setEndDate(endDate);
    reservation.setEndTime(endTime);
    reservation.setUserCreated(userCreated);
    reservation.setBandwidth(bandwidth);
    reservation.setReservationId(reservationId);

    if (!Strings.isNullOrEmpty(surfConextGroupName)) {
      reservation.getVirtualResourceGroup().setSurfConextGroupName(surfConextGroupName);
    }

    return reservation;
  }

  public ReservationFactory setId(Long id) {
    this.id = id;
    return this;
  }

  public ReservationFactory setVersion(Integer version) {
    this.version = version;
    return this;
  }

  public ReservationFactory setVirtualResourceGroup(VirtualResourceGroup vRGroup) {
    this.vrGroup = vRGroup;
    return this;
  }

  public ReservationFactory setStatus(ReservationStatus status) {
    this.status = status;
    return this;
  }

  public ReservationFactory setSourcePort(VirtualPort sourcePort) {
    this.sourcePort = sourcePort;
    return this;
  }

  public ReservationFactory setDestinationPort(VirtualPort endPort) {
    this.destinationPort = endPort;
    return this;
  }

  public ReservationFactory setUserCreated(String user) {
    this.userCreated = user;
    return this;
  }

  public ReservationFactory setEndDate(LocalDate endDate) {
    this.endDate = endDate;
    return this;
  }

  public ReservationFactory setEndDateTime(LocalDateTime endDateTime) {
    this.endDate = endDateTime.toLocalDate();
    this.endTime = endDateTime.toLocalTime();
    return this;
  }

  public ReservationFactory setStartDateTime(LocalDateTime startDateTime) {
    this.startDate = startDateTime.toLocalDate();
    this.startTime = startDateTime.toLocalTime();
    return this;
  }

  public ReservationFactory setStartDate(LocalDate startDate) {
    this.startDate = startDate;
    return this;
  }

  public ReservationFactory setStartTime(LocalTime startTime) {
    this.startTime = startTime;
    return this;
  }

  public ReservationFactory setEndTime(LocalTime endTime) {
    this.endTime = endTime;
    return this;
  }

  public ReservationFactory setBandwidth(Integer bandwidth) {
    this.bandwidth = bandwidth;
    return this;
  }

  public ReservationFactory setReservationId(String reservationid) {
    this.reservationId = reservationid;
    return this;
  }

  public ReservationFactory setSurfConextGroupName(String surfConextGroupName) {
    this.surfConextGroupName = surfConextGroupName;
    return this;
  }

  public ReservationFactory setStartAndDuration(LocalDateTime start, ReadablePeriod period) {
    setStartDateTime(start);
    
    setEndDateTime(start.plus(period));

    return this;
  }

}
