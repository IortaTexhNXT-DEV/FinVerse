package com.iortatechnxt.brokerverse.payrequest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/**
 * Where the payment of a request stands in Disbursement (MKT 1.20.0, 2.24.0): the gateway request,
 * its status, the DV and its stage, the instrument status reported by Disbursement, and the
 * hand-off of a check cancellation.
 *
 * @param requestNo gateway request number
 * @param status gateway status (SENT ... PAID, RETURNED, CANCELLED)
 * @param dvNo disbursement voucher number
 * @param dvStatus DV stage in Disbursement
 * @param instrumentStatus instrument status (PRINTED, RELEASED, CREDITED ...)
 * @param message return or cancellation reason, or other information
 * @param disbursedAt when Disbursement reported the payment
 * @param handoffRef hand-off of a check cancellation to Disbursement
 */
@Embeddable
public record DisbursementTrack(
    @Column(name = "disbursement_request_no", length = 30) String requestNo,
    @Column(name = "disbursement_status", length = 20) String status,
    @Column(name = "dv_no", length = 40) String dvNo,
    @Column(name = "dv_status", length = 30) String dvStatus,
    @Column(name = "instrument_status", length = 30) String instrumentStatus,
    @Column(name = "disbursement_message", length = 250) String message,
    @Column(name = "disbursed_at") Instant disbursedAt,
    @Column(name = "handoff_ref", length = 40) String handoffRef) {

  /** Not sent yet. */
  public static final DisbursementTrack NONE =
      new DisbursementTrack(null, null, null, null, null, null, null, null);

  /**
   * The same track with a new gateway status.
   *
   * @param newRequestNo gateway request number
   * @param newStatus gateway status
   * @param newDvNo DV number, null keeps the known one
   * @param newMessage message, may be null
   * @return new track
   */
  public DisbursementTrack status(
      String newRequestNo, String newStatus, String newDvNo, String newMessage) {
    return new DisbursementTrack(
        newRequestNo == null ? requestNo : newRequestNo,
        newStatus,
        newDvNo == null ? dvNo : newDvNo,
        dvStatus,
        instrumentStatus,
        newMessage == null ? message : newMessage,
        disbursedAt,
        handoffRef);
  }

  /**
   * The same track with the DV stage and instrument status reported by Disbursement.
   *
   * @param newDvStatus DV stage, null keeps the known one
   * @param newInstrumentStatus instrument status, null keeps the known one
   * @return new track
   */
  public DisbursementTrack tracked(String newDvStatus, String newInstrumentStatus) {
    return new DisbursementTrack(
        requestNo,
        status,
        dvNo,
        newDvStatus == null ? dvStatus : newDvStatus,
        newInstrumentStatus == null ? instrumentStatus : newInstrumentStatus,
        message,
        disbursedAt,
        handoffRef);
  }

  /**
   * The same track, paid.
   *
   * @param at when
   * @return new track
   */
  public DisbursementTrack disbursed(Instant at) {
    return new DisbursementTrack(
        requestNo, status, dvNo, dvStatus, instrumentStatus, message, at, handoffRef);
  }

  /**
   * The same track with the hand-off of a check cancellation.
   *
   * @param ref hand-off reference
   * @return new track
   */
  public DisbursementTrack handedOff(String ref) {
    return new DisbursementTrack(
        requestNo, status, dvNo, dvStatus, instrumentStatus, message, disbursedAt, ref);
  }
}
