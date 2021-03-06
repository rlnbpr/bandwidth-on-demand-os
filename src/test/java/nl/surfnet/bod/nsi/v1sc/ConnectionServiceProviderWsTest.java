/**
 * Copyright (c) 2012, SURFnet BV
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *     disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided with the distribution.
 *   * Neither the name of the SURFnet BV nor the names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nl.surfnet.bod.nsi.v1sc;

import static nl.surfnet.bod.nsi.v1sc.ConnectionServiceProviderFunctions.RESERVE_REQUEST_TO_CONNECTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.EnumSet;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import nl.surfnet.bod.domain.Connection;
import nl.surfnet.bod.domain.NsiRequestDetails;
import nl.surfnet.bod.domain.VirtualPort;
import nl.surfnet.bod.domain.VirtualResourceGroup;
import nl.surfnet.bod.domain.oauth.NsiScope;
import nl.surfnet.bod.nsi.ConnectionServiceProviderErrorCodes;
import nl.surfnet.bod.repo.ConnectionRepo;
import nl.surfnet.bod.service.ConnectionService;
import nl.surfnet.bod.service.ReservationService;
import nl.surfnet.bod.service.VirtualPortService;
import nl.surfnet.bod.support.ConnectionFactory;
import nl.surfnet.bod.support.RichUserDetailsFactory;
import nl.surfnet.bod.support.VirtualPortFactory;
import nl.surfnet.bod.support.VirtualResourceGroupFactory;
import nl.surfnet.bod.web.security.RichUserDetails;
import nl.surfnet.bod.web.security.Security;

import org.hamcrest.text.IsEmptyString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ogf.schemas.nsi._2011._10.connection._interface.QueryRequestType;
import org.ogf.schemas.nsi._2011._10.connection._interface.ReserveRequestType;
import org.ogf.schemas.nsi._2011._10.connection.provider.ServiceException;
import org.ogf.schemas.nsi._2011._10.connection.types.*;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionServiceProviderWsTest {

  @InjectMocks
  private ConnectionServiceProviderWs subject;

  @Mock
  private ConnectionRepo connectionRepoMock;

  @Mock
  private VirtualPortService virtualPortServiceMock;

  @Mock
  private ReservationService reservationServiceMock;

  @Mock
  private ConnectionService connectionServiceProviderComponent;

  private final String nsaProvider = "nsa:surfnet.nl";

  private final RichUserDetails userDetails = new RichUserDetailsFactory().addUserGroup("test").create();

  private final VirtualResourceGroup vrg = new VirtualResourceGroupFactory().setAdminGroup("test").create();

  private final VirtualPort sourcePort = new VirtualPortFactory().setVirtualResourceGroup(vrg).create();

  private final VirtualPort destinationPort = new VirtualPortFactory().setVirtualResourceGroup(vrg).create();

  private final Connection connection = new ConnectionFactory().setSourceStpId("Source Port").setDestinationStpId(
      "Destination Port").setProviderNSA(nsaProvider).create();

  private final int port = 55446;
  private final NsiRequestDetails request = new NsiRequestDetails("http://localhost:"+port, "123456");

  @Before
  public void setup() throws Exception {
    subject.addNsaProvider(nsaProvider);
    when(virtualPortServiceMock.findByNsiStpId("Source Port")).thenReturn(sourcePort);
    when(virtualPortServiceMock.findByNsiStpId("Destination Port")).thenReturn(destinationPort);
  }

  @Test
  public void shouldComplainAboutTheProviderNsa() {
    Connection connection = new ConnectionFactory().setSourceStpId("Source Port").setDestinationStpId(
        "Destination Port").setProviderNSA("non:existingh").create();

    try {
      subject.reserve(connection, request, userDetails);
      fail("Exception expected");
    }
    catch (ServiceException e) {
      assertThat(e.getFaultInfo().getErrorId(), is(ConnectionServiceProviderWs.SVC0001_INVALID_PARAM));
    }
  }

  @Test
  public void shouldComplainAboutNonExistingPort() {
    when(virtualPortServiceMock.findByNsiStpId("Destination Port")).thenReturn(null);

    try {
      subject.reserve(connection, request, userDetails);
      fail("Exception expected");
    }
    catch (ServiceException e) {
      assertThat(e.getFaultInfo().getErrorId(), is(ConnectionServiceProviderWs.SVC0001_INVALID_PARAM));
    }
  }

  @Test
  public void shouldCreateReservation() throws ServiceException {
    subject.reserve(connection, request, userDetails);
  }

  @Test
  public void shouldThrowInvalidCredentialsWhileBakingAReservationPieForSourcePort() {
    sourcePort.getVirtualResourceGroup().setAdminGroup("other");

    try {
      subject.reserve(connection, request, userDetails);
      fail("Exception expected");
    }
    catch (ServiceException e) {
      assertThat(e.getFaultInfo().getErrorId(), is(ConnectionServiceProviderWs.SVC0005_INVALID_CREDENTIALS));
    }
  }

  @Test
  public void shouldThrowInvalidCredentialsWhileBakingAReservationPieForDestinationPort() {
    destinationPort.getVirtualResourceGroup().setAdminGroup("other");

    try {
      subject.reserve(connection, request, userDetails);
      fail("Exception expected");
    }
    catch (ServiceException e) {
      assertThat(e.getFaultInfo().getErrorId(), is(ConnectionServiceProviderWs.SVC0005_INVALID_CREDENTIALS));
    }
  }

  @Test
  public void shouldThrowAlreadyExistsWhenNonUniqueConnectionIdIsUsed() {
    when(connectionRepoMock.findByConnectionId(anyString())).thenReturn(new Connection());

    try {
      subject.reserve(connection, request, userDetails);
      fail("Exception expected");
    }
    catch (ServiceException e) {
      assertThat(e.getFaultInfo().getErrorId(), is(ConnectionServiceProviderWs.SVC0003_ALREADY_EXISTS));
    }
  }

  @Test
  public void reserveTypeWithoutGlobalReservationIdShouldGetOne() {
    ReserveRequestType reserveRequestType = createReservationRequestType(1000, Optional.<String> absent());

    Connection connection = RESERVE_REQUEST_TO_CONNECTION.apply(reserveRequestType);

    assertThat(connection.getGlobalReservationId(), not(IsEmptyString.isEmptyOrNullString()));
    assertThat(connection.getDesiredBandwidth(), is(1000));
  }

  @Test
  public void reserveTypeWithGlobalReservationId() {
    ReserveRequestType reserveRequestType = createReservationRequestType(1000, Optional.of("urn:surfnet.nl:12345"));

    Connection connection = RESERVE_REQUEST_TO_CONNECTION.apply(reserveRequestType);

    assertThat(connection.getGlobalReservationId(), is("urn:surfnet.nl:12345"));
  }

  @Test
  public void reserveWithoutReserveScopeShouldFail() throws ServiceException {
    Security.setUserDetails(new RichUserDetailsFactory().setScopes(EnumSet.of(NsiScope.QUERY)).create());

    try {
      subject.reserve(createReservationRequestType(100, Optional.of("1234")));
      fail("Exception expected");
    }
    catch (ServiceException e) {
      assertThat(e.getFaultInfo().getErrorId(), is(ConnectionServiceProviderErrorCodes.SECURITY.MISSING_GRANTED_SCOPE
          .getId()));
    }
  }

  @Test
  public void queryWithoutReserveScopeShouldFail() throws ServiceException {
    Security.setUserDetails(new RichUserDetailsFactory().setScopes(EnumSet.of(NsiScope.RESERVE)).create());

    try {
      subject.query(new QueryRequestType());
      fail("Exception expected");
    }
    catch (ServiceException e) {
      assertThat(e.getFaultInfo().getErrorId(), is(ConnectionServiceProviderErrorCodes.SECURITY.MISSING_GRANTED_SCOPE
          .getId()));
    }
  }


  public static ReserveRequestType createReservationRequestType(int desiredBandwidth,
      Optional<String> globalReservationId) {
    ServiceTerminationPointType dest = new ServiceTerminationPointType();
    dest.setStpId("urn:stp:1");

    ServiceTerminationPointType source = new ServiceTerminationPointType();
    source.setStpId("urn:stp:1");

    PathType path = new PathType();
    path.setDestSTP(dest);
    path.setSourceSTP(source);

    ScheduleType schedule = new ScheduleType();
    XMLGregorianCalendar xmlGregorianCalendar;
    try {
      xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar();
      schedule.setStartTime(xmlGregorianCalendar);
      schedule.setEndTime(xmlGregorianCalendar);
    }
    catch (DatatypeConfigurationException e) {
      Throwables.propagate(e);
    }

    BandwidthType bandwidth = new BandwidthType();
    bandwidth.setDesired(desiredBandwidth);

    ServiceParametersType serviceParameters = new ServiceParametersType();
    serviceParameters.setSchedule(schedule);
    serviceParameters.setBandwidth(bandwidth);

    ReservationInfoType reservation = new ReservationInfoType();
    reservation.setPath(path);
    reservation.setServiceParameters(serviceParameters);
    reservation.setGlobalReservationId(globalReservationId.orNull());

    ReserveType reserve = new ReserveType();
    reserve.setReservation(reservation);

    ReserveRequestType reserveRequestType = new ReserveRequestType();
    reserveRequestType.setReserve(reserve);

    return reserveRequestType;
  }

}
