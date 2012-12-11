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
package nl.surfnet.bod.support;

import static junit.framework.Assert.fail;
import static nl.surfnet.bod.support.BodWebDriver.URL_UNDER_TEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import nl.surfnet.bod.pages.manager.*;

import org.apache.commons.lang.ArrayUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.remote.RemoteWebDriver;

public class BodManagerWebDriver {

  private final RemoteWebDriver driver;

  public BodManagerWebDriver(RemoteWebDriver driver) {
    this.driver = driver;
  }

  public void verifyManagerLabelChanged(String nmsPortId, String managerLabel) {
    ListPhysicalPortsPage listPage = ListPhysicalPortsPage.get(driver);

    listPage.findRow(nmsPortId, managerLabel);
  }

  public void verifyPhysicalPortSelected(String managerLabel) {
    NewVirtualPortPage page = NewVirtualPortPage.get(driver);

    String portName = page.getSelectedPhysicalPort();

    assertThat(portName, is(managerLabel));
  }

  public void changeManagerLabelOfPhyiscalPort(String nmsPortId, String managerLabel) {
    ListPhysicalPortsPage page = ListPhysicalPortsPage.get(driver, URL_UNDER_TEST);

    EditPhysicalPortPage editPage = page.edit(nmsPortId);

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

  public void verifyVirtualResourceGroupsEmpty() {
    ListVirtualPortPage page = ListVirtualPortPage.get(driver, URL_UNDER_TEST);

    assertThat(page.isTableEmpty(), is(true));
  }

  public void deleteVirtualPort(String name) {
    ListVirtualPortPage page = ListVirtualPortPage.get(driver, URL_UNDER_TEST);

    page.delete(name);
  }

  public void deleteVirtualPortAndVerifyAlertText(String name, String alertText) {
    ListVirtualPortPage page = ListVirtualPortPage.get(driver, URL_UNDER_TEST);

    page.deleteAndVerifyAlert(alertText, name);
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

  public void verifyNewVirtualPortHasProperties(String instituteName, String userLabel, Integer bandwidth) {
    NewVirtualPortPage page = NewVirtualPortPage.get(driver);

    assertThat(page.getUserLabel(), is(userLabel));
    assertThat(page.getSelectedPhysicalResourceGroup(), is(instituteName));
    assertThat(page.getBandwidth(), is(bandwidth));
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
    DashboardPage page = DashboardPage.get(driver, URL_UNDER_TEST);

    page.findRow("Physical ports", "2");
    page.findRow("Virtual ports", "2");
    page.findRow("Reservations past", "0");
    page.findRow("Active reservations", "0");
    page.findRow("Reservations in", "1");
  }

  public void verifyLogEventExistsCreatedWithin(int seconds, String... fields) {
    ListLogEventsPage page = ListLogEventsPage.get(driver, URL_UNDER_TEST);

    page.logEventShouldBe(DateTime.now(), seconds, fields);
  }

  public void verifyLogEventExists(String... fields) {
    verifyLogEventExistsCreatedWithin(-1, fields);
  }

  public void verifyLogEventDoesNotExist(String... fields) {
    ListLogEventsPage page = ListLogEventsPage.get(driver, URL_UNDER_TEST);
    page.verifyRowWithLabelDoesNotExist(fields);
  }

  public void createVirtualPort(String name) {
    createVirtualPort(name, null);
  }

  public void createVirtualPort(String name, String vpUserLabel) {
    NewVirtualPortPage page = NewVirtualPortPage.get(driver);
    page.sendName(name);
    page.sendUserLabel(vpUserLabel);
    page.sendVlanId("23");

    page.save();
  }

  public void acceptVirtualPort(String label) {
    NewVirtualPortPage page = NewVirtualPortPage.get(driver);

    page.sendName(label);
    page.sendVlanId("23");
    page.accept();
    page.save();
  }

  public void declineVirtualPort(String message) {
    NewVirtualPortPage page = NewVirtualPortPage.get(driver);

    page.decline();
    page.sendDeclineMessage(message);

    page.save();
  }

  public void switchToNoc() {
    switchTo("NOC Engineer");
  }

  public void switchToUser() {
    switchTo("User");
  }

  public void switchToManager(String manager) {
    switchTo("BoD Administrator", manager);
  }

  private void switchTo(String... role) {
    DashboardPage page = DashboardPage.get(driver, URL_UNDER_TEST);

    page.clickSwitchRole(role);
  }

  public void verifyReservationWasCreated(String reservationLabel, LocalDate startDate, LocalDate endDate,
      LocalTime startTime, LocalTime endTime) {

    ListReservationPage page = ListReservationPage.get(driver, URL_UNDER_TEST);

    page.verifyReservationExists(reservationLabel, startDate, endDate, startTime, endTime);
  }

  public void verifyReservationIsNotCancellable(String reservationLabel, LocalDate startDate, LocalDate endDate,
      LocalTime startTime, LocalTime endTime) {

    ListReservationPage page = ListReservationPage.get(driver, URL_UNDER_TEST);

    page.verifyReservationIsNotCancellable(reservationLabel, startDate, endDate, startTime, endTime, "state cannot");
  }

  public void verifyPhysicalPortHasEnabledUnallocateIcon(String nmsPortId, String nocLabel) {
    ListPhysicalPortsPage page = ListPhysicalPortsPage.get(driver, URL_UNDER_TEST);

    page.verifyPhysicalPortHasEnabledUnallocateIcon(nmsPortId, nocLabel);
  }

  public void verifyPhysicalPortHasDisabeldUnallocateIcon(String nmsPortId, String nocLabel, String toolTipText) {
    ListPhysicalPortsPage page = ListPhysicalPortsPage.get(driver, URL_UNDER_TEST);

    page.verifyPhysicalPortHasDisabledUnallocateIcon(nmsPortId, nocLabel, toolTipText);
  }

  public void verifyTeamToVirtualPortsLink(String teamName) {
    ListVirtualResourceGroupPage vrgListPage = ListVirtualResourceGroupPage.get(driver, URL_UNDER_TEST);

    int numberOfItems = vrgListPage.getNumberFromRowWithLinkAndClick(teamName, "manager/virtualports", "Show");

    ListVirtualPortPage vpListPage = ListVirtualPortPage.get(driver);
    vpListPage.verifyIsCurrentPage();
    vpListPage.verifyAmountOfRowsWithLabel(numberOfItems, teamName);
  }

  public void verifyPhysicalPortToVirtualPortsLink(String physicalPortBodAdminLabel, String virtualPortBodAdminLabel) {
    ListPhysicalPortsPage ppListPage = ListPhysicalPortsPage.get(driver, URL_UNDER_TEST);

    int numberOfItems = ppListPage.getNumberFromRowWithLinkAndClick(physicalPortBodAdminLabel, "manager/virtualports",
        "Show");
    ListVirtualPortPage vpListPage = ListVirtualPortPage.get(driver);
    vpListPage.verifyIsCurrentPage();
    vpListPage.verifyAmountOfRowsWithLabel(numberOfItems, virtualPortBodAdminLabel);
  }

  public void verifyDashboardToPhysicalPortsLink() {
    DashboardPage dashboardPage = DashboardPage.get(driver, URL_UNDER_TEST);

    int expectedAmount = dashboardPage.getNumberFromRowWithLinkAndClick("Physical", "manager/physicalports", "Show");

    ListPhysicalPortsPage listPhysicalPorts = ListPhysicalPortsPage.get(driver, URL_UNDER_TEST);
    listPhysicalPorts.verifyIsCurrentPage();
    listPhysicalPorts.verifyAmountOfRowsWithLabel(expectedAmount, ArrayUtils.EMPTY_STRING_ARRAY);
  }

  public void verifyDashboardToElapsedReservationsLink() {
    DashboardPage dashboardPage = DashboardPage.get(driver, URL_UNDER_TEST);

    int expectedAmount = dashboardPage.getNumberFromRowWithLinkAndClick("past", "manager/reservations/filter/elapsed",
        "Show");

    ListReservationPage reservationsPage = ListReservationPage.get(driver, URL_UNDER_TEST);
    reservationsPage.verifyIsCurrentPage();
    reservationsPage.filterReservations(ReservationFilterViewFactory.ELAPSED);
    reservationsPage.verifyAmountOfRowsWithLabel(expectedAmount, ArrayUtils.EMPTY_STRING_ARRAY);
  }

  /**
   * Not possible to create active reservations without timing issues
   */
  public void verifyDashboardToActiveReservationsLink() {
  }

  public void verifyDashboardToComingReservationsLink() {
    DashboardPage dashboardPage = DashboardPage.get(driver, URL_UNDER_TEST);

    int expectedAmount = dashboardPage.getNumberFromRowWithLinkAndClick("in", "manager/reservations/filter/coming",
        "Show");
    ListReservationPage reservationsPage = ListReservationPage.get(driver, URL_UNDER_TEST);
    reservationsPage.verifyIsCurrentPage();
    reservationsPage.filterReservations(ReservationFilterViewFactory.COMING);
    reservationsPage.verifyAmountOfRowsWithLabel(expectedAmount, ArrayUtils.EMPTY_STRING_ARRAY);
  }

  /**
   * Not possible to create unaligned port, depends on NMS
   */
  public void verifyDashboardToUnalignedPhysicalPortsLink() {

  }

  public void verifyMenu() {
    DashboardPage dashboardPage = DashboardPage.get(driver, URL_UNDER_TEST);
    dashboardPage.verifyNumberOfMenuItems();
    dashboardPage.verifyMenuOverview();
    dashboardPage.verifyMenuReservations();
    dashboardPage.verifyMenuTeams();
    dashboardPage.verifyMenuVirtualPorts();
    dashboardPage.verifyMenuPhysicalPorts();
    dashboardPage.verifyMenuLogEvents();
  }

}
