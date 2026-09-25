package com.iortatechnxt.brokerverse.collections.unapplied.service;

import com.iortatechnxt.brokerverse.collections.common.service.LovAttributes;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The rules of the collector dispositions of unapplied payments (BRCLXN.037-039, 047/048): only
 * active values of LOV {@code CLX_UPP_DISPOSITION} can be chosen; the attribute {@code
 * requires_invoice} makes the invoice number mandatory, validated by the parameter {@code
 * CLX_INVOICE_NO_PATTERN} (EBIX {@code I########} or BrokerVerse {@code BI-...}, CQ13) and by its
 * existence in the invoice ledger of the company; {@code cashiering_action} says what Cashiering is
 * asked to do (NONE = documentation only).
 */
@Component
@Transactional(readOnly = true)
public class UnappliedRules {

  /** Parameter of the invoice number pattern. */
  public static final String INVOICE_PATTERN = "CLX_INVOICE_NO_PATTERN";

  /** Cashiering action of a documentation-only disposition. */
  public static final String NONE = "NONE";

  private static final String DEFAULT_PATTERN = "^(I\\d{8}|BI-.+)$";

  private final LovService lovs;
  private final LovAttributes attributes;
  private final SystemParameterService parameters;
  private final InvoiceLedgerQueryService ledger;
  private final Clock clock;

  /**
   * Creates the rules.
   *
   * @param lovs lists of values
   * @param attributes Collections LOV attributes
   * @param parameters business parameters
   * @param ledger invoice ledger
   * @param clock clock
   */
  public UnappliedRules(
      LovService lovs,
      LovAttributes attributes,
      SystemParameterService parameters,
      InvoiceLedgerQueryService ledger,
      Clock clock) {
    this.lovs = lovs;
    this.attributes = attributes;
    this.parameters = parameters;
    this.ledger = ledger;
    this.clock = clock;
  }

  /**
   * The active dispositions with their rule, in LOV order (the disposition form).
   *
   * @return rules
   */
  public List<Rule> active() {
    return lovs.activeValues(LovAttributes.UPP_DISPOSITION, LocalDate.now(clock)).stream()
        .map(v -> rule(v.getCode(), v.getLabel()))
        .toList();
  }

  /**
   * The rule of an active disposition; a deactivated or unknown value is refused (BRCLXN.038).
   *
   * @param code disposition code
   * @return rule
   */
  public Rule require(String code) {
    LovValue value = lovs.requireValid(LovAttributes.UPP_DISPOSITION, code, LocalDate.now(clock));
    return rule(value.getCode(), value.getLabel());
  }

  private Rule rule(String code, String label) {
    Map<String, String> a = attributes.of(LovAttributes.UPP_DISPOSITION, code);
    return new Rule(
        code,
        label,
        Boolean.parseBoolean(a.get("requires_invoice")),
        a.getOrDefault("cashiering_action", NONE));
  }

  /**
   * The invoice number pattern of the parameter.
   *
   * @return regular expression
   */
  public String invoicePattern() {
    return parameters.text(INVOICE_PATTERN, DEFAULT_PATTERN).strip();
  }

  /**
   * Validates the target invoice of a disposition (BRCLXN.047/048): the format of the parameter,
   * then the invoice ledger of the company.
   *
   * @param companyId company
   * @param invoiceNo invoice number
   * @return the invoice
   */
  public OpsInvoice requireInvoice(Long companyId, String invoiceNo) {
    if (invoiceNo == null || invoiceNo.isBlank()) {
      throw new BusinessRuleException(
          "CLX_INVOICE_REQUIRED", "Enter the invoice number to apply the payment to");
    }
    String clean = invoiceNo.strip();
    if (!matches(clean)) {
      throw new BusinessRuleException(
          "CLX_INVOICE_FORMAT",
          "Invoice number "
              + clean
              + " does not have the expected format ("
              + invoicePattern()
              + ")");
    }
    return ledger
        .find(clean)
        .filter(i -> i.getCompanyId().equals(companyId))
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "CLX_INVOICE_UNKNOWN", "Invoice " + clean + " is not in the invoice ledger"));
  }

  private boolean matches(String invoiceNo) {
    try {
      return Pattern.compile(invoicePattern()).matcher(invoiceNo).matches();
    } catch (PatternSyntaxException ex) {
      return Pattern.compile(DEFAULT_PATTERN).matcher(invoiceNo).matches();
    }
  }

  /**
   * The rule of a collector disposition.
   *
   * @param code disposition code
   * @param label label
   * @param requiresInvoice whether the invoice number is mandatory
   * @param cashieringAction APPLY_TO_INVOICE, REFUND, RECLASS, TRANSFER or NONE
   */
  public record Rule(String code, String label, boolean requiresInvoice, String cashieringAction) {

    /**
     * Whether Cashiering is asked to act.
     *
     * @return false for NONE
     */
    public boolean sendsRequest() {
      return !NONE.equals(cashieringAction);
    }
  }
}
