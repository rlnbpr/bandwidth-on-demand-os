package nl.surfnet.bod;

import nl.surfnet.bod.support.TestExternalSupport;

import org.junit.Before;
import org.junit.Test;

public class VirtualPortTestSelenium extends TestExternalSupport {

  private final static String VP_DELETE_ALERT_TEXT = "reservations will be effected";

  @Before
  public void setup() {
    getNocDriver().createNewPhysicalResourceGroup("2COLLEGE", ICT_MANAGERS_GROUP, "test@test.nl");
    getNocDriver().linkPhysicalPort(NETWORK_ELEMENT_PK_2, "Request a virtual port", "2COLLEGE");

    getWebDriver().clickLinkInLastEmail();

    getUserDriver().requestVirtualPort("selenium-users");
    getUserDriver().selectInstituteAndRequest("2COLLEGE", 1000, "port 1");
    getWebDriver().clickLinkInLastEmail();
    getManagerDriver().createVirtualPort("First port");

    getUserDriver().requestVirtualPort("selenium-users");
    getUserDriver().selectInstituteAndRequest("2COLLEGE", 1000, "port 2");
    getWebDriver().clickLinkInLastEmail();
    getManagerDriver().createVirtualPort("Second port");
  }

  @Test
  public void lastPortDeletedShouldDeleteVirtualResourceGroup() {
    getManagerDriver().verifyVirtualResourceGroupExists("selenium-users", "2");

    getManagerDriver().deleteVirtualPortAndVerifyAlertText("First port", VP_DELETE_ALERT_TEXT);

    getManagerDriver().switchToUser();

    getUserDriver().verifyMemberOf("selenium-users");

    getUserDriver().switchToManager("2COLLEGE");

    getManagerDriver().verifyVirtualResourceGroupExists("selenium-users", "1");

    getManagerDriver().deleteVirtualPortAndVerifyAlertText("Second port", VP_DELETE_ALERT_TEXT);

    getManagerDriver().verifyVirtualResourceGroupsEmpty();

    getManagerDriver().switchToUser();

    getUserDriver().verifyNotMemberOf("selenium-users");
  }
}
