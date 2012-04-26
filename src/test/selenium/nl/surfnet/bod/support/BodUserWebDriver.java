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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import nl.surfnet.bod.domain.ReservationStatus;
import nl.surfnet.bod.pages.user.*;

import org.hamcrest.core.CombinableMatcher;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

public class BodUserWebDriver {

  private final RemoteWebDriver driver;

  public BodUserWebDriver(RemoteWebDriver driver) {
    this.driver = driver;
  }

  public void requestVirtualPort(String team) {
    RequestNewVirtualPortSelectTeamPage page = RequestNewVirtualPortSelectTeamPage.get(driver,
        BodWebDriver.URL_UNDER_TEST);

    page.selectInstitute(team);
  }

  public void verifyReservationWasCreated(LocalDate startDate, LocalDate endDate, LocalTime startTime,
      LocalTime endTime, LocalDateTime creationDateTime) {
    ListReservationPage page = ListReservationPage.get(driver);

    String start = BodWebDriver.RESERVATION_DATE_TIME_FORMATTER.print(startDate.toLocalDateTime(startTime));
    String end = BodWebDriver.RESERVATION_DATE_TIME_FORMATTER.print(endDate.toLocalDateTime(endTime));
    String creation = BodWebDriver.RESERVATION_DATE_TIME_FORMATTER.print(creationDateTime);

    WebElement row = page.findRow(start, end, creation);

    assertThat(
        row.getText(),
        CombinableMatcher.<String> either(containsString(ReservationStatus.REQUESTED.name())).or(
            containsString(ReservationStatus.SCHEDULED.name())));
  }

  public void verifyReservationStartDateHasError(String string) {
    NewReservationPage page = NewReservationPage.get(driver);
    String error = page.getStartDateError();

    assertThat(error, containsString(string));
  }

  public void cancelReservation(LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime) {
    ListReservationPage page = ListReservationPage.get(driver, BodWebDriver.URL_UNDER_TEST);

    page.deleteByDates(startDate, endDate, startTime, endTime);
  }

  public void verifyReservationWasCanceled(LocalDate startDate, LocalDate endDate, LocalTime startTime,
      LocalTime endTime) {
    ListReservationPage page = ListReservationPage.get(driver);

    page.reservationShouldBe(startDate, endDate, startTime, endTime, ReservationStatus.CANCELLED);
  }

  public void selectInstituteAndRequest(String institute, Integer bandwidth, String message) {
    RequestNewVirtualPortSelectInstitutePage page = RequestNewVirtualPortSelectInstitutePage.get(driver);

    RequestNewVirtualPortRequestPage requestPage = page.selectInstitute(institute);

    requestPage.sendMessage(message);
    requestPage.sendBandwidth("" + bandwidth);
    requestPage.sentRequest();
  }

  public void verifyRequestVirtualPortInstituteInactive(String instituteName) {
    RequestNewVirtualPortSelectInstitutePage page = RequestNewVirtualPortSelectInstitutePage.get(driver);

    WebElement row = page.findRow(instituteName);
    assertThat(row.getText(), containsString("Not active"));
  }

  public void createNewReservation(LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime) {
    NewReservationPage page = NewReservationPage.get(driver, BodWebDriver.URL_UNDER_TEST);

    page.sendStartDate(startDate);
    page.sendStartTime(startTime);
    page.sendEndDate(endDate);
    page.sendEndTime(endTime);
    page.sendBandwidth("500");

    page.save();
  }

  public void editVirtualPort(String oldLabel, String newLabel) {
    ListVirtualPortPage listPage = ListVirtualPortPage.get(driver, BodWebDriver.URL_UNDER_TEST);

    EditVirtualPortPage editPage = listPage.edit(oldLabel);
    editPage.sendUserLabel(newLabel);
    editPage.save();
  }

  public void verifyVirtualPortExists(String... fields) {
    ListVirtualPortPage listPage = ListVirtualPortPage.get(driver);

    listPage.findRow(fields);
  }

  public void switchToNoc() {
    switchTo("NOC Engineer");
  }

  public void switchToManager() {
    switchTo("BoD Manager");
  }

  private void switchTo(String role) {
    UserOverviewPage page = UserOverviewPage.get(driver, BodWebDriver.URL_UNDER_TEST);

    page.clickSwitchRole(role);
  }

}