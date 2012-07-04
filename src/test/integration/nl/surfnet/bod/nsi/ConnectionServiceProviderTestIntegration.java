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
package nl.surfnet.bod.nsi;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;

import javax.annotation.Resource;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ogf.schemas.nsi._2011._10.connection._interface.GenericAcknowledgmentType;
import org.ogf.schemas.nsi._2011._10.connection._interface.ReserveRequestType;
import org.ogf.schemas.nsi._2011._10.connection.types.PathType;
import org.ogf.schemas.nsi._2011._10.connection.types.ServiceTerminationPointType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;

import nl.surfnet.bod.domain.PhysicalPort;
import nl.surfnet.bod.domain.VirtualPort;
import nl.surfnet.bod.domain.VirtualResourceGroup;
import nl.surfnet.bod.nsi.ws.v1sc.ConnectionServiceProvider;
import nl.surfnet.bod.repo.PhysicalPortRepo;
import nl.surfnet.bod.repo.VirtualPortRepo;
import nl.surfnet.bod.repo.VirtualResourceGroupRepo;
import nl.surfnet.bod.support.MockHttpServer;
import nl.surfnet.bod.support.NsiReservationFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/spring/appCtx.xml", "/spring/appCtx-jpa-test.xml",
    "/spring/appCtx-nbi-client.xml", "/spring/appCtx-idd-client.xml" })
@TransactionConfiguration(defaultRollback = true, transactionManager = "transactionManager")
public class ConnectionServiceProviderTestIntegration extends AbstractTransactionalJUnit4SpringContextTests {

  private static MockHttpServer requesterEndpoint = new MockHttpServer(NsiReservationFactory.PORT);

  @Resource(name = "nsiProvider_v1_sc")
  private ConnectionServiceProvider nsiProvider;

  @Resource
  private VirtualPortRepo virtualPortRepo;

  @Resource
  private PhysicalPortRepo physicalPortRepo;

  @Resource
  private VirtualResourceGroupRepo virtualResourceGroupRepo;

  private final String correationId = "urn:uuid:f32cc82e-4d87-45ab-baab-4b7011652a2e";

  private final String virtualResourceGroupName = "nsi:group";

  private final String sourceLabel = "00-20-D8-DF-33-59_ETH-1-1-4";
  private final String destinationLabel = "00-20-D8-DF-33-86_ETH-1-13-1";

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    requesterEndpoint.addResponse("/bod/nsi/requester", new ClassPathResource(
        "web/services/nsi/mockNsiReservationFailedResponse.xml"));
    requesterEndpoint.startServer();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    requesterEndpoint.stopServer();
  }

  @Before
  public void setup() {

    final VirtualResourceGroup virtualResourceGroup = new VirtualResourceGroup();
    virtualResourceGroup.setDescription("description");
    virtualResourceGroup.setName(virtualResourceGroupName);
    // final VirtualPort[] ports = new VirtualPort[]{sourcePort,
    // destinationPort};
    // virtualResourceGroup.setVirtualPorts(new
    // HashSet<VirtualPort>(Arrays.asList(ports)));
    virtualResourceGroup.setSurfconextGroupId("some:surf:conext:group:id");
    VirtualResourceGroup savedVirtualResourceGroup = virtualResourceGroupRepo.save(virtualResourceGroup);

    final PhysicalPort ppSource = new PhysicalPort();
    ppSource.setManagerLabel(sourceLabel);
    ppSource.setNocLabel(sourceLabel);
    ppSource.setBodPortId(sourceLabel);
    ppSource.setNmsPortId(sourceLabel);
    final PhysicalPort savedSourcePp = physicalPortRepo.save(ppSource);

    final PhysicalPort ppDest = new PhysicalPort();
    ppDest.setManagerLabel(destinationLabel);
    ppDest.setNocLabel(destinationLabel);
    ppDest.setBodPortId(destinationLabel);
    ppDest.setNmsPortId(destinationLabel);
    final PhysicalPort savedDestinationPp = physicalPortRepo.save(ppDest);

    final VirtualPort sourcePort = new VirtualPort();
    sourcePort.setUserLabel(sourceLabel);
    sourcePort.setManagerLabel(sourceLabel);
    sourcePort.setMaxBandwidth(1000);
    sourcePort.setPhysicalPort(savedSourcePp);
    sourcePort.setVirtualResourceGroup(savedVirtualResourceGroup);
    final VirtualPort savedSourcePort = virtualPortRepo.save(sourcePort);

    final VirtualPort destinationPort = new VirtualPort();
    destinationPort.setUserLabel(destinationLabel);
    destinationPort.setManagerLabel(destinationLabel);
    destinationPort.setMaxBandwidth(1000);
    destinationPort.setPhysicalPort(savedDestinationPp);
    destinationPort.setVirtualResourceGroup(savedVirtualResourceGroup);

    final VirtualPort savedDestination = virtualPortRepo.save(destinationPort);
    final VirtualPort[] ports = new VirtualPort[] { savedSourcePort, savedDestination };
    savedVirtualResourceGroup.setVirtualPorts(new HashSet<VirtualPort>(Arrays.asList(ports)));
    savedVirtualResourceGroup = virtualResourceGroupRepo.save(virtualResourceGroup);
    savedVirtualResourceGroup.setSurfconextGroupId("some:surf:conext:group:id");
    virtualResourceGroupRepo.save(savedVirtualResourceGroup);
  }

  @Test
  public void should_return_generic_acknowledgement() throws Exception {
    final XMLGregorianCalendar startTime = DatatypeFactory.newInstance().newXMLGregorianCalendar();
    startTime.setDay(Calendar.getInstance().get(Calendar.DATE) + 1);
    startTime.setMonth(Calendar.getInstance().get(Calendar.MONTH) + 1);
    startTime.setYear(Calendar.getInstance().get(Calendar.YEAR));

    final XMLGregorianCalendar endTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(
        startTime.toGregorianCalendar());
    endTime.setDay(startTime.getDay() + 1);

    final PathType path = new PathType();

    final ServiceTerminationPointType dest = new ServiceTerminationPointType();
    dest.setStpId(destinationLabel);
    path.setDestSTP(dest);

    final ServiceTerminationPointType source = new ServiceTerminationPointType();
    source.setStpId(sourceLabel);
    path.setSourceSTP(source);

    final ReserveRequestType reservationRequest = new NsiReservationFactory().setScheduleStartTime(startTime)
        .setScheduleEndTime(endTime).setCorrelationId(correationId).setProviderNsa("urn:ogf:network:nsa:netherlight")
        .setPath(path).createReservation();

    final GenericAcknowledgmentType genericAcknowledgmentType = nsiProvider.reserve(reservationRequest);

    assertThat(genericAcknowledgmentType.getCorrelationId(), is(reservationRequest.getCorrelationId()));

    // The rest does not really work on jenkins
    final String lastRequest = requesterEndpoint.getOrWaitForRequest(15);
    assertThat(lastRequest, containsString(correationId));
    assertThat(lastRequest, containsString("reserveConfirmed"));
    assertThat(requesterEndpoint.getCallCounter(), is(1));
  }
}
