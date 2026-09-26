package com.iortatechnxt.brokerverse.eb.tracked.api.dto;

import com.iortatechnxt.brokerverse.eb.domain.EbResponsibleParty;
import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItem;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemService;
import java.time.LocalDate;

/** Request bodies of the Pending Items API (FR-EB-057). */
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

  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
