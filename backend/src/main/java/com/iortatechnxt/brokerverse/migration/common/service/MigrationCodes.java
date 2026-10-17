package com.iortatechnxt.brokerverse.migration.common.service;

/**
 * Shared codes of the Data Migration module: module name, audit and file owner entity types,
 * workflows, alert codes and the system user of the loads (DATA_MIGRATION_DESIGN sections 10, 18
 * and 20).
 */
public final class MigrationCodes {

  /** Module code (alerts, approvals, audit). */
  public static final String MODULE = "DATA_MIGRATION";

  /** System user the loaders run as; the batch records the approver of the load. */
  public static final String LOADER_USER = "mig-loader";

  /** Entity type of a data object. */
  public static final String ENTITY_OBJECT = "MigDataObject";

  /** Entity type of a decision. */
  public static final String ENTITY_DECISION = "MigObjectDecision";

  /** Entity type of a code map version. */
  public static final String ENTITY_MAP_VERSION = "MigCodeMapVersion";

  /** Entity type of a layout. */
  public static final String ENTITY_LAYOUT = "MigLayout";

  /** Entity type (and file owner type) of an extract. */
  public static final String ENTITY_EXTRACT = "MigExtract";

  /** Entity type of a batch. */
  public static final String ENTITY_BATCH = "MigBatch";

  /** Entity type of a rollback request (workflow). */
  public static final String ENTITY_ROLLBACK = "MigRollback";

  /** Entity type (and file owner type) of a sign-off. */
  public static final String ENTITY_SIGNOFF = "MigSignoff";

  /** Entity type of a resubmission. */
  public static final String ENTITY_RESUBMISSION = "MigResubmission";

  /** Entity type of a true-up. */
  public static final String ENTITY_TRUEUP = "MigTrueup";

  /** Entity type (and file owner type) of a cutover plan and its tasks. */
  public static final String ENTITY_CUTOVER = "MigCutoverPlan";

  /** Entity type (and attachment type) of an archive record. */
  public static final String ENTITY_ARCHIVE = "MigArchiveRecord";

  /** Owner type of legacy documents staged for the archive document index (H02). */
  public static final String ENTITY_DOCUMENT_DROP = "MigDocumentDrop";

  /** Workflow of a data object decision (gate G1). */
  public static final String WF_DECISION = "MIG_OBJECT_DECISION";

  /** Workflow of a code map version (gate G2). */
  public static final String WF_MAP_VERSION = "MIG_MAP_VERSION";

  /** Workflow of a batch rollback. */
  public static final String WF_ROLLBACK = "MIG_BATCH_ROLLBACK";

  /** Workflow of a true-up. */
  public static final String WF_TRUEUP = "MIG_OPENING_TRUEUP";

  /** Workflow of a resubmission of corrected rows. */
  public static final String WF_RESUBMISSION = "MIG_RESUBMISSION";

  /** Alert: extract rejected. */
  public static final String ALERT_EXTRACT_REJECTED = "MIG_EXTRACT_REJECTED";

  /** Alert: load failed. */
  public static final String ALERT_LOAD_FAILED = "MIG_LOAD_FAILED";

  /** Alert: reconciliation break. */
  public static final String ALERT_RECON_BREAK = "MIG_RECON_BREAK";

  /** Alert: unmapped codes. */
  public static final String ALERT_UNMAPPED = "MIG_UNMAPPED";

  /** Alert: migration clearing not zero. */
  public static final String ALERT_CLEARING = "MIG_CLEARING_NOT_ZERO";

  /** Alert: staging not purged in time. */
  public static final String ALERT_PURGE_OVERDUE = "MIG_STAGING_PURGE_OVERDUE";

  /** Alert: unusual legacy access. */
  public static final String ALERT_ACCESS_UNUSUAL = "MIG_LEGACY_ACCESS_UNUSUAL";

  /** Alert: true-up break. */
  public static final String ALERT_TRUEUP_BREAK = "MIG_TRUEUP_BREAK";

  /** Record class of extract files (5-day lifecycle bucket). */
  public static final String RECORD_CLASS_EXTRACT = "MIGRATION_EXTRACT";

  /** Document type of sign-off evidence. */
  public static final String DOC_EVIDENCE = "MIG_EVIDENCE";

  /** Document type of legacy archive documents. */
  public static final String DOC_LEGACY = "LEGACY_DOCUMENT";

  /** Origin of migrated records in the owning modules. */
  public static final String ORIGIN_MIGRATED = "MIGRATED";

  private MigrationCodes() {}
}
