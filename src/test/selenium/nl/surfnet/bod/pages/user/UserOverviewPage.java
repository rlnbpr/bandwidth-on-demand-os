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
package nl.surfnet.bod.pages.user;

import nl.surfnet.bod.pages.AbstractListPage;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.PageFactory;

public class UserOverviewPage extends AbstractListPage {

  private UserOverviewPage(RemoteWebDriver driver) {
    super(driver);
  }

  public static UserOverviewPage get(RemoteWebDriver driver, String urlUnderTest) {
    driver.get(urlUnderTest + "/user");
    UserOverviewPage page = new UserOverviewPage(driver);
    PageFactory.initElements(driver, page);

    return page;
  }

  public void selectInstitute(String team) {
    try {
      driver.findElementByPartialLinkText(team).click();
    }
    catch (NoSuchElementException e) {
      clickRowIcon("icon-envelope", team);
    }
  }

}
