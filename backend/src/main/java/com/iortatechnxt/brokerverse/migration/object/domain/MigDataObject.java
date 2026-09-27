package com.iortatechnxt.brokerverse.migration.object.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * A data object of the migration register (BRID 1.1a; FR-DM-001): source systems, target in BIBS,
 * business owner and data steward, the four criteria of the BRD (Day-1 need, compliance need,
 * read-only / archival option, data trust), the proposed and the decided class, dependencies and
 * load order. An object is never deleted, only decided as EXCLUDED.
 */
@Entity
@Table(name = "mig_data_object")
public class MigDataObject extends BaseEntity {

  @Column(nullable = false, length = 10, updatable = false)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 20)
  private String category;

  @Column(name = "source_systems", nullable = false, length = 100)
  private String sourceSystems;

  @Column(nullable = false, length = 300)
  private String target;

  @Column(name = "business_owner", length = 50)
  private String businessOwner;

  @Column(name = "data_steward", length = 50)
  private String dataSteward;

  @Column(name = "owner_title", length = 200)
  private String ownerTitle;

  @Column(name = "steward_title", length = 200)
  private String stewardTitle;

  @Column(name = "day1_need", nullable = false)
  private boolean day1Need;

  @Column(name = "day1_note", length = 300)
  private String day1Note;

  @Column(name = "compliance_need", nullable = false)
  private boolean complianceNeed;

  @Column(name = "compliance_note", length = 300)
  private String complianceNote;

  @Column(name = "archival_option", nullable = false)
  private boolean archivalOption;

  @Column(name = "archival_note", length = 300)
  private String archivalNote;

  @Column(name = "data_trust", nullable = false, length = 10)
  private String dataTrust;

  @Enumerated(EnumType.STRING)
  @Column(name = "proposed_class", nullable = false, length = 20)
  private MigrationClass proposedClass;

  @Enumerated(EnumType.STRING)
  @Column(name = "decided_class", length = 20)
  private MigrationClass decidedClass;

  @Column(name = "condition_text", length = 500)
  private String conditionText;

  @Column(name = "condition_met", nullable = false)
  private boolean conditionMet;

  @Column(name = "depends_on", length = 200)
  private String dependsOn;

  @Column(name = "load_order", nullable = false)
  private int loadOrder;

  @Column(nullable = false)
  private boolean financial;

  @Column(nullable = false, length = 2000)
  private String rationale;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ObjectStatus status = ObjectStatus.PROPOSED;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  protected MigDataObject() {}

  /**
   * A new object, PROPOSED.
   *
   * @param code object code
   * @param data object data
   */
  public MigDataObject(String code, ObjectData data) {
    this.code = code;
    assign(data);
  }

  /**
   * Replaces the maintainable data (the decided class stays until a new decision).
   *
   * @param data object data
   */
  public void apply(ObjectData data) {
    assign(data);
  }

  private void assign(ObjectData data) {
    this.name = data.name();
    this.category = data.category();
    this.sourceSystems = data.sourceSystems();
    this.target = data.target();
    this.businessOwner = data.businessOwner();
    this.dataSteward = data.dataSteward();
    this.ownerTitle = data.ownerTitle();
    this.stewardTitle = data.stewardTitle();
    Criteria c = data.criteria();
    this.day1Need = c.day1Need();
    this.day1Note = c.day1Note();
    this.complianceNeed = c.complianceNeed();
    this.complianceNote = c.complianceNote();
    this.archivalOption = c.archivalOption();
    this.archivalNote = c.archivalNote();
    this.dataTrust = c.dataTrust();
    this.proposedClass = data.proposedClass();
    this.conditionText = data.conditionText();
    this.dependsOn = data.dependsOn();
    this.loadOrder = data.loadOrder();
    this.financial = data.financial();
    this.rationale = data.rationale();
  }

  /** The decision was submitted to the data owner. */
  public void submitted() {
    if (status == ObjectStatus.FOR_DECISION) {
      throw new BusinessRuleException(
          "MIG_DECISION_PENDING",
          "A decision of object " + code + " is already waiting for approval");
    }
    this.status = ObjectStatus.FOR_DECISION;
  }

  /**
   * The data owner approved the class.
   *
   * @param approved class
   * @param met condition of a conditional class met
   * @param user approver
   * @param when time
   */
  public void decide(MigrationClass approved, boolean met, String user, Instant when) {
    this.decidedClass = approved;
    this.conditionMet = met;
    this.status = ObjectStatus.DECIDED;
    this.decidedBy = user;
    this.decidedAt = when;
  }

  /** The data owner returned the decision; the previous decision (if any) stays in force. */
  public void returned() {
    this.status = decidedClass == null ? ObjectStatus.PROPOSED : ObjectStatus.DECIDED;
  }

  /**
   * Whether the decided class allows loading into BIBS business tables.
   *
   * @return true when decided MIGRATE, CARRY_FORWARD or a met CONDITIONAL
   */
  public boolean loadable() {
    return decidedClass != null && decidedClass.loadable(conditionMet);
  }

  /**
   * Whether the decided class is ARCHIVE (loaded into the legacy archive only).
   *
   * @return true for ARCHIVE
   */
  public boolean archive() {
    return decidedClass == MigrationClass.ARCHIVE;
  }

  /**
   * Codes of the objects this object depends on.
   *
   * @return codes
   */
  public List<String> dependencies() {
    return split(dependsOn);
  }

  /**
   * Source systems of the object.
   *
   * @return codes
   */
  public List<String> sources() {
    return split(sourceSystems);
  }

  private static List<String> split(String csv) {
    if (csv == null || csv.isBlank()) {
      return List.of();
    }
    return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  public Criteria criteria() {
    return new Criteria(
        day1Need,
        day1Note,
        complianceNeed,
        complianceNote,
        archivalOption,
        archivalNote,
        dataTrust);
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }

  public String getSourceSystems() {
    return sourceSystems;
  }

  public String getTarget() {
    return target;
  }

  public String getBusinessOwner() {
    return businessOwner;
  }

  public String getDataSteward() {
    return dataSteward;
  }

  public String getOwnerTitle() {
    return ownerTitle;
  }

  public String getStewardTitle() {
    return stewardTitle;
  }

  public MigrationClass getProposedClass() {
    return proposedClass;
  }

  public MigrationClass getDecidedClass() {
    return decidedClass;
  }

  public String getConditionText() {
    return conditionText;
  }

  public boolean isConditionMet() {
    return conditionMet;
  }

  public String getDependsOn() {
    return dependsOn;
  }

  public int getLoadOrder() {
    return loadOrder;
  }

  public boolean isFinancial() {
    return financial;
  }

  public String getRationale() {
    return rationale;
  }

  public ObjectStatus getStatus() {
    return status;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  /**
   * The four criteria of the BRD.
   *
   * @param day1Need needed on Day 1
   * @param day1Note note
   * @param complianceNeed needed for compliance
   * @param complianceNote note
   * @param archivalOption a read-only or archival option exists
   * @param archivalNote note
   * @param dataTrust HIGH, MEDIUM or LOW
   */
  public record Criteria(
      boolean day1Need,
      String day1Note,
      boolean complianceNeed,
      String complianceNote,
      boolean archivalOption,
      String archivalNote,
      String dataTrust) {}

  /**
   * Maintainable data of an object.
   *
   * @param name name
   * @param category category (list MIG_OBJECT_CATEGORY)
   * @param sourceSystems source systems, comma separated
   * @param target target in BIBS
   * @param businessOwner user of the business owner
   * @param dataSteward user of the data steward
   * @param ownerTitle function of the owner
   * @param stewardTitle function of the steward
   * @param criteria the four criteria
   * @param proposedClass proposed class
   * @param conditionText condition of a conditional class
   * @param dependsOn objects depended on, comma separated
   * @param loadOrder load order
   * @param financial open items, unapplied payments or trial balance (error-rate limit)
   * @param rationale rationale
   */
  public record ObjectData(
      String name,
      String category,
      String sourceSystems,
      String target,
      String businessOwner,
      String dataSteward,
      String ownerTitle,
      String stewardTitle,
      Criteria criteria,
      MigrationClass proposedClass,
      String conditionText,
      String dependsOn,
      int loadOrder,
      boolean financial,
      String rationale) {}
}
