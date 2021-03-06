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
package nl.surfnet.bod.nbi.mtosi;

import static nl.surfnet.bod.util.TestHelper.devProperties;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;

import nl.surfnet.bod.domain.PhysicalPort;
import nl.surfnet.bod.util.TestHelper.PropertiesEnvironment;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.tmforum.mtop.msi.xsd.sir.v1.ServiceInventoryDataType.RfsList;
import org.tmforum.mtop.sb.xsd.svc.v1.ResourceFacingServiceType;
import org.tmforum.mtop.sb.xsd.svc.v1.ServiceAccessPointType;

public class InventoryRetrievalClientTestIntegration {

  private InventoryRetrievalClient subject;

  @Before
  public void setup() {
    PropertiesEnvironment testEnv = devProperties();
    subject = new InventoryRetrievalClient(testEnv.getProperty("nbi.mtosi.inventory.retrieval.endpoint"));
  }

  @Test
  public void getPhysicalPorts() {
    List<PhysicalPort> physicalPorts = subject.getPhysicalPorts();

    assertThat(physicalPorts, hasSize(greaterThan(0)));

    PhysicalPort firstPhysicalPort = physicalPorts.get(0);
    assertThat(firstPhysicalPort.getBodPortId(), startsWith("SAP-"));
    assertThat(firstPhysicalPort.getNmsPortId(), containsString("1-1"));
    assertThat(firstPhysicalPort.getNmsPortSpeed(), containsString("0"));
    assertThat(firstPhysicalPort.getNmsSapName(), startsWith("SAP-"));
    assertThat(firstPhysicalPort.getNmsSapName(), equalTo(firstPhysicalPort.getBodPortId()));
    assertThat(firstPhysicalPort.isAlignedWithNMS(), is(true));

//    for (PhysicalPort physicalPort : physicalPorts) {
//      System.err.println(physicalPort.getNmsSapName() + " " + physicalPort.getNmsPortId());
//    }
  }

  @Test
  public void getPhysicalPortCount() {
    assertThat(subject.getPhysicalPortCount(), greaterThan(0));
  }

  @Test
  @Ignore("For dubugging..")
  public void getRfsInventory() {
    RfsList inventory = subject.getCachedRfsInventory();
    for (ResourceFacingServiceType rfs : inventory.getRfs()) {

      System.err.println("RFS: " + MtosiUtils.getRfsName(rfs));
      System.err.println("Starts: " + MtosiUtils.getStartTime(rfs));
      System.err.println("Second state: " + MtosiUtils.getSecondaryState(rfs));
      System.err.println("Service state: " + rfs.getServiceState());
      System.err.println("Operational state: " + rfs.getOperationalState());

      System.err.println("SAPs: ");
      for (ServiceAccessPointType sap : rfs.getSapList()) {
        System.err.println("sap: " + MtosiUtils.getSapName(sap));
        System.err.println("PTP: " + MtosiUtils.findRdnValue("PTP", sap.getResourceRef()).get());
        System.err.println("ME: " + MtosiUtils.findRdnValue("ME", sap.getResourceRef()).get());
      }
    }
  }

  //00:03:18:58:ce:80-[5, 7]
  // SAP-00:03:18:58:ce:80-5 00:03:18:58:ce:80@1-1-1-5
  // SAP-00:03:18:58:ce:80-7 00:03:18:58:ce:80@1-1-1-7
  //00:03:18:58:ce:20-[1, 4, 5, 8]
  // SAP-00:03:18:58:ce:20-1 00:03:18:58:ce:20@1-1-1-1
  // SAP-00:03:18:58:ce:20-4 00:03:18:58:ce:20@1-1-1-4
  // SAP-00:03:18:58:ce:20-5 00:03:18:58:ce:20@1-1-1-5
  // SAP-00:03:18:58:ce:20-8 00:03:18:58:ce:20@1-1-1-8
}