package com.iortatechnxt.brokerverse.eb.tracked.api.dto;

import com.iortatechnxt.brokerverse.eb.domain.EbResponsibleParty;
import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItem;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemQuery;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemService;
import java.time.LocalDate;

/** Request bodies of the Pending Items API (FR-EB-057). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class ItemRequests {

  private ItemRequests() {}

  /**
   * A tracked item to open or change.
   *
   * @param programmeId programme (open only)
   * @param cycleId cycle, may be null (open only)
   * @param itemType type (list EB_TRACKED_ITEM_TYPE; open only)
   * @param subject what is expected
   * @param memberRef employee number or member name
   * @param memberChangeRef member change number
   * @param accountArn account
   * @param responsible INSURER, CLIENT or BDOI
   * @param partyCode insurer party code
   * @param recipientEmail follow-up recipients, comma separated
   * @param dueDate due date
   * @param remarks remarks
   */
  public record ItemRequest(
      Long programmeId,
      Long cycleId,
      String itemType,
      String subject,
      String memberRef,
      String memberChangeRef,
      String accountArn,
      EbResponsibleParty responsible,
      String partyCode,
      String recipientEmail,
      LocalDate dueDate,
      String remarks) {

    /**
     * The item details, blanks as null.
     *
     * @return details
     */
    public EbTrackedItem.Details toDetails() {
      return new EbTrackedItem.Details(
          blank(subject),
          blank(memberRef),
          blank(memberChangeRef),
          blank(accountArn),
          responsible,
          blank(partyCode),
          blank(recipientEmail),
          dueDate,
          blank(remarks));
    }

    /**
     * The service input of a new item.
     *
     * @return input
     */
    public TrackedItemService.ItemInput toInput() {
      return new TrackedItemService.ItemInput(blank(itemType), cycleId, toDetails());
    }
  }

  /**
   * A status change.
   *
   * @param action RECEIVE, RELEASE or CLOSE
   * @param date date of the change
   * @param receivedOn date received when closing a pending item
   * @param remarks remarks
   */
  public record StatusRequest(
      TrackedItemService.Action action, LocalDate date, LocalDate receivedOn, String remarks) {

    /**
     * The service change.
     *
     * @return change
     */
    public TrackedItemService.StatusChange toChange() {
      return new TrackedItemService.StatusChange(action, date, receivedOn, remarks);
    }
  }

  /**
   * Query parameters of the Pending Items list.
   *
   * @param programmeId programme
   * @param member employee number or member name
   * @param type item type
   * @param responsible INSURER, CLIENT or BDOI
   * @param status status
   * @param overdue only pending items past due
   * @param q programme, client, subject or ARN
   * @param page page, 0 when empty
   * @param size page size, 20 when empty (at most 200)
   */
  public record ItemFilter(
      Long programmeId,
      String member,
      String type,
      String responsible,
      String status,
      Boolean overdue,
      String q,
      Integer page,
      Integer size) {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 200;

    /**
     * The query criteria.
     *
     * @return criteria
     */
    public TrackedItemQuery.ItemCriteria criteria() {
      return new TrackedItemQuery.ItemCriteria(
          programmeId, member, type, responsible, status, Boolean.TRUE.equals(overdue), q);
    }

    /**
     * The page asked for.
     *
     * @return zero-based page
     */
    public int pageNo() {
      return page == null ? 0 : Math.max(0, page);
    }

    /**
     * The page size asked for.
     *
     * @return size between 1 and 200
     */
    public int pageSize() {
      return size == null ? DEFAULT_SIZE : Math.clamp(size, 1, MAX_SIZE);
    }
  }

  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
