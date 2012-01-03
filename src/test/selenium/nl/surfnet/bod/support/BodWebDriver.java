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

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.io.File;
import java.util.concurrent.TimeUnit;

import nl.surfnet.bod.domain.PhysicalResourceGroup;
import nl.surfnet.bod.pages.physical.ListPhysicalResourceGroupPage;
import nl.surfnet.bod.pages.physical.NewPhysicalResourceGroupPage;
import nl.surfnet.bod.pages.virtual.ListVirtualPortPage;
import nl.surfnet.bod.pages.virtual.ListVirtualResourceGroupPage;
import nl.surfnet.bod.pages.virtual.NewVirtualPortPage;
import nl.surfnet.bod.pages.virtual.NewVirtualResourceGroupPage;

import org.hamcrest.Matcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.google.common.io.Files;

public class BodWebDriver {

  private static final String URL_UNDER_TEST = withEndingSlash(System.getProperty("selenium.test.url",
      "http://localhost:8080/bod"));

  private FirefoxDriver driver;

  public synchronized void initializeOnce() {
    if (driver == null) {
      this.driver = new FirefoxDriver();
      this.driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          if (driver != null) {
            driver.quit();
          }
        }
      });
    }
  }

  public void takeScreenshot(File screenshot) throws Exception {
    if (driver != null) {
      File temp = driver.getScreenshotAs(OutputType.FILE);
      Files.copy(temp, screenshot);
    }
  }

  public void createNewPhysicalGroup(String name) throws Exception {
    NewPhysicalResourceGroupPage page = NewPhysicalResourceGroupPage.get(driver, URL_UNDER_TEST);
    page.sendName(name);
    page.sendInstitution("Utrecht");

    page.save();
  }

  private static String withEndingSlash(String path) {
    return path.endsWith("/") ? path : path + "/";
  }

  public void deletePhysicalGroup(PhysicalResourceGroup group) {
    ListPhysicalResourceGroupPage page = ListPhysicalResourceGroupPage.get(driver, URL_UNDER_TEST);

    page.deleteByName(group.getName());
  }

  public void verifyGroupWasCreated(String name) {
    assertListTable(containsString(name));
  }

  public void verifyGroupWasDeleted(PhysicalResourceGroup group) {
    assertListTable(not(containsString(group.getName())));
  }

  private void assertListTable(Matcher<String> tableMatcher) {
    ListPhysicalResourceGroupPage page = ListPhysicalResourceGroupPage.get(driver);
    String row = page.getTable();

    assertThat(row, tableMatcher);
  }

  public NewVirtualResourceGroupPage createNewVirtualResourceGroup(String name) throws Exception {
    NewVirtualResourceGroupPage page = NewVirtualResourceGroupPage.get(driver, URL_UNDER_TEST);
    page.sendName(name);
    page.sendSurfConextGroupName(name);
    page.save();

    return page;
  }

  public void deleteVirtualResourceGroup(String vrgName) {
    ListVirtualResourceGroupPage page = ListVirtualResourceGroupPage.get(driver, URL_UNDER_TEST);

    page.deleteByName(vrgName);
  }

  public void verifyVirtualResourceGroupWasCreated(String name) {
    assertVirtualResourceGroupListTable(containsString(name));
  }

  public void verifyVirtualResourceGroupWasDeleted(String vrgName) {
    assertVirtualResourceGroupListTable(not(containsString(vrgName)));
  }

  private void assertVirtualResourceGroupListTable(Matcher<String> tableMatcher) {
    ListVirtualResourceGroupPage page = ListVirtualResourceGroupPage.get(driver);
    String row = page.getTable();

    assertThat(row, tableMatcher);
  }

  public void verifyHasValidationError() {
    NewVirtualResourceGroupPage page = NewVirtualResourceGroupPage.get(driver);

    assertTrue(page.hasNameValidationError());
  }

  public void createNewVirtualPort(String name) {
    NewVirtualPortPage page = NewVirtualPortPage.get(driver, URL_UNDER_TEST);

    page.sendName(name);
    page.save();
  }

  public void verifyVirtualPortWasCreated(String name) {
    ListVirtualPortPage page = ListVirtualPortPage.get(driver);

    String table = page.getTable();

    assertThat(table, containsString(name));
  }

}
