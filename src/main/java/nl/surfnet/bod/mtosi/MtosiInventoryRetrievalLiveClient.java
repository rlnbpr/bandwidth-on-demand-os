/**
 * Copyright (c) 2012, SURFnet BV
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *   * Neither the name of the SURFnet BV nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nl.surfnet.bod.mtosi;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tmforum.mtop.fmw.xsd.hdr.v1.CommunicationPatternType;
import org.tmforum.mtop.fmw.xsd.hdr.v1.CommunicationStyleType;
import org.tmforum.mtop.fmw.xsd.hdr.v1.Header;
import org.tmforum.mtop.fmw.xsd.hdr.v1.MessageTypeType;
import org.tmforum.mtop.fmw.xsd.nam.v1.RelativeDistinguishNameType;
import org.tmforum.mtop.msi.wsdl.sir.v1_0.GetServiceInventoryException;
import org.tmforum.mtop.msi.wsdl.sir.v1_0.ServiceInventoryRetrievalHttp;
import org.tmforum.mtop.msi.wsdl.sir.v1_0.ServiceInventoryRetrievalRPC;
import org.tmforum.mtop.msi.xsd.sir.v1.GetServiceInventoryRequest;
import org.tmforum.mtop.msi.xsd.sir.v1.GranularityType;
import org.tmforum.mtop.msi.xsd.sir.v1.ObjectFactory;
import org.tmforum.mtop.msi.xsd.sir.v1.ServiceInventoryDataType;
import org.tmforum.mtop.msi.xsd.sir.v1.SimpleServiceFilterType;
import org.tmforum.mtop.sb.xsd.svc.v1.ServiceAccessPointType;
import org.tmforum.mtop.sb.xsd.svc.v1.ServiceCharacteristicValueType;

import com.google.common.annotations.VisibleForTesting;

import nl.surfnet.bod.domain.PhysicalPort;

@Service("mtosiInventoryRetrievalLiveClient")
public class MtosiInventoryRetrievalLiveClient {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private ServiceInventoryRetrievalHttp serviceInventoryRetrievalHttp;

  private final String resourceInventoryRetrievalUrl;
  private final String senderUri;

  private boolean isInited = false;

  @Autowired
  public MtosiInventoryRetrievalLiveClient(@Value("${mtosi.inventory.retrieval.endpoint}") String retrievalUrl,
      @Value("${mtosi.inventory.sender.uri}") String senderUri) {
    this.resourceInventoryRetrievalUrl = retrievalUrl;
    this.senderUri = senderUri;
  }

  // If we do this using a postconstruct then spring will try to initialise this
  // bean (using it's annotation scan path thingie), fails and therefore the
  // complete context will fail during (junit) testing when there is no
  // connection with the mtosi server.
  private void init() {
    if (isInited) {
      return;
    }
    else {
      try {
        serviceInventoryRetrievalHttp = new ServiceInventoryRetrievalHttp(new URL(resourceInventoryRetrievalUrl),
            new QName("http://www.tmforum.org/mtop/msi/wsdl/sir/v1-0", "ServiceInventoryRetrievalHttp"));

        final Map<String, Object> requestContext = ((BindingProvider) serviceInventoryRetrievalHttp
            .getPort(ServiceInventoryRetrievalRPC.class)).getRequestContext();

        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, resourceInventoryRetrievalUrl);
        isInited = true;
      }
      catch (MalformedURLException e) {
        log.error("Error: ", e);
      }
    }

  }

  private ServiceInventoryDataType getInventory() {
    log.info("Retrieving inventory at: {} ", resourceInventoryRetrievalUrl);
    this.init();
    try {
      final GetServiceInventoryRequest inventoryRequest = new ObjectFactory().createGetServiceInventoryRequest();
      inventoryRequest.setFilter(getInventoryRequestSimpleFilter());
      return serviceInventoryRetrievalHttp.getServiceInventoryRetrievalSoapHttp()
          .getServiceInventory(getInventoryRequestHeaders(), inventoryRequest).getInventoryData();
    }
    catch (GetServiceInventoryException e) {
      log.error("Error: ", e);
      return null;
    }
  }

  private SimpleServiceFilterType getInventoryRequestSimpleFilter() {
    final SimpleServiceFilterType simpleFilter = new ObjectFactory().createSimpleServiceFilterType();
    simpleFilter.getScopeAndSelection().add(GranularityType.FULL);
    final SimpleServiceFilterType.Scope scope = new ObjectFactory().createSimpleServiceFilterTypeScope();
    scope.setServiceObjectType("SAP");
    simpleFilter.getScopeAndSelection().add(scope);
    return simpleFilter;
  }

  public List<PhysicalPort> getUnallocatedPorts() {

    // [x] nmsPortId = sapList.sap.resourceRef.rdn.value where
    // sapList.sap.resourceRef.rdn.type == PTP

    // [x] nmsNEId = sapList.sap.resourceRef.rdn.value where
    // sapList.sap.resourceRef.rdn.type == ME

    // [x] nsmPortSpeed = sapList.sap.describedByList.value where
    // sapList.sap.describedByList.sscRef.rdn.value == AdministrativeSpeedRate

    // [x] bodPortId = spaList.sap.name

    // [x] vlanRequired = true if sapList.sap.describedByList.value in (EVPL,
    // EVPLAN) where sapList.sap.describedByList.sscRef.rdn.value ==
    // SupportedServiceType

    // [] nocLabel = nmsNeId + nmsPortId

    // [] nmsNeId = will be human friendly name for NE + snmp ifalias

    final List<PhysicalPort> mtosiPorts = new ArrayList<>();

    for (final ServiceAccessPointType sap : getInventory().getSapList().getSap()) {
      final String nmsSapName = sap.getName().getValue().getRdn().get(0).getValue();

      String nmsNeId = null, nmsPortId = null, nmsPortSpeed = null, supportedServiceType = null;
      boolean isVlanRequired = false;

      for (final RelativeDistinguishNameType relativeDistinguishNameType : sap.getResourceRef().getRdn()) {
        if (relativeDistinguishNameType.getType().equals("ME")) {
          nmsNeId = relativeDistinguishNameType.getValue();
        }
        else if (relativeDistinguishNameType.getType().equals("PTP")) {
          nmsPortId = relativeDistinguishNameType.getValue();
        }
      }

      for (final ServiceCharacteristicValueType serviceCharacteristicValueType : sap.getDescribedByList()) {
        final String value = serviceCharacteristicValueType.getValue();
        for (final RelativeDistinguishNameType rdn : serviceCharacteristicValueType.getSscRef().getRdn()) {
          final String rdnValue = rdn.getValue();
          if ("AdministrativeSpeedRate".equals(rdnValue)) {
            nmsPortSpeed = value;
          }
          else if ("SupportedServiceType".equals(rdnValue)) {
            supportedServiceType = value;
            if ("EVPL".equals(value) || "EVPLAN".equals(value)) {
              isVlanRequired = true;
            }
          }
        }
      }

      final PhysicalPort physicalPort = new PhysicalPort(isVlanRequired);
      final String convertedPortName = convertPortName(nmsPortId);
      physicalPort.setNmsPortId(convertedPortName);
      physicalPort.setNmsNeId(nmsNeId);
      physicalPort.setBodPortId(nmsSapName);
      physicalPort.setNmsPortSpeed(nmsPortSpeed);
      physicalPort.setNmsSapName(nmsSapName);
      physicalPort.setNocLabel(nmsNeId + " " + convertedPortName);
      physicalPort.setSupportedServiceType(supportedServiceType);
      physicalPort.setSignalingType("NA");

      mtosiPorts.add(physicalPort);
    }
    return mtosiPorts;
  }

  @VisibleForTesting
  public String convertPortName(final String mtosiPortName) {
    return mtosiPortName.replace("rack=", "").replace("shelf=", "").replace("sub_slot=", "").replace("slot=", "")
        .replace("port=", "").replaceFirst("/", "").replaceAll("/", "-");
  }

  public int getUnallocatedMtosiPortCount() {
    return getInventory().getSapList().getSap().size();
  }

  private Holder<Header> getInventoryRequestHeaders() {
    final Header header = new Header();
    header.setDestinationURI(resourceInventoryRetrievalUrl);
    header.setCommunicationPattern(CommunicationPatternType.SIMPLE_RESPONSE);
    header.setCommunicationStyle(CommunicationStyleType.RPC);
    header.setActivityName("getServiceInventory");
    header.setMsgName("getServiceInventoryRequest");
    header.setSenderURI(senderUri);
    header.setMsgType(MessageTypeType.REQUEST);
    return new Holder<Header>(header);
  }

  // static {
  // System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump",
  // Boolean.toString(true));
  // }

}
