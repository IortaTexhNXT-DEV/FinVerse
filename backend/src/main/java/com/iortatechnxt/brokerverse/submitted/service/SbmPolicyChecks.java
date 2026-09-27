package com.iortatechnxt.brokerverse.submitted.service;

import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyData;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The checks of the data of a masterlist record (FRS FR-SP-001, 003): the fields a segment needs
 * (PN for CBG, the unit and serial number of a motor policy, the location of a fire policy), the
 * mandatory fields of the source, amounts not negative and the expiry after the inception.
 */
public final class SbmPolicyChecks {

  private static final Map<String, Field> FIELDS =
      Map.ofEntries(
          Map.entry("assuredName", new Field("Assured", d -> d.assured().assuredName())),
          Map.entry("insurerCode", new Field("Insurer", d -> d.terms().insurerCode())),
          Map.entry("policyNo", new Field("Policy number", d -> d.terms().policyNo())),
          Map.entry("inceptionDate", new Field("Inception", d -> d.terms().inceptionDate())),
          Map.entry("expiryDate", new Field("Expiry", d -> d.terms().expiryDate())),
          Map.entry("sumInsured", new Field("Sum insured", d -> d.terms().sumInsured())),
          Map.entry("totalPremium", new Field("Total premium", d -> d.terms().totalPremium())),
          Map.entry("pnNo", new Field("PN", d -> d.loan().pnNo())),
          Map.entry("borrowerName", new Field("Borrower", d -> d.loan().borrowerName())),
          Map.entry(
              "mailingAddress", new Field("Mailing address", d -> d.assured().mailingAddress())));

  private SbmPolicyChecks() {}

  /**
   * The problems of the data, as messages for the user.
   *
   * @param d data
   * @param mandatory mandatory fields of the source (names of the data)
   * @return messages, empty when valid
   */
  public static List<String> problems(SbmPolicyData d, List<String> mandatory) {
    List<String> out = new ArrayList<>();
    if (d.segment() == null || d.segment().isBlank()) {
      out.add("Segment is required");
    }
    if (d.businessType() == null) {
      out.add("Business type is required");
    }
    require(out, "Assured", d.assured() == null ? null : d.assured().assuredName());
    require(out, "Expiry", d.terms() == null ? null : d.terms().expiryDate());
    if (d.assured() == null || d.terms() == null) {
      return out;
    }
    for (String name : mandatory) {
      Field f = FIELDS.get(name);
      if (f != null && !"Assured".equals(f.label()) && !"Expiry".equals(f.label())) {
        require(out, f.label(), f.value().apply(d));
      }
    }
    segmentFields(d, out);
    amounts(d, out);
    if (d.terms().inceptionDate() != null
        && d.terms().expiryDate() != null
        && !d.terms().expiryDate().isAfter(d.terms().inceptionDate())) {
      out.add("The expiry date must be after the inception date");
    }
    boolean noKey = blank(d.loan().pnNo()) && blank(d.terms().policyNo());
    if (noKey) {
      out.add("Enter the PN number or the policy number");
    }
    return out.stream().distinct().toList();
  }

  private static void segmentFields(SbmPolicyData d, List<String> out) {
    if (SubmittedCodes.PN_SEGMENTS.contains(d.segment())) {
      require(out, "PN", d.loan().pnNo());
    }
    if (SubmittedCodes.CBG_MOTOR.equals(d.segment())) {
      require(out, "Unit", d.risk().unitDescription());
      require(out, "Serial number", d.risk().serialNo());
    }
    if (SubmittedCodes.CBG_FIRE.equals(d.segment())) {
      require(out, "Property location", d.risk().propertyLocation());
    }
  }

  private static void amounts(SbmPolicyData d, List<String> out) {
    negative(out, "Sum insured", d.terms().sumInsured());
    negative(out, "Total premium", d.terms().totalPremium());
  }

  private static void negative(List<String> out, String label, BigDecimal value) {
    if (value != null && value.signum() < 0) {
      out.add(label + " cannot be negative");
    }
  }

  private static void require(List<String> out, String label, Object value) {
    if (value == null || value instanceof String s && s.isBlank()) {
      out.add(label + " is required");
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private record Field(String label, Function<SbmPolicyData, Object> value) {}
}
