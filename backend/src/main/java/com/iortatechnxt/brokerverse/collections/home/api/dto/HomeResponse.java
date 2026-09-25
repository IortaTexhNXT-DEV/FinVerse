package com.iortatechnxt.brokerverse.collections.home.api.dto;

import com.iortatechnxt.brokerverse.collections.common.service.CollectionsWorkCountSource.WorkCount;
import com.iortatechnxt.brokerverse.collections.home.service.CollectionsHomeService.Home;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistQueryService.AgingCell;
import java.math.BigDecimal;
import java.util.List;

/**
 * The Collections home (COLLECTIONS_DESIGN 11, CQ21).
 *
 * @param tiles tiles in order
 * @param aging open amounts per segment and bracket
 */
public record HomeResponse(List<HomeResponse.Tile> tiles, List<HomeResponse.Aging> aging) {

  /** Defensive copies. */
  public HomeResponse {
    tiles = List.copyOf(tiles);
    aging = List.copyOf(aging);
  }

  /**
   * Maps the home.
   *
   * @param h home
   * @return response
   */
  public static HomeResponse from(Home h) {
    return new HomeResponse(
        h.tiles().stream().map(Tile::from).toList(), h.aging().stream().map(Aging::from).toList());
  }

  /**
   * A tile.
   *
   * @param key key
   * @param label label
   * @param value count
   * @param link route
   * @param alert draws attention to a non-zero count
   */
  public record Tile(String key, String label, long value, String link, boolean alert) {

    static Tile from(WorkCount w) {
      return new Tile(w.key(), w.label(), w.value(), w.link(), w.alert());
    }
  }

  /**
   * A cell of the aging chart.
   *
   * @param segment segment
   * @param bracket bracket
   * @param items accounts
   * @param netOutstanding net outstanding
   */
  public record Aging(String segment, String bracket, long items, BigDecimal netOutstanding) {

    static Aging from(AgingCell c) {
      return new Aging(c.segment(), c.bracket(), c.items(), c.netOutstanding());
    }
  }
}
