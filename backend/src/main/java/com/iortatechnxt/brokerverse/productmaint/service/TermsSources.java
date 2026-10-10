package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponseRepository;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequestRepository;
import com.iortatechnxt.brokerverse.nonpackage.domain.ResponseStatus;
import com.iortatechnxt.brokerverse.productmaint.domain.NegotiationRoundRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageInsurerResponse;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.TermsRecord;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * What a comparative table reads from its record: the reference, client and requestor, the QS value
 * of each field (what was asked of the insurers) and the insurers with the terms they gave (the
 * response of a quotation request, the latest round of a package request).
 */
@Component
public class TermsSources {

  private final ProposalRequestRepository proposals;
  private final InsurerResponseRepository quotationResponses;
  private final PackageRequests packages;
  private final NegotiationRoundRepository rounds;
  private final PackageResponseService packageResponses;
  private final TermsCodec codec;

  /**
   * Creates the sources.
   *
   * @param proposals quotation requests
   * @param quotationResponses insurer responses of quotation requests
   * @param packages package requests
   * @param rounds negotiation rounds
   * @param packageResponses insurer responses of package requests
   * @param codec terms JSON
   */
  public TermsSources(
      ProposalRequestRepository proposals,
      InsurerResponseRepository quotationResponses,
      PackageRequests packages,
      NegotiationRoundRepository rounds,
      PackageResponseService packageResponses,
      TermsCodec codec) {
    this.proposals = proposals;
    this.quotationResponses = quotationResponses;
    this.packages = packages;
    this.rounds = rounds;
    this.packageResponses = packageResponses;
    this.codec = codec;
  }

  /**
   * The facts of a record.
   *
   * @param record the record
   * @return facts
   */
  public Facts facts(TermsRecord record) {
    if (record.quotation()) {
      ProposalRequest p =
          proposals
              .findById(record.id())
              .orElseThrow(() -> new ResourceNotFoundException("Quotation request", record.id()));
      Map<TermsField, String> qs = new EnumMap<>(TermsField.class);
      qs.put(TermsField.COVERAGE, p.getProductCode() + " (" + p.getLineCode() + ")");
      qs.put(TermsField.SUM_INSURED, money(p.getTotalSumInsured()));
      qs.put(
          TermsField.PERIOD,
          DisplayFormat.date(p.getPeriodFrom()) + " to " + DisplayFormat.date(p.getPeriodTo()));
      return new Facts(
          p.getCompanyId(),
          p.getArn() == null ? p.getPrfNo() : p.getArn(),
          p.getPrfNo(),
          p.getClientName(),
          p.getCreatedBy(),
          qs,
          quotationInsurers(p.getId()));
    }
    PackageRequest p = packages.get(record.id());
    return new Facts(
        p.getCompanyId(),
        p.getRequestNo(),
        p.getRequestNo(),
        p.getClientName() == null ? p.getTitle() : p.getClientName(),
        p.getCreatedBy(),
        packageQs(codec.terms(p.getRequestedTerms())),
        packageInsurers(p.getId()));
  }

  private List<InsurerTerms> quotationInsurers(Long id) {
    List<InsurerTerms> out = new ArrayList<>();
    for (InsurerResponse r : quotationResponses.findByProposalIdOrderById(id)) {
      Map<TermsField, String> v = new EnumMap<>(TermsField.class);
      v.put(TermsField.PREMIUM, money(r.getPremium()));
      v.put(TermsField.RATE, DisplayFormat.rate(r.getRate()));
      v.put(TermsField.DEDUCTIBLES, r.getDeductibles());
      v.put(TermsField.CONDITIONS, r.getConditions());
      v.put(TermsField.OTHER, r.getRemarks());
      out.add(
          new InsurerTerms(
              r.getInsurerCode(),
              r.getInsurerName(),
              answer(r.getStatus()),
              r.getStatus() != ResponseStatus.PENDING,
              v));
    }
    return out;
  }

  private List<InsurerTerms> packageInsurers(Long id) {
    return rounds
        .findFirstByRequestIdOrderByRoundNoDesc(id)
        .map(
            round ->
                packageResponses.ofRound(round).stream()
                    .map(TermsSources::packageTerms)
                    .collect(Collectors.toList()))
        .orElse(List.of());
  }

  private static InsurerTerms packageTerms(PackageInsurerResponse r) {
    Map<TermsField, String> v = new EnumMap<>(TermsField.class);
    v.put(TermsField.RATE, DisplayFormat.rate(r.getRate()));
    v.put(TermsField.PREMIUM, money(r.getMinimumPremium()));
    v.put(TermsField.COVERAGE, r.getTerms());
    v.put(TermsField.CONDITIONS, r.getConditions());
    v.put(TermsField.OTHER, r.getRemarks());
    boolean declined = r.getOutcome() != null && r.getOutcome().contains("DECLINED");
    return new InsurerTerms(
        r.getInsurerCode(),
        r.getInsurerName(),
        declined ? "NOT_COVERED" : "APPROVED",
        r.isAnswered(),
        v);
  }

  private static Map<TermsField, String> packageQs(PackageTerms t) {
    Map<TermsField, String> qs = new EnumMap<>(TermsField.class);
    qs.put(
        TermsField.COVERAGE,
        t.coverages().stream().map(c -> c.coverageCode()).collect(Collectors.joining(", ")));
    if (t.scheme() != null) {
      qs.put(TermsField.RATE, DisplayFormat.rate(t.scheme().defaultRate()));
      qs.put(TermsField.COMMISSION, DisplayFormat.rate(t.scheme().commissionRate()));
      qs.put(TermsField.PREMIUM, money(t.scheme().minimumPremium()));
      qs.put(TermsField.SUM_INSURED, money(t.scheme().maxSumInsured()));
    }
    qs.put(
        TermsField.DEDUCTIBLES,
        t.coverages().stream()
            .map(c -> c.deductibleText())
            .filter(d -> d != null && !d.isBlank())
            .collect(Collectors.joining("; ")));
    if (t.dates() != null && t.dates().packageStartDate() != null) {
      qs.put(
          TermsField.PERIOD,
          DisplayFormat.date(t.dates().packageStartDate())
              + " to "
              + DisplayFormat.date(t.dates().packageEndDate()));
    }
    return qs;
  }

  private static String answer(ResponseStatus status) {
    return status == ResponseStatus.DECLINED ? "NOT_COVERED" : "APPROVED";
  }

  private static String money(BigDecimal amount) {
    return amount == null ? null : DisplayFormat.amount(amount);
  }

  /**
   * The facts of a record.
   *
   * @param companyId company
   * @param reference reference number of the file names (ARN or request number)
   * @param requestNo request number
   * @param clientName client or programme
   * @param requestor requestor (told of the proposal)
   * @param qsValues the QS value of each field
   * @param insurers the insurers and the terms they gave
   */
  public record Facts(
      Long companyId,
      String reference,
      String requestNo,
      String clientName,
      String requestor,
      Map<TermsField, String> qsValues,
      List<InsurerTerms> insurers) {}

  /**
   * The terms an insurer gave.
   *
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param answer APPROVED or NOT_COVERED
   * @param responded whether the insurer has responded
   * @param values the value of each field
   */
  public record InsurerTerms(
      String insurerCode,
      String insurerName,
      String answer,
      boolean responded,
      Map<TermsField, String> values) {}
}
