/**
 * Configuration Promotion: the configuration catalogue of the platform (every configuration
 * dataset, its owner module, natural key, dependencies and environment-specific values), signed
 * configuration packages, the import into another environment (compatibility check, dry run with a
 * field-level difference, maker-checker approval, apply in one transaction with id remapping by
 * natural key, reconciliation, snapshot and rollback), configuration baselines and drift, and the
 * uploads of the configuration screens.
 *
 * <p>See docs/modules/CONFIG_PROMOTION.md.
 */
package com.iortatechnxt.brokerverse.configpromo;
