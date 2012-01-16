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

import static nl.surfnet.bod.domain.ReservationStatus.*;

import java.util.ArrayList;
import java.util.List;

import nl.surfnet.bod.domain.PhysicalPort;
import nl.surfnet.bod.domain.PhysicalResourceGroup;
import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.domain.ReservationStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.nortel.appcore.app.drac.common.security.policy.types.UserGroupName;
import com.nortel.appcore.app.drac.common.types.DracService;
import com.nortel.appcore.app.drac.common.types.EndPointType;
import com.nortel.appcore.app.drac.common.types.Facility;
import com.nortel.appcore.app.drac.common.types.NetworkElementHolder;
import com.nortel.appcore.app.drac.common.types.PathType;
import com.nortel.appcore.app.drac.common.types.Schedule;
import com.nortel.appcore.app.drac.common.types.State.SCHEDULE;
import com.nortel.appcore.app.drac.common.types.TaskType.State;
import com.nortel.appcore.app.drac.common.types.UserType;
import com.nortel.appcore.app.drac.common.utility.CryptoWrapper;
import com.nortel.appcore.app.drac.common.utility.CryptoWrapper.CryptedString;
import com.nortel.appcore.app.drac.security.ClientLoginType;
import com.nortel.appcore.app.drac.security.LoginToken;
import com.nortel.appcore.app.drac.server.nrb.NrbInterface;
import com.nortel.appcore.app.drac.server.requesthandler.RemoteConnectionProxy;
import com.nortel.appcore.app.drac.server.requesthandler.RequestHandlerException;

/**
 * A wrapper around OpenDRAC's {@link NrbInterface}. Everything is in one class
 * so that only this class is poisent with OpenDRAC related classes.
 * 
 * @author robert
 * 
 */
class NbiServiceOpenDrac implements NbiService {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private RemoteConnectionProxy nrbProxy;

  @Value("${nbi.user}")
  private String username;

  @Value("${nbi.password}")
  private String encryptedPassword;

  @Value("${nbi.url.primary}")
  private String primaryController;

  @Value("${nbi.url.secondary}")
  private String secondaryController;

  @Value("${nbi.group.name}")
  private String groupName;

  @Value("${nbi.resource.group.name}")
  private String resourceGroupName;

  /*
   * (non-Javadoc)
   * 
   * @see nl.surfnet.bod.nbi.NbiService#getScheduleStatus(java.lang.String)
   */
  @Override
  public ReservationStatus getReservationStatus(final String reservationId) {
    try {
      SCHEDULE status = null;
      try {
        status = getNrbInterface().getSchedule(getLoginToken(), reservationId).getStatus();
      }
      catch (Exception e) {
        log.info("No Schedule found for {} trying to retrieve state", reservationId);
      }

      // Translate OpenDRAC schedule status or state to BoD's reservation status
      if (status == null) {
        final State state = getNrbInterface().getTaskInfo(getLoginToken(), reservationId).getState();
        switch (state) {
        case SUBMITTED:
          return PENDING;

        case IN_PROGRESS:
          return ReservationStatus.IN_PROGRESS;

        case ABORTED:
          return CANCELLED;

        case DONE:
          // Creation of schedule is done, so the Reservation is pending
          return PENDING;

        default:
          log.warn("Unknow status: " + status);
        }
      }
      else {
        switch (status) {
        case CONFIRMATION_PENDING:
          return PENDING;

        case CONFIRMATION_TIMED_OUT:
          return FAILED;

        case CONFIRMATION_CANCELLED:
          return CANCELLED;

        case EXECUTION_PENDING:
          return PENDING;

        case EXECUTION_INPROGRESS:
          return ReservationStatus.IN_PROGRESS;

        case EXECUTION_SUCCEEDED:
          return SUCCEEDED;

          // An OpenDRAC service can be partially successful, a single schedule
          // not (thats a reservation in BoD context).
        case EXECUTION_PARTIALLY_SUCCEEDED:
          return FAILED;

        case EXECUTION_TIME_OUT:
          return TIMED_OUT;

        case EXECUTION_FAILED:
          return FAILED;

        case EXECUTION_PARTIALLY_CANCELLED:
          return CANCELLED;

        case EXECUTION_CANCELLED:
          return CANCELLED;

        default:
          log.warn("Unknow status: {}", status);
          break;
        }
      }
      return null;

    }
    catch (Exception e) {
      log.error("Error: ", e);
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see nl.surfnet.bod.nbi.NbiService#cancelSchedule(java.lang.String)
   */
  @Override
  public void cancelReservation(final String reservationId) {
    try {
      getNrbInterface().cancelSchedule(getLoginToken(), reservationId);
    }
    catch (Exception e) {
      log.error("Error: ", e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see nl.surfnet.bod.nbi.NbiService#extendSchedule(java.lang.String, int)
   */
  @Override
  public void extendReservation(final String reservationId, final int minutes) {
    try {
      final DracService dracService = getNrbInterface().getCurrentlyActiveServiceByScheduleId(getLoginToken(),
          reservationId);
      getNrbInterface().extendServiceTime(getLoginToken(), dracService, minutes);
    }
    catch (Exception e) {
      log.error("Error: ", e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see nl.surfnet.bod.nbi.NbiService#createReservation(nl.surfnet.bod.domain.
   * Reservation)
   */
  @Override
  public String createReservation(final Reservation reservation) {
    try {
      return getNrbInterface().asyncCreateSchedule(getLoginToken(), createSchedule(reservation));
    }
    catch (Exception e) {
      log.error("Error: ", e);
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see nl.surfnet.bod.nbi.NbiService#findAllPhysicalPorts()
   */
  @Override
  public List<PhysicalPort> findAllPhysicalPorts() {
    final List<PhysicalPort> ports = new ArrayList<PhysicalPort>();
    for (final Facility facility : getAllUniFacilities()) {
      final PhysicalPort port = new PhysicalPort();
      port.setDisplayName(facility.getAid());
      port.setName(facility.getTna());
      PhysicalResourceGroup physicalResourceGroup = new PhysicalResourceGroup();
      physicalResourceGroup.setAdminGroup(groupName);
      port.setPhysicalResourceGroup(physicalResourceGroup);
      port.setPhysicalResourceGroup(physicalResourceGroup);
      ports.add(port);
    }
    return ports;
  }

  @Override
  public long getPhysicalPortsCount() {
    return findAllPhysicalPorts().size();
  }

  @Override
  public PhysicalPort findPhysicalPortByName(final String name) {
    // TODO: There must be a better way...
    final List<PhysicalPort> allPhysicalPorts = findAllPhysicalPorts();
    for (final PhysicalPort port : allPhysicalPorts) {
      if (port.getName().equals(name)) {
        return port;
      }
    }
    return null;
  }

  private NrbInterface getNrbInterface() {
    if (nrbProxy == null) {
      System.setProperty("org.opendrac.controller.primary", primaryController);
      System.setProperty("org.opendrac.controller.secondary", secondaryController);
      nrbProxy = new RemoteConnectionProxy();
    }
    try {
      return nrbProxy.getNrbInterface();
    }
    catch (RequestHandlerException e) {
      log.error("Error: ", e);
      return null;
    }
  }

  private List<Facility> getAllUniFacilities() {
    try {
      final List<Facility> facilities = new ArrayList<Facility>();
      for (final NetworkElementHolder networkElement : getNrbInterface().getAllNetworkElements(getLoginToken())) {
        try {
          for (final Facility facility : getNrbInterface().getFacilities(getLoginToken(), networkElement.getId())) {
            if ("UNI".equals(facility.getSigType())) {
              facilities.add(facility);
            }
          }
        }
        catch (Exception e) {
          log.warn("Error retrieving failities for networkElement at: {}. {}", networkElement.getIp(), e.getMessage());
        }
      }
      return facilities;
    }
    catch (Exception e) {
      log.error("Error: ", e);
      return null;
    }
  }

  private LoginToken getLoginToken() {
    final String password = CryptoWrapper.INSTANCE.decrypt(new CryptedString(encryptedPassword));
    try {
      return getNrbInterface()
          .login(ClientLoginType.INTERNAL_LOGIN, username, password.toCharArray(), null, null, null);
    }
    catch (Exception e) {
      log.error("Error: ", e);
      return null;
    }
  }

  private Schedule createSchedule(final Reservation reservation) {
    final PathType pathType = createPath(reservation);
    final UserType userType = createUser(reservation);
    final Schedule schedule = new Schedule();
    schedule.setActivationType(Schedule.ACTIVATION_TYPE.RESERVATION_AUTOMATIC);
    schedule.setName(reservation.getUserCreated() + "-" + System.currentTimeMillis());
    final long start = reservation.getStartDateTime().toDate().getTime();
    final long end = reservation.getEndDateTime().toDate().getTime();
    schedule.setStartTime(start);
    schedule.setEndTime(end);
    schedule.setRecurring(false);
    schedule.setRate(reservation.getBandwidth());
    schedule.setDuration(end - start);
    schedule.setUserInfo(userType);
    schedule.setPath(pathType);
    return schedule;
  }

  private PathType createPath(final Reservation reservation) {
    final EndPointType sourceEndpoint = new EndPointType();
    final EndPointType destEndpoint = new EndPointType();
    sourceEndpoint.setName(reservation.getSourcePort().getPhysicalPort().getName());
    destEndpoint.setName(reservation.getDestinationPort().getPhysicalPort().getName());

    final PathType pathType = new PathType();
    pathType.setRate(reservation.getBandwidth());

    // TODO 1+1 or no protection, vcat or ccat?
    pathType.setProtectionType(PathType.PROTECTION_TYPE.PATH1PLUS1);
    pathType.setVcatRoutingOption(true);

    pathType.setSourceEndPoint(sourceEndpoint);
    pathType.setTargetEndPoint(destEndpoint);
    return pathType;
  }

  private UserType createUser(final Reservation reservation) {
    final UserType userType = new UserType();
    userType.setUserId(reservation.getUserCreated());
    userType.setBillingGroup(new UserGroupName(groupName));
    userType.setSourceEndpointUserGroup(groupName);
    userType.setTargetEndpointUserGroup(groupName);
    userType.setSourceEndpointResourceGroup(resourceGroupName);
    userType.setTargetEndpointResourceGroup(resourceGroupName);
    return userType;
  }

}