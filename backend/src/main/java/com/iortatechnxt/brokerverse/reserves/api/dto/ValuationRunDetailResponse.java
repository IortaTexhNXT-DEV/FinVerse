package com.iortatechnxt.brokerverse.reserves.api.dto;

import com.iortatechnxt.brokerverse.reserves.domain.ReserveKey.PostingKey;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveLineValues;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveType;
import com.iortatechnxt.brokerverse.reserves.domain.ValuationRun;
import com.iortatechnxt.brokerverse.reserves.service.ReserveEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Valuation run with its reserve lines, totals per reserve type and the journal movements it posts
 * (or would post) against the previous posted run.
 *
 * @param run header
 * @param totals totals per reserve type
 * @param lines reserve lines
 * @param movements journal amount components per branch and line of business
 */
public record ValuationRunDetailResponse(
    ValuationRunResponse run, List<Total> totals, List<Line> lines, List<Movement> movements) {

  /**
   * Builds the detail.
   *
   * @param run run
   * @param lines line values
   * @param movements movements by posting unit and event
   * @param branchCodes branch code by id
   * @return response
   */
  public static ValuationRunDetailResponse of(
      ValuationRun run,
      List<ReserveLineValues> lines,
      Map<PostingKey, Map<ReserveEvent, Map<String, BigDecimal>>> movements,
      Function<Long, String> branchCodes) {
    Map<ReserveType, BigDecimal[]> sums = new EnumMap<>(ReserveType.class);
    for (ReserveLineValues l : lines) {
      BigDecimal[] s =
          sums.computeIfAbsent(l.type(), t -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
      s[0] = s[0].add(l.gross());
      s[1] = s[1].add(l.ri());
    }
    List<Total> totals = new ArrayList<>();
    sums.forEach(
        (t, s) -> totals.add(new Total(t.name(), t.label(), s[0], s[1], s[0].subtract(s[1]))));
    List<Movement> moves = new ArrayList<>();
    movements.forEach(
        (unit, events) ->
            events.forEach(
                (event, amounts) ->
                    amounts.forEach(
                        (component, amount) ->
                            moves.add(
                                new Movement(
                                    branchCodes.apply(unit.branchId()),
                                    unit.businessLine(),
                                    event.name(),
                                    component,
                                    amount)))));
    return new ValuationRunDetailResponse(
        ValuationRunResponse.from(run),
        totals,
        lines.stream().map(l -> Line.from(l, branchCodes)).toList(),
        moves);
  }

  /**
   * Total of one reserve type.
   *
   * @param type reserve type code
   * @param label reserve label
   * @param gross gross
   * @param ri reinsurers' share (DAC: UCR)
   * @param net net
   */
  public record Total(String type, String label, BigDecimal gross, BigDecimal ri, BigDecimal net) {}

  /**
   * One reserve line.
   *
   * @param type reserve type
   * @param branchId branch id
   * @param branchCode branch code
   * @param businessLine line of business
   * @param productCode product
   * @param sourceType channel
   * @param gross gross
   * @param ri reinsurers' share
   * @param net net
   * @param base calculation base (IBNR: earned premium, ULAE / MfAD: OSLR + IBNR)
   * @param rate rate %
   * @param method method
   */
  public record Line(
      String type,
      Long branchId,
      String branchCode,
      String businessLine,
      String productCode,
      String sourceType,
      BigDecimal gross,
      BigDecimal ri,
      BigDecimal net,
      BigDecimal base,
      BigDecimal rate,
      String method) {

    /**
     * Maps a line.
     *
     * @param l values
     * @param branchCodes branch code by id
     * @return line
     */
    public static Line from(ReserveLineValues l, Function<Long, String> branchCodes) {
      return new Line(
          l.type().name(),
          l.key().branchId(),
          branchCodes.apply(l.key().branchId()),
          l.key().businessLine(),
          l.key().productCode(),
          l.key().sourceType(),
          l.gross(),
          l.ri(),
          l.net(),
          l.base(),
          l.rate(),
          l.method());
    }
  }

  /**
   * One journal amount component.
   *
   * @param branchCode branch
   * @param businessLine line of business
   * @param eventType accounting event
   * @param component amount component
   * @param amount signed movement
   */
  public record Movement(
      String branchCode,
      String businessLine,
      String eventType,
      String component,
      BigDecimal amount) {}
}
