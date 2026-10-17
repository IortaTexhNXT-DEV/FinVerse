package com.iortatechnxt.brokerverse.approval.api.dto;

import com.iortatechnxt.brokerverse.approval.service.BulkApprovalService.Item;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Inbox items to approve at once (FRBS 2.5.6, BASAU 2.5.3).
 *
 * @param items items as listed in the inbox
 */
public record BulkApprovalRequest(@NotEmpty @Size(max = 200) List<@Valid ItemRef> items) {

  /**
   * The service items.
   *
   * @return items
   */
  public List<Item> toItems() {
    return items.stream()
        .map(i -> new Item(i.module(), i.type(), i.reference(), i.companyId()))
        .toList();
  }

  /**
   * One item.
   *
   * @param module module of the inbox item
   * @param type type of the inbox item
   * @param reference reference of the inbox item
   * @param companyId company of the inbox item
   */
  public record ItemRef(
      @NotBlank @Size(max = 40) String module,
      @NotBlank @Size(max = 80) String type,
      @NotBlank @Size(max = 60) String reference,
      Long companyId) {}
}
