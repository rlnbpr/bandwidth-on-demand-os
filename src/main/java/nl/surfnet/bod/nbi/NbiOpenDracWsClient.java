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
package nl.surfnet.bod.nbi;

import static nl.surfnet.bod.domain.ReservationStatus.*;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

import nl.surfnet.bod.domain.PhysicalPort;
import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.domain.ReservationStatus;
import nl.surfnet.bod.domain.VirtualPort;
import nl.surfnet.bod.nbi.generated.NetworkMonitoringServiceFault;
import nl.surfnet.bod.nbi.generated.NetworkMonitoringService_v30Stub;
import nl.surfnet.bod.nbi.generated.ResourceAllocationAndSchedulingServiceFault;
import nl.surfnet.bod.nbi.generated.ResourceAllocationAndSchedulingService_v30Stub;

import org.joda.time.Minutes;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0_xsd.Security;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0_xsd.SecurityDocument;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0_xsd.UsernameToken;
import org.opendrac.www.ws.networkmonitoringservicetypes_v3_0.*;
import org.opendrac.www.ws.networkmonitoringservicetypes_v3_0.QueryEndpointRequestDocument.QueryEndpointRequest;
import org.opendrac.www.ws.networkmonitoringservicetypes_v3_0.QueryEndpointsRequestDocument.QueryEndpointsRequest;
import org.opendrac.www.ws.resourceallocationandschedulingservicetypes_v3_0.*;
import org.opendrac.www.ws.resourceallocationandschedulingservicetypes_v3_0.ActivateReservationOccurrenceRequestDocument.ActivateReservationOccurrenceRequest;
import org.opendrac.www.ws.resourceallocationandschedulingservicetypes_v3_0.CancelReservationScheduleRequestDocument.CancelReservationScheduleRequest;
import org.opendrac.www.ws.resourceallocationandschedulingservicetypes_v3_0.CreateReservationScheduleRequestDocument.CreateReservationScheduleRequest;
import org.opendrac.www.ws.resourceallocationandschedulingservicetypes_v3_0.QueryReservationScheduleRequestDocument.QueryReservationScheduleRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nortel.www.drac._2007._07._03.ws.ct.draccommontypes.CompletionResponseDocument;
import com.nortel.www.drac._2007._07._03.ws.ct.draccommontypes.ValidCompletionTypeT.Enum;
import com.nortel.www.drac._2007._07._03.ws.ct.draccommontypes.ValidLayerT;

/**
 * A bridge to OpenDRAC's web services. Everything is contained in this one
 * class so that only this class is linked to OpenDRAC related classes.
 *
 */
class NbiOpenDracWsClient implements NbiClient {

  private static final String ROUTING_ALGORITHM = "VCAT";
  private static final String DEFAULT_VID = "Untagged";
  private static final ValidProtectionTypeT.Enum DEFAULT_PROTECTIONTYPE = ValidProtectionTypeT.X_1_PLUS_1_PATH;
  private static final Minutes MAX_DURATION = Minutes.MAX_VALUE;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private NetworkMonitoringService_v30Stub networkingService;
  private ResourceAllocationAndSchedulingService_v30Stub schedulingService;
  private SecurityDocument securityDocument;

  private final ConcurrentMap<String, String> idToTnaCache = Maps.newConcurrentMap();

  @Value("${nbi.billing.group.name}")
  private String billingGroupName;

  @Value("${nbi.password}")
  private String password;

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

  @PostConstruct
  void init() {
    try {
      schedulingService = new ResourceAllocationAndSchedulingService_v30Stub(schedulingServiceUrl);
      networkingService = new NetworkMonitoringService_v30Stub(inventoryServiceUrl);
      getSecurityDocument();
    }
    catch (IOException e) {
      logger.error("Error: ", e);
    }
  }

  @Override
  public boolean activateReservation(final String reservationId) {
    String serviceId = findServiceId(reservationId);

    ActivateReservationOccurrenceRequestDocument activateReservationOccurrenceRequestDocument =
        ActivateReservationOccurrenceRequestDocument.Factory.newInstance();

    ActivateReservationOccurrenceRequest activateReservationOccurrenceRequest = activateReservationOccurrenceRequestDocument
        .addNewActivateReservationOccurrenceRequest();
    activateReservationOccurrenceRequest.setOccurrenceId(serviceId);

    try {
      CompletionResponseDocument completionResponseDocument = schedulingService.activateReservationOccurrence(
          activateReservationOccurrenceRequestDocument, getSecurityDocument());

      return completionResponseDocument.getCompletionResponse().getResult() == Enum.forInt(1);

    }
    catch (ResourceAllocationAndSchedulingServiceFault | RemoteException e) {
      logger.error("Error: ", e);
    }

    return false;
  }

  @Override
  public void cancelReservation(final String reservationId) {
    CancelReservationScheduleRequestDocument requestDocument = CancelReservationScheduleRequestDocument.Factory.newInstance();
    CancelReservationScheduleRequest request = requestDocument.addNewCancelReservationScheduleRequest();
    request.setReservationScheduleId(reservationId);
    try {
      CompletionResponseDocument response = schedulingService.cancelReservationSchedule(requestDocument, getSecurityDocument());
      logger.info("Status: {}", response.getCompletionResponse().getResult());
    }
    catch (ResourceAllocationAndSchedulingServiceFault | RemoteException e) {
      logger.error("Error: ", e);
    }

  }

  @Override
  public Reservation createReservation(final Reservation reservation, boolean autoProvision) {
    try {
      CreateReservationScheduleResponseDocument responseDocument = schedulingService.createReservationSchedule(
          createSchedule(reservation, autoProvision), getSecurityDocument());

      logger.debug("Create reservation response: {}", responseDocument.getCreateReservationScheduleResponse());

      String reservationId = responseDocument.getCreateReservationScheduleResponse().getReservationScheduleId();
      ReservationStatus status = OpenDracStatusTranslator.translate(responseDocument
          .getCreateReservationScheduleResponse().getResult(), autoProvision);

      if (status == FAILED) {
        List<String> reasons = Lists.newArrayList();
        for (final ReservationOccurrenceInfoT occurenceInfo : responseDocument.getCreateReservationScheduleResponse()
            .getOccurrenceInfoArray()) {
          reasons.add(occurenceInfo.getReason());
        }

        String failedReason = Joiner.on(", ").join(reasons);
        reservation.setFailedReason(failedReason);

        logger.info("Create reservation ({}) failed with '{}'", reservationId, failedReason);
      }

      reservation.setReservationId(reservationId);
      reservation.setStatus(status);
    }
    catch (ResourceAllocationAndSchedulingServiceFault e) {
      logger.warn("Creating a reservation failed", e);
      reservation.setStatus(FAILED);
    }
    catch (Exception e) {
      logger.error("Unexpected Exception while request reservation to OpenDRAC", e);
      reservation.setStatus(FAILED);
    }

    return reservation;
  }

  @Override
  public List<PhysicalPort> findAllPhysicalPorts() {
    try {
      final List<PhysicalPort> ports = Lists.newArrayList();

      for (final EndpointT endpoint : findAllEndPoints()) {
        ports.add(getPhysicalPort(endpoint));
      }

      return ports;
    }
    catch (NetworkMonitoringServiceFault e) {
      logger.warn("Could not query OpenDrac for all endpoints", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public PhysicalPort findPhysicalPortByNmsPortId(final String nmsPortId) {
    try {
      EndpointT endpoint = findEndPointById(nmsPortId);
      return getPhysicalPort(endpoint);
    }
    catch (NetworkMonitoringServiceFault e) {
      logger.warn("Could not query OpenDrac for end point by id '{}'", nmsPortId);
      throw new RuntimeException(e);
    }
  }

  /*
   * Find by id is a little more complex, because the webservice only supports a
   * lookup by tna and not by id. In BoD the tna is locally stored as the
   * nocLabel field. This field can be edited. The tna can also be edited in
   * OpenDRAC. So the only save thing todo is use the id. The service keeps a
   * chache of id to tna.
   */
  private EndpointT findEndPointById(String id) throws NetworkMonitoringServiceFault {
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
      endPoint = findEndpointByTna(tna);

      if (endPoint.getId().equals(id)) {
        return endPoint;
      }
      else {
        idToTnaCache.remove(id);
        return findEndPointById(id);
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
      logger.error("Error: ", e);
    }

    if (responseDocument != null && responseDocument.getQueryReservationScheduleResponse().getIsFound()) {
      final ReservationScheduleT schedule = responseDocument.getQueryReservationScheduleResponse()
          .getReservationSchedule();
      final ValidReservationScheduleStatusT.Enum status = schedule.getStatus();

      return OpenDracStatusTranslator.translate(status);
    }
    else {
      logger.info("No reservation found for reservationId: {}, returning FAILED", reservationId);
      return FAILED;
    }

  }

  protected static final class OpenDracStatusTranslator {
    private static ImmutableMap<ValidReservationScheduleCreationResultT.Enum, ReservationStatus> creationAutoProvionResultTranslations =
      new ImmutableMap.Builder<ValidReservationScheduleCreationResultT.Enum, ReservationStatus>()
        .put(ValidReservationScheduleCreationResultT.FAILED, FAILED)
        .put(ValidReservationScheduleCreationResultT.SUCCEEDED, SCHEDULED)
        .put(ValidReservationScheduleCreationResultT.SUCCEEDED_PARTIALLY, SCHEDULED)
        .put(ValidReservationScheduleCreationResultT.UNKNOWN, FAILED).build();

    private static ImmutableMap<ValidReservationScheduleCreationResultT.Enum, ReservationStatus> creationResultTranslations =
      new ImmutableMap.Builder<ValidReservationScheduleCreationResultT.Enum, ReservationStatus>()
        .put(ValidReservationScheduleCreationResultT.FAILED, FAILED)
        .put(ValidReservationScheduleCreationResultT.SUCCEEDED, RESERVED)
        .put(ValidReservationScheduleCreationResultT.SUCCEEDED_PARTIALLY, RESERVED)
        .put(ValidReservationScheduleCreationResultT.UNKNOWN, FAILED).build();

    private static ImmutableMap<ValidReservationScheduleStatusT.Enum, ReservationStatus> scheduleStatusTranslations =
      new ImmutableMap.Builder<ValidReservationScheduleStatusT.Enum, ReservationStatus>()
        .put(ValidReservationScheduleStatusT.CONFIRMATION_PENDING, REQUESTED)
        .put(ValidReservationScheduleStatusT.CONFIRMATION_TIMED_OUT, FAILED)
        .put(ValidReservationScheduleStatusT.CONFIRMATION_CANCELLED, CANCELLED)
        .put(ValidReservationScheduleStatusT.EXECUTION_PENDING, SCHEDULED)
        .put(ValidReservationScheduleStatusT.EXECUTION_IN_PROGRESS, RUNNING)
        .put(ValidReservationScheduleStatusT.EXECUTION_SUCCEEDED, SUCCEEDED)
        .put(ValidReservationScheduleStatusT.EXECUTION_PARTIALLY_SUCCEEDED, FAILED)
         // means that the start time has passed and the reservation did not get an activate will activation was manual
        .put(ValidReservationScheduleStatusT.EXECUTION_TIMED_OUT, FAILED)
        .put(ValidReservationScheduleStatusT.EXECUTION_FAILED, FAILED)
        .put(ValidReservationScheduleStatusT.EXECUTION_PARTIALLY_CANCELLED, CANCELLED)
        .put(ValidReservationScheduleStatusT.EXECUTION_CANCELLED, CANCELLED).build();

    private OpenDracStatusTranslator() {
    }

    public static ReservationStatus translate(ValidReservationScheduleCreationResultT.Enum status, boolean autoProvision) {
      if (autoProvision) {
        return creationAutoProvionResultTranslations.get(status);
      }

      return creationResultTranslations.get(status);
    }

    public static ReservationStatus translate(ValidReservationScheduleStatusT.Enum status) {
      return scheduleStatusTranslations.get(status);
    }
  }

  CreateReservationScheduleRequestDocument createSchedule(final Reservation reservation, final boolean autoProvision)
      throws NetworkMonitoringServiceFault {

    CreateReservationScheduleRequestDocument requestDocument = CreateReservationScheduleRequestDocument.Factory
        .newInstance();

    CreateReservationScheduleRequest request = requestDocument.addNewCreateReservationScheduleRequest();
    ReservationScheduleRequestT schedule = request.addNewReservationSchedule();

    schedule.setName(reservation.getUserCreated() + "-" + System.currentTimeMillis());
    if (autoProvision) {
      schedule.setType(ValidReservationScheduleTypeT.RESERVATION_SCHEDULE_AUTOMATIC);
      logger.info("Created autoprovisioned reservation: {}", schedule.getName());
    }
    else {
      schedule.setType(ValidReservationScheduleTypeT.RESERVATION_SCHEDULE_MANUAL);
      logger.info("Created manual provisioned reservation: {}", schedule.getName());
    }
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(reservation.getStartDateTime().toDate());
    schedule.setStartTime(calendar);

    Minutes duration = reservation.getEndDateTime() == null ? MAX_DURATION : Minutes.minutesBetween(
        reservation.getStartDateTime(), reservation.getEndDateTime());
    schedule.setReservationOccurrenceDuration(duration.getMinutes());
    schedule.setIsRecurring(false);
    schedule.setPath(createPath(reservation));
    schedule.setUserInfo(createUser());

    return requestDocument;
  }

  @VisibleForTesting
  String findServiceId(final String reservationId) {

    try {
      QueryReservationScheduleRequestDocument queryReservationScheduleDocument = QueryReservationScheduleRequestDocument.Factory
          .newInstance();

      QueryReservationScheduleRequest queryReservationScheduleRequest = queryReservationScheduleDocument
          .addNewQueryReservationScheduleRequest();
      queryReservationScheduleRequest.setReservationScheduleId(reservationId);

      QueryReservationScheduleResponseDocument responseDocument = schedulingService.queryReservationSchedule(
          queryReservationScheduleDocument, getSecurityDocument());
      return responseDocument.getQueryReservationScheduleResponse().getReservationSchedule().getOccurrenceIdArray()[0];
    }
    catch (ResourceAllocationAndSchedulingServiceFault | RemoteException e) {
      logger.error("Error: ", e);
    }
    return null;
  }

  private UserInfoT createUser() {
    final UserInfoT userInfo = UserInfoT.Factory.newInstance();
    userInfo.setBillingGroup(billingGroupName);
    userInfo.setSourceEndpointResourceGroup(resourceGroupName);
    userInfo.setSourceEndpointUserGroup(groupName);
    userInfo.setTargetEndpointResourceGroup(resourceGroupName);
    userInfo.setTargetEndpointUserGroup(groupName);
    return userInfo;
  }

  private PathRequestT createPath(final Reservation reservation) throws NetworkMonitoringServiceFault {
    final PathRequestT pathRequest = PathRequestT.Factory.newInstance();

    final VirtualPort virtualSourcePort = reservation.getSourcePort();
    final VirtualPort virtualDestinationPort = reservation.getDestinationPort();

    EndpointT sourceEndPoint = findEndPointById(virtualSourcePort.getPhysicalPort().getNmsPortId());
    EndpointT destinationEndPoint = findEndPointById(virtualDestinationPort.getPhysicalPort().getNmsPortId());

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

  private List<EndpointT> findAllEndPoints() throws NetworkMonitoringServiceFault {
    final QueryEndpointsRequestDocument requestDocument = QueryEndpointsRequestDocument.Factory.newInstance();
    final QueryEndpointsRequest request = requestDocument.addNewQueryEndpointsRequest();
    request.setUserGroup(groupName);
    request.setLayer(ValidLayerT.LAYER_2);
    request.setType(ValidEndpointsQueryTypeT.QUERY_ENDPOINTS_BY_LAYER_AND_USER_GROUP_T);

    try {
      final QueryEndpointsResponseDocument response = networkingService.queryEndpoints(requestDocument,
          getSecurityDocument());

      logger.debug("Find all endpoints response: {}", response);

      final List<EndpointT> endPoints = Lists.newArrayList();
      for (final String tna : response.getQueryEndpointsResponse().getTnaArray()) {
        endPoints.add(findEndpointByTna(tna));
      }

      return endPoints;
    }
    catch (RemoteException e) {
      logger.warn("Could not query openDRAC for end points", e);
      throw new RuntimeException(e);
    }
  }

  private EndpointT findEndpointByTna(final String tna) throws NetworkMonitoringServiceFault {
    final QueryEndpointRequestDocument requestDocument = QueryEndpointRequestDocument.Factory.newInstance();
    final QueryEndpointRequest request = requestDocument.addNewQueryEndpointRequest();
    request.setTna(tna);

    try {
      QueryEndpointResponseDocument response = networkingService.queryEndpoint(requestDocument, getSecurityDocument());
      final EndpointT endpointFound = response.getQueryEndpointResponse().getEndpoint();

      idToTnaCache.put(endpointFound.getId(), endpointFound.getTna());

      return endpointFound;
    }
    catch (RemoteException e) {
      logger.warn("Can query openDrac for end point by tna", e);
      throw new RuntimeException(e);
    }
  }

  private PhysicalPort getPhysicalPort(final EndpointT endpoint) {
    final PhysicalPort port = new PhysicalPort(isVlanRequired(endpoint.getTna()));
    if (endpoint.getUserLabel() == null || endpoint.getUserLabel().isEmpty()) {
      port.setNocLabel(endpoint.getTna());
    }
    else {
      port.setNocLabel(endpoint.getUserLabel());
    }
    port.setNmsPortId(endpoint.getId());
    port.setBodPortId(endpoint.getTna());

    return port;
  }

  /**
   * @return true when a VlanId is required for this port. This is only the case
   *         when the tna of the port contains NOT
   *         {@link NbiClient#VLAN_REQUIRED_SELECTOR}
   */
  private boolean isVlanRequired(String tna) {
    return tna == null ? false : !tna.toLowerCase().contains(VLAN_REQUIRED_SELECTOR);
  }

  private SecurityDocument getSecurityDocument() {
    if (securityDocument == null) {
      securityDocument = SecurityDocument.Factory.newInstance();

      Security security = securityDocument.addNewSecurity();
      UsernameToken token = security.addNewUsernameToken();
      token.setUsername(username);
      token.setPassword(password);
    }
    return securityDocument;
  }

  void setPassword(String password) {
    this.password = password;
  }

  void setUsername(String username) {
    this.username = username;
  }

  void setInventoryServiceUrl(String inventoryServiceUrl) {
    this.inventoryServiceUrl = inventoryServiceUrl;
  }

}
