/**
 * Renewal (BDOI BRD-6, RMEL Phase 2 Online Dispositioning with Addendum 1 and the Workshop
 * addendum; docs/architecture/RENEWAL_DESIGN.md): the renewal of the client policies placed and
 * booked by BDOI. Before the renewal is agreed, an expiring policy is a renewal candidate -
 * extracted from the Operations ledger (or from the migrated policy headers at go-live), checked,
 * classified Clean / Review / Exception, initiated, dispositioned by Marketing, processed with the
 * insurer, offered to the client in a Renewal Advice and accepted. Once the renewal proceeds it is
 * an ordinary BRD-1 account of business type RENEWAL (shared work item BT0) that the New Business
 * modules place, issue and book unchanged; the candidate closes RENEWED when that account is
 * booked. Renewal posts no journal.
 *
 * <p>Layout, as in the other later-BRD modules: {@code renewal.domain} holds every entity and
 * repository; each feature has a {@code service} and an {@code api} sub-package ({@code candidate,
 * extraction, check, rules, setup, remap, assignment, transfer, disposition, review, nbpath,
 * client, processing, insurer, lamd, letter, acceptance, progression, followup, home, report,
 * retention, seed}); {@code renewal.service} holds the shared codes, parameters and data scope, and
 * {@code renewal.service.port} the ports implemented by other modules ({@code LegacyPolicySource}
 * by Data Migration, {@code RecipientPolicy} by messaging once BDOI defines the recipient rules).
 *
 * <p>The module depends on {@code opsledger}, {@code account}, {@code catalog}, {@code crm}, {@code
 * booking}, {@code placement}, {@code quotation}, {@code nonpackage}, {@code adjustment}, {@code
 * brokerclaims} (read) and the platform modules; no module depends on it.
 */
package com.iortatechnxt.brokerverse.renewal;
