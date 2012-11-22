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
package nl.surfnet.bod.nsi.v1sc;

import static com.jayway.awaitility.Awaitility.await;
import static nl.surfnet.bod.nsi.NsiConstants.URN_PROVIDER_NSA;
import static nl.surfnet.bod.nsi.NsiConstants.URN_STP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;

import java.util.EnumSet;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.xml.datatype.DatatypeConfigurationException;

import nl.surfnet.bod.domain.*;
import nl.surfnet.bod.domain.oauth.NsiScope;
import nl.surfnet.bod.repo.*;
import nl.surfnet.bod.service.*;
import nl.surfnet.bod.support.*;
import nl.surfnet.bod.web.security.RichUserDetails;
import nl.surfnet.bod.web.security.Security;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.jadira.usertype.dateandtime.joda.PersistentDateTime;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ogf.schemas.nsi._2011._10.connection._interface.*;
import org.ogf.schemas.nsi._2011._10.connection.provider.ServiceException;
import org.ogf.schemas.nsi._2011._10.connection.types.ConnectionStateType;
import org.ogf.schemas.nsi._2011._10.connection.types.PathType;
import org.ogf.schemas.nsi._2011._10.connection.types.ScheduleType;
import org.ogf.schemas.nsi._2011._10.connection.types.ServiceTerminationPointType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Optional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/spring/appCtx.xml", "/spring/appCtx-jpa-integration.xml",
    "/spring/appCtx-nbi-client.xml", "/spring/appCtx-idd-client.xml" })
@TransactionConfiguration(defaultRollback = true, transactionManager = "transactionManager")
@Transactional
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ConnectionServiceProviderTestIntegration extends AbstractTransactionalJUnit4SpringContextTests {

  private static MockHttpServer nsiRequester = new MockHttpServer(ConnectionServiceProviderFactory.PORT);

  @Resource
  private ConnectionServiceProviderWs nsiProvider;

  @Resource
  private VirtualPortRepo virtualPortRepo;

  @Resource
  private PhysicalPortRepo physicalPortRepo;

  @Resource
  private VirtualResourceGroupRepo virtualResourceGroupRepo;

  @Resource
  private PhysicalResourceGroupRepo physicalResourceGroupRepo;

  @Resource
  private InstituteRepo instituteRepo;

  @Resource
  private ConnectionRepo connectionRepo;

  @Resource
  private ReservationService reservationService;

  @Resource
  private ReservationEventPublisher reservationEventPublisher;

  @PersistenceContext
  private EntityManager entityManager;

  @Resource
  private EntityManagerFactory entityManagerFactory;

  @Resource
  private ReservationPoller reservationPoller;

  private static final String URN_REQUESTER_NSA = "urn:requester";
  private final String virtualResourceGroupName = "nsi:group";
  private VirtualPort sourceVirtualPort;
  private VirtualPort destinationVirtualPort;
  private final RichUserDetails userDetails = new RichUserDetailsFactory()
    .setScopes(EnumSet.allOf(NsiScope.class))
    .addBodRoles(BodRole.createUser())
    .addUserGroup("test").create();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    nsiRequester.addResponse("/bod/nsi/requester", new ClassPathResource(
        "web/services/nsi/mockNsiReservationFailedResponse.xml"));
    nsiRequester.startServer();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    nsiRequester.stopServer();
    Security.clearUserDetails();
  }

  @BeforeTransaction
  public void setup() {
    setLoggedInUser(userDetails);

    PhysicalResourceGroup physicalResourceGroup = createPhysicalResourceGroup();
    VirtualResourceGroup virtualResourceGroup = createVirtualResoruceGroup();

    PhysicalPort sourcePp = createPhysicalPort(physicalResourceGroup);
    PhysicalPort destinationPp = createPhysicalPort(physicalResourceGroup);

    this.sourceVirtualPort = createVirtualPort(virtualResourceGroup, sourcePp);
    this.destinationVirtualPort = createVirtualPort(virtualResourceGroup, destinationPp);
  }

  private VirtualPort createVirtualPort(VirtualResourceGroup virtualResourceGroup, PhysicalPort port) {
    VirtualPort vPort = new VirtualPortFactory().setMaxBandwidth(100).setPhysicalPort(port)
        .setVirtualResourceGroup(virtualResourceGroup).create();
    vPort = virtualPortRepo.save(vPort);
    virtualResourceGroup.addVirtualPort(vPort);
    virtualResourceGroupRepo.save(virtualResourceGroup);
    return vPort;
  }

  private PhysicalPort createPhysicalPort(PhysicalResourceGroup physicalResourceGroup) {
    return physicalPortRepo.save(new PhysicalPortFactory().setPhysicalResourceGroup(
        physicalResourceGroup).create());
  }

  private PhysicalResourceGroup createPhysicalResourceGroup() {
    Institute institute = instituteRepo.findAll().get(0);
    PhysicalResourceGroup physicalResourceGroup = new PhysicalResourceGroupFactory().setInstitute(institute).create();
    physicalResourceGroup = physicalResourceGroupRepo.save(physicalResourceGroup);
    return physicalResourceGroup;
  }

  private VirtualResourceGroup createVirtualResoruceGroup() {
    VirtualResourceGroup virtualResourceGroup = new VirtualResourceGroupFactory().setName(virtualResourceGroupName)
        .setSurfconextGroupId(userDetails.getUserGroupIds().iterator().next()).create();
    virtualResourceGroup = virtualResourceGroupRepo.save(virtualResourceGroup);
    return virtualResourceGroup;
  }

  private void setLoggedInUser(RichUserDetails userDetails) {
    SecurityContextHolder.setStrategyName("MODE_GLOBAL");
    Security.setUserDetails(userDetails);
  }

  @AfterTransaction
  public void teardown() {
    EntityManager em = entityManagerFactory.createEntityManager();
    SQLQuery query = ((Session) em.getDelegate())
        .createSQLQuery("truncate physical_resource_group, virtual_resource_group, connection cascade;");
    query.executeUpdate();

    nsiRequester.clearRequests();
  }

  @Test
  public void shouldReserveProvisionTerminate() throws Exception {
    ReserveRequestType reservationRequest = createReserveRequest(Optional.of(DateTime.now().plusDays(1)), Optional.<DateTime>absent());
    final String connectionId = reservationRequest.getReserve().getReservation().getConnectionId();
    final String reserveCorrelationId = reservationRequest.getCorrelationId();

    final DummyReservationListener listener = new DummyReservationListener();
    reservationEventPublisher.addListener(listener);

    // reserve
    GenericAcknowledgmentType reserveAcknowledgment = nsiProvider.reserve(reservationRequest);

    listener.waitForEventWithNewStatus(ReservationStatus.RESERVED);

    assertThat(reserveAcknowledgment.getCorrelationId(), is(reserveCorrelationId));

    final Connection connection = connectionRepo.findByConnectionId(connectionId);
    assertThat(connection.getConnectionId(), is(connectionId));

    Reservation reservation = reservationService.find(connection.getReservation().getId());
    entityManager.refresh(reservation);
    entityManager.refresh(connection);

    assertThat(reservation.getStatus(), is(ReservationStatus.RESERVED));
    assertThat(connection.getCurrentState(), is(ConnectionStateType.RESERVED));

    ProvisionRequestType provisionRequest = createProvisionRequest(connectionId);

    // provision
    GenericAcknowledgmentType provisionAck = nsiProvider.provision(provisionRequest);
    listener.waitForEventWithNewStatus(ReservationStatus.AUTO_START);

    final String provisionCorrelationId = provisionAck.getCorrelationId();

    assertThat(provisionAck.getCorrelationId(), is(provisionCorrelationId));

    entityManager.refresh(reservation);
    entityManager.refresh(connection);
    assertThat(reservation.getStatus(), is(ReservationStatus.AUTO_START));
    assertThat(connection.getCurrentState(), is(ConnectionStateType.AUTO_PROVISION));
    assertThat(connection.getProvisionRequestDetails(), notNullValue());

    // terminate
    TerminateRequestType terminateRequest = createTerminateRequest(connectionId);
    GenericAcknowledgmentType terminateAck = nsiProvider.terminate(terminateRequest);
    listener.waitForEventWithNewStatus(ReservationStatus.CANCELLED);

    final String terminateCorrelationId = terminateAck.getCorrelationId();

    assertThat(terminateCorrelationId, is(terminateRequest.getCorrelationId()));

    entityManager.refresh(reservation);
    entityManager.refresh(connection);
    assertThat(reservation.getStatus(), is(ReservationStatus.CANCELLED));
    assertThat(connection.getCurrentState(), is(ConnectionStateType.TERMINATED));
  }

  /**
   * After reading from the database the timestamps are converted to the default
   * jvm timezone by the @See {@link PersistentDateTime} annotation on the
   * timestamp fields
   *
   * @throws Exception
   */
  @Test
  public void shouldReserveAndConvertTimeZoneToJVMDefault() throws Exception {
    int jvmOffesetInMillis = DateTimeZone.getDefault().getOffset(new DateTime().getMillis());
    int offsetInHours = -4;
    DateTime start = new DateTime().plusHours(1).withSecondOfMinute(0).withMillisOfSecond(0).withZoneRetainFields(
        DateTimeZone.forOffsetHours(offsetInHours));

    ReserveRequestType reservationRequest = createReserveRequest(Optional.of(start), Optional.<DateTime> absent());
    final String connectionId = reservationRequest.getReserve().getReservation().getConnectionId();
    final String reserveCorrelationId = reservationRequest.getCorrelationId();

    final DummyReservationListener listener = new DummyReservationListener();
    reservationEventPublisher.addListener(listener);

    GenericAcknowledgmentType reserveAcknowledgment = nsiProvider.reserve(reservationRequest);
    listener.waitForEventWithNewStatus(ReservationStatus.RESERVED);

    assertThat(reserveAcknowledgment.getCorrelationId(), is(reserveCorrelationId));

    final Connection connection = connectionRepo.findByConnectionId(connectionId);
    assertThat(connection.getConnectionId(), is(connectionId));

    Reservation reservation = reservationService.find(connection.getReservation().getId());
    entityManager.refresh(reservation);
    entityManager.refresh(connection);

    assertThat("Has reservation default JVM timezone?", reservation.getStartDateTime().getZone().getOffset(
        reservation.getStartDateTime().getMillis()), is(jvmOffesetInMillis));

    assertThat("Has connection default JVM timezone?", connection.getStartTime().get().getZone().getOffset(
        connection.getStartTime().get().getMillis()), is(jvmOffesetInMillis));

    assertThat("Is reservation timestamp converted, compare both in UTC", reservation.getStartDateTime().withZone(
        DateTimeZone.UTC).getMillis(), is(start.withZone(DateTimeZone.UTC).getMillis()));

    assertThat("Is connection timestamp converted, compare both in UTC", connection.getStartTime().get().withZone(
        DateTimeZone.UTC).getMillis(), is(start.withZone(DateTimeZone.UTC).getMillis()));
  }

  /**
   * After reading from the database the timestamps are converted to the default
   * jvm timezone by the @See {@link PersistentDateTime} annotation on the
   * timestamp fields
   *
   * @throws Exception
   */
  @Test
  public void shouldSetEndDateWhenNoneIsPresentOrBeforeStart() throws Exception {
    DateTime start = DateTime.now().plusHours(1).withSecondOfMinute(0).withMillisOfSecond(0);
    ReserveRequestType reservationRequest = createReserveRequest(Optional.of(start), Optional.<DateTime> absent());

    final String connectionId = reservationRequest.getReserve().getReservation().getConnectionId();
    final String reserveCorrelationId = reservationRequest.getCorrelationId();

    final DummyReservationListener listener = new DummyReservationListener();
    reservationEventPublisher.addListener(listener);

    GenericAcknowledgmentType reserveAcknowledgment = nsiProvider.reserve(reservationRequest);

    listener.waitForEventWithNewStatus(ReservationStatus.RESERVED);

    assertThat(reserveAcknowledgment.getCorrelationId(), is(reserveCorrelationId));

    final Connection connection = connectionRepo.findByConnectionId(connectionId);
    assertThat(connection.getConnectionId(), is(connectionId));

    Reservation reservation = reservationService.find(connection.getReservation().getId());
    entityManager.refresh(reservation);
    entityManager.refresh(connection);

    assertThat("StartDate is unchanged on reservation", reservation.getStartDateTime(), is(start));
    assertThat("StartDate is unchanged on connection", connection.getStartTime().get(), is(start));

    assertThat("EndDate is still null, infinite on reservation", reservation.getEndDateTime(), nullValue());
    assertFalse("EndDate is still null, infinite on connection", connection.getEndTime().isPresent());
  }

  private class DummyReservationListener implements ReservationListener {
    private Optional<ReservationStatusChangeEvent> event = Optional.absent();

    @Override
    public void onStatusChange(ReservationStatusChangeEvent event) {
      this.event = Optional.of(event);
    }

    public Optional<ReservationStatusChangeEvent> getEvent() {
      return event;
    }

    public void reset() {
      event = Optional.absent();
    }

    public void waitForEventWithNewStatus(final ReservationStatus status) throws Exception {
      await("Wait for status change to " + status).atMost(10, TimeUnit.SECONDS).until(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          return getEvent().isPresent() && getEvent().get().getNewStatus().equals(status);
        }
      });

      reset();
    }
  }

  @Test
  public void queryShouldGiveAConfirm() throws ServiceException, DatatypeConfigurationException {
    ReserveRequestType reserveRequest = createReserveRequest();
    final String connectionId = reserveRequest.getReserve().getReservation().getConnectionId();

    nsiProvider.reserve(reserveRequest);
    awaitReserveConfirmed();

    QueryRequestType queryRequest = createQueryRequest(connectionId);
    GenericAcknowledgmentType genericAcknowledgment = nsiProvider.query(queryRequest);
    assertThat(genericAcknowledgment.getCorrelationId(), is(queryRequest.getCorrelationId()));

    String response = awaitQueryConfirmed();
    assertThat(response, containsString(queryRequest.getCorrelationId()));
    assertThat(response, containsString("<connectionState>Reserved</connectionState"));
    assertThat(response, containsString("<connectionId>"+connectionId+"</connectionId"));
    assertThat(response, containsString("<providerNSA>"+URN_PROVIDER_NSA+"</providerNSA>"));
    assertThat(response, containsString("<requesterNSA>"+URN_REQUESTER_NSA+"</requesterNSA>"));
  }

  @Test
  public void shouldTerminateWhenCancelledByGUI() throws Exception {
    final DummyReservationListener listener = new DummyReservationListener();
    reservationEventPublisher.addListener(listener);

    ReserveRequestType reservationRequest = createReserveRequest();
    final String connectionId = reservationRequest.getReserve().getReservation().getConnectionId();

    nsiProvider.reserve(reservationRequest);

    listener.waitForEventWithNewStatus(ReservationStatus.RESERVED);

    Connection connection = connectionRepo.findByConnectionId(connectionId);
    reservationService.cancel(connection.getReservation(), new RichUserDetailsFactory().addNocRole().create());

    listener.waitForEventWithNewStatus(ReservationStatus.CANCELLED);

    Reservation reservation = connection.getReservation();
    entityManager.refresh(reservation);
    entityManager.refresh(connection);

    assertThat(reservation.getStatus(), is(ReservationStatus.CANCELLED));
    assertThat(connection.getCurrentState(), is(ConnectionStateType.TERMINATED));
  }

  @Test
  public void terminateShouldFailWhenStateIsTerminated() throws Exception {
    ReserveRequestType reservationRequest = createReserveRequest();
    final String connectionId = reservationRequest.getReserve().getReservation().getConnectionId();

    nsiProvider.reserve(reservationRequest);

    awaitReserveConfirmed();

    TerminateRequestType terminateRequest = createTerminateRequest(connectionId);
    nsiProvider.terminate(terminateRequest);

    awaitTerminateConfirmed();

    nsiProvider.terminate(terminateRequest);

    awaitTerminateFailed();
  }

  @Test
  public void provisionShouldFailWhenStateIsTerminated() throws Exception {
    ReserveRequestType reservationRequest = createReserveRequest();
    final String connectionId = reservationRequest.getReserve().getReservation().getConnectionId();

    nsiProvider.reserve(reservationRequest);
    awaitReserveConfirmed();

    TerminateRequestType terminateRequest = createTerminateRequest(connectionId);
    nsiProvider.terminate(terminateRequest);
    awaitTerminateConfirmed();

    ProvisionRequestType provisionRequest = createProvisionRequest(connectionId);
    nsiProvider.provision(provisionRequest);

    awaitProvisionFailed();
  }

  @Test
  public void provisionShouldSucceedWhenStateIsProvisioned() throws Exception {
    final ReserveRequestType reservationRequest = createReserveRequest();
    final String connectionId = reservationRequest.getReserve().getReservation().getConnectionId();

    final DummyReservationListener listener = new DummyReservationListener();
    reservationEventPublisher.addListener(listener);

    runInThePast(4, TimeUnit.MINUTES, new TimeTraveller() {
      @Override
      public void apply() throws ServiceException {
        nsiProvider.reserve(reservationRequest);
        awaitReserveConfirmed();
      }
    });

    ProvisionRequestType provisionRequest = createProvisionRequest(connectionId);
    nsiProvider.provision(provisionRequest);

    // or RUNNING when poller just ran...
    listener.waitForEventWithNewStatus(ReservationStatus.AUTO_START);

    reservationPoller.pollReservationsThatAreAboutToChangeStatusOrShouldHaveChanged();

    awaitProvisionConfirmed();

    nsiProvider.provision(provisionRequest);
    awaitProvisionConfirmed();
  }

  @Test
  public void connectionWithNoProvisionShouldMoveToScheduledAfterStartTime() throws Exception {
    final ReserveRequestType reservationRequest = createReserveRequest();
    final String connectionId = reservationRequest.getReserve().getReservation().getConnectionId();

    final DummyReservationListener listener = new DummyReservationListener();
    reservationEventPublisher.addListener(listener);

    runInThePast(4, TimeUnit.MINUTES, new TimeTraveller() {
      @Override
      public void apply() throws ServiceException {
        nsiProvider.reserve(reservationRequest);
        awaitReserveConfirmed();
      }
    });

    reservationPoller.pollReservationsThatAreAboutToChangeStatusOrShouldHaveChanged();

    listener.waitForEventWithNewStatus(ReservationStatus.SCHEDULED);

    Connection connection = connectionRepo.findByConnectionId(connectionId);
    entityManager.refresh(connection);
    assertThat(connection.getCurrentState(), is(ConnectionStateType.SCHEDULED));
  }

  private void runInThePast(long seconds, TimeUnit unit, TimeTraveller runnable) {
    try {
      DateTimeUtils.setCurrentMillisOffset(- unit.toMillis(seconds));
      runnable.apply();
    } catch (Exception e) {
      throw new AssertionError(e);
    } finally {
      DateTimeUtils.setCurrentMillisSystem();
    }
  }

  interface TimeTraveller {
    public void apply() throws Exception;
  }

  private ReserveRequestType createReserveRequest() throws DatatypeConfigurationException {
    return createReserveRequest(Optional.<DateTime> absent(), Optional.<DateTime> absent());
  }

  private ReserveRequestType createReserveRequest(Optional<DateTime> start, Optional<DateTime> end) {
    PathType path = new PathType();

    ServiceTerminationPointType dest = new ServiceTerminationPointType();
    dest.setStpId(URN_STP + ":" + sourceVirtualPort.getId());
    path.setDestSTP(dest);

    ServiceTerminationPointType source = new ServiceTerminationPointType();
    source.setStpId(URN_STP + ":" + destinationVirtualPort.getId());
    path.setSourceSTP(source);

    ReserveRequestType reservationRequest = new ReserveRequestTypeFactory().setProviderNsa(URN_PROVIDER_NSA).setPath(
        path).create();

    ScheduleType scheduleType = reservationRequest.getReserve().getReservation().getServiceParameters().getSchedule();
    scheduleType.setDuration(null);
    scheduleType.setStartTime(ConnectionServiceProviderFunctions.getXmlTimeStampFromDateTime(start.orNull()).orNull());
    scheduleType.setEndTime(ConnectionServiceProviderFunctions.getXmlTimeStampFromDateTime(end.orNull()).orNull());

    return reservationRequest;
  }

  private QueryRequestType createQueryRequest(String connectionId) {
    return new ConnectionServiceProviderFactory().setConnectionId(connectionId).setProviderNsa(URN_PROVIDER_NSA)
        .setRequesterNsa(URN_REQUESTER_NSA).createQueryRequest();
  }

  private TerminateRequestType createTerminateRequest(String connId) {
    return new ConnectionServiceProviderFactory().setConnectionId(connId).setProviderNsa(URN_PROVIDER_NSA)
        .createTerminateRequest();
  }

  private ProvisionRequestType createProvisionRequest(String connId) {
    return new ConnectionServiceProviderFactory().setConnectionId(connId).setProviderNsa(URN_PROVIDER_NSA)
        .createProvisionRequest();
  }

  private String awaitReserveConfirmed() {
    String response = nsiRequester.awaitRequest(5, TimeUnit.SECONDS);
    assertThat(response, containsString("reserveConfirmed"));
    return response;
  }

  private String awaitTerminateConfirmed() {
    return awaitRequestFor("terminateConfirmed");
  }

  private String awaitTerminateFailed() {
    return awaitRequestFor("terminateFailed");
  }

  private String awaitQueryConfirmed() {
    return awaitRequestFor("queryConfirmed");
  }

  private String awaitProvisionFailed() {
    return awaitRequestFor("provisionFailed");
  }

  private String awaitProvisionConfirmed() {
    return awaitRequestFor("provisionConfirmed");
  }

  private String awaitRequestFor(String responseTag) {
    String response = nsiRequester.awaitRequest(10, TimeUnit.SECONDS);
    assertThat(response, containsString(responseTag));
    return response;
  }
}