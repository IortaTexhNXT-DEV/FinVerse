/**
 * Submitted Policies (BDOI BRD-12; docs/architecture/SUBMITTED_POLICIES_DESIGN.md): the policies a
 * bank borrower bought elsewhere and submitted to the bank to insure the collateral of a loan. BDOI
 * did not place them, so a submitted policy is not an account: it lives in the Submitted Masterlist
 * ({@code sbm_policy}) with its history, is sanitised, matched against the LAMD loan snapshot,
 * classified and bucketed by rules that are data, reviewed for adequacy (IAAF), checked against the
 * insurer limits (TOR), and handed to the Renewal module when it nears expiry. From the hand-off on
 * the renewal is an ordinary BRD-1 account of business type RENEWAL; the masterlist only follows it
 * (placed, booked, not renewed). Money appears only in the handling fee (recognised by Cashiering)
 * and the No Touch service fee.
 *
 * <p>Layout: {@code submitted.domain} holds every entity and repository; each area has a {@code
 * service} and an {@code api} sub-package ({@code intake, masterlist, processing, review, renewal,
 * fee, setup, home, report, seed}); {@code submitted.service} holds the shared codes, parameters
 * and data scope, {@code submitted.service.port} the ports ({@code RenewalHandOff} implemented by
 * Renewal, {@code SubmittedSourceFeed}, {@code MailHouseGateway}, {@code SignatureProvider}) and
 * {@code submitted.service.adapter} their defaults.
 *
 * <p>The module depends on {@code account}, {@code catalog}, {@code crm} (read), {@code booking}
 * (events, service invoices), {@code placement} (hold cover re-assignment), {@code issuance}
 * (extraction), {@code opsledger} (unapplied payment ports) and the platform modules; {@code
 * renewal} depends on it through {@code RenewalHandOff}, and no other module does.
 */
package com.iortatechnxt.brokerverse.submitted;
