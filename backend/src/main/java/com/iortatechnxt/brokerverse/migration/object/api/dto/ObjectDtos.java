package com.iortatechnxt.brokerverse.migration.object.api.dto;

import com.iortatechnxt.brokerverse.migration.object.domain.MigDataObject;
import com.iortatechnxt.brokerverse.migration.object.domain.MigObjectDecision;
import com.iortatechnxt.brokerverse.migration.object.domain.MigrationClass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Request and response bodies of the data object register (FR-DM-001, FR-DM-002). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class ObjectDtos {

  private ObjectDtos() {}

  /**
   * A data object.
   *
   * @param code code
   * @param name name
   * @param category category
   * @param sourceSystems source systems
   * @param target target in BIBS
   * @param businessOwner business owner user
   * @param dataSteward data steward user
   * @param ownerTitle owner function
   * @param stewardTitle steward function
   * @param day1Need Day-1 need
   * @param day1Note note
   * @param complianceNeed compliance need
   * @param complianceNote note
   * @param archivalOption read-only or archival option
   * @param archivalNote note
   * @param dataTrust data trust
   * @param proposedClass proposed class
   * @param decidedClass decided class
   * @param conditionText condition
   * @param conditionMet condition met
   * @param dependsOn objects depended on
   * @param loadOrder load order
   * @param financial financial object
   * @param rationale rationale
   * @param status status
   * @param decidedBy approver
   * @param decidedAt time
   */
  public record ObjectResponse(
      String code,
      String name,
      String category,
      List<String> sourceSystems,
      String target,
      String businessOwner,
      String dataSteward,
      String ownerTitle,
      String stewardTitle,
      boolean day1Need,
      String day1Note,
      boolean complianceNeed,
      String complianceNote,
      boolean archivalOption,
      String archivalNote,
      String dataTrust,
      String proposedClass,
      String decidedClass,
      String conditionText,
      boolean conditionMet,
      List<String> dependsOn,
      int loadOrder,
      boolean financial,
      String rationale,
      String status,
      String decidedBy,
      Instant decidedAt) {

    /**
     * Maps an object.
     *
     * @param o object
     * @return response
     */
    public static ObjectResponse from(MigDataObject o) {
      MigDataObject.Criteria c = o.criteria();
      return new ObjectResponse(
          o.getCode(),
          o.getName(),
          o.getCategory(),
          o.sources(),
          o.getTarget(),
          o.getBusinessOwner(),
          o.getDataSteward(),
          o.getOwnerTitle(),
          o.getStewardTitle(),
          c.day1Need(),
          c.day1Note(),
          c.complianceNeed(),
          c.complianceNote(),
          c.archivalOption(),
          c.archivalNote(),
          c.dataTrust(),
          o.getProposedClass().name(),
          o.getDecidedClass() == null ? null : o.getDecidedClass().name(),
          o.getConditionText(),
          o.isConditionMet(),
          o.dependencies(),
          o.getLoadOrder(),
          o.isFinancial(),
          o.getRationale(),
          o.getStatus().name(),
          o.getDecidedBy(),
          o.getDecidedAt());
    }
  }

  /**
   * Data of an object to create or change.
   *
   * @param code code (create only)
   * @param name name
   * @param category category
   * @param sourceSystems source systems
   * @param target target in BIBS
   * @param businessOwner business owner user
   * @param dataSteward data steward user
   * @param day1Need Day-1 need
   * @param day1Note note
   * @param complianceNeed compliance need
   * @param complianceNote note
   * @param archivalOption archival option
   * @param archivalNote note
   * @param dataTrust HIGH, MEDIUM or LOW
   * @param proposedClass proposed class
   * @param conditionText condition of a conditional class
   * @param dependsOn objects depended on
   * @param loadOrder load order
   * @param financial financial object
   * @param rationale rationale
   */
  public record ObjectRequest(
      String code,
      @NotBlank @Size(max = 120) String name,
      @NotBlank String category,
      @NotNull List<String> sourceSystems,
      @NotBlank @Size(max = 300) String target,
      String businessOwner,
      String dataSteward,
      boolean day1Need,
      String day1Note,
      boolean complianceNeed,
      String complianceNote,
      boolean archivalOption,
      String archivalNote,
      @NotBlank String dataTrust,
      @NotNull MigrationClass proposedClass,
      String conditionText,
      List<String> dependsOn,
      int loadOrder,
      boolean financial,
      @NotBlank @Size(max = 2000) String rationale) {

    /**
     * The domain data.
     *
     * @return data
     */
    public MigDataObject.ObjectData toData() {
      return new MigDataObject.ObjectData(
          name,
          category,
          String.join(",", sourceSystems),
          target,
          businessOwner,
          dataSteward,
          null,
          null,
          new MigDataObject.Criteria(
              day1Need,
              day1Note,
              complianceNeed,
              complianceNote,
              archivalOption,
              archivalNote,
              dataTrust),
          proposedClass,
          conditionText,
          dependsOn == null ? null : String.join(",", dependsOn),
          loadOrder,
          financial,
          rationale);
    }
  }

  /**
   * Submission of a decision.
   *
   * @param conditionMet the condition of a conditional class is met
   */
  public record SubmitRequest(boolean conditionMet) {}

  /**
   * A comment of an approval.
   *
   * @param comment comment
   */
  public record CommentRequest(@Size(max = 1000) String comment) {}

  /**
   * A decision.
   *
   * @param decisionNo number
   * @param objectCode object
   * @param proposedClass class
   * @param conditionText condition
   * @param conditionMet condition met
   * @param criteria criteria
   * @param rationale rationale
   * @param status status
   * @param submittedBy submitter
   * @param submittedAt time
   * @param decidedBy approver
   * @param decidedAt time
   * @param returnReason return reason
   */
  public record DecisionResponse(
      String decisionNo,
      String objectCode,
      String proposedClass,
      String conditionText,
      boolean conditionMet,
      String criteria,
      String rationale,
      String status,
      String submittedBy,
      Instant submittedAt,
      String decidedBy,
      Instant decidedAt,
      String returnReason) {

    /**
     * Maps a decision.
     *
     * @param d decision
     * @return response
     */
    public static DecisionResponse from(MigObjectDecision d) {
      return new DecisionResponse(
          d.getDecisionNo(),
          d.getObjectCode(),
          d.getProposedClass().name(),
          d.getConditionText(),
          d.isConditionMet(),
          d.getCriteria(),
          d.getRationale(),
          d.getStatus().name(),
          d.getSubmittedBy(),
          d.getSubmittedAt(),
          d.getDecidedBy(),
          d.getDecidedAt(),
          d.getReturnReason());
    }
  }
}
