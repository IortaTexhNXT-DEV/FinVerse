package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.prodrecon.domain.ReconSide;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * The comparison rules of production reconciliation (PRCID.022/026/027): how an insurer line is
 * paired with a booked invoice (configurable keys, OQ30) and which of the eight criteria differ.
 * Text is compared normalised (case, spaces and punctuation ignored); amounts are equal when they
 * differ by no more than the tolerance ({@code RECON_TOLERANCE}, 1.00 per amount field); a field
 * the insurer left blank is not compared.
 */
public final class ReconMatcher {

  private ReconMatcher() {}

  /** Keys that pair an insurer line with a booked invoice (parameter RECON_MATCH_KEYS). */
  public enum MatchKey {
    /** Reference / invoice number. */
    INVOICE_NO(ReconSide::referenceNo),
    /** Policy number. */
    POLICY_NO(ReconSide::policyNo),
    /** Promissory note number (one of the invoice's PN numbers). */
    PN_NO(ReconSide::pnNo);

    private final Function<ReconSide, String> value;

    MatchKey(Function<ReconSide, String> value) {
      this.value = value;
    }

    /**
     * Whether both sides carry the key and it agrees.
     *
     * @param bdoi booked side
     * @param insurer insurer side
     * @return true when paired by this key
     */
    public boolean pairs(ReconSide bdoi, ReconSide insurer) {
      String theirs = normalise(value.apply(insurer));
      if (theirs.isEmpty()) {
        return false;
      }
      if (this == PN_NO) {
        return pnNumbers(bdoi.pnNo()).contains(theirs);
      }
      return theirs.equals(normalise(value.apply(bdoi)));
    }
  }

  /** The compared fields (PRCID.027), in display order. */
  public enum Field {
    /** Policy number. */
    POLICY_NO,
    /** Reference / invoice number. */
    REFERENCE_NO,
    /** PN number. */
    PN_NO,
    /** Period start. */
    PERIOD_FROM,
    /** Period end. */
    PERIOD_TO,
    /** Assured name. */
    ASSURED_NAME,
    /** Commission amount. */
    COMMISSION,
    /** Basic premium. */
    BASIC_PREMIUM,
    /** Gross premium. */
    GROSS_PREMIUM
  }

  /**
   * Whether an insurer line pairs with a booked invoice by the first key both sides carry.
   *
   * @param keys keys in order
   * @param bdoi booked side
   * @param insurer insurer side
   * @return true when a key agrees
   */
  public static boolean pairs(List<MatchKey> keys, ReconSide bdoi, ReconSide insurer) {
    return keys.stream().anyMatch(k -> k.pairs(bdoi, insurer));
  }

  /**
   * The fields that differ beyond the tolerance.
   *
   * @param bdoi booked side
   * @param insurer insurer side
   * @param tolerance amount tolerance
   * @return differing fields in display order, empty when matched
   */
  public static List<Field> differences(ReconSide bdoi, ReconSide insurer, BigDecimal tolerance) {
    List<Field> out = new ArrayList<>();
    text(out, Field.POLICY_NO, bdoi.policyNo(), insurer.policyNo());
    text(out, Field.REFERENCE_NO, bdoi.referenceNo(), insurer.referenceNo());
    if (!normalise(insurer.pnNo()).isEmpty()
        && !pnNumbers(bdoi.pnNo()).contains(normalise(insurer.pnNo()))) {
      out.add(Field.PN_NO);
    }
    date(out, Field.PERIOD_FROM, bdoi.periodFrom(), insurer.periodFrom());
    date(out, Field.PERIOD_TO, bdoi.periodTo(), insurer.periodTo());
    if (!names(insurer.assuredName()).isEmpty()
        && !names(insurer.assuredName()).equals(names(bdoi.assuredName()))) {
      out.add(Field.ASSURED_NAME);
    }
    amount(out, Field.COMMISSION, bdoi.commission(), insurer.commission(), tolerance);
    amount(out, Field.BASIC_PREMIUM, bdoi.basicPremium(), insurer.basicPremium(), tolerance);
    amount(out, Field.GROSS_PREMIUM, bdoi.grossPremium(), insurer.grossPremium(), tolerance);
    return out;
  }

  private static void text(List<Field> out, Field field, String ours, String theirs) {
    String t = normalise(theirs);
    if (!t.isEmpty() && !t.equals(normalise(ours))) {
      out.add(field);
    }
  }

  private static void date(List<Field> out, Field field, LocalDate ours, LocalDate theirs) {
    if (theirs != null && !Objects.equals(ours, theirs)) {
      out.add(field);
    }
  }

  private static void amount(
      List<Field> out, Field field, BigDecimal ours, BigDecimal theirs, BigDecimal tolerance) {
    if (theirs == null) {
      return;
    }
    BigDecimal base = ours == null ? BigDecimal.ZERO : ours;
    if (base.subtract(theirs).abs().compareTo(tolerance) > 0) {
      out.add(field);
    }
  }

  /**
   * An identifier without case, spaces, dashes, slashes or dots.
   *
   * @param value value
   * @return normalised value, empty for null
   */
  static String normalise(String value) {
    return value == null ? "" : value.replaceAll("[\\s./-]", "").toUpperCase(Locale.ROOT);
  }

  private static String names(String value) {
    return value == null
        ? ""
        : value.replaceAll("[^\\p{L}\\p{Nd}]+", " ").strip().toUpperCase(Locale.ROOT);
  }

  private static List<String> pnNumbers(String pnNos) {
    return pnNos == null
        ? List.of()
        : Arrays.stream(pnNos.split("[,;]")).map(ReconMatcher::normalise).toList();
  }
}
