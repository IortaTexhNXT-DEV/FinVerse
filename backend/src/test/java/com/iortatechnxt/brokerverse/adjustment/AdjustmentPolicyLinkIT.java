package com.iortatechnxt.brokerverse.adjustment;

import static com.iortatechnxt.brokerverse.adjustment.AdjustmentFixtures.FROM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentQueryService;
import com.iortatechnxt.brokerverse.adjustment.service.PolicyLinks;
import com.iortatechnxt.brokerverse.adjustment.service.PolicyLinks.PolicyLink;
import com.iortatechnxt.brokerverse.cashiering.CashFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.PolicyTransactionService;
import com.iortatechnxt.brokerverse.opsledger.service.PolicyTransactionService.Journal;
import com.iortatechnxt.brokerverse.opsledger.service.PolicyTransactionService.PolicyTransactions;
import com.iortatechnxt.brokerverse.opsledger.service.PolicyTransactionService.Row;
import com.iortatechnxt.brokerverse.opsledger.service.port.PolicyTransactionSource.Kind;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Every endorsement request is linked to its policy (request → invoice → account / ARN → placement
 * → insurer policy number), searchable by it, and the policy's transaction history lists the
 * original booking, then each endorsement, cancellation and refund with its change, the position
 * after it, its status and its GL journals with their lines.
 */
@IntegrationTest
class AdjustmentPolicyLinkIT {

  private static final String BASE = "/api/v1/adjustment";

  @Autowired private AdjustmentFixtures fx;
  @Autowired private CashFixtures cash;
  @Autowired private AdjustmentQueryService queries;
  @Autowired private PolicyLinks links;
  @Autowired private PolicyTransactionService transactions;
  @Autowired private AccountRepository accounts;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Api api;
  @Autowired private AsUser as;

  private void issuePolicy(OpsInvoice invoice, String policyNo) {
    jdbc.update(
        "update ops_invoice set policy_no = ? where invoice_no = ?",
        policyNo,
        invoice.getInvoiceNo());
  }

  private static String policyNo() {
    return "MGIC-MC-" + System.nanoTime();
  }

  @Test
  void aRequestIsLinkedToItsInvoiceAccountProductAndThePolicyIssuedAfterIt() {
    OpsInvoice invoice = fx.invoice();
    Account account = accounts.findByArn(invoice.getArn()).orElseThrow();
    EndorsementRequest request =
        fx.raise(
            invoice,
            AdjustmentFixtures.terms("NF_ASSURED_INFO", null, null, FROM, null),
            AmountInput.NONE);
    assertThat(request.getSubject().productCode()).isEqualTo(account.getProductCode());
    assertThat(request.getSubject().accountId()).isEqualTo(invoice.getAccountId());

    String policy = policyNo();
    issuePolicy(invoice, policy);

    PolicyLink link = as.run("adjust", () -> links.forRequest(fx.reload(request)));
    assertThat(link.policyNo()).isEqualTo(policy);
    assertThat(link.arn()).isEqualTo(invoice.getArn());
    assertThat(link.invoiceNo()).isEqualTo(invoice.getInvoiceNo());
    assertThat(link.insurerCode()).isEqualTo(invoice.getInsurerCode());
    assertThat(link.productCode()).isEqualTo(account.getProductCode());
    assertThat(link.productName()).isNotBlank().isNotEqualTo(account.getProductCode());
    assertThat(link.periodFrom()).isEqualTo(invoice.getClassification().inceptionDate());
    assertThat(link.periodTo()).isEqualTo(invoice.getClassification().expiryDate());

    PolicyLink before = as.run("adjust", () -> links.forInvoice(invoice.getInvoiceNo()));
    assertThat(before.policyNo()).isEqualTo(policy);
    assertThat(before.accountId()).isEqualTo(account.getId());
    assertThat(before.productName()).isEqualTo(link.productName());
  }

  @Test
  void theWorkbenchSearchFindsARequestByPolicyArnInvoiceClientProductOrRequest() {
    OpsInvoice invoice = fx.invoice();
    EndorsementRequest request =
        fx.raise(
            invoice,
            AdjustmentFixtures.terms("NF_DESCRIPTIVE", null, null, FROM, null),
            AmountInput.NONE);
    String policy = policyNo();
    issuePolicy(invoice, policy);
    Long company = fx.company();
    for (String text :
        List.of(
            policy.toLowerCase(java.util.Locale.ROOT),
            invoice.getArn(),
            invoice.getInvoiceNo(),
            invoice.getClientCode(),
            invoice.getAssuredName(),
            request.getRequestNo())) {
      assertThat(queries.search(company, null, text, Pageable.ofSize(200)).getContent())
          .as("search by %s", text)
          .extracting(EndorsementRequest::getId)
          .contains(request.getId());
    }
    assertThat(
            queries
                .search(company, null, request.getSubject().productCode(), Pageable.ofSize(500))
                .getContent())
        .extracting(EndorsementRequest::getId)
        .contains(request.getId());
    assertThat(queries.search(company, null, "no-such-policy-x9", Pageable.ofSize(20))).isEmpty();
  }

  @Test
  void theWorkbenchListAndTheRequestShowThePolicyOverHttp() throws Exception {
    OpsInvoice invoice = fx.invoice();
    EndorsementRequest request =
        fx.raise(
            invoice,
            AdjustmentFixtures.terms("NF_COVER_EXTENSION", null, null, FROM, null),
            AmountInput.NONE);
    String policy = policyNo();
    issuePolicy(invoice, policy);
    String q = "?companyId=" + fx.company() + "&q=" + policy;
    api.doGet("adjust", BASE + "/requests" + q)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].requestNo").value(request.getRequestNo()))
        .andExpect(jsonPath("$.content[0].policy.policyNo").value(policy))
        .andExpect(jsonPath("$.content[0].policy.arn").value(invoice.getArn()))
        .andExpect(jsonPath("$.content[0].policy.productName").isNotEmpty());
    api.doGet("adjust", BASE + "/requests/" + request.getId())
        .andExpect(jsonPath("$.policy.policyNo").value(policy))
        .andExpect(jsonPath("$.policy.invoiceNo").value(invoice.getInvoiceNo()));
    api.doGet("mktcoll", BASE + "/policies/" + invoice.getInvoiceNo())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.policyNo").value(policy))
        .andExpect(jsonPath("$.clientCode").value(invoice.getClientCode()));
  }

  @Test
  void aPostedCancellationShowsInThePolicyHistoryWithItsJournals() throws Exception {
    OpsInvoice invoice = fx.invoice();
    EndorsementRequest pending =
        fx.raise(
            fx.invoice(),
            AdjustmentFixtures.terms("NF_ASSURED_INFO", null, null, FROM, null),
            AmountInput.NONE);
    EndorsementRequest cancelled =
        fx.raiseAndPost(
            invoice, AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM), AmountInput.NONE);
    assertThat(cancelled.getJournals()).isNotEmpty();

    PolicyTransactions history =
        as.run("adjust", () -> transactions.forInvoice(invoice.getInvoiceNo()));
    assertThat(history.rootInvoiceNo()).isEqualTo(invoice.getInvoiceNo());
    assertThat(history.rows()).hasSize(2);
    Row booking = history.rows().get(0);
    assertThat(booking.kind()).isEqualTo(Kind.BOOKING);
    assertThat(booking.typeLabel()).isEqualTo("Original Booking");
    assertThat(booking.refs().invoiceNo()).isEqualTo(invoice.getInvoiceNo());
    assertThat(booking.change().gross()).isEqualByComparingTo(invoice.getGrossPremium());
    assertThat(booking.change().premium()).isPositive();
    assertThat(booking.change().taxes()).isPositive();
    assertThat(booking.after().gross()).isEqualByComparingTo(invoice.getGrossPremium());
    assertThat(booking.journals()).isNotEmpty();

    Row cancellation = history.rows().get(1);
    assertThat(cancellation.kind()).isEqualTo(Kind.CANCELLATION);
    assertThat(cancellation.typeLabel()).isEqualTo("Financial – Change of Cover");
    assertThat(cancellation.detail()).isEqualTo("Flat Cancellation");
    assertThat(cancellation.refs().requestNo()).isEqualTo(cancelled.getRequestNo());
    assertThat(cancellation.refs().invoiceNo()).isEqualTo(cancelled.outcome().newInvoiceNo());
    assertThat(cancellation.change().gross())
        .isEqualByComparingTo(invoice.getGrossPremium().negate());
    assertThat(cancellation.after().gross()).isZero();
    assertThat(cancellation.after().premium()).isZero();
    assertThat(cancellation.statusLabel()).isEqualTo("Posted");
    assertThat(cancellation.journals())
        .extracting(Journal::batchNo)
        .containsAll(cancelled.getJournals());
    for (Journal j : cancellation.journals()) {
      assertThat(j.id()).isNotNull();
      assertThat(j.lines()).isNotEmpty();
      BigDecimal debit =
          j.lines().stream()
              .map(l -> l.debit() == null ? BigDecimal.ZERO : l.debit())
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal credit =
          j.lines().stream()
              .map(l -> l.credit() == null ? BigDecimal.ZERO : l.credit())
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      assertThat(debit).isEqualByComparingTo(credit);
    }

    PolicyTransactions other =
        as.run("adjust", () -> transactions.forInvoice(pending.getSubject().invoiceNo()));
    Row open = other.rows().get(other.rows().size() - 1);
    assertThat(open.refs().requestNo()).isEqualTo(pending.getRequestNo());
    assertThat(open.posted()).isFalse();
    assertThat(open.after()).isNull();
    assertThat(open.statusLabel()).isEqualTo("Draft");
    assertThat(open.typeLabel()).isEqualTo("Non-financial – Assured Information");

    JsonNode json =
        api.read(
            api.doGet("adjust", "/api/v1/ops/invoices/" + invoice.getInvoiceNo() + "/transactions")
                .andExpect(status().isOk()));
    assertThat(json.path("rows")).hasSize(2);
    assertThat(json.path("rows").get(1).path("journals").get(0).path("lines")).isNotEmpty();
    api.doGet("adjust", "/api/v1/ops/accounts/" + invoice.getArn() + "/transactions")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rows[0].kind").value("BOOKING"));
    api.doGet("mktcoll", "/api/v1/ops/invoices/" + invoice.getInvoiceNo() + "/transactions")
        .andExpect(status().isOk());
  }

  @Test
  void aCancellationOfAPaidInvoiceKeepsTheReapplicationJournalsAndShowsTheRefund() {
    OpsInvoice invoice = cash.motorInvoice();
    cash.pay(invoice.getInvoiceNo(), invoice.premiumBalance());
    EndorsementRequest cancelled =
        fx.raiseAndPost(
            invoice, AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM), AmountInput.NONE);
    assertThat(cancelled.outcome().excessAmount()).isPositive();

    PolicyTransactions history =
        as.run("adjust", () -> transactions.forInvoice(invoice.getInvoiceNo()));
    Row refund =
        history.rows().stream().filter(r -> r.kind() == Kind.REFUND).findFirst().orElseThrow();
    assertThat(refund.refs().requestNo()).isEqualTo(cancelled.getRequestNo());
    assertThat(refund.detail()).isEqualTo(cancelled.outcome().unappliedRef());
    assertThat(refund.journals()).isNotEmpty();
    assertThat(cancelled.getJournals())
        .containsAll(refund.journals().stream().map(Journal::batchNo).toList());
    Row cancellation =
        history.rows().stream()
            .filter(r -> r.kind() == Kind.CANCELLATION)
            .findFirst()
            .orElseThrow();
    assertThat(cancellation.journals())
        .extracting(Journal::batchNo)
        .doesNotContainAnyElementsOf(refund.journals().stream().map(Journal::batchNo).toList());
  }
}
