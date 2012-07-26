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

import static nl.surfnet.bod.support.BodWebDriver.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.hamcrest.Matchers;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Assert;
import org.openqa.selenium.remote.RemoteWebDriver;

import nl.surfnet.bod.domain.ReservationStatus;
import nl.surfnet.bod.pages.user.EditVirtualPortPage;
import nl.surfnet.bod.pages.user.ListReservationPage;
import nl.surfnet.bod.pages.user.ListVirtualPortPage;
import nl.surfnet.bod.pages.user.NewReservationPage;
import nl.surfnet.bod.pages.user.RequestNewVirtualPortRequestPage;
import nl.surfnet.bod.pages.user.RequestNewVirtualPortSelectInstitutePage;
import nl.surfnet.bod.pages.user.RequestNewVirtualPortSelectTeamPage;
import nl.surfnet.bod.pages.user.UserOverviewPage;

public class BodUserWebDriver {

  private final RemoteWebDriver driver;

  public BodUserWebDriver(RemoteWebDriver driver) {
    this.driver = driver;
  }

  public void requestVirtualPort(String team) {
    UserOverviewPage page = UserOverviewPage.get(driver, URL_UNDER_TEST);

    page.selectInstitute(team);
  }

  public void verifyReservationStartDateHasError(String string) {
    NewReservationPage page = NewReservationPage.get(driver);
    String error = page.getStartDateError();

    assertThat(error, containsString(string));
  }

  public void cancelReservation(LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime) {
    ListReservationPage page = ListReservationPage.get(driver, URL_UNDER_TEST);

    page.deleteByDates(startDate, endDate, startTime, endTime);
  }

  public void verifyReservationWasCanceled(LocalDate startDate, LocalDate endDate, LocalTime startTime,
      LocalTime endTime) {
    ListReservationPage page = ListReservationPage.get(driver);

    page.reservationShouldBe(startDate, endDate, startTime, endTime, ReservationStatus.CANCELLED);
  }

  public void selectTeamInstituteAndRequest(String team, String institute, String userLabel, Integer bandwidth, String message) {
    ListVirtualPortPage page = ListVirtualPortPage.get(driver, URL_UNDER_TEST);

    RequestNewVirtualPortSelectTeamPage selectTeamPage = page.requestVirtualPort();
    selectTeamPage.selectTeam(team);

    selectInstituteAndRequest(institute, userLabel, bandwidth, message);
  }

  public void selectInstituteAndRequest(String institute, String userLabel, Integer bandwidth, String message) {
    RequestNewVirtualPortSelectInstitutePage page = RequestNewVirtualPortSelectInstitutePage.get(driver);

    RequestNewVirtualPortRequestPage requestPage = page.selectInstitute(institute);

    requestPage.sendUserLabel(userLabel);
    requestPage.sendBandwidth("" + bandwidth);
    requestPage.sendMessage(message);
    requestPage.sentRequest();
  }

  public void selectInstituteAndRequest(String institute, Integer bandwidth, String message) {
    selectInstituteAndRequest(institute, "", bandwidth, message);
  }

  public void verifyRequestVirtualPortInstituteInactive(String instituteName) {
    RequestNewVirtualPortSelectInstitutePage page = RequestNewVirtualPortSelectInstitutePage.get(driver);

    try {
      page.selectInstitute(instituteName);
      Assert.fail("Found a link for institute " + instituteName);
    }
    catch (org.openqa.selenium.NoSuchElementException e) {
      // expected
    }
  }

  public void createNewReservation(String label, LocalDate startDate, LocalDate endDate, LocalTime startTime,
      LocalTime endTime) {
    NewReservationPage page = NewReservationPage.get(driver, URL_UNDER_TEST);

    page.sendLabel(label);
    page.sendStartDate(startDate);
    page.sendStartTime(startTime);
    page.sendEndDate(endDate);
    page.sendEndTime(endTime);
    page.sendBandwidth("500");

    page.save();
  }

  public void createNewReservation(String label) {
    NewReservationPage page = NewReservationPage.get(driver, URL_UNDER_TEST);

    page.sendLabel(label);
    page.clickStartNow();
    page.clickForever();

    page.save();

  }

  public void editVirtualPort(String oldLabel, String newLabel) {
    ListVirtualPortPage listPage = ListVirtualPortPage.get(driver, URL_UNDER_TEST);

    EditVirtualPortPage editPage = listPage.edit(oldLabel);
    editPage.sendUserLabel(newLabel);
    editPage.save();
  }

  public void verifyVirtualPortExists(String... fields) {
    ListVirtualPortPage listPage = ListVirtualPortPage.get(driver, URL_UNDER_TEST);

    listPage.findRow(fields);
  }

  public void switchToNoc() {
    switchTo("NOC Engineer");
  }

  public void switchToManager(String manager) {
    switchTo("BoD Administrator", manager);
  }

  private void switchTo(String... role) {
    UserOverviewPage page = UserOverviewPage.get(driver, URL_UNDER_TEST);

    page.clickSwitchRole(role);
  }

  public void verifyReservationIsCancellable(String reservationLabel, LocalDate startDate, LocalDate endDate,
      LocalTime startTime, LocalTime endTime) {

    ListReservationPage page = ListReservationPage.get(driver);

    page.verifyReservationIsCancellable(reservationLabel, startDate, endDate, startTime, endTime);
  }

  public void verifyReservationWasCreated(String reservationLabel, LocalDate startDate, LocalDate endDate,
      LocalTime startTime, LocalTime endTime) {
    ListReservationPage page = ListReservationPage.get(driver);

    page.verifyReservationExists(reservationLabel, startDate, endDate, startTime, endTime);
  }

  public void verifyReservationWasCreated(String label) {
    ListReservationPage page = ListReservationPage.get(driver);

    assertThat(page.getInfoMessages(), Matchers.<String> hasItem(containsString(label)));

    page.verifyReservationExists(label);
  }

  public void verifyNotMemberOf(String teamName) {
    UserOverviewPage page = UserOverviewPage.get(driver, URL_UNDER_TEST);

    assertThat(page.getTeams(), not(hasItem(teamName)));
  }

  public void verifyMemberOf(String teamName) {
    UserOverviewPage page = UserOverviewPage.get(driver, URL_UNDER_TEST);

    assertThat(page.getTeams(), hasItem(teamName));
  }

}
