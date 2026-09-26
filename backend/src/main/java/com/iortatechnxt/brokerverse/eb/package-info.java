/**
 * Employee Benefits (BDOI BRD-8, BRID-001-030; docs/architecture/EMPLOYEE_BENEFITS_DESIGN.md): the
 * EB programmes of corporate clients (HMO, group life, group personal accident) and their yearly
 * cycles - renewal advice, feedback, BOR, franchise, TOR, insurer requests and proposals,
 * comparative with sign-off and value-threshold approval, client confirmation - up to the placement
 * trigger, which creates one account per benefit line through {@code AccountService.createDraft}
 * (business type from the cycle, shared work item BT0). From there the BRD-1 spine (placement,
 * issuance, booking) and Operations work unchanged; EB never posts a journal. Servicing follows:
 * member rosters and member changes (endorsement requests to Adjustment), tracked items with
 * follow-ups, insurer SOA intake, and the EB reports.
 *
 * <p>Scope: BDOI Drop 2 "Employee Benefits (No Portal Feature)". The partner portal of the design
 * (sections 2.2, 6.3, 10.2) is parked: insurers and client HR send files by e-mail and the EB users
 * record them with the source INSURER or CLIENT.
 *
 * <p>Foundation (wave E0): permissions {@code EB_*}; V1030 (roles, grants, lists of values, EB
 * document types, parameters, exception codes, notification events, workflows {@code EB_CYCLE},
 * {@code EB_FRANCHISE}, {@code EB_MEMBER_CHANGE}, {@code EB_SOA}, templates); V1031 (document
 * access classes, process tags, insurer billing number); V1033 (programme, line, contact, cycle,
 * document register, activity log) with their entities in {@code eb.domain}; the constants {@link
 * com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes} and {@link
 * com.iortatechnxt.brokerverse.eb.domain.EbCodes}, the parameters {@link
 * com.iortatechnxt.brokerverse.eb.service.EbParameters}, the event {@link
 * com.iortatechnxt.brokerverse.eb.domain.EbCycleStageChanged}, the report category and the
 * navigation entry. The build waves own the rest by class: E1-B the marketing cycle (sub-packages
 * {@code programme, cycle, bor, franchise, tor, proposal, comparative, placement}), E1-C servicing
 * and reports ({@code roster, member, tracked, soa, report}). The module depends on {@code crm},
 * {@code catalog}, {@code account}, {@code booking} and {@code opsledger} (read), {@code
 * adjustment} and the platform modules; no module depends on it.
 */
package com.iortatechnxt.brokerverse.eb;
