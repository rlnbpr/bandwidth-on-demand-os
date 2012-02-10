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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class InstituteCacheRefresher {

  static final String CACHE_NAME = "institutesCache";

  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private InstituteService instituteService;

  /**
   * Clears the cache of the institutes every 8 hours.
   * Also makes sure that the cache is loaded on startup.
   */
  @Scheduled(fixedRate = 1000 * 60 * 60 * 8)
  public void refreshCache() {
    Cache cache = cacheManager.getCache(CACHE_NAME);
    cache.clear();
    instituteService.getInstitutes();
  }
}
