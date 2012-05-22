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
package nl.surfnet.bod.support;

import static junit.framework.Assert.fail;
import static nl.surfnet.bod.support.BodWebDriver.URL_UNDER_TEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import nl.surfnet.bod.pages.manager.EditPhysicalPortPage;
import nl.surfnet.bod.pages.manager.EditPhysicalResourceGroupPage;
import nl.surfnet.bod.pages.manager.EditVirtualPortPage;
import nl.surfnet.bod.pages.manager.ListPhysicalPortsPage;
import nl.surfnet.bod.pages.manager.ListReservationPage;
import nl.surfnet.bod.pages.manager.ListVirtualPortPage;
import nl.surfnet.bod.pages.manager.ListVirtualResourceGroupPage;
import nl.surfnet.bod.pages.manager.ManagerOverviewPage;
import nl.surfnet.bod.pages.manager.NewVirtualPortPage;
import nl.surfnet.bod.pages.noc.ListPhysicalResourceGroupPage;
import nl.surfnet.bod.pages.noc.NocOverviewPage;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.remote.RemoteWebDriver;

public class BodManagerWebDriver {

  private final RemoteWebDriver driver;

  public BodManagerWebDriver(RemoteWebDriver driver) {
    this.driver = driver;
  }

  public void verifyManagerLabelChanged(String networkElementPk, String managerLabel) {
    ListPhysicalPortsPage listPage = ListPhysicalPortsPage.get(driver);

    listPage.findRow(networkElementPk, managerLabel);
  }

  public void verifyPhysicalPortSelected(String managerLabel) {
    NewVirtualPortPage page = NewVirtualPortPage.get(driver);

    String portName = page.getSelectedPhysicalPort();

    assertThat(portName, is(managerLabel));
  }

  public void changeManagerLabelOfPhyiscalPort(String networkElementPk, String managerLabel) {
    ListPhysicalPortsPage page = ListPhysicalPortsPage.get(driver, URL_UNDER_TEST);

    EditPhysicalPortPage editPage = page.edit(networkElementPk);

    editPage.sendMagerLabel(managerLabel);
    editPage.save();
  }

  public void verifyOnEditPhysicalResourceGroupPage(String expectedMailAdress) {
    EditPhysicalResourceGroupPage page = EditPhysicalResourceGroupPage.get(driver);

    String email = page.getEmailValue();
    assertThat(email, is(expectedMailAdress));

    assertThat(page.getInfoMessages(), hasSize(1));
    assertThat(page.getInfoMessages().get(0), containsString("Your institute is not activated"));
  }

  public void verifyVirtualPortExists(String... fields) {
    ListVirtualPortPage page = ListVirtualPortPage.get(driver, URL_UNDER_TEST);

    page.findRow(fields);
  }

  public void deleteVirtualPort(String name) {
    ListVirtualPortPage page = ListVirtualPortPage.get(driver, URL_UNDER_TEST);

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

  public void deleteVirtualResourceGroup(String vrgName) {
    ListVirtualResourceGroupPage page = ListVirtualResourceGroupPage.get(driver, URL_UNDER_TEST);

    page.delete(vrgName);
  }

  public void verifyVirtualResourceGroupExists(String... fields) {
    ListVirtualResourceGroupPage page = ListVirtualResourceGroupPage.get(driver, URL_UNDER_TEST);
    page.findRow(fields);
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

  public void verifyNewVirtualPortHasProperties(String instituteName, Integer bandwidth) {
    NewVirtualPortPage page = NewVirtualPortPage.get(driver);

    String group = page.getSelectedPhysicalResourceGroup();
    Integer ban = page.getBandwidth();

    assertThat(group, is(instituteName));
    assertThat(ban, is(bandwidth));
  }

  public void editVirtualPort(String orignalName, String newName, int bandwidth, String vlanId) {
    ListVirtualPortPage listPage = ListVirtualPortPage.get(driver, URL_UNDER_TEST);

    EditVirtualPortPage editPage = listPage.edit(orignalName);

    editPage.sendName(newName);
    editPage.sendMaxBandwidth(bandwidth);
    editPage.sendVlanId(vlanId);

    editPage.save();
  }

  public void verifyReservationIsCancellable(String reservationLabel, LocalDate startDate, LocalDate endDate,
      LocalTime startTime, LocalTime endTime) {

    ListReservationPage page = ListReservationPage.get(driver, URL_UNDER_TEST);

    page.verifyReservationIsCancellable(reservationLabel, startDate, endDate, startTime, endTime);
  }

  public void verifyStatistics() {
    ManagerOverviewPage page = ManagerOverviewPage.get(driver, URL_UNDER_TEST);

    page.findRow("Physical ports", "2");
    page.findRow("Virtual ports", "2");
    page.findRow("Elapsed reservations", "0");
    page.findRow("Active reservations", "0");
    page.findRow("Coming reservations", "1");
  }

  public void createVirtualPort(String name) {
    NewVirtualPortPage page = NewVirtualPortPage.get(driver);
    page.sendName(name);

    page.save();
  }

  public void declineVirtualPort(String message) {
    NewVirtualPortPage page = NewVirtualPortPage.get(driver);

    page.decline();
    page.sendDeclineMessage(message);

    page.save();
  }

  public void verifyPhysicalResourceGroupExists(String... fields) {
    ListPhysicalResourceGroupPage page = ListPhysicalResourceGroupPage.get(driver, URL_UNDER_TEST);

    page.findRow(fields);
  }

  public void switchToNoc() {
    swithTo("NOC Engineer");
  }

  public void switchToUser() {
    swithTo("User");
  }

  private void swithTo(String role) {
    NocOverviewPage page = NocOverviewPage.get(driver, URL_UNDER_TEST);

    page.clickSwitchRole(role);
  }

  public void verifyReservationWasCreated(String reservationLabel, LocalDate startDate, LocalDate endDate,
      LocalTime startTime, LocalTime endTime) {

    ListReservationPage page = ListReservationPage.get(driver, URL_UNDER_TEST);

    page.verifyReservationWasCreated(reservationLabel, startDate, endDate, startTime, endTime);
  }

  public void verifyReservationIsNotCancellable(String reservationLabel, LocalDate startDate, LocalDate endDate,
      LocalTime startTime, LocalTime endTime) {

    ListReservationPage page = ListReservationPage.get(driver, URL_UNDER_TEST);

    page.verifyReservationIsNotCancellable(reservationLabel, startDate, endDate, startTime, endTime, "state cannot");
  }

}
