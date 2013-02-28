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
package nl.surfnet.bod;

import nl.surfnet.bod.support.TestExternalSupport;

import org.junit.Before;
import org.junit.Test;

public class PhysicalPortTestSelenium extends TestExternalSupport {

  @Before
  public void setup() {
    getNocDriver().createNewPhysicalResourceGroup(GROUP_SURFNET, ICT_MANAGERS_GROUP, "test@example.com");
  }

  @Test
  public void allocateAndUnallocatePhysicalPortFromInstitutePage() {
    getNocDriver().addPhysicalPortToInstitute(GROUP_SURFNET, "NOC label", "Mock_Poort 1de verdieping toren1a");

    getNocDriver().verifyPhysicalPortWasAllocated(BOD_PORT_ID_1, "NOC label");

    getNocDriver().unlinkPhysicalPort(BOD_PORT_ID_1);
  }

  @Test
  public void createRenameAndDeleteAPhysicalPort() {
    String nocLabel = "My Selenium Port (Noc)";
    String managerLabel1 = "My Selenium Port (Manager 1st)";
    String managerLabel2 = "My Selenium Port (Manager 2nd)";

    getNocDriver().linkPhysicalPort(NMS_PORT_ID_1, nocLabel, managerLabel1, GROUP_SURFNET);

    getNocDriver().verifyPhysicalPortWasAllocated(BOD_PORT_ID_1, nocLabel);

    getNocDriver().verifyPhysicalPortHasEnabledUnallocateIcon(BOD_PORT_ID_1, nocLabel);

    getNocDriver().gotoEditPhysicalPortAndVerifyManagerLabel(BOD_PORT_ID_1, managerLabel1);

    getNocDriver().switchToManager();

    getManagerDriver().changeManagerLabelOfPhyiscalPort(BOD_PORT_ID_1, managerLabel2);

    getManagerDriver().verifyManagerLabelChanged(BOD_PORT_ID_1, managerLabel2);

    getManagerDriver().switchToNoc();

    getNocDriver().gotoEditPhysicalPortAndVerifyManagerLabel(BOD_PORT_ID_1, managerLabel2);

    getNocDriver().unlinkPhysicalPort(BOD_PORT_ID_1);
  }

  @Test
  public void checkUnallocateState() {
    String vpOne = "VirtualPort One";
    String nocLabel = "My Selenium Port (Noc)";
    String managerLabel1 = "My Selenium Port (Manager 1st)";
    setupVirtualPort(vpOne, nocLabel, managerLabel1);

    getNocDriver().verifyPhysicalPortIsNotOnUnallocatedPage(BOD_PORT_ID_1, nocLabel);

    getNocDriver().switchToManager();
    getManagerDriver().deleteVirtualPort(vpOne);

    // After delete VP, the PysicalPort should be able to unallocated
    getManagerDriver().switchToNoc();
    getNocDriver().unlinkPhysicalPort(BOD_PORT_ID_1);
  }

  @Test
  public void verifyManagerLinkFromPhysicalPortToVIrtualPorts() {
    String vpOne = "VirtualPort One";
    String nocLabel = "My Selenium Port (Noc)";
    String managerLabel1 = "My Selenium Port (Manager 1st)";
    setupVirtualPort(vpOne, nocLabel, managerLabel1);

    getManagerDriver().verifyPhysicalPortToVirtualPortsLink(managerLabel1, vpOne);
  }

  private void setupVirtualPort(String vpOne, String nocLabel, String managerLabel1) {
    getNocDriver().linkPhysicalPort(NMS_PORT_ID_1, nocLabel, managerLabel1, GROUP_SURFNET);
    getWebDriver().clickLinkInLastEmail();

    getManagerDriver().switchToUser();
    getUserDriver().requestVirtualPort("Selenium users");
    getUserDriver().selectInstituteAndRequest(GROUP_SURFNET, 1000, "Doe mijn een nieuw poort...");
    getUserDriver().switchToManager(GROUP_SURFNET);

    getWebDriver().clickLinkInLastEmail();
    getManagerDriver().acceptVirtualPort(vpOne);
  }

}