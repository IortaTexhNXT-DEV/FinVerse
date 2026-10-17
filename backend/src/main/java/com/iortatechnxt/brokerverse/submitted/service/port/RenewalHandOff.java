package com.iortatechnxt.brokerverse.submitted.service.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Port: starts the renewal of a masterlist record and answers where it stands (BRIDSP-15, 22, 23,
 * 25, 26; SUBMITTED_POLICIES_DESIGN sections 2.2 and 3.5, cross-BRD decision D2). The Renewal
 * module implements it ({@code SubmittedPolicyRenewalHandOff}, wave R3) and owns the renewal from
 * the hand-off on: renewal candidate, renewal account, hold cover request and the renewal letters.
 * The default adapter of this module only records the hand-off as PENDING; the expiry scan offers
 * the pending hand-offs again, so they are replayed once the Renewal adapter is deployed.
 * Idempotent on the masterlist number.
 */
public interface RenewalHandOff {

  /**
   * Hands a masterlist record to Renewal.
   *
   * @param request record, terms and requester
   * @return outcome with the renewal reference
   */
  HandOffResult handOff(HandOffRequest request);

  /**
   * Where the renewal of a handed-over record stands.
   *
   * @param companyId company
   * @param sbmNo masterlist number
   * @return status, empty when nothing was handed over (or only recorded as pending)
   */
  Optional<HandOffStatus> status(Long companyId, String sbmNo);

  /** Outcome of a hand-off. */
  enum Outcome {
    /** Recorded only; the Renewal module is not connected yet. */
    PENDING,
    /** A renewal candidate exists for the record. */
    HANDED_OFF,
    /** Refused (for example the policy data is not enough to renew). */
    REFUSED
  }

  /** State of a renewal. */
  enum State {
    /** In progress. */
    OPEN,
    /** Renewed and booked. */
    RENEWED,
    /** Not renewed (client declined or not for renewal). */
    NOT_RENEWED,
    /** Lost to another broker or insurer. */
    LOST,
    /** Expired without renewal. */
    EXPIRED_UNRENEWED
  }

  /**
   * A hand-off.
   *
   * @param companyId company
   * @param sbmNo masterlist number (idempotency key)
   * @param policy policy, risk and loan data of the record
   * @param terms how to renew
   * @param requestedBy user or SYSTEM (expiry scan)
   */
  record HandOffRequest(
      Long companyId, String sbmNo, PolicyData policy, RenewalTerms terms, String requestedBy) {}

  /**
   * The policy data travelling with a hand-off.
   *
   * @param segment segment (CBG_MOTOR, CBG_FIRE, NONCBG_CORPORATE, NONCBG_RETAIL)
   * @param businessType NB or RB
   * @param assuredName assured
   * @param borrowerName borrower, may be null
   * @param cif bank client number, may be null
   * @param contact mailing address, e-mail and mobile
   * @param insurerCode expiring insurer
   * @param policyNo expiring policy number
   * @param period expiring inception and expiry
   * @param sumInsured amount insured
   * @param totalPremium expiring premium
   * @param risk risk details
   * @param loan PN, loan status and mortgagee
   */
  record PolicyData(
      String segment,
      String businessType,
      String assuredName,
      String borrowerName,
      String cif,
      Contact contact,
      String insurerCode,
      String policyNo,
      Period period,
      BigDecimal sumInsured,
      BigDecimal totalPremium,
      Risk risk,
      Loan loan) {}

  /**
   * Contact data of the assured.
   *
   * @param mailingAddress mailing address
   * @param email e-mail
   * @param mobile mobile number
   */
  record Contact(String mailingAddress, String email, String mobile) {}

  /**
   * Policy period.
   *
   * @param inception inception date
   * @param expiry expiry date
   */
  record Period(LocalDate inception, LocalDate expiry) {}

  /**
   * Risk details.
   *
   * @param description unit description or property location
   * @param serialNo serial (chassis) number, motor only
   * @param motorNo motor (engine) number, motor only
   * @param plateNo plate number, motor only
   * @param occupancy occupancy, fire only
   */
  record Risk(
      String description, String serialNo, String motorNo, String plateNo, String occupancy) {}

  /**
   * Loan data.
   *
   * @param pnNo promissory note number
   * @param loanStatus LAMD loan status
   * @param mortgagee mortgagee
   */
  record Loan(String pnNo, String loanStatus, String mortgagee) {}

  /**
   * How to renew.
   *
   * @param raTemplate Renewal Advice template: GENERIC or FFY
   * @param assignedInsurer insurer assigned by the insurer rules, null when assigned by hand later
   * @param handlerUsername handler of the record
   * @param aoUsername account officer, may be null
   * @param manual true for "Renew with BDOI" (the renewal starts unassigned for a manual
   *     disposition)
   */
  record RenewalTerms(
      String raTemplate,
      String assignedInsurer,
      String handlerUsername,
      String aoUsername,
      boolean manual) {}

  /**
   * Answer to a hand-off.
   *
   * @param outcome pending, handed off or refused
   * @param renewalRef reference of the renewal, null when pending
   * @param arn renewal account, null until created
   * @param message what happened
   */
  record HandOffResult(Outcome outcome, String renewalRef, String arn, String message) {}

  /**
   * Status of a renewal.
   *
   * @param state open or closed state
   * @param renewalRef renewal reference
   * @param arn renewal account, may be null
   * @param reason reason of a closure, may be null
   */
  record HandOffStatus(State state, String renewalRef, String arn, String reason) {}
}
