package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestSubject;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProductRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.placement.domain.PlacementSlip;
import com.iortatechnxt.brokerverse.placement.domain.PlacementSlipRepository;
import com.iortatechnxt.brokerverse.placement.domain.SlipStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The policy an endorsement request is against (ADJID.001/020/024): the chain request → invoice →
 * account (ARN) → placement slip → insurer policy number, read from the Operations ledger, the
 * account, the catalog and the placement slips. The policy number is the one the ledger holds now
 * (it is often issued after the request is raised).
 */
@Service
@Transactional(readOnly = true)
public class PolicyLinks {

  private final InvoiceLedgerQueryService ledger;
  private final AccountRepository accounts;
  private final RiskProductRepository products;
  private final PlacementSlipRepository slips;

  /**
   * Creates the service.
   *
   * @param ledger Operations ledger
   * @param accounts accounts
   * @param products catalog products (names)
   * @param slips placement slips
   */
  public PolicyLinks(
      InvoiceLedgerQueryService ledger,
      AccountRepository accounts,
      RiskProductRepository products,
      PlacementSlipRepository slips) {
    this.ledger = ledger;
    this.accounts = accounts;
    this.products = products;
    this.slips = slips;
  }

  /**
   * The policy of a booked invoice, before any request is raised (New Request form).
   *
   * @param invoiceNo invoice
   * @return link
   */
  public PolicyLink forInvoice(String invoiceNo) {
    OpsInvoice invoice = ledger.require(invoiceNo);
    Optional<Account> account = accountOf(invoice.getAccountId(), invoice.getArn());
    return link(invoice, account.orElse(null), new Names());
  }

  /**
   * The policies of several requests (work list page).
   *
   * @param requests requests
   * @return link per request id
   */
  public Map<Long, PolicyLink> forRequests(Collection<EndorsementRequest> requests) {
    Set<String> invoiceNos = new LinkedHashSet<>();
    requests.forEach(r -> invoiceNos.add(r.getSubject().invoiceNo()));
    Map<String, OpsInvoice> invoices = ledger.byNumbers(invoiceNos);
    Names names = new Names();
    Map<Long, PolicyLink> links = new HashMap<>();
    for (EndorsementRequest r : requests) {
      links.put(r.getId(), link(r.getSubject(), invoices.get(r.getSubject().invoiceNo()), names));
    }
    return links;
  }

  /**
   * The policy of one request.
   *
   * @param request request
   * @return link
   */
  public PolicyLink forRequest(EndorsementRequest request) {
    return forRequests(List.of(request)).get(request.getId());
  }

  /**
   * The product of an invoice's account, copied on the request when it is raised.
   *
   * @param invoice invoice
   * @return product code, null when the account is not known (legacy invoice)
   */
  public String productCodeOf(OpsInvoice invoice) {
    return accountOf(invoice.getAccountId(), invoice.getArn())
        .map(Account::getProductCode)
        .orElse(null);
  }

  private PolicyLink link(RequestSubject subject, OpsInvoice invoice, Names names) {
    String policyNo =
        invoice == null || invoice.getPolicyNo() == null
            ? subject.policyNo()
            : invoice.getPolicyNo();
    Long accountId = invoice == null ? subject.accountId() : invoice.getAccountId();
    String productCode = subject.productCode();
    if (productCode == null) {
      productCode = accountOf(accountId, subject.arn()).map(Account::getProductCode).orElse(null);
    }
    return new PolicyLink(
        subject.invoiceNo(),
        subject.arn(),
        accountId,
        policyNo,
        subject.clientCode(),
        subject.assuredName(),
        subject.insurerCode(),
        productCode,
        names.product(productCode),
        subject.productLine(),
        invoice == null ? null : invoice.getClassification().inceptionDate(),
        invoice == null ? null : invoice.getClassification().expiryDate(),
        names.slip(subject.arn(), subject.insurerCode()),
        subject.currency(),
        invoice == null ? null : invoice.getGrossPremium());
  }

  private PolicyLink link(OpsInvoice invoice, Account account, Names names) {
    String productCode = account == null ? null : account.getProductCode();
    return new PolicyLink(
        invoice.getInvoiceNo(),
        invoice.getArn(),
        invoice.getAccountId() != null
            ? invoice.getAccountId()
            : account == null ? null : account.getId(),
        invoice.getPolicyNo(),
        invoice.getClientCode(),
        invoice.getAssuredName(),
        invoice.getInsurerCode(),
        productCode,
        names.product(productCode),
        invoice.getClassification().productLine(),
        invoice.getClassification().inceptionDate(),
        invoice.getClassification().expiryDate(),
        names.slip(invoice.getArn(), invoice.getInsurerCode()),
        invoice.getCurrency(),
        invoice.getGrossPremium());
  }

  private Optional<Account> accountOf(Long accountId, String arn) {
    Optional<Account> byId = accountId == null ? Optional.empty() : accounts.findById(accountId);
    return byId.isPresent() || arn == null ? byId : accounts.findByArn(arn);
  }

  /** Product names and slips looked up once per page. */
  private final class Names {

    private final Map<String, String> productNames = new HashMap<>();
    private final Map<String, String> slipNos = new HashMap<>();

    String product(String code) {
      if (code == null) {
        return null;
      }
      return productNames.computeIfAbsent(
          code, c -> products.findByCode(c).map(RiskProduct::getName).orElse(null));
    }

    /**
     * The placement slip of the account for the insurer: the newest version sent or generated, else
     * the newest slip of the account.
     */
    String slip(String arn, String insurerCode) {
      if (arn == null) {
        return null;
      }
      return slipNos.computeIfAbsent(
          arn + "|" + insurerCode,
          k -> {
            List<PlacementSlip> covering = slips.findByArn(arn);
            return covering.stream()
                .filter(s -> s.getStatus() != SlipStatus.SUPERSEDED)
                .filter(s -> insurerCode == null || insurerCode.equals(s.getInsurerCode()))
                .max(Comparator.comparingInt(PlacementSlip::getVersionNo))
                .or(() -> covering.stream().findFirst())
                .map(PlacementSlip::getSlipNo)
                .orElse(null);
          });
    }
  }

  /**
   * The policy and placement of a request.
   *
   * @param invoiceNo invoice the request is on
   * @param arn Account Reference Number
   * @param accountId account id (account and placement pages), may be null
   * @param policyNo insurer policy number, null while not issued
   * @param clientCode client code
   * @param assuredName assured name
   * @param insurerCode lead insurer
   * @param productCode product, may be null
   * @param productName product name, may be null
   * @param productLine product line
   * @param periodFrom inception of the invoice's cover
   * @param periodTo expiry of the invoice's cover
   * @param slipNo placement slip, null when none
   * @param currency currency
   * @param grossPremium gross premium of the invoice
   */
  public record PolicyLink(
      String invoiceNo,
      String arn,
      Long accountId,
      String policyNo,
      String clientCode,
      String assuredName,
      String insurerCode,
      String productCode,
      String productName,
      String productLine,
      LocalDate periodFrom,
      LocalDate periodTo,
      String slipNo,
      String currency,
      BigDecimal grossPremium) {}
}
