package com.iortatechnxt.brokerverse.csf.service;

/**
 * Codes of the Customer Servicing Facility (CUSTOMER_SERVICING_DESIGN sections 4 and 7): audit
 * entity, lists of values, parameters, alerts, document types, messaging purposes and the port of
 * the fulfilment referrals.
 */
public final class CsfCodes {

  /** Module code of the activity, alerts and hand-offs. */
  public static final String MODULE = "CSF";

  /** Audit entity of the contact changes. */
  public static final String ENTITY_CHANGE = "CsfContactChange";

  /** Audit entity of the verifications. */
  public static final String ENTITY_VERIFICATION = "CsfVerification";

  /** Client entity of the audit trail and attachments. */
  public static final String ENTITY_CLIENT = "Client";

  /** Account entity of the attachments. */
  public static final String ENTITY_ACCOUNT = "Account";

  /** Quotation entity of the attachments. */
  public static final String ENTITY_QUOTATION = "Quotation";

  /** List of the CSF statuses. */
  public static final String LOV_STATUS = "CSF_STATUS";

  /** List of the status mapping rows. */
  public static final String LOV_STATUS_MAP = "CSF_STATUS_MAP";

  /** List of the upload document types. */
  public static final String LOV_DOCUMENT_TYPE = "CSF_DOCUMENT_TYPE";

  /** List of the verification checks. */
  public static final String LOV_VERIFY_CHECK = "CSF_VERIFY_CHECK";

  /** List of the change reasons. */
  public static final String LOV_CHANGE_REASON = "CSF_CHANGE_REASON";

  /** List of the contact channels. */
  public static final String LOV_CHANNEL = "CSF_CHANNEL";

  /** List of the client information referred to the fulfilment unit. */
  public static final String LOV_REFERRAL_FIELD = "CSF_REFERRAL_FIELD";

  /** Parameter: months of payment history. */
  public static final String PAYMENT_HISTORY_MONTHS = "CSF_PAYMENT_HISTORY_MONTHS";

  /** Parameter: checks that must match. */
  public static final String VERIFY_MIN_MATCHES = "CSF_VERIFY_MIN_MATCHES";

  /** Parameter: failed verifications of a client in a day before the alert. */
  public static final String VERIFY_MAX_FAILS = "CSF_VERIFY_MAX_FAILS";

  /** Parameter: minutes a passed verification is valid. */
  public static final String VERIFICATION_VALID_MINUTES = "CSF_VERIFICATION_VALID_MINUTES";

  /** Parameter: legacy write-back on. */
  public static final String LEGACY_SYNC_ENABLED = "CSF_LEGACY_SYNC_ENABLED";

  /** Parameter: minimum characters of a name search. */
  public static final String SEARCH_MIN_CHARS = "CSF_SEARCH_MIN_CHARS";

  /** Parameter: maximum clients of a search. */
  public static final String SEARCH_MAX_RESULTS = "CSF_SEARCH_MAX_RESULTS";

  /** Alert: repeated failed verifications. */
  public static final String ALERT_VERIFICATION_FAILED = "CSF_VERIFICATION_FAILED_REPEAT";

  /** Alert: legacy sending failed. */
  public static final String ALERT_SYNC_FAILED = "CSF_SYNC_FAILED";

  /** Document type of the renewal advices (cross-BRD decision D3). */
  public static final String DOC_RENEWAL_ADVICE = "RENEWAL_ADVICE";

  /** Messaging purpose of a resent renewal advice. */
  public static final String PURPOSE_RESEND_RA = "CSF_RESEND_RA";

  /** Operations hand-off port of the referrals to the fulfilment unit. */
  public static final String PORT_FULFILMENT = "CSF_FULFILMENT_REFERRAL";

  /** Permission of the fulfilment unit, notified of a referral. */
  public static final String FULFILMENT_TEAM = "CLIENT_MAINTAIN";

  /** Source of the contact changes in the client master audit. */
  public static final String SOURCE = "CSF";

  /** Prefix of the change numbers ({@code CSF-yyyy-nnnnnn}). */
  public static final String NUMBER_PREFIX = "CSF-";

  private CsfCodes() {}
}
