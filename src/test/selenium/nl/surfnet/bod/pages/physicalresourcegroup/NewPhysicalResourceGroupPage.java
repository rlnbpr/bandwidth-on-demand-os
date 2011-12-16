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
package nl.surfnet.bod.pages.physicalresourcegroup;

import nl.surfnet.bod.support.Probes;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class NewPhysicalResourceGroupPage {

  private static final String PAGE = "noc/physicalresourcegroups/create";

  private final Probes probes;

  @FindBy(id = "_name_id")
  private WebElement nameInput;

  @FindBy(css = "input[name='institutionName_search']")
  private WebElement institutionInput;

  @FindBy(css = "input[type='submit']")
  private WebElement saveButton;

  public NewPhysicalResourceGroupPage(WebDriver driver) {
    this.probes = new Probes(driver);
  }

  public static NewPhysicalResourceGroupPage get(RemoteWebDriver driver, String host) {
    driver.get(host + PAGE);
    NewPhysicalResourceGroupPage page = new NewPhysicalResourceGroupPage(driver);
    PageFactory.initElements(driver, page);

    return page;
  }

  public void sendName(String name) {
    nameInput.clear();
    nameInput.sendKeys(name);
  }

  public void sendInstitution(String institution) throws Exception {
    institutionInput.clear();
    institutionInput.sendKeys(institution);

    probes.assertTextPresent(By.className("as-results"), institution);

    institutionInput.sendKeys("\t");
    institutionInput.sendKeys("\n");
  }

  public void save() {
    saveButton.click();
  }

}
