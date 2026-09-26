package com.iortatechnxt.brokerverse.collections.worklist.api.dto;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.ItemStatus;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter.Ranges;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter.Scope;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter.Work;
import java.math.BigDecimal;
import java.util.List;

/**
 * Query parameters of the PR worklist (COLLECTIONS_DESIGN 11): segment, unit, UH, handler, AO,
 * status, aging bracket, category, disposition, amount, promise status, escalation, client.
 *
 * @param q search text
 * @param status statuses (default OPEN)
 * @param segment segment
 * @param salesUnit sales unit
 * @param unitHead Unit Head
 * @param handler handler
 * @param mine only the caller's accounts
 * @param unassigned only accounts without a handler
 * @param ao account officer
 * @param bracket aging bracket
 * @param category tagging category
 * @param disposition current disposition
 * @param amountFrom lowest net outstanding
 * @param amountTo highest net outstanding
 * @param agingFrom lowest age
 * @param agingTo highest age
 * @param promise promise flag
 * @param escalated escalated only / not escalated
 * @param client client
 */
public record WorklistQuery(
    String q,
    List<ItemStatus> status,
    String segment,
    String salesUnit,
    String unitHead,
    String handler,
    Boolean mine,
    Boolean unassigned,
    String ao,
    String bracket,
    String category,
    String disposition,
    BigDecimal amountFrom,
    BigDecimal amountTo,
    Integer agingFrom,
    Integer agingTo,
    String promise,
    Boolean escalated,
    String client) {

  /** Defensive copy. */
  public WorklistQuery {
    status = status == null ? List.of() : List.copyOf(status);
  }

  /**
   * The filter for a user.
   *
   * @param username caller (for "mine")
   * @return filter
   */
  public WorklistFilter toFilter(String username) {
    List<ItemStatus> statuses = status.isEmpty() ? List.of(ItemStatus.OPEN) : status;
    return new WorklistFilter(
        q,
        statuses,
        new Scope(segment, salesUnit, unitHead, ao, client),
        new Work(
            Boolean.TRUE.equals(mine) ? username : handler,
            unassigned,
            bracket,
            category,
            disposition,
            promise,
            escalated),
        new Ranges(amountFrom, amountTo, agingFrom, agingTo));
  }
}
