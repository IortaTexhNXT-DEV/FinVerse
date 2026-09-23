package com.iortatechnxt.finverse.closing.api.dto;

import com.iortatechnxt.finverse.closing.service.CheckItem;
import java.util.List;

/**
 * Checklist result.
 *
 * @param ready true when no blocking control failed
 * @param items controls
 */
public record ChecklistResponse(boolean ready, List<CheckItem> items) {

  /**
   * Builds a response.
   *
   * @param items controls
   * @return response
   */
  public static ChecklistResponse of(List<CheckItem> items) {
    return new ChecklistResponse(items.stream().noneMatch(CheckItem::blocks), List.copyOf(items));
  }
}
