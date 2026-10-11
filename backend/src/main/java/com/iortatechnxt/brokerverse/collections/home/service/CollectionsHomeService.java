package com.iortatechnxt.brokerverse.collections.home.service;

import com.iortatechnxt.brokerverse.collections.common.service.CollectionsWorkCountSource;
import com.iortatechnxt.brokerverse.collections.common.service.CollectionsWorkCountSource.WorkCount;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistQueryService;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistQueryService.AgingCell;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Collections home (COLLECTIONS_DESIGN 11, CQ21): the tiles of every Collections sub-module
 * ({@link CollectionsWorkCountSource}) for the signed-in user, in order, and the open amounts per
 * segment and aging bracket.
 */
@Service
@Transactional(readOnly = true)
public class CollectionsHomeService {

  private final WorklistQueryService worklist;
  private final List<CollectionsWorkCountSource> sources;
  private final CurrentUser currentUser;

  /**
   * Creates the service.
   *
   * @param worklist worklist reads
   * @param sources every tile source
   * @param currentUser signed-in user
   */
  public CollectionsHomeService(
      WorklistQueryService worklist,
      List<CollectionsWorkCountSource> sources,
      CurrentUser currentUser) {
    this.worklist = worklist;
    this.sources = List.copyOf(sources);
    this.currentUser = currentUser;
  }

  /**
   * The home of the signed-in user.
   *
   * @param companyId company
   * @return tiles in order and the aging chart
   */
  public Home home(Long companyId) {
    String me = currentUser.username();
    List<WorkCount> tiles = new ArrayList<>();
    for (CollectionsWorkCountSource source : sources) {
      tiles.addAll(source.counts(companyId, me));
    }
    tiles.sort(Comparator.comparingInt(WorkCount::order).thenComparing(WorkCount::key));
    return new Home(tiles, worklist.aging(companyId));
  }

  /**
   * The Collections home.
   *
   * @param tiles tiles in order
   * @param aging open amounts per segment and bracket
   */
  public record Home(List<WorkCount> tiles, List<AgingCell> aging) {

    /** Defensive copies. */
    public Home {
      tiles = List.copyOf(tiles);
      aging = List.copyOf(aging);
    }
  }
}
