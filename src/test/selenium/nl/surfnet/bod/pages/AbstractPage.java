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
package nl.surfnet.bod.pages;

import java.util.List;

import org.joda.time.LocalDateTime;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import nl.surfnet.bod.support.Probes;

public class AbstractPage {

  private final RemoteWebDriver driver;

  private final Probes probes;

  @FindBy(id = "alerts")
  private WebElement messagesDiv;

  @FindBy(css = ".user-box .dropdown-toggle")
  private WebElement userBox;

  public AbstractPage(RemoteWebDriver driver) {
    this.driver = driver;
    probes = new Probes(driver);
  }

  public List<String> getInfoMessages() {
    List<WebElement> messageDivs = messagesDiv.findElements(By.className("alert-info"));
    return Lists.transform(messageDivs, new Function<WebElement, String>() {
      @Override
      public String apply(WebElement input) {
        return input.getText();
      }
    });
  }

  public void clickSwitchRole(String... roleNames) {
    userBox.click();

    List<WebElement> roles = userBox.findElements(By.tagName("li"));
    for (WebElement role : roles) {
      if (containsAll(role.getText(), roleNames)) {
        role.click();
        return;
      }
    }

    throw new NoSuchElementException("Could not find role with name " + Joiner.on(", ").join(roleNames) + " in "
        + Joiner.on(", ").join(Iterables.transform(roles, new Function<WebElement, String>() {
          @Override
          public String apply(WebElement input) {
            return input.getText();
          }
        })));
  }

  /**
   * Gets a time stamp from the specific row starting with the given year
   *
   * @param year
   *          Timestamp should start with this year
   * @param row
   *          Row to search
   * @return {@link LocalDateTime} time stamp
   */
  protected LocalDateTime getLocalDateTimeFromRow(WebElement row) {
    Optional<LocalDateTime> extractedDateTime = PageUtils.extractDateTime(row.getText());
    if (!extractedDateTime.isPresent()) {
      throw new AssertionError("Could not find date time form row: " + row.getText());
    }
    return extractedDateTime.get();
  }

  protected Probes getProbes() {
    return probes;
  }

  protected RemoteWebDriver getDriver() {
    return driver;
  }

  private boolean containsAll(String input, String[] needles) {
    for (String needle : needles) {
      if (!input.contains(needle)) {
        return false;
      }
    }
    return true;
  }
}
