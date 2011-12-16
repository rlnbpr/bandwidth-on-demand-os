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
package nl.surfnet.bod.service;

import java.util.Collection;
import java.util.List;

import nl.surfnet.bod.domain.Institution;
import nl.surfnet.bod.idd.IddClient;
import nl.surfnet.bod.idd.generated.Klanten;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.googlecode.ehcache.annotations.Cacheable;

@Service
public class InstitutionIddService implements InstitutionService {

  private IddClient iddClient;

  @Override
  @Cacheable(cacheName = "institutionsCache")
  public Collection<Institution> getInstitutions() {
    Collection<Klanten> klanten = iddClient.getKlanten();

    return toInstitutions(klanten);
  }

  private Collection<Institution> toInstitutions(Collection<Klanten> klantnamen) {
    List<Institution> institutions = Lists.newArrayList();
    for (Klanten klant : klantnamen) {
      String klantnaam = klant.getKlantnaam().trim();
      if (Strings.isNullOrEmpty(klantnaam)) {
        continue;
      }
      institutions.add(new Institution(klantnaam));
    }

    return institutions;
  }

  @Autowired
  public void setIddClient(IddClient iddClient) {
    this.iddClient = iddClient;
  }
}
