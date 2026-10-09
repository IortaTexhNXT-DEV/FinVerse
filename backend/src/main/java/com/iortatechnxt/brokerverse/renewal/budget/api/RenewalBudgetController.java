package com.iortatechnxt.brokerverse.renewal.budget.api;

import com.iortatechnxt.brokerverse.renewal.budget.service.RenewalBudgetService;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalBudget;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalBudgetHistory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Renewal Annual Budget Maintenance (BDOI Renewal FRS FRRN.042): the Budget Inquiry of a fiscal
 * year, the creation and update of a record and its history. The upload runs through the bulk
 * upload of handler {@code RNW_BUDGET}.
 */
@RestController
@RequestMapping("/api/v1/renewal/budgets")
@PreAuthorize("hasAuthority('RNW_BUDGET')")
public class RenewalBudgetController {

  private final RenewalBudgetService budgets;

  /**
   * Creates the controller.
   *
   * @param budgets budget service
   */
  public RenewalBudgetController(RenewalBudgetService budgets) {
    this.budgets = budgets;
  }

  /**
   * The budget records of a fiscal year.
   *
   * @param companyId company
   * @param fiscalYear fiscal year
   * @return records
   */
  @GetMapping
  public List<BudgetView> list(@RequestParam Long companyId, @RequestParam int fiscalYear) {
    return budgets.list(companyId, fiscalYear).stream().map(BudgetView::of).toList();
  }

  /**
   * Creates or updates a budget record.
   *
   * @param companyId company
   * @param body record
   * @return the record
   */
  @PostMapping
  public BudgetView save(@RequestParam Long companyId, @Valid @RequestBody BudgetBody body) {
    return BudgetView.of(budgets.save(companyId, body.input()));
  }

  /**
   * The history of a budget record.
   *
   * @param companyId company
   * @param id record
   * @return changes, latest first
   */
  @GetMapping("/{id}/history")
  public List<HistoryView> history(@RequestParam Long companyId, @PathVariable Long id) {
    return budgets.history(companyId, id).stream().map(HistoryView::of).toList();
  }

  /**
   * A budget record to save.
   *
   * @param fiscalYear fiscal year
   * @param measure PREMIUM or COMMISSION
   * @param segment market segment
   * @param region region
   * @param team team
   * @param subTeam sub-team
   * @param heads unit head, section head, team head, team lead
   * @param accountOfficer account officer
   * @param months monthly amounts
   */
  public record BudgetBody(
      int fiscalYear,
      String measure,
      String segment,
      String region,
      String team,
      String subTeam,
      RenewalBudget.Heads heads,
      String accountOfficer,
      @NotNull List<RenewalBudget.Month> months) {

    RenewalBudgetService.BudgetInput input() {
      return new RenewalBudgetService.BudgetInput(
          new RenewalBudget.Key(
              fiscalYear,
              measure == null ? RenewalBudgetService.PREMIUM : measure,
              segment,
              region,
              team,
              subTeam,
              accountOfficer),
          heads == null ? new RenewalBudget.Heads(null, null, null, null) : heads,
          months);
    }
  }

  /**
   * A budget record as listed.
   *
   * @param id record
   * @param fiscalYear fiscal year
   * @param measure PREMIUM or COMMISSION
   * @param segment segment
   * @param region region
   * @param team team
   * @param subTeam sub-team
   * @param heads heads of the hierarchy
   * @param accountOfficer account officer
   * @param months monthly amounts
   * @param totals annual totals
   * @param createdBy created by
   * @param createdAt created on
   * @param updatedBy last change by
   * @param updatedAt last change at
   */
  public record BudgetView(
      Long id,
      int fiscalYear,
      String measure,
      String segment,
      String region,
      String team,
      String subTeam,
      RenewalBudget.Heads heads,
      String accountOfficer,
      List<RenewalBudget.Month> months,
      RenewalBudget.Totals totals,
      String createdBy,
      Instant createdAt,
      String updatedBy,
      Instant updatedAt) {

    static BudgetView of(RenewalBudget b) {
      return new BudgetView(
          b.getId(),
          b.getFiscalYear(),
          b.getMeasure(),
          b.getSegment(),
          b.getRegion(),
          b.getTeam(),
          b.getSubTeam(),
          new RenewalBudget.Heads(
              b.getUnitHead(), b.getSectionHead(), b.getTeamHead(), b.getTeamLead()),
          b.getAccountOfficer(),
          b.getMonths(),
          b.totals(),
          b.getCreatedBy(),
          b.getCreatedAt(),
          b.getUpdatedBy() == null ? b.getCreatedBy() : b.getUpdatedBy(),
          b.getUpdatedAt() == null ? b.getCreatedAt() : b.getUpdatedAt());
    }
  }

  /**
   * One change of a budget amount.
   *
   * @param field the amount
   * @param previous previous value
   * @param updated updated value
   * @param modifiedBy user
   * @param modifiedAt time
   */
  public record HistoryView(
      String field,
      BigDecimal previous,
      BigDecimal updated,
      String modifiedBy,
      Instant modifiedAt) {

    static HistoryView of(RenewalBudgetHistory h) {
      return new HistoryView(
          h.getField(), h.getPrevious(), h.getUpdated(), h.getModifiedBy(), h.getModifiedAt());
    }
  }
}
