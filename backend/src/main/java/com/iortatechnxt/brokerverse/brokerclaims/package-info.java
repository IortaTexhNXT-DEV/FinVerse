/**
 * Claims Handling (BDOI BRD-7, BRCLM.001-043; docs/architecture/CLAIMS_BROKING_DESIGN.md): the
 * broker's claim case file on a cover. A claim is recorded against an account (ARN) and policy year
 * with a snapshot of the cover, the premium check of the Operations invoice ledger, the loss, the
 * locations of the cover, one or more insurer claim lines with the reserve and settlement as
 * reported by the insurer, the BDOI status (a list of values mapped to a fixed phase), follow-up,
 * diary and the Claims Handling reports. It posts no journal and keeps no reserve of its own; it
 * reaches Operations only through the {@code opsledger} queries, events and the {@code ClaimsFeed}
 * port it implements. The insurer-side {@code claims} module is unrelated and stays hidden from
 * BDOI roles.
 *
 * <p>Foundation (wave CL0): permissions {@code BCL_*}, V1020 (roles, grants, lists of values and
 * their attributes, status access matrix, handler register, parameters, alerts, notification
 * events, workflow {@code BCL_CLAIM}, loss advice template, retention rule), V1021 (claim tables),
 * the {@code Claim} entity with its embeddables, the {@code ClaimStatusChanged} event, job crons
 * and the navigation entry. Sub-packages are owned by the build waves: {@code cover, claim,
 * insurer, location, feed} (CL1-A) and {@code status, diary, report, home, setup} (CL1-B). The
 * module depends on {@code account}, {@code booking}, {@code opsledger}, {@code catalog}, {@code
 * crm}, {@code nbadmin} and the platform modules; no module depends on it except a future {@code
 * renewal} through {@code ClaimExperienceQueryService}.
 */
package com.iortatechnxt.brokerverse.brokerclaims;
