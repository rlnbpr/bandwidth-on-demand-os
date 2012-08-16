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

import java.util.List;

import org.joda.time.DurationFieldType;
import org.joda.time.Months;
import org.joda.time.ReadablePeriod;
import org.springframework.stereotype.Component;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;

import nl.surfnet.bod.domain.ReservationStatus;
import nl.surfnet.bod.web.view.ReservationFilterView;

@Component
public class ReservationFilterViewFactory {

  public static final ReadablePeriod DEFAULT_FILTER_INTERVAL = Months.FOUR;
  public static final String DEFAULT_FILTER_INTERVAL_STRING = DEFAULT_FILTER_INTERVAL.getValue(0) + " "
      + DEFAULT_FILTER_INTERVAL.getFieldType(0);

  public static final String COMING = "coming";
  public static final String ELAPSED = "elapsed";
  public static final String ACTIVE = "active";
  public static final ReservationFilterView DEFAULT_FILTER = new ReservationFilterView(COMING, String.format(
      "In %d months", DEFAULT_FILTER_INTERVAL.get(DurationFieldType.months())), DEFAULT_FILTER_INTERVAL, false);

  public ReservationFilterView create(String id) {

    // No filter, use default
    if (!StringUtils.hasText(id)) {
      return DEFAULT_FILTER;
    }

    try {
      // If it is a number we assume it is a year
      Integer year = NumberUtils.parseNumber(id, Integer.class);
      return new ReservationFilterView(year);
    }
    catch (IllegalArgumentException exc) {
      if (ELAPSED.equals(id)) {
        return new ReservationFilterView(ELAPSED, String.format("Past %d months",
            DEFAULT_FILTER_INTERVAL.get(DurationFieldType.months())), DEFAULT_FILTER_INTERVAL, true);
      }
      else if (COMING.equals(id)) {
        return new ReservationFilterView(COMING, String.format("In %d months",
            DEFAULT_FILTER_INTERVAL.get(DurationFieldType.months())), DEFAULT_FILTER_INTERVAL, false);
      }
      else if (ACTIVE.equals(id)) {
        return new ReservationFilterView(ACTIVE, "Active", ReservationStatus.RUNNING);
      }
      else {
        throw new IllegalArgumentException("No filter related to: " + id);
      }
    }
  }

  public List<ReservationFilterView> create(List<Integer> reservationYears) {
    List<ReservationFilterView> filterViews = Lists.newArrayList();

    // Years with reservations
    for (Integer year : reservationYears) {
      filterViews.add(create(year.toString()));
    }

    return filterViews;
  }
}
