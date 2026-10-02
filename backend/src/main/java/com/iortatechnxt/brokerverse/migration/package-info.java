/**
 * Data Migration (BDOI BRD-13, BRID 1.1a-12.1; docs/architecture/DATA_MIGRATION_DESIGN.md): the
 * Migration Console that takes the legacy data of EBIX, QPS, ISYS, CMS and the Excel trackers into
 * BIBS for the single go-live of January 2028.
 *
 * <p>Lifecycle: every data object of the register ({@code object}) is decided (gate G1) as MIGRATE,
 * CARRY_FORWARD, ARCHIVE, EXCLUDED or CONDITIONAL; its layouts and versioned code maps ({@code
 * mapping}) are approved (G2); extracts arrive with a control file and are checked, masked outside
 * production and staged ({@code intake}); a batch validates the staged rows against the
 * data-quality rules ({@code quality}, G3), is approved (G4) and loaded through the owning module's
 * service by one {@link com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader} per
 * object ({@code load}); the load is reconciled on five levels ({@code recon}, G5) and the object
 * is accepted ({@code signoff}, G6). Clients are matched and deduplicated first ({@code matching}).
 * The cutover plans, go / no-go (G7), run-off and decommissioning are in {@code cutover}; the
 * FY2027 opening-balance true-ups in {@code trueup}; the read-only archive and its access log in
 * {@code archive}; the ports served to Renewal and Customer Servicing in {@code legacy}.
 *
 * <p>Business tables are written only through the owning module's services; this module writes only
 * its own {@code mig_*} tables (Flyway V1080-V1085, seed data V1980-V1982). No module depends on
 * it.
 */
package com.iortatechnxt.brokerverse.migration;
