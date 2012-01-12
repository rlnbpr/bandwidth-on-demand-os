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
package nl.surfnet.bod.opendrac;

import java.util.ArrayList;
import java.util.List;

import nl.surfnet.bod.domain.PhysicalPort;
import nl.surfnet.bod.domain.PhysicalResourceGroup;
import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.service.NbiPortService;

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
 * A wrapper 'service' around OpenDRAC's {@link NrbInterface}.
 * 
 * @author robert
 * 
 */
public class NbiService implements NbiPortService {

  private static final String DEFAULT_VLANID = "0";

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
      for (NetworkElementHolder holder : getNrbInterface().getAllNetworkElements(getLoginToken())) {
        final List<Facility> facilitiesPerNetworkElement = getNrbInterface().getFacilities(getLoginToken(),
            holder.getId());
        for (final Facility facility : facilitiesPerNetworkElement) {
          // System.out.println(facility);
          if ("UNI".equals(facility.getSigType())) {
            facilities.add(facility);
          }
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

  /**
   * 
   * @param scheduleId
   *          the id of the schedule of interest
   * @return {@link String} representation of an OpenDRAC schedule {@link State}
   *         . Possible values are {@link State#ABORTED}, {@link State#DONE},
   *         {@link State#IN_PROGRESS}, {@link State#SUBMITTED} (minus the
   *         "State." prefix) or <code>null</code> in case of an error.
   */
  public String getScheduleStatus(final String scheduleId) {
    try {
      return getNrbInterface().getTaskInfo(getLoginToken(), scheduleId).getState().name();
    }
    catch (Exception e) {
      log.error("Error: ", e);
      return null;
    }
  }

  /**
   * 
   * @param scheduleId
   */
  public void cancelSchedule(final String scheduleId) {
    try {
      getNrbInterface().cancelSchedule(getLoginToken(), scheduleId);
    }
    catch (Exception e) {
      log.error("Error: ", e);
    }
  }

  /**
   * 
   * @param scheduleId
   * @param minutes
   */
  public void extendSchedule(final String scheduleId, int minutes) {
    try {
      final DracService dracService = getNrbInterface().getCurrentlyActiveServiceByScheduleId(getLoginToken(),
          scheduleId);
      getNrbInterface().extendServiceTime(getLoginToken(), dracService, minutes);
    }
    catch (Exception e) {
      log.error("Error: ", e);
    }
  }

  @Override
  public List<PhysicalPort> findAll() {
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
  public long count() {
    return findAll().size();
  }

  @Override
  public PhysicalPort findByName(String name) {
    // TODO: There must be a better way
    final List<PhysicalPort> allPhysicalPorts = findAll();
    for (final PhysicalPort port : allPhysicalPorts) {
      if (port.getName().equals(name)) {
        return port;
      }
    }
    return null;
  }

  @Override
  public String createReservation(final Reservation reservation) {
    final Schedule schedule = new Schedule();
    final PathType pathType = new PathType();
    final UserType userType = new UserType();

    // User info
    userType.setUserId(reservation.getUser());
    userType.setBillingGroup(new UserGroupName(groupName));
    userType.setSourceEndpointUserGroup(groupName);
    userType.setTargetEndpointUserGroup(groupName);
    userType.setSourceEndpointResourceGroup(resourceGroupName);
    userType.setTargetEndpointResourceGroup(resourceGroupName);

    // End point information.
    final EndPointType sourceEndpoint = new EndPointType();
    final EndPointType destEndpoint = new EndPointType();
    sourceEndpoint.setName(reservation.getSourcePort().getName());
    destEndpoint.setName(reservation.getDestinationPort().getName());
    
    // Path info
    pathType.setRate(reservation.getBandwidth());
    // TODO 1+1 or no protection, vcat or ccat?
    pathType.setProtectionType(PathType.PROTECTION_TYPE.PATH1PLUS1);
    pathType.setVcatRoutingOption(true);
    pathType.setSourceEndPoint(sourceEndpoint);
    pathType.setTargetEndPoint(destEndpoint);

    // Schedule Info
    schedule.setActivationType(Schedule.ACTIVATION_TYPE.RESERVATION_AUTOMATIC);
    schedule.setName(reservation.getUser() + "-" + System.currentTimeMillis());
    final long start = reservation.getStartDateTime().toDate().getTime();
    final long end = reservation.getEndDateTime().toDate().getTime();
    schedule.setStartTime(start);
    schedule.setEndTime(end);
    schedule.setRecurring(false);
    schedule.setRate(reservation.getBandwidth());
    schedule.setDuration(end - start);
    schedule.setUserInfo(userType);
    schedule.setPath(pathType);

    try {
      return getNrbInterface().asyncCreateSchedule(getLoginToken(), schedule);
    }
    catch (Exception e) {
      log.error("Error: ", e);
      return null;
    }
  }

}