package com.iortatechnxt.brokerverse.renewal.setup.api.dto;

import com.iortatechnxt.brokerverse.renewal.check.service.CheckNames;
import com.iortatechnxt.brokerverse.renewal.domain.BucketRule;
import com.iortatechnxt.brokerverse.renewal.domain.BucketRuleSet;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSetting;
import com.iortatechnxt.brokerverse.renewal.domain.DecisionMatrix;
import com.iortatechnxt.brokerverse.renewal.domain.DecisionRule;
import com.iortatechnxt.brokerverse.renewal.domain.NonRenewableRiskCode;
import com.iortatechnxt.brokerverse.renewal.domain.PackageChoice;
import com.iortatechnxt.brokerverse.renewal.domain.PackageMapEntry;
import com.iortatechnxt.brokerverse.renewal.domain.VersionedRules;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Responses of Renewal Setup and of the package choices. */
public final class SetupDtos {

  private SetupDtos() {}

  /**
   * Maker-checker state of a record.
   *
   * @param recordStatus record status
   * @param maker maker
   * @param authorizedBy checker
   * @param authorizedAt authorization time
   */
  public record Approval(
      String recordStatus, String maker, String authorizedBy, Instant authorizedAt) {}

  /**
   * A non-renewable risk code.
   *
   * @param id id
   * @param riskCode risk code
   * @param lineCode product line
   * @param reason reason
   * @param effectiveFrom effective from
   * @param effectiveTo effective to
   * @param approval maker-checker state
   */
  public record RiskCodeView(
      Long id,
      String riskCode,
      String lineCode,
      String reason,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      Approval approval) {

    /**
     * Maps a code.
     *
     * @param c code
     * @return view
     */
    public static RiskCodeView of(NonRenewableRiskCode c) {
      return new RiskCodeView(
          c.getId(),
          c.getRiskCode(),
          c.getLineCode(),
          c.getReason(),
          c.getEffectiveFrom(),
          c.getEffectiveTo(),
          new Approval(
              c.getRecordStatus().name(), c.getMaker(), c.getAuthorizedBy(), c.getAuthorizedAt()));
    }
  }

  /**
   * A check setting.
   *
   * @param checkCode check
   * @param checkName name
   * @param active active
   * @param severity severity
   * @param parameters parameters
   * @param approval maker-checker state
   */
  public record CheckSettingView(
      String checkCode,
      String checkName,
      boolean active,
      String severity,
      String parameters,
      Approval approval) {

    /**
     * Maps a setting.
     *
     * @param s setting
     * @return view
     */
    public static CheckSettingView of(CheckSetting s) {
      return new CheckSettingView(
          s.getCheckCode(),
          CheckNames.of(s.getCheckCode()),
          s.isEnabled(),
          s.getSeverity().name(),
          s.getParameters(),
          new Approval(
              s.getRecordStatus().name(), s.getMaker(), s.getAuthorizedBy(), s.getAuthorizedAt()));
    }
  }

  /**
   * A version of bucket rules or of the decision matrix.
   *
   * @param id id
   * @param versionNo version
   * @param status status
   * @param effectiveFrom effective date
   * @param description description
   * @param maker maker
   * @param submittedAt submission
   * @param approvedBy checker
   * @param approvedAt decision
   * @param decisionRemarks remarks of a rejection
   * @param rules rules in priority order
   * @param <R> rule type
   */
  public record VersionView<R>(
      Long id,
      int versionNo,
      String status,
      LocalDate effectiveFrom,
      String description,
      String maker,
      Instant submittedAt,
      String approvedBy,
      Instant approvedAt,
      String decisionRemarks,
      List<R> rules) {

    private static <R> VersionView<R> of(VersionedRules v, List<R> rules) {
      return new VersionView<>(
          v.getId(),
          v.getVersionNo(),
          v.getStatus().name(),
          v.getEffectiveFrom(),
          v.getDescription(),
          v.getSubmittedBy() == null ? v.getCreatedBy() : v.getSubmittedBy(),
          v.getSubmittedAt(),
          v.getApprovedBy(),
          v.getApprovedAt(),
          v.getDecisionRemarks(),
          rules);
    }

    /**
     * Maps bucket rules.
     *
     * @param s rule set
     * @return view
     */
    public static VersionView<BucketRule.Data> ofBuckets(BucketRuleSet s) {
      return of(
          s,
          s.getRules().stream()
              .map(
                  r ->
                      new BucketRule.Data(
                          r.getPriority(),
                          r.getCheckCode(),
                          r.getSeverity(),
                          r.getOutcome(),
                          r.getResultBucket()))
              .toList());
    }

    /**
     * Maps a decision matrix.
     *
     * @param m matrix
     * @return view
     */
    public static VersionView<DecisionRule.Data> ofMatrix(DecisionMatrix m) {
      return of(
          m,
          m.getRules().stream()
              .map(
                  r ->
                      new DecisionRule.Data(
                          r.getPriority(),
                          r.criteria(),
                          r.getOutcome(),
                          r.getAutomation(),
                          r.getLetterHint()))
              .toList());
    }
  }

  /**
   * An entry of the package map.
   *
   * @param id id
   * @param data entry
   * @param action MAP or REJECT
   * @param mapVersion version of the entry
   * @param source MIGRATION, SETUP or CHOICE
   * @param approval maker-checker state
   */
  public record PackageMapView(
      Long id,
      PackageMapEntry.Data data,
      String action,
      int mapVersion,
      String source,
      Approval approval) {

    /**
     * Maps an entry.
     *
     * @param e entry
     * @return view
     */
    public static PackageMapView of(PackageMapEntry e) {
      return new PackageMapView(
          e.getId(),
          new PackageMapEntry.Data(
              e.getLegacyPackageCode(),
              e.getLegacyPackageVersion(),
              e.getRiskCode(),
              e.getInsurerCode(),
              e.getSiFrom(),
              e.getSiTo(),
              e.getProductCode(),
              e.getProductVersionNo(),
              e.getRemarks()),
          e.getAction(),
          e.getMapVersion(),
          e.getSource(),
          new Approval(
              e.getRecordStatus().name(), e.getMaker(), e.getAuthorizedBy(), e.getAuthorizedAt()));
    }
  }

  /**
   * A package choice.
   *
   * @param id id
   * @param candidateId renewal
   * @param legacyPackage legacy package and version
   * @param productCode chosen package
   * @param productVersionNo chosen version
   * @param reason reason
   * @param status status
   * @param maker maker
   * @param createdAt time
   * @param decidedBy checker
   * @param decidedAt decision
   * @param decisionRemarks remarks
   */
  public record PackageChoiceView(
      Long id,
      Long candidateId,
      String legacyPackage,
      String productCode,
      int productVersionNo,
      String reason,
      String status,
      String maker,
      Instant createdAt,
      String decidedBy,
      Instant decidedAt,
      String decisionRemarks) {

    /**
     * Maps a choice.
     *
     * @param c choice
     * @return view
     */
    public static PackageChoiceView of(PackageChoice c) {
      return new PackageChoiceView(
          c.getId(),
          c.getCandidateId(),
          c.getLegacyPackageCode()
              + (c.getLegacyPackageVersion() == null ? "" : " v" + c.getLegacyPackageVersion()),
          c.getProductCode(),
          c.getProductVersionNo(),
          c.getReason(),
          c.getStatus().name(),
          c.getCreatedBy(),
          c.getCreatedAt(),
          c.getDecidedBy(),
          c.getDecidedAt(),
          c.getDecisionRemarks());
    }
  }
}
