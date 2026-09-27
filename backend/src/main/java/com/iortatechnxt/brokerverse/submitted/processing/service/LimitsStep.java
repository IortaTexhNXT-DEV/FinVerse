package com.iortatechnxt.brokerverse.submitted.processing.service;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitCheck;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitCheck.Result;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitCheckRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitRuleRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The limits step (BRIDSP-16; FRS FR-SP-050): the record is compared with the active limit rules of
 * its insurer and segment - maximum sum insured, maximum vehicle age and the allowed values of one
 * other fact. Every check is recorded; a breach flags the record "Insurer approval required".
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class LimitsStep {

  /** Attribute of the sum insured limit. */
  public static final String SUM_INSURED = "Sum insured";

  /** Attribute of the vehicle age limit. */
  public static final String VEHICLE_AGE = "Vehicle age";

  private final SbmLimitRuleRepository rules;
  private final SbmLimitCheckRepository checks;

  /**
   * Creates the step.
   *
   * @param rules limit rules
   * @param checks limit checks
   */
  public LimitsStep(SbmLimitRuleRepository rules, SbmLimitCheckRepository checks) {
    this.rules = rules;
    this.checks = checks;
  }

  /**
   * Checks a record.
   *
   * @param runId run, may be null
   * @param p record
   * @param facts facts of the record
   * @return the breached checks
   */
  public List<SbmLimitCheck> check(Long runId, SbmPolicy p, Map<String, Object> facts) {
    String insurer = p.getTerms().insurerCode();
    if (insurer == null) {
      return List.of();
    }
    List<SbmLimitCheck> breached = new ArrayList<>();
    for (SbmLimitRule rule :
        rules.findByCompanyIdAndInsurerCodeAndRecordStatus(
            p.getCompanyId(), insurer, RecordStatus.ACTIVE)) {
      if (rule.getSegment() != null && !rule.getSegment().equals(p.getSegment())) {
        continue;
      }
      for (Result r : results(rule, p, facts)) {
        SbmLimitCheck saved = checks.save(new SbmLimitCheck(runId, p.getId(), rule.getId(), r));
        if (r.breached()) {
          breached.add(saved);
        }
      }
    }
    return breached;
  }

  private static List<Result> results(SbmLimitRule rule, SbmPolicy p, Map<String, Object> facts) {
    List<Result> out = new ArrayList<>();
    BigDecimal si = p.getTerms().sumInsured();
    if (rule.getMaxSumInsured() != null && si != null) {
      out.add(
          new Result(
              SUM_INSURED,
              DisplayFormat.amount(rule.getMaxSumInsured()),
              DisplayFormat.amount(si),
              si.compareTo(rule.getMaxSumInsured()) > 0));
    }
    Object age = facts.get("vehicleAge");
    if (rule.getMaxVehicleAge() != null && age instanceof Integer years) {
      out.add(
          new Result(
              VEHICLE_AGE,
              rule.getMaxVehicleAge() + " years",
              years + " years",
              years > rule.getMaxVehicleAge()));
    }
    if (rule.getAttribute() != null && rule.getAttributeLimit() != null) {
      Object value = facts.get(rule.getAttribute());
      String actual = value == null ? null : value.toString();
      boolean allowed =
          actual == null
              || Arrays.stream(rule.getAttributeLimit().split(","))
                  .map(v -> v.strip().toUpperCase(Locale.ROOT))
                  .anyMatch(actual.toUpperCase(Locale.ROOT)::equals);
      out.add(new Result(rule.getAttribute(), rule.getAttributeLimit(), actual, !allowed));
    }
    return out;
  }
}
