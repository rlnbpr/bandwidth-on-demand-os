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
import org.tmforum.mtop.msi.xsd.sir.v1.ServiceInventoryDataType.SapList;
import org.tmforum.mtop.msi.xsd.sir.v1.SimpleServiceFilterType;
import org.tmforum.mtop.sb.xsd.svc.v1.ServiceAccessPointType;
import org.tmforum.mtop.sb.xsd.svc.v1.ServiceCharacteristicValueType;

import com.google.common.annotations.VisibleForTesting;

import nl.surfnet.bod.domain.PhysicalPort;

@Service("mtosiLiveClient")
public class MtosiLiveClient {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private ServiceInventoryRetrievalHttp serviceInventoryRetrievalHttp;

  private final String resourceInventoryRetrievalUrl;
  private final String senderUri;

  private boolean isInited = false;

  @Autowired
  public MtosiLiveClient(@Value("${mtosi.inventory.retrieval.endpoint}") String retrievalUrl,
      @Value("${mtosi.inventory.sender.uri}") String senderUri) {
    this.resourceInventoryRetrievalUrl = retrievalUrl;
    this.senderUri = senderUri;
  }

  // If we do this using a postconstruct then spring will not initialise this
  // bean and therefore the complete context will fail during (junit) testing
  // when there is no
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

  /**
   * @return
   */
  private SimpleServiceFilterType getInventoryRequestSimpleFilter() {

    final SimpleServiceFilterType simpleFilter = new ObjectFactory().createSimpleServiceFilterType();
    simpleFilter.getScopeAndSelection().add(GranularityType.FULL);

    final SimpleServiceFilterType.Scope scope = new ObjectFactory().createSimpleServiceFilterTypeScope();
    scope.setServiceObjectType("SAP");
    simpleFilter.getScopeAndSelection().add(scope);

    return simpleFilter;
  }

  // [x] nmsPortId = sapList.sap.resourceRef.rdn.value where
  // sapList.sap.resourceRef.rdn.type == PTP

  // [x] nmsNEId = sapList.sap.resourceRef.rdn.value where
  // sapList.sap.resourceRef.rdn.type == ME

  // [x] nsmPortSpeed = sapList.sap.describedByList.value where
  // sapList.sap.describedByList.sscRef.rdn.value == AdministrativeSpeedRate

  // [x] bodPortId = <nmsNEId>_<nmsPortId> (until furter notice)

  public List<PhysicalPort> getUnallocatedPorts() {
    this.init();

    final List<PhysicalPort> mtosiPorts = new ArrayList<PhysicalPort>();
    final SapList saps = getInventory().getSapList();

    for (final ServiceAccessPointType sap : saps.getSap()) {
      String nocLabel = "NA", nmsNeId = "", nmsPortId = "", nmsSapName = "", vlanRequired = "", nmsPortSpeed = "";
      nmsSapName = sap.getName().getValue().getRdn().get(0).getValue();

      final List<RelativeDistinguishNameType> resourceRefsRdns = sap.getResourceRef().getRdn();

      for (final RelativeDistinguishNameType relativeDistinguishNameType : resourceRefsRdns) {
        if (relativeDistinguishNameType.getType().equals("ME")) {
          nmsNeId = relativeDistinguishNameType.getValue();
        }
        else if (relativeDistinguishNameType.getType().equals("PTP")) {
          nmsPortId = relativeDistinguishNameType.getValue();
        }
      }

      final List<ServiceCharacteristicValueType> describedByList = sap.getDescribedByList();

      String tmpSpeed = "";
      for (final ServiceCharacteristicValueType serviceCharacteristicValueType : describedByList) {
        tmpSpeed = serviceCharacteristicValueType.getValue();
        final List<RelativeDistinguishNameType> rdns = serviceCharacteristicValueType.getSscRef().getRdn();
        for (final RelativeDistinguishNameType rdn : rdns) {
          if ("AdministrativeSpeedRate".equals(rdn.getValue())) {
            nmsPortSpeed = tmpSpeed;
          }
        }
      }

      final PhysicalPort physicalPort = new PhysicalPort();
      physicalPort.setNmsPortId(convertPortName(nmsPortId));
      physicalPort.setNmsNeId(nmsNeId);
      physicalPort.setBodPortId(nmsNeId + "_" + convertPortName(nmsPortId));
      physicalPort.setNmsPortSpeed(nmsPortSpeed);
      physicalPort.setNmsSapName(nmsSapName);
      physicalPort.setNocLabel(nocLabel);

      mtosiPorts.add(physicalPort);
    }
    return mtosiPorts;
  }

  @VisibleForTesting
  public String convertPortName(final String mtosiPortName) {
    final String converted = mtosiPortName.replace("rack=", "").replace("shelf=", "").replace("slot=", "")
        .replace("port=", "").replaceFirst("/", "").replaceAll("/", "-");
    return converted;
  }

  public long getUnallocatedMtosiPortCount() {
    return getUnallocatedPorts().size();
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
