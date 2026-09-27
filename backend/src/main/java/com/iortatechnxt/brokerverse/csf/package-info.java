/**
 * Customer Servicing Facility (BDOI BRD-9; docs/architecture/CUSTOMER_SERVICING_DESIGN.md): the
 * servicing workspace of the BDO Insure Contact Center. The client, account, invoice, payment,
 * e-policy, renewal advice and document data stay in their owning modules; the CSF reads them
 * through their public query services (customer search, servicing view, CSF status of each
 * account, payment history, documents) and keeps only what is its own: the verification of the
 * caller, the contact changes with their field rows, the referrals of other changes to the
 * fulfilment unit, the legacy contact sync outbox and the agent activity log ({@code csf_*}).
 *
 * <p>Agents hold neither {@code CLIENT_MAINTAIN} nor {@code EPOLICY_SEND}: the CSF endpoints call
 * the contact-only update of the client master ({@code crm.service.ClientService#updateContact})
 * and the e-policy dispatch service of Issuance under the CSF permissions. There is no workflow and
 * no accounting: every action is immediate, audited and logged in {@code csf_activity}.
 *
 * <p>Layout: {@code csf.domain} entities and repositories; {@code csf.service} the services, the
 * job {@code CSF_LEGACY_SYNC} and the retention provider; {@code csf.service.port} the ports
 * {@code ContactSyncGateway} (write-back to QPS / EBIX, default outbox with status NOT_CONFIGURED)
 * and {@code LegacyAccountLookup} (accounts that exist only in the legacy systems, empty by
 * default); {@code csf.api} the REST API; {@code csf.report} the reports CSF-CONTACT-CHANGES and
 * CSF-ACTIVITY.
 *
 * <p>The module depends on {@code crm}, {@code account}, {@code placement}, {@code quotation},
 * {@code issuance}, {@code opsledger}, {@code cashiering} (read), {@code attachment}, {@code
 * messaging} and the platform modules; no module depends on it.
 */
package com.iortatechnxt.brokerverse.csf;
