package com.iortatechnxt.brokerverse.renewal.marketing.service;

import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.RiskDetails;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalDraft;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalService;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft;
import com.iortatechnxt.brokerverse.quotation.service.QuotationService;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The New Business path of a renewal (FR-RN-048): Start NB Path creates a package quotation or a
 * non-package proposal request pre-filled from the expiring account and linked by the renewal
 * reference; its accounts are created with business type RENEWAL and point to the expiring account.
 * One NB path per renewal.
 */
@Service
@Transactional
public class NbPathService {

  private static final String PHP = "PHP";

  private final RenewalRecords records;
  private final AccountQueryService accounts;
  private final QuotationService quotations;
  private final ProposalService proposals;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param accounts expiring accounts
   * @param quotations quotations
   * @param proposals proposal requests
   * @param audit audit trail
   */
  public NbPathService(
      RenewalRecords records,
      AccountQueryService accounts,
      QuotationService quotations,
      ProposalService proposals,
      AuditTrailService audit) {
    this.records = records;
    this.accounts = accounts;
    this.quotations = quotations;
    this.proposals = proposals;
    this.audit = audit;
  }

  /**
   * Starts the New Business path.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param kind QUOTATION or PROPOSAL; by default from the disposition
   * @return the quotation or PRF number
   */
  public String start(Long companyId, String renewalRef, Kind kind) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    RenewalRecords.requireStage(c, RenewalStage.NB_PATH);
    if (c.getQuotationRef() != null || c.getProposalRef() != null) {
      throw new BusinessRuleException(
          "RNW_NB_PATH_EXISTS",
          "Renewal "
              + c.getRenewalRef()
              + " already has "
              + (c.getQuotationRef() != null
                  ? "quotation " + c.getQuotationRef()
                  : "proposal request " + c.getProposalRef()));
    }
    Kind chosen = kind != null ? kind : defaultKind(c);
    Terms terms = terms(c);
    String renewalOf = c.getExpiringArn() != null ? c.getExpiringArn() : c.getSourceRef();
    String number;
    if (chosen == Kind.QUOTATION) {
      Quotation q =
          quotations.createForRenewal(companyId, quotation(terms), c.getRenewalRef(), renewalOf);
      c.linkNewBusiness(q.getQuotationNo(), null);
      number = q.getQuotationNo();
    } else {
      ProposalRequest p =
          proposals.createForRenewal(companyId, proposal(terms), c.getRenewalRef(), renewalOf);
      c.linkNewBusiness(null, p.getPrfNo());
      number = p.getPrfNo();
    }
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.CREATE,
        "New Business path started with " + number);
    return number;
  }

  private static Kind defaultKind(RenewalCandidate c) {
    return c.getDisposition().code() == RenewalDisposition.FOR_PROPOSAL
        ? Kind.PROPOSAL
        : Kind.QUOTATION;
  }

  private Terms terms(RenewalCandidate c) {
    CandidateSnapshot s = c.getSnapshot();
    String product =
        c.getResolvedProductCode() != null
            ? c.getResolvedProductCode()
            : s.product() == null ? null : s.product().productCode();
    if (c.getExpiringArn() != null) {
      AccountDraft d = accounts.draftOf(c.getExpiringArn());
      LocalDate from = d.periodTo();
      Period term = Period.between(d.periodFrom(), d.periodTo());
      return new Terms(
          d.clientId(),
          product != null ? product : d.productCode(),
          d.marketSegment(),
          d.sourceChannel(),
          d.currency(),
          d.insurerCode(),
          d.insurerBranch(),
          from,
          from.plus(term),
          d.paymentArrangement() == PaymentArrangement.DIRECT_TO_INSURER,
          d.items());
    }
    LocalDate from = s.expiryDate();
    return new Terms(
        s.client() == null ? null : s.client().clientId(),
        product,
        s.product() == null ? null : s.product().segment(),
        s.product() == null ? null : s.product().businessOrigin(),
        s.premium() == null || s.premium().currency() == null ? PHP : s.premium().currency(),
        s.insurerCode(),
        null,
        from,
        from.plusYears(1),
        false,
        List.of());
  }

  private static QuotationDraft quotation(Terms t) {
    return new QuotationDraft(
        t.clientId(),
        t.productCode(),
        t.segment(),
        t.channel(),
        null,
        t.currency(),
        new QuotationDraft.Terms(
            t.insurerCode(),
            t.insurerBranch(),
            t.from(),
            t.to(),
            null,
            t.directPayment(),
            null,
            "Renewal"),
        t.items().stream().map(i -> new QuotationDraft.DraftItem(1, i)).toList());
  }

  private static ProposalDraft proposal(Terms t) {
    return new ProposalDraft(
        t.clientId(),
        t.productCode(),
        t.segment(),
        t.channel(),
        t.currency(),
        t.from(),
        t.to(),
        new RiskDetails(
            List.of(), t.items().stream().map(i -> new RiskDetails.Item(1, i)).toList()),
        t.insurerCode() == null ? List.of() : List.of(t.insurerCode()));
  }

  /** Kind of New Business path. */
  public enum Kind {
    /** Package quotation. */
    QUOTATION,
    /** Non-package proposal request (PRF). */
    PROPOSAL
  }

  private record Terms(
      Long clientId,
      String productCode,
      String segment,
      String channel,
      String currency,
      String insurerCode,
      String insurerBranch,
      LocalDate from,
      LocalDate to,
      boolean directPayment,
      List<RiskItemData> items) {}
}
