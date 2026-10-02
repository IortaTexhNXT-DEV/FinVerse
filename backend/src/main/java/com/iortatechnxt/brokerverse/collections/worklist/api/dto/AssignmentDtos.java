package com.iortatechnxt.brokerverse.collections.worklist.api.dto;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.AssignmentKind;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.ItemStatus;
import com.iortatechnxt.brokerverse.collections.worklist.domain.AssignmentRule;
import com.iortatechnxt.brokerverse.collections.worklist.domain.AssignmentRule.Details;
import com.iortatechnxt.brokerverse.collections.worklist.domain.RuleCriteria;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentService.ItemSelection;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentService.Reassign;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentService.Reassigned;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentService.Selection;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter.Ranges;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter.Scope;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter.Work;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Requests and responses of the assignment endpoints (BRCLXN.052). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class AssignmentDtos {

  private AssignmentDtos() {}

  /**
   * Criteria of a rule or a reassignment by criteria.
   *
   * @param segment segment
   * @param salesUnit sales unit
   * @param clientCode client
   * @param amountFrom lowest net outstanding
   * @param amountTo highest net outstanding
   * @param agingFrom lowest age in days
   * @param agingTo highest age in days
   * @param handler current handler (reassignment only)
   */
  public record CriteriaDto(
      @Size(max = 40) String segment,
      @Size(max = 20) String salesUnit,
      @Size(max = 30) String clientCode,
      BigDecimal amountFrom,
      BigDecimal amountTo,
      Integer agingFrom,
      Integer agingTo,
      @Size(max = 50) String handler) {

    RuleCriteria toCriteria() {
      return new RuleCriteria(
          blank(segment),
          blank(salesUnit),
          blank(clientCode),
          amountFrom,
          amountTo,
          agingFrom,
          agingTo);
    }

    WorklistFilter toFilter() {
      return WorklistFilter.of(ItemStatus.OPEN)
          .with(new Scope(blank(segment), blank(salesUnit), null, null, blank(clientCode)))
          .with(new Work(blank(handler), null, null, null, null, null, null))
          .with(new Ranges(amountFrom, amountTo, agingFrom, agingTo));
    }

    /**
     * Maps criteria.
     *
     * @param c criteria
     * @return dto
     */
    public static CriteriaDto from(RuleCriteria c) {
      return new CriteriaDto(
          c.segment(),
          c.salesUnit(),
          c.clientCode(),
          c.amountFrom(),
          c.amountTo(),
          c.agingFrom(),
          c.agingTo(),
          null);
    }

    private static String blank(String v) {
      return v == null || v.isBlank() ? null : v.strip();
    }
  }

  /**
   * A rule to create or change.
   *
   * @param priority priority (lower first)
   * @param name name
   * @param criteria criteria
   * @param handler handler
   */
  public record RuleRequest(
      @Min(1) @Max(9999) int priority,
      @NotBlank @Size(max = 120) String name,
      CriteriaDto criteria,
      @NotBlank @Size(max = 50) String handler) {

    /**
     * The rule details.
     *
     * @return details
     */
    public Details toDetails() {
      return new Details(
          priority,
          name.strip(),
          criteria == null ? RuleCriteria.NONE : criteria.toCriteria(),
          handler.strip());
    }
  }

  /**
   * A rule.
   *
   * @param id id
   * @param priority priority
   * @param name name
   * @param criteria criteria
   * @param handler handler
   * @param active active
   * @param updatedBy last maintained by
   */
  public record RuleResponse(
      Long id,
      int priority,
      String name,
      CriteriaDto criteria,
      String handler,
      boolean active,
      String updatedBy) {

    /**
     * Maps a rule.
     *
     * @param r rule
     * @return response
     */
    public static RuleResponse from(AssignmentRule r) {
      return new RuleResponse(
          r.getId(),
          r.getPriority(),
          r.getName(),
          CriteriaDto.from(r.getCriteria()),
          r.getHandlerUsername(),
          r.isActive(),
          r.getUpdatedBy() == null ? r.getCreatedBy() : r.getUpdatedBy());
    }
  }

  /**
   * A reassignment of selected accounts or of the accounts matching criteria.
   *
   * @param invoiceNos selected accounts; when empty the criteria apply
   * @param criteria criteria
   * @param handler new handler (not needed for a preview)
   * @param kind PERMANENT or TEMPORARY
   * @param validTo last day of a temporary reassignment
   * @param reason reason
   */
  public record ReassignRequest(
      @Size(max = 2000) List<String> invoiceNos,
      CriteriaDto criteria,
      @Size(max = 50) String handler,
      AssignmentKind kind,
      LocalDate validTo,
      @Size(max = 500) String reason) {

    /**
     * The selection.
     *
     * @return selection
     */
    public ItemSelection selection() {
      return new ItemSelection(invoiceNos, criteria == null ? null : criteria.toFilter());
    }

    /**
     * The reassignment.
     *
     * @return command
     */
    public Reassign command() {
      return new Reassign(handler, kind == null ? AssignmentKind.PERMANENT : kind, validTo, reason);
    }
  }

  /**
   * The preview of a reassignment.
   *
   * @param total accounts selected
   * @param items first accounts
   */
  public record SelectionResponse(int total, List<ItemResponse> items) {

    /**
     * Maps a selection.
     *
     * @param s selection
     * @return response
     */
    public static SelectionResponse from(Selection s) {
      return new SelectionResponse(s.total(), s.items().stream().map(ItemResponse::from).toList());
    }
  }

  /**
   * Activation of a rule.
   *
   * @param active new state
   */
  public record ActivateRequest(@NotNull Boolean active) {}

  /**
   * The result of a reassignment.
   *
   * @param bulkRef bulk reference, null for one account
   * @param moved accounts moved
   */
  public record ReassignedResponse(String bulkRef, int moved) {

    /**
     * Maps a result.
     *
     * @param r result
     * @return response
     */
    public static ReassignedResponse from(Reassigned r) {
      return new ReassignedResponse(r.bulkRef(), r.moved());
    }
  }
}
