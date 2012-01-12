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
package nl.surfnet.bod.idd;

import java.util.Arrays;
import java.util.Collection;

import nl.surfnet.bod.idd.generated.InvoerKlant;
import nl.surfnet.bod.idd.generated.Klanten;
import nl.surfnet.bod.idd.generated.KsrBindingStub;
import nl.surfnet.bod.idd.generated.KsrLocator;

import org.springframework.beans.factory.annotation.Value;

public class IddLiveClient implements IddClient {

  private static final String IDD_VERSION = "1.09";

  @Value("${idd.user}")
  private String username;

  @Value("${idd.password}")
  private String password;

  @Value("${idd.url}")
  private String endPoint;

  @Override
  public Collection<Klanten> getKlanten() {
    try {
      KsrLocator locator = new KsrLocator();
      locator.setksrPortEndpointAddress(endPoint);

      KsrBindingStub port = (KsrBindingStub) locator.getksrPort();
      port.setUsername(username);
      port.setPassword(password);

      Klanten[] klantnamen = port.getKlantList(new InvoerKlant("list", "", IDD_VERSION)).getKlantnamen();

      return Arrays.asList(klantnamen);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Klanten getKlantById(final Long klantId) {
    Klanten matchedKlant = null;

    Collection<Klanten> klanten = getKlanten();

    for (Klanten foundKlant : klanten) {
      if (klantId == foundKlant.getKlant_id()) {
        matchedKlant = foundKlant;
        break;
      }
    }
    return matchedKlant;
  }

  protected void setUsername(String username) {
    this.username = username;
  }

  protected void setPassword(String password) {
    this.password = password;
  }

  protected void setEndPoint(String endPoint) {
    this.endPoint = endPoint;
  }

}
