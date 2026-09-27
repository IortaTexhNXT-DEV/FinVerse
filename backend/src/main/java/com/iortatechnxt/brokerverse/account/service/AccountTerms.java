package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.AccountContact;
import com.iortatechnxt.brokerverse.account.domain.AccountData;
import com.iortatechnxt.brokerverse.account.domain.AccountData.ClientRef;
import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.AccountData.ProductRef;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.List;

/**
 * Product terms an account draft must respect (BRNB.112/113/114): segment offered, period, multi-
 * year term, direct payment, Free First Year and mortgage only where the product allows them; and
 * the conversion of the draft into account data.
 */
final class AccountTerms {

  private static final String PRODUCT = "Product ";
  private static final String BASE_CURRENCY = "PHP";

  private AccountTerms() {}

  /**
   * Checks a draft against its product.
   *
   * @param product product
   * @param draft draft
   */
  static void check(RiskProduct product, AccountDraft draft) {
    segment(product, draft.marketSegment());
    period(draft);
    multiYear(product, draft);
    directPayment(product, draft.paymentArrangement());
    freeFirstYear(product, draft);
    mortgage(product, draft.mortgage());
  }

  private static void segment(RiskProduct product, String segment) {
    String given = segment == null || segment.isBlank() ? null : segment;
    if (!product.allowsSegment(given)) {
      throw new BusinessRuleException(
          "SEGMENT_NOT_ALLOWED",
          PRODUCT + product.getCode() + " is not offered to segment " + segment);
    }
  }

  private static void period(AccountDraft draft) {
    if (draft.periodFrom() != null
        && draft.periodTo() != null
        && !draft.periodTo().isAfter(draft.periodFrom())) {
      throw new BusinessRuleException(
          "ACCOUNT_PERIOD_INVALID", "The period end must be after the period start");
    }
  }

  private static void multiYear(RiskProduct product, AccountDraft draft) {
    boolean allowed =
        product.isMultiYearAllowed() && draft.termYears() <= product.getMaxTermYears();
    if (draft.multiYear() && !allowed) {
      throw new BusinessRuleException(
          "MULTI_YEAR_NOT_ALLOWED",
          PRODUCT
              + product.getCode()
              + " allows a term of up to "
              + product.getMaxTermYears()
              + " year(s)");
    }
  }

  private static void directPayment(RiskProduct product, PaymentArrangement arrangement) {
    if (arrangement == PaymentArrangement.DIRECT_TO_INSURER && !product.isDirectPaymentEligible()) {
      throw new BusinessRuleException(
          "DIRECT_PAYMENT_NOT_ELIGIBLE",
          PRODUCT + product.getCode() + " cannot be paid directly to the insurer");
    }
  }

  private static void freeFirstYear(RiskProduct product, AccountDraft draft) {
    if (draft.ffyStart() != null && !product.isFfyEligible()) {
      throw new BusinessRuleException(
          "FFY_NOT_ELIGIBLE", PRODUCT + product.getCode() + " is not Free First Year eligible");
    }
  }

  private static void mortgage(RiskProduct product, Mortgage mortgage) {
    if (mortgage != null && mortgage.bank() != null && !product.isMortgageApplicable()) {
      throw new BusinessRuleException(
          "MORTGAGE_NOT_APPLICABLE", PRODUCT + product.getCode() + " carries no mortgagee");
    }
  }

  /**
   * Account data of a checked draft.
   *
   * @param draft draft
   * @param client client
   * @param product product
   * @param contact account contact
   * @return data
   */
  static AccountData dataOf(
      AccountDraft draft, ClientRef client, ProductRef product, AccountContact contact) {
    String currency = draft.currency();
    return new AccountData(
        client,
        product,
        draft.marketSegment(),
        draft.sourceChannel(),
        draft.insurerCode(),
        draft.insurerCode() == null ? null : draft.insurerBranch(),
        draft.periodFrom(),
        draft.periodTo(),
        draft.multiYear(),
        draft.multiYear() ? draft.termYears() : 1,
        currency == null || currency.isBlank() ? BASE_CURRENCY : currency,
        draft.paymentArrangement() == null
            ? PaymentArrangement.VIA_BDOI
            : draft.paymentArrangement(),
        draft.mortgage() == null ? Mortgage.NONE : draft.mortgage(),
        contact,
        forKind(product.itemKind(), draft.items()));
  }

  /**
   * Keeps only the block of the line's item kind on each item (a motor draft carries no location, a
   * fire draft no vehicle).
   *
   * @param kind item kind of the product line
   * @param items items as entered
   * @return items with only the relevant block
   */
  static List<RiskItemData> forKind(RiskItemKind kind, List<RiskItemData> items) {
    return items.stream()
        .map(
            i ->
                new RiskItemData(
                    i.description(),
                    i.sumInsured(),
                    i.rate(),
                    i.biLimit(),
                    i.pdLimit(),
                    kind == RiskItemKind.VEHICLE ? i.vehicle() : null,
                    kind == RiskItemKind.PROPERTY_LOCATION ? i.location() : null,
                    kind == RiskItemKind.PERSON ? i.person() : null))
        .toList();
  }
}
