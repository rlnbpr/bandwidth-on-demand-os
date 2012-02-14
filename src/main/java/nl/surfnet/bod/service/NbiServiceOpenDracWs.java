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

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import nl.surfnet.bod.domain.PhysicalPort;
import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.domain.ReservationStatus;
import nl.surfnet.bod.domain.VirtualPort;

import org.joda.time.Minutes;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0_xsd.Security;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0_xsd.SecurityDocument;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0_xsd.UsernameToken;
import org.opendrac.www.ws.networkmonitoringservice.v3_0.NetworkMonitoringServiceFault;
import org.opendrac.www.ws.networkmonitoringservice.v3_0.NetworkMonitoringService_v30Stub;
import org.opendrac.www.ws.networkmonitoringservicetypes_v3_0.*;
import org.opendrac.www.ws.networkmonitoringservicetypes_v3_0.QueryEndpointRequestDocument.QueryEndpointRequest;
import org.opendrac.www.ws.networkmonitoringservicetypes_v3_0.QueryEndpointsRequestDocument.QueryEndpointsRequest;
import org.opendrac.www.ws.resourceallocationandschedulingservice.v3_0.ResourceAllocationAndSchedulingServiceFault;
import org.opendrac.www.ws.resourceallocationandschedulingservice.v3_0.ResourceAllocationAndSchedulingService_v30Stub;
import org.opendrac.www.ws.resourceallocationandschedulingservicetypes_v3_0.*;
import org.opendrac.www.ws.resourceallocationandschedulingservicetypes_v3_0.CancelReservationScheduleRequestDocument.CancelReservationScheduleRequest;
import org.opendrac.www.ws.resourceallocationandschedulingservicetypes_v3_0.CreateReservationScheduleRequestDocument.CreateReservationScheduleRequest;
import org.opendrac.www.ws.resourceallocationandschedulingservicetypes_v3_0.ExtendCurrentServiceForScheduleRequestDocument.ExtendCurrentServiceForScheduleRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nortel.appcore.app.drac.common.utility.CryptoWrapper;
import com.nortel.appcore.app.drac.common.utility.CryptoWrapper.CryptedString;
import com.nortel.www.drac._2007._07._03.ws.ct.draccommontypes.CompletionResponseDocument;
import com.nortel.www.drac._2007._07._03.ws.ct.draccommontypes.ValidLayerT;

/**
 * A bridge to OpenDRAC's web services. Everything is contained in this one
 * class so that only this class is linked to OpenDRAC related classes.
 *
 * @author robert
 *
 */
class NbiServiceOpenDracWs implements NbiService {

  public static final String ROUTING_ALGORITHM = "VCAT";
  public static final String DEFAULT_VID = "Untagged";
  public static final ValidProtectionTypeT.Enum DEFAULT_PROTECTIONTYPE = ValidProtectionTypeT.X_1_PLUS_1_PATH;

  private final Logger log = LoggerFactory.getLogger(getClass());

  private NetworkMonitoringService_v30Stub networkingService;
  private ResourceAllocationAndSchedulingService_v30Stub schedulingService;
  private SecurityDocument securityDocument;

  private final Map<String, String> idToTnaCache = Maps.newHashMap();

  @Value("${nbi.billing.group.name}")
  private String billingGroupName;

  @Value("${nbi.password}")
  private String encryptedPassword;

  @Value("${nbi.group.name}")
  private String groupName;

  @Value("${nbi.resource.group.name}")
  private String resourceGroupName;

  @Value("${nbi.user}")
  private String username;

  @Value("${nbi.service.inventory}")
  private String inventoryServiceUrl;

  @Value("${nbi.service.scheduling}")
  private String schedulingServiceUrl;

  @SuppressWarnings("unused")
  @PostConstruct
  private void init() {
    try {
      schedulingService = new ResourceAllocationAndSchedulingService_v30Stub(schedulingServiceUrl);
      networkingService = new NetworkMonitoringService_v30Stub(inventoryServiceUrl);
      getSecurityDocument();
    }
    catch (IOException e) {
      log.error("Error: ", e);
    }
  }

  @Override
  public void cancelReservation(final String reservationId) {
    final CancelReservationScheduleRequestDocument requestDocument = CancelReservationScheduleRequestDocument.Factory
        .newInstance();
    final CancelReservationScheduleRequest request = requestDocument.addNewCancelReservationScheduleRequest();
    request.setReservationScheduleId(reservationId);
    try {
      final CompletionResponseDocument response = schedulingService.cancelReservationSchedule(requestDocument,
          getSecurityDocument());
      log.info("Status: {}", response.getCompletionResponse().getResult());
    }
    catch (Exception e) {
      log.error("Error: ", e);
    }
  }

  @Override
  public Reservation createReservation(final Reservation reservation) {
    try {
      CreateReservationScheduleResponseDocument responseDocument = schedulingService.createReservationSchedule(
          createSchedule(reservation), getSecurityDocument());

      log.debug("Response: {}", responseDocument.getCreateReservationScheduleResponse());

      String reservationId = responseDocument.getCreateReservationScheduleResponse().getReservationScheduleId();
      ReservationStatus status = OpenDracStatusTranslator.translate(responseDocument
          .getCreateReservationScheduleResponse().getResult());

      if (status == FAILED) {
        String failedReason = responseDocument.getCreateReservationScheduleResponse().getOccurrenceInfoArray()[0]
            .getReason();
        log.info(String.format("Reservation '%s' failed '%s'", reservationId, failedReason));
      }

      reservation.setReservationId(reservationId);
      reservation.setStatus(status);
    }
    catch (RemoteException e) {
      throw new RuntimeException(e);
    }
    catch (ResourceAllocationAndSchedulingServiceFault e) {
      log.warn("Creating a reservation failed", e);
      reservation.setStatus(FAILED);
    }

    return reservation;
  }

  @Override
  public void extendReservation(final String reservationId, final int minutes) {
    final ExtendCurrentServiceForScheduleT extensionDefinition = ExtendCurrentServiceForScheduleT.Factory.newInstance();
    extensionDefinition.setScheduleId(reservationId);
    extensionDefinition.setMinutesToExtend(minutes);

    final ExtendCurrentServiceForScheduleRequestDocument requestDocument = ExtendCurrentServiceForScheduleRequestDocument.Factory
        .newInstance();

    final ExtendCurrentServiceForScheduleRequest request = requestDocument
        .addNewExtendCurrentServiceForScheduleRequest();
    request.setExtensionDefinition(extensionDefinition);
    try {
      schedulingService.extendCurrentServiceForSchedule(requestDocument, getSecurityDocument());
    }
    catch (Exception e) {
      log.error("Error: ", e);
    }
  }

  @Override
  public List<PhysicalPort> findAllPhysicalPorts() {
    final List<PhysicalPort> ports = Lists.newArrayList();
    for (final EndpointT endpoint : findAllEndPoints()) {
      ports.add(getPhysicalPort(endpoint));
    }

    return ports;
  }

  public PhysicalPort findPhysicalPortByName(final String name) {
    try {
      return getPhysicalPort(findEndpointByTna(name));
    }
    catch (Exception e) {
      log.error("Error: ", e);
      return null;
    }
  }

  @Override
  public PhysicalPort findPhysicalPortByNetworkElementId(final String networkElementId) {
    EndpointT endpoint = findEndPointById(networkElementId);

    return getPhysicalPort(endpoint);
  }

  /*
   * Find by id is a little more complex, because the webservice only supports a
   * lookup by tna and not by id. In BoD the tna is locally stored as the
   * nocLabel field. This field can be edited. The tna can also be edited in
   * OpenDRAC. So the only save thing todo is use the id. The service keeps a
   * chache of id to tna.
   */
  private EndpointT findEndPointById(String id) {
    String tna = idToTnaCache.get(id);

    if (tna == null) {
      List<EndpointT> endPoints = findAllEndPoints();
      for (EndpointT endPoint : endPoints) {
        if (endPoint.getId().equals(id)) {
          return endPoint;
        }
      }

      throw new IllegalStateException("Could not find endPoint for id " + id);
    }
    else {
      EndpointT endPoint;
      try {
        endPoint = findEndpointByTna(tna);

        if (endPoint.getId().equals(id)) {
          return endPoint;
        }
        else {
          idToTnaCache.remove(id);
          return findEndPointById(id);
        }
      }
      catch (Exception e) {
        return null;
      }
    }
  }

  @Override
  public long getPhysicalPortsCount() {
    return findAllPhysicalPorts().size();
  }

  @Override
  public ReservationStatus getReservationStatus(final String reservationId) {
    final QueryReservationScheduleRequestDocument requestDocument = QueryReservationScheduleRequestDocument.Factory
        .newInstance();
    requestDocument.addNewQueryReservationScheduleRequest();
    requestDocument.getQueryReservationScheduleRequest().setReservationScheduleId(reservationId);

    QueryReservationScheduleResponseDocument responseDocument = null;
    try {
      responseDocument = schedulingService.queryReservationSchedule(requestDocument, getSecurityDocument());
    }
    catch (Exception e) {
      log.error("Error: ", e);
    }

    if (responseDocument != null && responseDocument.getQueryReservationScheduleResponse().getIsFound()) {
      final ReservationScheduleT schedule = responseDocument.getQueryReservationScheduleResponse()
          .getReservationSchedule();
      final ValidReservationScheduleStatusT.Enum status = schedule.getStatus();

      return OpenDracStatusTranslator.translate(status);
    }
    else {
      log.info("No reservation found for reservation id: {}, returning FAILED", reservationId);
      return FAILED;
    }

  }

  private static final class OpenDracStatusTranslator {
    private static Logger logger = LoggerFactory.getLogger(OpenDracStatusTranslator.class);

    private OpenDracStatusTranslator() {
    }

    public static ReservationStatus translate(ValidReservationScheduleCreationResultT.Enum status) {
      if (status == ValidReservationScheduleCreationResultT.FAILED) {
        return FAILED;
      }
      else if (status == ValidReservationScheduleCreationResultT.SUCCEEDED) {
        return SUBMITTED;
      }
      else if (status == ValidReservationScheduleCreationResultT.SUCCEEDED_PARTIALLY) {
        return SUBMITTED;
      }
      else if (status == ValidReservationScheduleCreationResultT.UNKNOWN) {
        return FAILED;
      }
      else {
        logger.error("Could not translate status: " + status);
        throw new RuntimeException("Could not translate status: " + status);
      }
    }

    public static ReservationStatus translate(ValidReservationScheduleStatusT.Enum status) {
      if (status == ValidReservationScheduleStatusT.CONFIRMATION_PENDING) {
        return PREPARING;
      }
      else if (status == ValidReservationScheduleStatusT.CONFIRMATION_TIMED_OUT) {
        return FAILED;
      }
      else if (status == ValidReservationScheduleStatusT.CONFIRMATION_CANCELLED) {
        return CANCELLED;
      }
      else if (status == ValidReservationScheduleStatusT.EXECUTION_PENDING) {
        return SCHEDULED;
      }
      else if (status == ValidReservationScheduleStatusT.EXECUTION_IN_PROGRESS) {
        return RUNNING;
      }
      else if (status == ValidReservationScheduleStatusT.EXECUTION_SUCCEEDED) {
        return SUCCEEDED;
      }
      else if (status == ValidReservationScheduleStatusT.EXECUTION_PARTIALLY_SUCCEEDED) {
        // An OpenDRAC service can be partially successful, a single schedule
        // not (thats a reservation in BoD context).
        return FAILED;
      }
      else if (status == ValidReservationScheduleStatusT.EXECUTION_TIMED_OUT) {
        return FAILED;
      }
      else if (status == ValidReservationScheduleStatusT.EXECUTION_FAILED) {
        return FAILED;
      }
      else if (status == ValidReservationScheduleStatusT.EXECUTION_PARTIALLY_CANCELLED) {
        return CANCELLED;
      }
      else if (status == ValidReservationScheduleStatusT.EXECUTION_CANCELLED) {
        return CANCELLED;
      }
      else {
        logger.error("Could not translate status: " + status);
        throw new RuntimeException("Could not translate status: " + status);
      }
    }
  }

  private CreateReservationScheduleRequestDocument createSchedule(final Reservation reservation) {
    final CreateReservationScheduleRequestDocument requestDocument = CreateReservationScheduleRequestDocument.Factory
        .newInstance();

    final CreateReservationScheduleRequest request = requestDocument.addNewCreateReservationScheduleRequest();
    final ReservationScheduleRequestT schedule = request.addNewReservationSchedule();

    schedule.setName(reservation.getUserCreated() + "-" + System.currentTimeMillis());
    schedule.setType(ValidReservationScheduleTypeT.RESERVATION_SCHEDULE_AUTOMATIC);

    final Calendar calendar = Calendar.getInstance();
    calendar.setTime(reservation.getStartDateTime().toDate());
    schedule.setStartTime(calendar);

    schedule.setReservationOccurrenceDuration(Minutes.minutesBetween(reservation.getStartDateTime(),
        reservation.getEndDateTime()).getMinutes());

    schedule.setIsRecurring(false);
    schedule.setPath(createPath(reservation));
    schedule.setUserInfo(createUser(reservation));

    return requestDocument;
  }

  private UserInfoT createUser(final Reservation reservation) {
    final UserInfoT userInfo = UserInfoT.Factory.newInstance();
    userInfo.setBillingGroup(billingGroupName);
    userInfo.setSourceEndpointResourceGroup(resourceGroupName);
    userInfo.setSourceEndpointUserGroup(groupName);
    userInfo.setTargetEndpointResourceGroup(resourceGroupName);
    userInfo.setTargetEndpointUserGroup(groupName);
    return userInfo;
  }

  private PathRequestT createPath(final Reservation reservation) {
    final PathRequestT pathRequest = PathRequestT.Factory.newInstance();

    final VirtualPort virtualSourcePort = reservation.getSourcePort();
    final VirtualPort virtualDestinationPort = reservation.getDestinationPort();

    EndpointT sourceEndPoint = findEndPointById(virtualSourcePort.getPhysicalPort().getNetworkElementPk());
    EndpointT destinationEndPoint = findEndPointById(virtualDestinationPort.getPhysicalPort().getNetworkElementPk());

    pathRequest.setSourceTna(sourceEndPoint.getTna());
    pathRequest.setTargetTna(destinationEndPoint.getTna());
    pathRequest.setRate(reservation.getBandwidth());

    pathRequest.setSourceVlanId(translateVlanId(virtualSourcePort));
    pathRequest.setTargetVlanId(translateVlanId(virtualDestinationPort));

    pathRequest.setRoutingAlgorithm(ROUTING_ALGORITHM);
    pathRequest.setProtectionType(DEFAULT_PROTECTIONTYPE);

    return pathRequest;
  }

  private String translateVlanId(VirtualPort virtualPort) {
    return virtualPort.getVlanId() == null ? DEFAULT_VID : virtualPort.getVlanId().toString();
  }

  private List<EndpointT> findAllEndPoints() {
    try {
      final QueryEndpointsRequestDocument requestDocument = QueryEndpointsRequestDocument.Factory.newInstance();
      final QueryEndpointsRequest request = requestDocument.addNewQueryEndpointsRequest();
      request.setUserGroup(groupName);
      request.setLayer(ValidLayerT.LAYER_2);
      request.setType(ValidEndpointsQueryTypeT.QUERY_ENDPOINTS_BY_LAYER_AND_USER_GROUP_T);

      final QueryEndpointsResponseDocument response = networkingService.queryEndpoints(requestDocument,
          getSecurityDocument());

      final List<EndpointT> endPoints = Lists.newArrayList();
      for (final String tna : response.getQueryEndpointsResponse().getTnaArray()) {
        endPoints.add(findEndpointByTna(tna));
      }

      return endPoints;
    }
    catch (Exception e) {
      log.error("Error: ", e);
      return null;
    }
  }

  private EndpointT findEndpointByTna(final String tna) throws RemoteException, NetworkMonitoringServiceFault {
    final QueryEndpointRequestDocument requestDocument = QueryEndpointRequestDocument.Factory.newInstance();
    final QueryEndpointRequest request = requestDocument.addNewQueryEndpointRequest();
    request.setTna(tna);

    final QueryEndpointResponseDocument response = networkingService.queryEndpoint(requestDocument,
        getSecurityDocument());
    final EndpointT endpointFound = response.getQueryEndpointResponse().getEndpoint();

    idToTnaCache.put(endpointFound.getId(), endpointFound.getTna());

    return endpointFound;
  }

  private PhysicalPort getPhysicalPort(final EndpointT endpoint) {
    final PhysicalPort port = new PhysicalPort();
    port.setNocLabel(endpoint.getTna());
    port.setManagerLabel(endpoint.getUserLabel());
    port.setNetworkElementPk(endpoint.getId());

    return port;
  }

  private SecurityDocument getSecurityDocument() {
    if (securityDocument == null) {
      securityDocument = SecurityDocument.Factory.newInstance();
      final Security security = securityDocument.addNewSecurity();
      final UsernameToken token = security.addNewUsernameToken();
      token.setUsername(username);
      try {
        token.setPassword(CryptoWrapper.getInstance().decrypt(new CryptedString(encryptedPassword)));
      }
      catch (Exception e) {
        log.error("Error: ", e);
        securityDocument = null;
      }
    }
    return securityDocument;
  }

  void setEncryptedPassword(String encryptedPassword) {
    this.encryptedPassword = encryptedPassword;
  }

}
