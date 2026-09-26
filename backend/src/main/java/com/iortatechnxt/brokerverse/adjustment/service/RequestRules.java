package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.Computation;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest.Content;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestClass;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestTerms;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Validation of endorsement requests (ADJID.001-004/010/023):
 *
 * <ul>
 *   <li>the endorsement type decides the class (parent FINANCIAL / NON_FINANCIAL / INTERNAL); a
 *       financial request has an Annex V request type; a non-financial one carries no amount;
 *   <li>the inputs of the computation (cancellation reason, TSI change, amounts) and the dates
 *       (effective date within the policy year, period changes);
 * </ul>
 *
 * The checks against the invoice and the other requests are in {@link RequestConflicts}.
 */
@Component
public class RequestRules {

  /** List of endorsement types. */
  public static final String TYPE_LOV = "ENDORSEMENT_TYPE";

  /** List of Annex V request types. */
  public static final String REQUEST_TYPE_LOV = "ENDORSEMENT_REQUEST_TYPE";

  /** List of cancellation reasons. */
  public static final String REASON_LOV = "CANCELLATION_REASON";

  private static final String FIN_EXTENSION = "FIN_EXTENSION";

  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the rules.
   *
   * @param lovs lists of values
   * @param clock clock
   */
  public RequestRules(LovService lovs, Clock clock) {
    this.lovs = lovs;
    this.clock = clock;
  }

  /**
   * Validates a draft and derives its class, computation, approval and sign.
   *
   * @param draft draft
   * @param invoice invoice of the request
   * @return content of the request
   */
  public Content content(RequestDraft draft, OpsInvoice invoice) {
    RequestTerms terms = draft.terms();
    requireComplete(terms);
    LocalDate today = LocalDate.now(clock);
    LovValue type = lovs.requireValid(TYPE_LOV, terms.endorsementType(), today);
    RequestClass requestClass = classOf(type);
    lovs.validateOptional(REQUEST_TYPE_LOV, terms.requestType(), today);
    lovs.validateOptional(REASON_LOV, terms.reasonCode(), today);
    Computation computation =
        requestClass == RequestClass.NON_FINANCIAL
            ? Computation.NONE
            : Computation.forRequestType(terms.requestType());
    requireClassRules(requestClass, terms, draft.amounts());
    requireInputs(computation, terms, draft.amounts());
    RequestChecks.requireDates(invoice, terms);
    AmountInput amounts = computation == Computation.AMOUNTS ? draft.amounts() : AmountInput.NONE;
    boolean needsApproval =
        computation.isFinancial() || FIN_EXTENSION.equals(terms.endorsementType());
    return new Content(
        requestClass,
        computation,
        terms,
        amounts,
        needsApproval,
        RequestChecks.negative(computation, terms, amounts));
  }

  private static void requireComplete(RequestTerms terms) {
    boolean missing =
        terms == null
            || terms.endorsementType() == null
            || terms.effectiveDate() == null
            || terms.description() == null
            || terms.description().isBlank();
    if (missing) {
      throw new BusinessRuleException(
          "ADJ_REQUEST_INCOMPLETE", "Enter the endorsement type, effective date and description");
    }
  }

  private static RequestClass classOf(LovValue type) {
    String parent = type.getParentCode();
    for (RequestClass c : RequestClass.values()) {
      if (c.name().equals(parent)) {
        return c;
      }
    }
    throw new BusinessRuleException(
        "ADJ_TYPE_WITHOUT_CLASS",
        "Endorsement type " + type.getCode() + " has no class (FINANCIAL / NON_FINANCIAL)");
  }

  private static void requireClassRules(RequestClass c, RequestTerms t, AmountInput a) {
    boolean requestType = t.requestType() != null && !t.requestType().isBlank();
    if (c == RequestClass.FINANCIAL && !requestType) {
      throw new BusinessRuleException(
          "ADJ_REQUEST_TYPE_REQUIRED", "Select the request type of a financial endorsement");
    }
    boolean money =
        requestType || t.sumInsuredChange() != null || a.changesPremium() || a.changesCommission();
    if (c == RequestClass.NON_FINANCIAL && money) {
      throw new BusinessRuleException(
          "ADJ_NON_FINANCIAL_WITH_AMOUNTS",
          "A non-financial endorsement carries no request type, sum insured or amount change");
    }
  }

  private static void requireInputs(Computation c, RequestTerms t, AmountInput a) {
    if (c.isCancellation()) {
      requireReason(t);
    } else if (c == Computation.SUM_INSURED) {
      requireTsiChange(t);
    } else if (c == Computation.AMOUNTS) {
      requireAmounts(a);
    }
  }

  private static void requireReason(RequestTerms t) {
    if (t.reasonCode() == null || t.reasonCode().isBlank()) {
      throw new BusinessRuleException(
          "ADJ_CANCELLATION_REASON_REQUIRED", "Select the reason for cancellation");
    }
  }

  private static void requireTsiChange(RequestTerms t) {
    if (t.sumInsuredChange() == null || t.sumInsuredChange().signum() == 0) {
      throw new BusinessRuleException(
          "ADJ_TSI_CHANGE_REQUIRED", "Enter the increase or decrease of the total sum insured");
    }
  }

  private static void requireAmounts(AmountInput a) {
    if (!a.changesPremium() && !a.changesCommission()) {
      throw new BusinessRuleException(
          "ADJ_AMOUNTS_REQUIRED", "Enter the premium, charges or commission change");
    }
  }
}
