package com.iortatechnxt.brokerverse.eb.domain;

/**
 * Workflow codes, entity types (attachments, work cases, audit) and document number series of
 * Employee Benefits (EMPLOYEE_BENEFITS_DESIGN sections 7 and 8.4; workflows seeded by V1030).
 * Shared by the build waves; changed only by additions.
 */
public final class EbCodes {

  /** Workflow of a cycle (design 7.1). */
  public static final String WORKFLOW_CYCLE = "EB_CYCLE";

  /** Workflow of a franchise request (BRID-026, 027, 029). */
  public static final String WORKFLOW_FRANCHISE = "EB_FRANCHISE";

  /** Workflow of a member change (BRID-013, 025). */
  public static final String WORKFLOW_MEMBER_CHANGE = "EB_MEMBER_CHANGE";

  /** Workflow of an insurer SOA (BRID-021). */
  public static final String WORKFLOW_SOA = "EB_SOA";

  /** Entity type of a programme. */
  public static final String ENTITY_PROGRAMME = "EbProgramme";

  /** Entity type of a cycle (work case of EB_CYCLE). */
  public static final String ENTITY_CYCLE = "EbCycle";

  /** Entity type of a franchise request (work case of EB_FRANCHISE). */
  public static final String ENTITY_FRANCHISE = "EbFranchise";

  /** Entity type of a member change (work case of EB_MEMBER_CHANGE). */
  public static final String ENTITY_MEMBER_CHANGE = "EbMemberChange";

  /** Entity type of an insurer SOA (work case of EB_SOA). */
  public static final String ENTITY_SOA = "EbSoa";

  /** Programme numbers {@code EBP-<yyyy>-nnnnnn}. */
  public static final String PREFIX_PROGRAMME = "EBP";

  /** Cycle numbers {@code EBC-<yyyy>-nnnnnn}. */
  public static final String PREFIX_CYCLE = "EBC";

  /** Franchise request numbers {@code EBF-<yyyy>-nnnnnn}. */
  public static final String PREFIX_FRANCHISE = "EBF";

  /** Insurer request numbers {@code EBR-<yyyy>-nnnnnn}. */
  public static final String PREFIX_REQUEST = "EBR";

  /** Proposal numbers {@code EBPR-<yyyy>-nnnnnn}. */
  public static final String PREFIX_PROPOSAL = "EBPR";

  /** Comparative numbers {@code EBCA-<yyyy>-nnnnnn}. */
  public static final String PREFIX_COMPARATIVE = "EBCA";

  /** Member change numbers {@code EBM-<yyyy>-nnnnnn}. */
  public static final String PREFIX_MEMBER_CHANGE = "EBM";

  /** SOA intake numbers {@code EBS-<yyyy>-nnnnnn}. */
  public static final String PREFIX_SOA = "EBS";

  /** List of values of the benefit lines. */
  public static final String LOV_BENEFIT_LINE = "EB_BENEFIT_LINE";

  /** List of values of the teams. */
  public static final String LOV_TEAM = "EB_TEAM";

  private EbCodes() {}

  /**
   * The number series of a prefix and year, for {@code DocumentNumberService.next}.
   *
   * @param prefix one of the {@code PREFIX_*} constants
   * @param year year
   * @return series, e.g. {@code EBP-2026}
   */
  public static String series(String prefix, int year) {
    return prefix + "-" + year;
  }
}
