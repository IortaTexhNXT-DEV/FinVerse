package com.iortatechnxt.brokerverse.submitted.processing.service;

import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoan;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The facts of a masterlist record the rules can test (BRIDSP-08; design section 3.2): the fields
 * of the record, the lists it is on, the LAMD loan found by the matching step and the facts the
 * sanitation computes (missing PN, duplicates). A rule condition on another name is refused.
 */
public final class SbmFacts {

  /** Every fact name a rule condition may use, in the order of the Setup screen. */
  public static final List<String> NAMES =
      List.of(
          "always",
          "segment",
          "businessType",
          "sourceCode",
          "migrated",
          "hasDocuments",
          "adequacyStatus",
          "insurerCode",
          "sumInsured",
          "vehicleType",
          "vehicleAge",
          "daysToExpiry",
          "ffy",
          "employeeAccount",
          "noTouch",
          "pnMissing",
          "duplicatePn",
          "duplicateUnit",
          "lamdFound",
          "loanStatus",
          "amortised",
          "unitMatches",
          "classification");

  private SbmFacts() {}

  /**
   * The facts of a record before matching.
   *
   * @param p record
   * @param today business date
   * @param duplicates duplicate PN and unit found by the sanitation
   * @return facts by name
   */
  public static Map<String, Object> of(SbmPolicy p, LocalDate today, Duplicates duplicates) {
    Map<String, Object> f = new HashMap<>();
    f.put("always", Boolean.TRUE);
    f.put("segment", p.getSegment());
    f.put("businessType", p.getBusinessType().name());
    f.put("sourceCode", p.getSourceCode());
    f.put("migrated", p.isMigrated());
    f.put("hasDocuments", p.isHasDocuments());
    f.put("adequacyStatus", p.getAdequacyStatus());
    f.put("insurerCode", p.getTerms().insurerCode());
    f.put("sumInsured", p.getTerms().sumInsured());
    f.put("vehicleType", p.getRisk().vehicleType());
    Integer year = p.getRisk().vehicleYear();
    f.put("vehicleAge", year == null ? null : today.getYear() - year);
    f.put("daysToExpiry", ChronoUnit.DAYS.between(today, p.getTerms().expiryDate()));
    f.put("ffy", p.getMarks().ffy());
    f.put("employeeAccount", p.getMarks().employeeAccount());
    f.put("noTouch", p.getMarks().noTouch());
    String pn = p.getLoan().pnNo();
    f.put("pnMissing", pn == null || pn.isBlank());
    f.put("duplicatePn", duplicates.pn());
    f.put("duplicateUnit", duplicates.unit());
    f.put("lamdFound", Boolean.FALSE);
    f.put("classification", p.getClassification() == null ? null : p.getClassification().name());
    return f;
  }

  /**
   * Adds the facts of the LAMD loan found for the PN.
   *
   * @param f facts
   * @param p record
   * @param loan latest snapshot row of the PN, null when none
   */
  public static void matched(Map<String, Object> f, SbmPolicy p, SbmLamdLoan loan) {
    f.put("lamdFound", loan != null);
    f.put("loanStatus", loan == null ? null : loan.getLoanStatus());
    f.put("amortised", loan != null && loan.isAmortised());
    f.put("unitMatches", loan == null || unitMatches(p, loan));
  }

  private static boolean unitMatches(SbmPolicy p, SbmLamdLoan loan) {
    boolean serialKnown = loan.getSerialNo() != null && p.getRisk().serialNo() != null;
    boolean motorKnown = loan.getMotorNo() != null && p.getRisk().motorNo() != null;
    boolean serialOk = !serialKnown || Objects.equals(loan.getSerialNo(), p.getRisk().serialNo());
    boolean motorOk = !motorKnown || Objects.equals(loan.getMotorNo(), p.getRisk().motorNo());
    return serialOk && motorOk;
  }

  /**
   * Duplicates found by the sanitation.
   *
   * @param pn another open record has the same PN
   * @param unit another open record has the same serial or motor number
   */
  public record Duplicates(boolean pn, boolean unit) {}
}
