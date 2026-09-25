/**
 * Collections (BDOI BRD-4, BRCLXN.001-060; docs/architecture/COLLECTIONS_DESIGN.md): the follow-up
 * of premium receivables read from the Operations invoice ledger. It owns the collection worklist
 * per invoice, assignment, efforts, PR dispositions, promises, installment plans, escalation,
 * billing statements, collector dispositions on unapplied payments and the scheduled Collections
 * files. It posts no journal and no ledger movement: money effects happen in the modules that own
 * them, reached through the {@code opsledger} ports ({@code CollectionFeed} implemented here with
 * transport IN_APP, {@code UnappliedDirectory} and {@code UnappliedDispositionRequests} called).
 *
 * <p>Foundation (wave C0): permissions ({@code CLX_*}), V1000 (roles, grants, LOV types,
 * parameters, alerts, notification events, workflow {@code CLX_ESCALATION}, feed transport), job
 * crons and the navigation entry. Sub-packages are owned by the build waves: {@code worklist,
 * disposition, feed, files, audit, home, common} (C1-A), {@code installment, promise, escalation,
 * billing, bulk} (C1-B) and {@code unapplied} (C1-C). The module depends on {@code opsledger} and
 * the platform / BRD-1 modules only, never on {@code cashiering} or {@code commission}.
 */
package com.iortatechnxt.brokerverse.collections;
