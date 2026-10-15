package com.iortatechnxt.brokerverse.dashboard.api.dto;

import com.iortatechnxt.brokerverse.dashboard.domain.HomeWidget;

/**
 * A widget of the catalogue.
 *
 * @param code widget code
 * @param name widget name
 */
public record HomeWidgetResponse(String code, String name) {

  /**
   * Maps a widget.
   *
   * @param w widget
   * @return the response
   */
  public static HomeWidgetResponse from(HomeWidget w) {
    return new HomeWidgetResponse(w.name(), w.displayName());
  }
}
