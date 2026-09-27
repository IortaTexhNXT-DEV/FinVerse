package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkFileReader;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile.RawRow;
import com.iortatechnxt.brokerverse.commission.domain.DpBilling;
import com.iortatechnxt.brokerverse.commission.domain.DpBillingRepository;
import com.iortatechnxt.brokerverse.commission.service.DpFeedbackService.Answer;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInContext;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Handler of the flow-in feed {@code INSURER_DP_RESPONSE} (CMRID.009/012): the billing file
 * returned by the insurer with a Decision (APPROVED / REJECTED), Reason and Comment per account.
 * Each answered account is one flow-in record; an account already answered is skipped. Insurer
 * channels are parked (OQ38): the file is uploaded.
 */
@Component
public class DpResponseHandler implements FlowInHandler {

  /** Feed code. */
  public static final String FEED = "INSURER_DP_RESPONSE";

  private final BulkFileReader reader;
  private final DpBillingRepository billings;
  private final DpFeedbackService feedback;

  /**
   * Creates the handler.
   *
   * @param reader file reader
   * @param billings billings
   * @param feedback insurer answers
   */
  public DpResponseHandler(
      BulkFileReader reader, DpBillingRepository billings, DpFeedbackService feedback) {
    this.reader = reader;
    this.billings = billings;
    this.feedback = feedback;
  }

  @Override
  public String feedCode() {
    return FEED;
  }

  @Override
  public void handle(FlowInFile file, FlowInContext context) {
    ParsedFile parsed = reader.read(file.fileName(), file.content());
    for (String header : List.of(DpResponseLayout.BILLING, DpResponseLayout.INVOICE)) {
      if (!parsed.headers().contains(header)) {
        throw new BusinessRuleException(
            "DP_RESPONSE_LAYOUT", "The insurer's answer has no '" + header + "' column");
      }
    }
    for (RawRow row : parsed.rows()) {
      Map<String, String> v = row.values();
      String decision = text(v, DpResponseLayout.DECISION);
      if (decision == null) {
        continue;
      }
      String billingNo = text(v, DpResponseLayout.BILLING);
      String invoice = text(v, DpResponseLayout.INVOICE);
      context.accept(
          billingNo + ":" + invoice, v.toString(), () -> answer(billingNo, invoice, decision, v));
    }
  }

  private String answer(String billingNo, String invoice, String decision, Map<String, String> v) {
    DpBilling billing =
        billings
            .findByBillingNo(billingNo == null ? "" : billingNo)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "DP_BILLING_UNKNOWN", "Billing " + billingNo + " does not exist"));
    boolean approved = approved(decision);
    feedback.answer(
        billing.getId(),
        List.of(
            new Answer(
                invoice,
                approved,
                text(v, DpResponseLayout.REASON),
                text(v, DpResponseLayout.COMMENT))));
    return billingNo + " " + invoice + (approved ? " approved" : " rejected");
  }

  /**
   * Reads a decision.
   *
   * @param decision APPROVED / YES / Y or REJECTED / NO / N
   * @return true when approved
   */
  static boolean approved(String decision) {
    String d = decision.strip().toUpperCase(Locale.ROOT);
    if (List.of("APPROVED", "APPROVE", "YES", "Y").contains(d)) {
      return true;
    }
    if (List.of("REJECTED", "REJECT", "NO", "N").contains(d)) {
      return false;
    }
    throw new BusinessRuleException(
        "DP_RESPONSE_DECISION", "Decision '" + decision + "' is neither APPROVED nor REJECTED");
  }

  private static String text(Map<String, String> v, String header) {
    String value = v.get(header);
    return value == null || value.isBlank() ? null : value.strip();
  }
}
