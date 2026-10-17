package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientNotesService;
import com.iortatechnxt.brokerverse.csf.domain.ActivityAction;
import com.iortatechnxt.brokerverse.csf.domain.CsfActivity;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.AccountLine;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.ApplicationView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.ClientSummary;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.Contact;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.EpolicyView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.PaymentHistory;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.PaymentView;
import com.iortatechnxt.brokerverse.issuance.domain.EpolicyStatus;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceQueryService;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService.ClientPayment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Servicing View of a client (FR-CSF-011 to 013, 031; BRCSF-002, 005, 008): the summary card,
 * the accounts with their CSF status, the payment history of the window and the e-policies. The
 * data are read from their owning modules when a tab opens; nothing is copied. Opening a view is
 * logged.
 */
@Service
@Transactional(readOnly = true)
public class ServicingViewService {

  private static final int MAX_MONTHS = 120;

  private final CsfClients clients;
  private final ClientNotesService notes;
  private final AccountLines lines;
  private final InvoiceLedgerQueryService ledger;
  private final CashReceiptService receipts;
  private final IssuanceQueryService issuance;
  private final VerificationService verifications;
  private final CsfParameters parameters;
  private final CsfSupport support;

  /**
   * Creates the service.
   *
   * @param clients clients of the company
   * @param notes client tags and instructions
   * @param lines account lines
   * @param ledger Operations invoice ledger
   * @param receipts receipt modes
   * @param issuance e-policies
   * @param verifications caller verifications
   * @param parameters CSF parameters
   * @param support platform collaborators
   */
  @SuppressWarnings("java:S107") // collaborators
  public ServicingViewService(
      CsfClients clients,
      ClientNotesService notes,
      AccountLines lines,
      InvoiceLedgerQueryService ledger,
      CashReceiptService receipts,
      IssuanceQueryService issuance,
      VerificationService verifications,
      CsfParameters parameters,
      CsfSupport support) {
    this.clients = clients;
    this.notes = notes;
    this.lines = lines;
    this.ledger = ledger;
    this.receipts = receipts;
    this.issuance = issuance;
    this.verifications = verifications;
    this.parameters = parameters;
    this.support = support;
  }

  /**
   * The summary card of a client; the opening of the Servicing View is logged.
   *
   * @param companyId company
   * @param clientId client
   * @return summary
   */
  public ClientSummary summary(Long companyId, Long clientId) {
    Client c = clients.require(companyId, clientId);
    int accounts = clients.accountsOf(c).size();
    support
        .activity()
        .record(
            companyId,
            ActivityAction.VIEW,
            new CsfActivity.Subject(c.getId(), c.getCode(), null, c.getDisplayName()));
    return new ClientSummary(
        c.getId(),
        c.getCode(),
        c.getProspectCode(),
        c.getDisplayName(),
        c.getClientType().name(),
        c.getStatus().name(),
        c.getKycStatus().name(),
        c.getMarketSegment(),
        c.isBankClient(),
        new Contact(
            c.getEmail(),
            c.getMobile(),
            c.getPhone(),
            c.getAddressLine(),
            c.getCity(),
            c.getProvince(),
            c.getPostalCode()),
        notes.banner(c.getId()),
        accounts,
        verifications.current(c.getId()).orElse(null));
  }

  /**
   * The accounts of the client with their CSF status, BIBS stage, policy number, period and
   * balance, newest first (all segments, CBG and non-CBG).
   *
   * @param companyId company
   * @param clientId client
   * @return accounts
   */
  public List<AccountLine> accounts(Long companyId, Long clientId) {
    Client c = clients.require(companyId, clientId);
    return lines.of(clients.accountsOf(c));
  }

  /**
   * The payments of the client in a window, newest first, each with the invoices it was applied to
   * and their current balance (FR-CSF-013; BRCSF-005).
   *
   * @param companyId company
   * @param clientId client
   * @param months months back, null for {@code CSF_PAYMENT_HISTORY_MONTHS}
   * @param arn only the payments of one account, may be null
   * @return payment history
   */
  public PaymentHistory payments(Long companyId, Long clientId, Integer months, String arn) {
    Client c = clients.require(companyId, clientId);
    int window = months == null ? parameters.paymentHistoryMonths() : months;
    if (window < 1 || window > MAX_MONTHS) {
      throw new BusinessRuleException(
          "CSF_HISTORY_WINDOW", "Show between 1 and " + MAX_MONTHS + " months of payments");
    }
    LocalDate from = BusinessClock.today(support.clock()).minusMonths(window);
    List<ClientPayment> rows =
        ledger.paymentsOfClient(companyId, c.getCode(), from).stream()
            .filter(p -> arn == null || arn.isBlank() || arn.equals(p.arn()))
            .toList();
    return new PaymentHistory(from, window, group(rows));
  }

  private List<PaymentView> group(List<ClientPayment> rows) {
    Map<String, List<ClientPayment>> byReceipt = new LinkedHashMap<>();
    rows.forEach(
        p ->
            byReceipt
                .computeIfAbsent(receiptOf(p) + "|" + p.type(), k -> new ArrayList<>())
                .add(p));
    Set<String> numbers =
        rows.stream()
            .flatMap(p -> Stream.of(p.orNo(), p.arNo()))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<String, String> modes = receipts.modesOf(numbers);
    Map<String, OpsInvoice> invoices =
        ledger.byNumbers(rows.stream().map(ClientPayment::invoiceNo).distinct().toList());
    return byReceipt.values().stream().map(g -> payment(g, modes, invoices)).toList();
  }

  private static PaymentView payment(
      List<ClientPayment> group, Map<String, String> modes, Map<String, OpsInvoice> invoices) {
    ClientPayment first = group.get(0);
    String mode = first.orNo() == null ? null : modes.get(first.orNo());
    if (mode == null && first.arNo() != null) {
      mode = modes.get(first.arNo());
    }
    BigDecimal amount =
        group.stream().map(ClientPayment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new PaymentView(
        receiptOf(first),
        first.orNo(),
        first.arNo(),
        first.valueDate(),
        mode,
        kindOf(first.type()),
        amount,
        group.stream().map(p -> application(p, invoices.get(p.invoiceNo()))).toList());
  }

  private static ApplicationView application(ClientPayment p, OpsInvoice invoice) {
    return new ApplicationView(
        p.invoiceNo(),
        p.arn(),
        p.amount(),
        invoice == null ? null : invoice.premiumBalance(),
        invoice == null ? null : invoice.getPaymentStatus().name());
  }

  private static String receiptOf(ClientPayment p) {
    if (p.orNo() != null) {
      return p.orNo();
    }
    return p.arNo() != null ? p.arNo() : p.sourceRef();
  }

  private static String kindOf(MovementType type) {
    return switch (type) {
      case UNAPPLIED -> "REVERSAL";
      case LEGACY_PAID -> "LEGACY";
      default -> "PAYMENT";
    };
  }

  /**
   * The e-policies of the client's accounts, newest first; a confirmed e-policy can be resent.
   *
   * @param companyId company
   * @param clientId client
   * @return e-policies
   */
  public List<EpolicyView> epolicies(Long companyId, Long clientId) {
    Client c = clients.require(companyId, clientId);
    List<EpolicyView> found = new ArrayList<>();
    for (Account a : clients.accountsOf(c)) {
      issuance.policyFor(a.getArn()).epolicies().stream()
          .map(
              e ->
                  new EpolicyView(
                      e.getId(),
                      e.getArn(),
                      e.getFileName(),
                      e.getPolicyNumberList(),
                      e.getStatus().name(),
                      e.getCreatedAt(),
                      e.getDispatchCount(),
                      e.getDispatchedTo(),
                      e.getDispatchedAt(),
                      e.getStatus() == EpolicyStatus.CONFIRMED))
          .forEach(found::add);
    }
    return found;
  }
}
