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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import nl.surfnet.bod.domain.Reservation;
import nl.surfnet.bod.domain.ReservationStatus;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.util.concurrent.Uninterruptibles;

/**
 * This class is responsible for monitoring changes of a
 * {@link Reservation#getStatus()}. A scheduler is started upon the call to
 * {@link #monitorStatus(Reservation)}, whenever a state change is detected the
 * new state will be updated in the specific {@link Reservation} object and will
 * be persisted. The scheduler will be cancelled afterwards.
 *
 * @author Franky
 *
 */
@Component
@Transactional
public class ReservationPoller {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final ExecutorService executorService = Executors.newFixedThreadPool(10);

  @Autowired
  private ReservationService reservationService;

  @Autowired
  private ReservationEventPublisher reservationEventPublisher;

  @Value("${reservation.poll.max.tries}")
  private int maxPollingTries;

  @Value("${reservation.poll.interval.milliseconds}")
  private long pollingIntervalInMillis;

  /**
   * The polling of reservations is scheduled every minute. This is because the
   * precision of reservations is in minutes.
   */
  @Scheduled(cron = "0 * * * * *")
  public void pollReservationsThatAreAboutToChangeStatusOrShouldHaveChanged() {
    LocalDateTime dateTime = LocalDateTime.now().withSecondOfMinute(0).withMillisOfSecond(0);
    Collection<Reservation> reservations = reservationService.findReservationsToPoll(dateTime);

    logger.debug("Found {} reservations that are about to change status or should have change", reservations.size());

    for (Reservation reservation : reservations) {
      executorService.submit(new ReservationStatusChecker(reservation, maxPollingTries));
    }
  }

  @Scheduled(cron = "0 * * * * *")
  public void pollReservationsThatHaveARunningStatus() {
    // Only check for RUNNING state, otherwise it might interfere with other
    // schedule jobs. Status PREPARING and SCHEDULED are
    // temporary status which might change fast which will lead to concurrent
    // updates and staleObjectExceptions
    List<Reservation> reservations = reservationService.findRunningReservations();

    logger.debug("Found {} reservations that have a running state.", reservations.size());

    for (Reservation reservation : reservations) {
      executorService.submit(new ReservationStatusChecker(reservation, 1));
    }
  }

  protected void setMaxPollingTries(int maxPollingTries) {
    this.maxPollingTries = maxPollingTries;
  }

  protected void setPollingInterval(long sleepFor, TimeUnit timeUnit) {
    this.pollingIntervalInMillis = timeUnit.toMillis(sleepFor);
  }

  ExecutorService getExecutorService() {
    return executorService;
  }

  private class ReservationStatusChecker implements Runnable {
    private final Reservation reservation;
    private final ReservationStatus startStatus;
    private final int maxPollingTries;
    private int numberOfTries;

    public ReservationStatusChecker(Reservation reservation, int maxPollingTries) {
      this.reservation = reservation;
      this.maxPollingTries = maxPollingTries;
      this.startStatus = reservation.getStatus();
    }

    @Override
    public void run() {
      ReservationStatus currentStatus = null;

      // No need to retrieve status when there is no reservationId
      while ((numberOfTries < maxPollingTries) && (reservation.getReservationId() != null)) {
        logger.debug("Checking status update for: '{}' (try {})", reservation.getReservationId(), numberOfTries);

        currentStatus = reservationService.getStatus(reservation);
        numberOfTries++;

        if (!currentStatus.equals(startStatus)) {
          logger.info("Startstatus {} will be updated to {} for reservation {}", new Object[] { startStatus,
              currentStatus, reservation.getReservationId() });

          reservation.setStatus(currentStatus);
          reservationService.update(reservation);

          reservationEventPublisher.notifyListeners(new ReservationStatusChangeEvent(startStatus, reservation));

          return;
        }

        Uninterruptibles.sleepUninterruptibly(pollingIntervalInMillis, TimeUnit.MILLISECONDS);
      }
    }
  }
}
