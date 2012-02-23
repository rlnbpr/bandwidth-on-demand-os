package nl.surfnet.bod.support;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import nl.surfnet.bod.pages.manager.*;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.remote.RemoteWebDriver;

public class BodManagerWebDriver {

  private final RemoteWebDriver driver;

  public BodManagerWebDriver(RemoteWebDriver driver) {
    this.driver = driver;
  }

  public void managerDashboard() {
    driver.get(BodWebDriver.URL_UNDER_TEST + "manager");
  }

  public void verifyManagerLabelChanged(String networkElementPk, String managerLabel) {
    ListPhysicalPortsPage listPage = ListPhysicalPortsPage.get(driver);

    listPage.findRow(networkElementPk, managerLabel);
  }

  public void createNewVirtualPortForPhysicalPort(String networkElementPk) {
    ListPhysicalPortsPage listPage = ListPhysicalPortsPage.get(driver);

    listPage.newVirtualPort(networkElementPk);
  }

  public void verifyPhysicalPortSelected(String managerLabel) {
    NewVirtualPortPage page = NewVirtualPortPage.get(driver);

    String portName = page.getSelectedPhysicalPort();

    assertThat(portName, is(managerLabel));
  }

  public void changeManagerLabelOfPhyiscalPort(String networkElementPk, String managerLabel) {
    ListPhysicalPortsPage page = ListPhysicalPortsPage.get(driver, BodWebDriver.URL_UNDER_TEST);

    EditPhysicalPortPage editPage = page.edit(networkElementPk);

    editPage.sendMagerLabel(managerLabel);
    editPage.save();
  }

  public void verifyOnEditPhysicalResourceGroupPage(String expectedMailAdress) {
    EditPhysicalResourceGroupPage page = EditPhysicalResourceGroupPage.get(driver);

    String email = page.getEmailValue();
    assertThat(email, is(expectedMailAdress));

    assertThat(page.getInfoMessages(), hasSize(1));
    assertThat(page.getInfoMessages().get(0), containsString("Your Physical Resource Group is not activated"));
  }

  public void createNewVirtualPort(String name, String maxBandwidth) {
    NewVirtualPortPage page = NewVirtualPortPage.get(driver, BodWebDriver.URL_UNDER_TEST);

    page.sendName(name);
    page.sendMaxBandwidth(maxBandwidth);
    page.save();
  }

  public void verifyVirtualPortWasCreated(String name, String maxBandwidth) {
    ListVirtualPortPage page = ListVirtualPortPage.get(driver);

    String table = page.getTable();

    assertThat(table, containsString(name));
    assertThat(table, containsString(maxBandwidth));
  }

  public void deleteVirtualPort(String name) {
    ListVirtualPortPage page = ListVirtualPortPage.get(driver, BodWebDriver.URL_UNDER_TEST);

    page.delete(name);
  }

  public void verifyVirtualPortWasDeleted(String name) {
    ListVirtualPortPage page = ListVirtualPortPage.get(driver);

    try {
      page.findRow(name);
      fail(String.format("Virtual port with name %s was not deleted", name));
    }
    catch (NoSuchElementException e) {
      // fine
    }
  }

  public NewVirtualResourceGroupPage createNewVirtualResourceGroup(String name) throws Exception {
    NewVirtualResourceGroupPage page = NewVirtualResourceGroupPage.get(driver, BodWebDriver.URL_UNDER_TEST);
    page.sendName(name);
    page.sendSurfConextGroupName(name);
    page.save();

    return page;
  }

  public void deleteVirtualResourceGroup(String vrgName) {
    ListVirtualResourceGroupPage page = ListVirtualResourceGroupPage.get(driver, BodWebDriver.URL_UNDER_TEST);

    page.delete(vrgName);
  }

  public void verifyVirtualResourceGroupWasCreated(String name) {
    ListVirtualResourceGroupPage page = ListVirtualResourceGroupPage.get(driver);
    page.findRow(name);
  }

  public void verifyVirtualResourceGroupWasDeleted(String vrgName) {
    ListVirtualResourceGroupPage page = ListVirtualResourceGroupPage.get(driver);
    try {
      page.findRow(vrgName);
      fail(String.format("Virtual Resource group with vrgName %s was not deleted", vrgName));
    }
    catch (NoSuchElementException e) {
      // fine
    }
  }

  public void verifyNewVirtualResoruceGroupHasValidationError() {
    NewVirtualResourceGroupPage page = NewVirtualResourceGroupPage.get(driver);

    assertTrue(page.hasNameValidationError());
  }

  public void verifyNewVirtualPortHasPhysicalResourceGroup(String instituteName) {
    NewVirtualPortPage page = NewVirtualPortPage.get(driver);

    String group = page.getSelectedPhysicalResourceGroup();

    assertThat(group, is(instituteName));
  }

}
