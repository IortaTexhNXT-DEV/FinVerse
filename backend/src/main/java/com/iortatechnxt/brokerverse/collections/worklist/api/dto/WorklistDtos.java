package com.iortatechnxt.brokerverse.collections.worklist.api.dto;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.ItemStatus;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistQueryService.GroupTotal;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistRefreshService.RefreshOutcome;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

/** Responses of the worklist endpoints (BRCLXN.001-015). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class WorklistDtos {

  private WorklistDtos() {}

  /**
   * Total of a client or account (BRCLXN.003).
   *
   * @param key client code or ARN
   * @param name an assured name
   * @param items accounts
   * @param netOutstanding net outstanding
   */
  public record GroupTotalResponse(String key, String name, long items, BigDecimal netOutstanding) {

    /**
     * Maps a total.
     *
     * @param t total
     * @return response
     */
    public static GroupTotalResponse from(GroupTotal t) {
      return new GroupTotalResponse(t.key(), t.name(), t.items(), t.netOutstanding());
    }
  }

  /**
   * Counts of a refresh (BRCLXN.013-015).
   *
   * @param created listed
   * @param updated refreshed
   * @param closed completed or excluded
   * @param reopened reopened
   * @param reverted temporary assignments ended
   * @param assigned assigned by rule
   * @param message summary
   */
  public record RefreshResponse(
      int created,
      int updated,
      int closed,
      int reopened,
      int reverted,
      int assigned,
      String message) {

    /**
     * Maps an outcome.
     *
     * @param o outcome
     * @return response
     */
    public static RefreshResponse from(RefreshOutcome o) {
      return new RefreshResponse(
          o.created(),
          o.updated(),
          o.closed(),
          o.reopened(),
          o.reverted(),
          o.assigned(),
          o.message());
    }
  }

  /**
   * The client view (BRCLXN.003).
   *
   * @param clientCode client
   * @param name an assured name of the client
   * @param items accounts, newest booking first
   * @param openOutstanding net outstanding of the open accounts
   * @param openPr2307 PR2307 of the open accounts
   */
  public record ClientViewResponse(
      String clientCode,
      String name,
      List<ItemResponse> items,
      BigDecimal openOutstanding,
      BigDecimal openPr2307) {

    /** Defensive copy. */
    public ClientViewResponse {
      items = List.copyOf(items);
    }

    /**
     * Maps the items of a client.
     *
     * @param clientCode client
     * @param items items
     * @return response
     */
    public static ClientViewResponse from(String clientCode, List<CollectionItem> items) {
      List<CollectionItem> open =
          items.stream().filter(i -> i.getStatus() == ItemStatus.OPEN).toList();
      return new ClientViewResponse(
          clientCode,
          items.isEmpty() ? null : items.get(0).getParties().assuredName(),
          items.stream().map(ItemResponse::from).toList(),
          sum(open, i -> i.getFigures().netOutstanding()),
          sum(open, i -> i.getFigures().outstandingPr2307()));
    }

    private static BigDecimal sum(
        List<CollectionItem> items, Function<CollectionItem, BigDecimal> amount) {
      return items.stream().map(amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
  }
}
