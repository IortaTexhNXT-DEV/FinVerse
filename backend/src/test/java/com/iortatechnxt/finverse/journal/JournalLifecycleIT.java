package com.iortatechnxt.finverse.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.coa.domain.BalanceSide;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.finverse.journal.api.dto.JournalRequest;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.journal.domain.JournalStatus;
import com.iortatechnxt.finverse.journal.domain.JournalType;
import com.iortatechnxt.finverse.journal.service.JournalAuthorizationService;
import com.iortatechnxt.finverse.journal.service.JournalEntryService;
import com.iortatechnxt.finverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

@IntegrationTest
class JournalLifecycleIT {

  @Autowired private JournalEntryService journals;
  @Autowired private JournalAuthorizationService authorization;
  @Autowired private LedgerQueryService ledger;
  @Autowired private ChartOfAccountsService accounts;
  @Autowired private UserDetailsService users;
  @Autowired private TestData data;

  private <T> T as(String user, Supplier<T> action) {
    var details = users.loadUserByUsername(user);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    try {
      return action.get();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private JournalRequest expenseJournal(String amount) {
    Long company = data.company().getId();
    Long branch = data.branch("HO").getId();
    return new JournalRequest(
        company,
        branch,
        JournalType.MANUAL,
        LocalDate.now(),
        "PHP",
        "Office rent for the month",
        "INV-100",
        List.of(
            new JournalLineRequest(
                "5603",
                BalanceSide.DEBIT,
                new BigDecimal(amount),
                null,
                null,
                null,
                "FIN",
                null,
                null,
                null,
                null),
            new JournalLineRequest(
                "1111",
                BalanceSide.CREDIT,
                new BigDecimal(amount),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null)));
  }

  @Test
  void makerCheckerPostsBalancedJournalToLedger() {
    Long company = data.company().getId();
    Long rentAccount = accounts.getByCode(company, "5603").getId();
    BigDecimal before = ledger.netBalance(company, rentAccount, null, LocalDate.now());

    JournalBatch draft = as("accountant", () -> journals.createDraft(expenseJournal("15000.00")));
    assertThat(draft.getStatus()).isEqualTo(JournalStatus.DRAFT);
    assertThat(draft.isBalanced()).isTrue();

    as("accountant", () -> journals.submit(draft.getId()));
    assertThatThrownBy(() -> as("accountant", () -> authorization.approve(draft.getId())))
        .isInstanceOf(BusinessRuleException.class);

    JournalBatch posted = as("checker", () -> authorization.approve(draft.getId()));
    assertThat(posted.getStatus()).isEqualTo(JournalStatus.POSTED);
    assertThat(posted.getAuthorizedBy()).isEqualTo("checker");
    assertThat(ledger.netBalance(company, rentAccount, null, LocalDate.now()))
        .isEqualByComparingTo(before.add(new BigDecimal("15000.00")));
  }

  @Test
  void reversalRestoresBalancesAndMarksOriginal() {
    Long company = data.company().getId();
    Long bank = accounts.getByCode(company, "1111").getId();
    BigDecimal before = ledger.netBalance(company, bank, null, LocalDate.now());

    JournalBatch draft = as("accountant", () -> journals.createDraft(expenseJournal("2500.00")));
    as("accountant", () -> journals.submit(draft.getId()));
    as("checker", () -> authorization.approve(draft.getId()));

    JournalBatch reversal =
        as(
            "accountant",
            () -> authorization.reverse(draft.getId(), LocalDate.now(), "Duplicate entry"));
    assertThat(reversal.getStatus()).isEqualTo(JournalStatus.PENDING_APPROVAL);
    as("checker", () -> authorization.approve(reversal.getId()));

    assertThat(journals.get(draft.getId()).getStatus()).isEqualTo(JournalStatus.REVERSED);
    assertThat(ledger.netBalance(company, bank, null, LocalDate.now()))
        .isEqualByComparingTo(before);
    assertThatThrownBy(
            () ->
                as(
                    "accountant",
                    () -> authorization.reverse(draft.getId(), LocalDate.now(), "again")))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void unbalancedJournalCannotBeSubmitted() {
    JournalRequest base = expenseJournal("100.00");
    JournalRequest unbalanced =
        new JournalRequest(
            base.companyId(),
            base.branchId(),
            base.journalType(),
            base.valueDate(),
            base.currency(),
            base.narration(),
            base.reference(),
            List.of(
                base.lines().get(0),
                new JournalLineRequest(
                    "1111",
                    BalanceSide.CREDIT,
                    new BigDecimal("90.00"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null)));
    JournalBatch draft = as("accountant", () -> journals.createDraft(unbalanced));
    assertThat(draft.isBalanced()).isFalse();
    assertThatThrownBy(() -> as("accountant", () -> journals.submit(draft.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("not balanced");
  }

  @Test
  void accountControlsAreEnforced() {
    JournalRequest base = expenseJournal("100.00");
    // 5603 requires a cost centre; 1201 is a control account closed to manual posting.
    JournalRequest bad =
        new JournalRequest(
            base.companyId(),
            base.branchId(),
            base.journalType(),
            base.valueDate(),
            base.currency(),
            base.narration(),
            base.reference(),
            List.of(
                new JournalLineRequest(
                    "5603",
                    BalanceSide.DEBIT,
                    new BigDecimal("100.00"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null),
                new JournalLineRequest(
                    "1201",
                    BalanceSide.CREDIT,
                    new BigDecimal("100.00"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null)));
    JournalBatch draft = as("accountant", () -> journals.createDraft(bad));
    assertThatThrownBy(() -> as("accountant", () -> journals.submit(draft.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Cost centre is mandatory")
        .hasMessageContaining("Manual posting is not allowed");
  }

  @Test
  void foreignCurrencyLinesConvertToBase() {
    JournalRequest base = expenseJournal("0.01");
    JournalRequest usd =
        new JournalRequest(
            base.companyId(),
            base.branchId(),
            base.journalType(),
            base.valueDate(),
            "USD",
            "Transfer to dollar account",
            null,
            List.of(
                new JournalLineRequest(
                    "1113",
                    BalanceSide.DEBIT,
                    new BigDecimal("1000.00"),
                    "USD",
                    new BigDecimal("58.00"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null),
                new JournalLineRequest(
                    "1111",
                    BalanceSide.CREDIT,
                    new BigDecimal("58000.00"),
                    "PHP",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null)));
    JournalBatch draft = as("accountant", () -> journals.createDraft(usd));
    assertThat(draft.getTotalDebit()).isEqualByComparingTo("58000.00");
    assertThat(draft.isBalanced()).isTrue();
  }
}
