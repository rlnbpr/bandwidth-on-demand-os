package nl.surfnet.bod.web.view;

import org.joda.time.DateTime;

public class NocReservationReport {

  private final DateTime periodStart;
  private final DateTime periodEnd;

  // Reservation requests
  private long amountRequestsCreatedSucceeded;
  private long amountRequestsCreatedFailed;
  private long amountRequestsModifiedSucceeded;
  private long amountRequestsModifiedFailed;
  private long amountRequestsCancelSucceeded;
  private long amountRequestsCancelFailed;

  // By channel
  private long amountRequestsThroughGUI;
  private long amountRequestsThroughNSI;

  // Reservations per protectionType
  private long amountReservationsProtected = 9;
  private long amountReservationsUnprotected = 10;
  private long amountReservationsRedundant = 11;

  // Running reservations
  private long amountRunningReservationsSucceeded = 12;
  private long amountRunningReservationsFailed = 13;
  private long amounRunningReservationsStillRunning = 14;
  private long amountRunningReservationsNeverProvisioned = 15;

  public NocReservationReport(DateTime start, DateTime end) {
    this.periodStart = start;
    this.periodEnd = end;
  }

  public long getTotalRequests() {
    return amountRequestsCreatedSucceeded + amountRequestsCreatedFailed + amountRequestsModifiedSucceeded
        + amountRequestsModifiedFailed + amountRequestsCancelSucceeded + amountRequestsCancelFailed;
  }

  public long getTotalReservations() {
    return amountReservationsProtected + amountReservationsUnprotected + amountReservationsRedundant;
  }

  public long getTotalActiveReservations() {
    return amountRunningReservationsSucceeded + amountRunningReservationsFailed + amounRunningReservationsStillRunning
        + amountRunningReservationsNeverProvisioned;
  }

  public long getAmountRequestsCreatedSucceeded() {
    return amountRequestsCreatedSucceeded;
  }

  public void setAmountRequestsCreatedSucceeded(long amountRequestsCreatedSucceeded) {
    this.amountRequestsCreatedSucceeded = amountRequestsCreatedSucceeded;
  }

  public long getAmountRequestsCreatedFailed() {
    return amountRequestsCreatedFailed;
  }

  public void setAmountRequestsCreatedFailed(long amountRequestsCreatedFailed) {
    this.amountRequestsCreatedFailed = amountRequestsCreatedFailed;
  }

  public long getAmountRequestsModifiedSucceeded() {
    return amountRequestsModifiedSucceeded;
  }

  public void setAmountRequestsModifiedSucceeded(long amountRequestsModifiedSucceeded) {
    this.amountRequestsModifiedSucceeded = amountRequestsModifiedSucceeded;
  }

  public long getAmountRequestsModifiedFailed() {
    return amountRequestsModifiedFailed;
  }

  public void setAmountRequestsModifiedFailed(long amountRequestsModifiedFailed) {
    this.amountRequestsModifiedFailed = amountRequestsModifiedFailed;
  }

  public long getAmountRequestsCancelSucceeded() {
    return amountRequestsCancelSucceeded;
  }

  public void setAmountRequestsCancelSucceeded(long amountRequestsCancelSucceeded) {
    this.amountRequestsCancelSucceeded = amountRequestsCancelSucceeded;
  }

  public long getAmountRequestsCancelFailed() {
    return amountRequestsCancelFailed;
  }

  public void setAmountRequestsCancelFailed(long amountRequestsCancelFailed) {
    this.amountRequestsCancelFailed = amountRequestsCancelFailed;
  }

  public long getAmountRequestsThroughGUI() {
    return amountRequestsThroughGUI;
  }

  public void setAmountRequestsThroughGUI(long amountRequestsThroughGUI) {
    this.amountRequestsThroughGUI = amountRequestsThroughGUI;
  }

  public long getAmountRequestsThroughNSI() {
    return amountRequestsThroughNSI;
  }

  public void setAmountRequestsThroughNSI(long amountRequestsThroughNSI) {
    this.amountRequestsThroughNSI = amountRequestsThroughNSI;
  }

  public long getAmountReservationsProtected() {
    return amountReservationsProtected;
  }

  public void setAmountReservationsProtected(long amountReservationsProtected) {
    this.amountReservationsProtected = amountReservationsProtected;
  }

  public long getAmountReservationsUnprotected() {
    return amountReservationsUnprotected;
  }

  public void setAmountReservationsUnprotected(long amountReservationsUnprotected) {
    this.amountReservationsUnprotected = amountReservationsUnprotected;
  }

  public long getAmountReservationsRedundant() {
    return amountReservationsRedundant;
  }

  public void setAmountReservationsRedundant(long amountReservationsRedundant) {
    this.amountReservationsRedundant = amountReservationsRedundant;
  }

  public long getAmountRunningReservationsSucceeded() {
    return amountRunningReservationsSucceeded;
  }

  public void setAmountRunningReservationsSucceeded(long amountRunningReservationsSucceeded) {
    this.amountRunningReservationsSucceeded = amountRunningReservationsSucceeded;
  }

  public long getAmountRunningReservationsFailed() {
    return amountRunningReservationsFailed;
  }

  public void setAmountRunningReservationsFailed(long amountRunningReservationsFailed) {
    this.amountRunningReservationsFailed = amountRunningReservationsFailed;
  }

  public long getAmounRunningReservationsStillRunning() {
    return amounRunningReservationsStillRunning;
  }

  public void setAmounRunningReservationsStillRunning(long amounRunningReservationsStillRunning) {
    this.amounRunningReservationsStillRunning = amounRunningReservationsStillRunning;
  }

  public long getAmountRunningReservationsNeverProvisioned() {
    return amountRunningReservationsNeverProvisioned;
  }

  public void setAmountRunningReservationsNeverProvisioned(long amountRunningReservationsNeverProvisioned) {
    this.amountRunningReservationsNeverProvisioned = amountRunningReservationsNeverProvisioned;
  }

  public DateTime getPeriodStart() {
    return periodStart;
  }

  public DateTime getPeriodEnd() {
    return periodEnd;
  }
}
