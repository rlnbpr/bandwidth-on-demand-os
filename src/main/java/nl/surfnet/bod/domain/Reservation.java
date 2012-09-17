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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

/**
 * Entity which represents a Reservation for a specific connection between a
 * source and a destination point on a specific moment in time.
 * 
 */
@Entity
@Indexed
public class Reservation implements Loggable {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Version
  private Integer version;

  @Field(index = Index.YES, store = Store.YES)
  @Analyzer(definition = "customanalyzer")
  private String name;

  @IndexedEmbedded
  @ManyToOne
  private VirtualResourceGroup virtualResourceGroup;

  @Enumerated(EnumType.STRING)
  @Field(index = Index.YES, store = Store.YES)
  @Analyzer(definition = "customanalyzer")
  private ReservationStatus status = ReservationStatus.REQUESTED;

  @Field(index = Index.YES, store = Store.YES)
  @Analyzer(definition = "customanalyzer")
  private String failedReason;

  @Field(index = Index.YES, store = Store.YES)
  @Analyzer(definition = "customanalyzer")
  private String cancelReason;

  @NotNull
  @ManyToOne(optional = false)
  @IndexedEmbedded
  private VirtualPort sourcePort;

  @NotNull
  @ManyToOne(optional = false)
  @IndexedEmbedded
  private VirtualPort destinationPort;

  @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDateTime")
  private LocalDateTime startDateTime;

  @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDateTime")
  private LocalDateTime endDateTime;

  @Field(index = Index.YES, store = Store.YES)
  @Analyzer(definition = "customanalyzer")
  @Column(nullable = false)
  private String userCreated;

  @NotNull
  @Column(nullable = false)
  @Field(index = Index.YES, store = Store.YES)
  @Analyzer(definition = "customanalyzer")
  private Integer bandwidth;

  @Basic
  @Field(index = Index.YES, store = Store.YES)
  @Analyzer(definition = "customanalyzer")
  private String reservationId;

  @NotNull
  @Column(nullable = false)
  @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDateTime")
  private final LocalDateTime creationDateTime;

  @OneToOne(mappedBy = "reservation")
  @IndexedEmbedded
  private Connection connection;

  @Enumerated(EnumType.STRING)
  @Field(index = Index.YES, store = Store.YES)
  @Analyzer(definition = "customanalyzer")
  private ProtectionType protectionType = ProtectionType.PROTECTED;

  public Reservation() {
    creationDateTime = LocalDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public VirtualResourceGroup getVirtualResourceGroup() {
    return virtualResourceGroup;
  }

  public ReservationStatus getStatus() {
    return status;
  }

  public void setStatus(ReservationStatus reservationStatus) {
    this.status = reservationStatus;
  }

  public VirtualPort getSourcePort() {
    return sourcePort;
  }

  /**
   * Sets the {@link #sourcePort} and the {@link #virtualResourceGroup} related
   * to this port.
   * 
   * @param sourcePort
   *          The source port to set
   * @throws IllegalStateException
   *           When the {@link #virtualResourceGroup} is already set and is not
   *           equal to the one reference by the given port
   */
  public void setSourcePort(VirtualPort sourcePort) {
    this.sourcePort = sourcePort;

    if ((virtualResourceGroup != null) && (!virtualResourceGroup.equals(sourcePort.getVirtualResourceGroup()))) {
      throw new IllegalStateException(
          "Reservation contains a sourcePort and destinationPort from a different VirtualResourceGroup");
    }
    else {
      this.virtualResourceGroup = sourcePort.getVirtualResourceGroup();
    }
  }

  public VirtualPort getDestinationPort() {
    return destinationPort;
  }

  /**
   * Sets the {@link #destinationPort} and the {@link #virtualResourceGroup}
   * related to this port.
   * 
   * @param destinationPort
   *          The destinationPort port to set
   * @throws IllegalStateException
   *           When the {@link #virtualResourceGroup} is already set and is not
   *           equal to the one reference by the given port
   */
  public void setDestinationPort(VirtualPort destinationPort) {
    this.destinationPort = destinationPort;

    if ((virtualResourceGroup != null) && (!virtualResourceGroup.equals(destinationPort.getVirtualResourceGroup()))) {
      throw new IllegalStateException(
          "Reservation contains a sourcePort and destinationPort from a different VirtualResourceGroup");
    }
    else {
      this.virtualResourceGroup = destinationPort.getVirtualResourceGroup();
    }
  }

  /**
   * 
   * @return LocalTime the time part of the {@link #startDateTime}
   */
  public LocalTime getStartTime() {
    return startDateTime == null ? null : startDateTime.toLocalTime();
  }

  /**
   * Sets the time part of the {@link #startDateTime}
   * 
   * @param startTime
   */
  public void setStartTime(LocalTime startTime) {

    if (startTime == null) {
      startDateTime = null;
      return;
    }

    if (startDateTime == null) {
      startDateTime = new LocalDateTime(startTime);
    }
    else {
      startDateTime = startDateTime.withTime(startTime.getHourOfDay(), startTime.getMinuteOfHour(),
          startTime.getSecondOfMinute(), startTime.getMillisOfSecond());
    }
  }

  public String getUserCreated() {
    return userCreated;
  }

  public void setUserCreated(String user) {
    this.userCreated = user;
  }

  /**
   * 
   * @return LocalDate The date part of the {@link #getStartDateTime()}
   */
  public LocalDate getStartDate() {
    return startDateTime == null ? null : startDateTime.toLocalDate();
  }

  /**
   * Sets the date part of the {@link #endDateTime}
   * 
   * @param startDate
   */
  public void setStartDate(LocalDate startDate) {

    if (startDate == null) {
      startDateTime = null;
      return;
    }

    if (startDateTime == null) {
      startDateTime = new LocalDateTime(startDate.toDate());
    }
    else {
      startDateTime = startDateTime
          .withDate(startDate.getYear(), startDate.getMonthOfYear(), startDate.getDayOfMonth());
    }
  }

  /**
   * 
   * @return LocalDate the date part of the {@link #endDateTime}
   */
  public LocalDate getEndDate() {
    return endDateTime == null ? null : endDateTime.toLocalDate();
  }

  /**
   * Sets the date part of the {@link #endDateTime}
   * 
   * @param endDate
   */
  public void setEndDate(LocalDate endDate) {
    if (endDate == null) {
      endDateTime = null;
      return;
    }

    if (endDateTime == null) {
      this.endDateTime = new LocalDateTime(endDate.toDate());
    }
    else {
      endDateTime = endDateTime.withDate(endDate.getYear(), endDate.getMonthOfYear(), endDate.getDayOfMonth());
    }
  }

  /**
   * 
   * @return LocalTime The time part of the {@link #endDateTime}
   */
  public LocalTime getEndTime() {
    return endDateTime == null ? null : endDateTime.toLocalTime();
  }

  /**
   * Sets the time part of the {@link #endDateTime}
   * 
   * @param endTime
   */
  public void setEndTime(LocalTime endTime) {

    if (endTime == null) {
      endDateTime = null;
      return;
    }

    if (endDateTime == null) {
      this.endDateTime = new LocalDateTime(endTime);
    }
    else {
      endDateTime = endDateTime.withTime(endTime.getHourOfDay(), endTime.getMinuteOfHour(),
          endTime.getSecondOfMinute(), endTime.getMillisOfSecond());
    }
  }

  public LocalDateTime getEndDateTime() {
    return endDateTime;
  }

  public void setEndDateTime(LocalDateTime endDateTime) {
    this.endDateTime = endDateTime;
  }

  public LocalDateTime getStartDateTime() {
    return startDateTime;
  }

  public void setStartDateTime(LocalDateTime startDateTime) {
    this.startDateTime = startDateTime;
  }

  public Integer getBandwidth() {
    return bandwidth;
  }

  public void setBandwidth(Integer bandwidth) {
    this.bandwidth = bandwidth;
  }

  public String getReservationId() {
    return reservationId;
  }

  public void setReservationId(String reservationId) {
    this.reservationId = reservationId;
  }

  public LocalDateTime getCreationDateTime() {
    return creationDateTime;
  }

  @Override
  public String getAdminGroup() {
    return virtualResourceGroup.getAdminGroup();
  }

  @Override
  public String getLabel() {
    return getName();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Reservation [");
    if (id != null) {
      builder.append("id=");
      builder.append(id);
      builder.append(", ");
    }
    if (version != null) {
      builder.append("version=");
      builder.append(version);
      builder.append(", ");
    }
    if (name != null) {
      builder.append("name=");
      builder.append(name);
      builder.append(", ");
    }
    if (virtualResourceGroup != null) {
      builder.append("virtualResourceGroup=");
      builder.append(virtualResourceGroup.getName());
      builder.append(", ");
    }
    if (status != null) {
      builder.append("status=");
      builder.append(status);
      builder.append(", ");
    }
    if (failedReason != null) {
      builder.append("failedReason=");
      builder.append(failedReason);
      builder.append(", ");
    }
    if (cancelReason != null) {
      builder.append("cancelReason=");
      builder.append(cancelReason);
      builder.append(", ");
    }
    if (sourcePort != null) {
      builder.append("sourcePort=");
      builder.append(sourcePort);
      builder.append(", ");
    }
    if (destinationPort != null) {
      builder.append("destinationPort=");
      builder.append(destinationPort);
      builder.append(", ");
    }
    if (startDateTime != null) {
      builder.append("startDateTime=");
      builder.append(startDateTime);
      builder.append(", ");
    }
    if (endDateTime != null) {
      builder.append("endDateTime=");
      builder.append(endDateTime);
      builder.append(", ");
    }
    if (userCreated != null) {
      builder.append("userCreated=");
      builder.append(userCreated);
      builder.append(", ");
    }
    if (bandwidth != null) {
      builder.append("bandwidth=");
      builder.append(bandwidth);
      builder.append(", ");
    }
    if (reservationId != null) {
      builder.append("reservationId=");
      builder.append(reservationId);
      builder.append(", ");
    }
    if (creationDateTime != null) {
      builder.append("creationDateTime=");
      builder.append(creationDateTime);
    }

    if (connection != null) {
      builder.append("connection=");
      builder.append(connection.getLabel());
    }
    builder.append("]");
    return builder.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((bandwidth == null) ? 0 : bandwidth.hashCode());
    result = prime * result + ((cancelReason == null) ? 0 : cancelReason.hashCode());
    result = prime * result + ((connection == null) ? 0 : connection.hashCode());
    result = prime * result + ((creationDateTime == null) ? 0 : creationDateTime.hashCode());
    result = prime * result + ((destinationPort == null) ? 0 : destinationPort.hashCode());
    result = prime * result + ((endDateTime == null) ? 0 : endDateTime.hashCode());
    result = prime * result + ((failedReason == null) ? 0 : failedReason.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((protectionType == null) ? 0 : protectionType.hashCode());
    result = prime * result + ((reservationId == null) ? 0 : reservationId.hashCode());
    result = prime * result + ((sourcePort == null) ? 0 : sourcePort.hashCode());
    result = prime * result + ((startDateTime == null) ? 0 : startDateTime.hashCode());
    result = prime * result + ((status == null) ? 0 : status.hashCode());
    result = prime * result + ((userCreated == null) ? 0 : userCreated.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    result = prime * result + ((virtualResourceGroup == null) ? 0 : virtualResourceGroup.getLabel().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Reservation other = (Reservation) obj;
    if (bandwidth == null) {
      if (other.bandwidth != null)
        return false;
    }
    else if (!bandwidth.equals(other.bandwidth))
      return false;
    if (cancelReason == null) {
      if (other.cancelReason != null)
        return false;
    }
    else if (!cancelReason.equals(other.cancelReason))
      return false;
    if (connection == null) {
      if (other.connection != null)
        return false;
    }
    else if (!connection.equals(other.connection))
      return false;
    if (creationDateTime == null) {
      if (other.creationDateTime != null)
        return false;
    }
    else if (!creationDateTime.equals(other.creationDateTime))
      return false;
    if (destinationPort == null) {
      if (other.destinationPort != null)
        return false;
    }
    else if (!destinationPort.equals(other.destinationPort))
      return false;
    if (endDateTime == null) {
      if (other.endDateTime != null)
        return false;
    }
    else if (!endDateTime.equals(other.endDateTime))
      return false;
    if (failedReason == null) {
      if (other.failedReason != null)
        return false;
    }
    else if (!failedReason.equals(other.failedReason))
      return false;
    if (id == null) {
      if (other.id != null)
        return false;
    }
    else if (!id.equals(other.id))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    }
    else if (!name.equals(other.name))
      return false;
    if (protectionType != other.protectionType)
      return false;
    if (reservationId == null) {
      if (other.reservationId != null)
        return false;
    }
    else if (!reservationId.equals(other.reservationId))
      return false;
    if (sourcePort == null) {
      if (other.sourcePort != null)
        return false;
    }
    else if (!sourcePort.equals(other.sourcePort))
      return false;
    if (startDateTime == null) {
      if (other.startDateTime != null)
        return false;
    }
    else if (!startDateTime.equals(other.startDateTime))
      return false;
    if (status != other.status)
      return false;
    if (userCreated == null) {
      if (other.userCreated != null)
        return false;
    }
    else if (!userCreated.equals(other.userCreated))
      return false;
    if (version == null) {
      if (other.version != null)
        return false;
    }
    else if (!version.equals(other.version))
      return false;
    if (virtualResourceGroup == null) {
      if (other.virtualResourceGroup != null)
        return false;
    }
    else if (!virtualResourceGroup.getLabel().equals(other.virtualResourceGroup.getLabel()))
      return false;
    return true;
  }

  public String getFailedReason() {
    return failedReason;
  }

  public void setFailedReason(String failedReason) {
    this.failedReason = failedReason;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  // needed for nsi and integration tests
  public final void setVirtualResourceGroup(VirtualResourceGroup virtualResourceGroup) {
    this.virtualResourceGroup = virtualResourceGroup;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public void setCancelReason(String cancelReason) {
    this.cancelReason = cancelReason;
  }

  public Connection getConnection() {
    return connection;
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  /**
   * 
   * @return True if this reservation was made using NSI, false otherwise
   */
  public boolean isNSICreated() {
    return connection != null;
  }

  public ProtectionType getProtectionType() {
    return protectionType;
  }

  public void setProtectionType(ProtectionType protectionType) {
    this.protectionType = protectionType;
  }
}
