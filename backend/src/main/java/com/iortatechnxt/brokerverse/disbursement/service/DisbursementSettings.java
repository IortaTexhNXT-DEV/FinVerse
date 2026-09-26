package com.iortatechnxt.brokerverse.disbursement.service;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Names shared by the Disbursement services (ACCOUNTING_DISBURSEMENT_DESIGN 5.1, 7, 9): module and
 * record types, workflow codes, permissions, parameters, number series and screen links.
 */
public final class DisbursementSettings {

  /** Source module of postings and requests created by Disbursement. */
  public static final String MODULE = "DISBURSEMENT";

  /** Record type of vouchers (workflow cases, audit, attachments). */
  public static final String VOUCHER = "DisbursementVoucher";

  /** Record type of payees. */
  public static final String PAYEE = "DisbursementPayee";

  /** Record type of funding requests. */
  public static final String FUNDING = "DisbursementFunding";

  /** Record type of status edits. */
  public static final String STATUS_EDIT = "DisbursementStatusEdit";

  /** Audit record type of payment requests. */
  public static final String REQUEST = "DisbursementIntake";

  /** Audit record type of instruments. */
  public static final String INSTRUMENT = "DisbursementInstrument";

  /** Voucher workflow (DIS 2.7-2.21). */
  public static final String WF_VOUCHER = "DISB_VOUCHER";

  /** Payee workflow (DIS 2.2). */
  public static final String WF_PAYEE = "DISB_PAYEE";

  /** Funding workflow (DIS 2.17). */
  public static final String WF_FUNDING = "DISB_FUNDING";

  /** Status edit workflow (DIS 2.8.5). */
  public static final String WF_STATUS_EDIT = "DISB_STATUS_EDIT";

  /** Processor permission. */
  public static final String PROCESS = "DISB_PROCESS";

  /** Team leader (checker) permission. */
  public static final String REVIEW = "DISB_REVIEW";

  /** Approver permission. */
  public static final String APPROVE = "DISB_APPROVE";

  /** Payee maintenance permission. */
  public static final String PAYEE_MAINTAIN = "DISB_PAYEE_MAINTAIN";

  /** Payee authorisation permission. */
  public static final String PAYEE_AUTHORIZE = "DISB_PAYEE_AUTHORIZE";

  /** Accounting event of an approved voucher. */
  public static final String EVENT_VOUCHER = "DISB_VOUCHER";

  /** Accounting event of a negotiated check. */
  public static final String EVENT_NEGOTIATED = "DISB_CHECK_NEGOTIATED";

  /** Accounting event of a stale check. */
  public static final String EVENT_STALE = "DISB_CHECK_STALE";

  /** Accounting event of an account funding. */
  public static final String EVENT_FUNDING = "DISB_FUND_TRANSFER";

  /** Parameter: days after which a check is stale (DIS 3.26.2). */
  public static final String STALE_DAYS = "DISB_STALE_DAYS";

  /** Parameter: checks credited to the clearing account until negotiated (DIS 3.27.1). */
  public static final String CHECK_CLEARING = "DISB_CHECK_CLEARING";

  /** Parameter: the checks-outstanding clearing account (V893, AQ02). */
  public static final String CHECK_CLEARING_ACCOUNT = "DISB_CHECK_CLEARING_ACCOUNT";

  /** Parameter: gateway types routed straight to the approver (DIS 3.25.0). */
  public static final String AUTO_ROUTING = "DISB_AUTO_APPROVER_ROUTING";

  /** Parameter: what to do with a request whose payee is not maintained (V893). */
  public static final String NO_PAYEE_ACTION = "DISB_NO_PAYEE_ACTION";

  /** Parameter: remaining check leaves that raise CHECK_SERIES_LOW (V893). */
  public static final String CHECK_SERIES_WARNING = "DISB_CHECK_SERIES_WARNING";

  /** Default of {@link #STALE_DAYS}. */
  public static final int DEFAULT_STALE_DAYS = 180;

  /** Return reason of a request whose payee is not maintained. */
  public static final String PAYEE_NOT_MAINTAINED = "PAYEE_NOT_MAINTAINED";

  /** Philippine time, the business day of Disbursement. */
  public static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private DisbursementSettings() {}

  /**
   * The screen of a voucher.
   *
   * @param id voucher
   * @return route
   */
  public static String voucherLink(Long id) {
    return "/disbursement/vouchers/" + id;
  }

  /**
   * The screen of a payee.
   *
   * @param id payee
   * @return route
   */
  public static String payeeLink(Long id) {
    return "/disbursement/payees/" + id;
  }

  /**
   * The screen of a funding request.
   *
   * @param id request
   * @return route
   */
  public static String fundingLink(Long id) {
    return "/disbursement/funding/" + id;
  }

  /**
   * A number series of the year of a date, e.g. {@code DV-2026}.
   *
   * @param prefix prefix
   * @param date date
   * @return series prefix
   */
  public static String series(String prefix, LocalDate date) {
    return prefix + "-" + date.getYear();
  }
}
