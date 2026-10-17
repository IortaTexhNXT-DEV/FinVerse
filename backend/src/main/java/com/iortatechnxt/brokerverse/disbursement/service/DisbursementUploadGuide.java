package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import java.util.List;

/** Shared texts of the disbursement upload templates: where they are uploaded, the modes. */
final class DisbursementUploadGuide {

  /** Who fills the bank confirmation files in. */
  static final String TREASURY = "Disbursement officers, from the bank's confirmation file";

  /** The modes of payment with their labels. */
  static final List<Choice> MODES =
      List.of(
          new Choice("CTA", "Credit to account"),
          new Choice("ATD", "Authority to debit"),
          new Choice("MC_DD", "Manager's check or demand draft"),
          new Choice("CREDIT_TICKET", "Credit ticket"),
          new Choice("TT", "Telegraphic transfer"),
          new Choice("ONLINE_BANKING", "Business online banking"),
          new Choice("CHECK", "Check"));

  private DisbursementUploadGuide() {}

  /**
   * The menu path of an upload tab.
   *
   * @param tab tab label
   * @return path in words
   */
  static String path(String tab) {
    return "Disbursement > Disbursement Uploads, tab " + tab;
  }
}
