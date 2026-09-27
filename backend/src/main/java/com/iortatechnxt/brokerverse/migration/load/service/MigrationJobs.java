package com.iortatechnxt.brokerverse.migration.load.service;

/** Names of the Data Migration jobs (DATA_MIGRATION_DESIGN section 20). */
public final class MigrationJobs {

  /** Picks up extracts from the inbox. */
  public static final String INTAKE_SCAN = "MIG_INTAKE_SCAN";

  /** Validates planned batches. */
  public static final String VALIDATE = "MIG_VALIDATE";

  /** Loads approved batches. */
  public static final String LOAD = "MIG_LOAD";

  /** Reconciles loaded batches. */
  public static final String RECONCILE = "MIG_RECONCILE";

  /** Purges staging data. */
  public static final String PURGE = "MIG_STAGING_PURGE";

  /** Loads the client delta of the day. */
  public static final String CLIENT_DELTA = "MIG_CLIENT_DELTA";

  private MigrationJobs() {}
}
