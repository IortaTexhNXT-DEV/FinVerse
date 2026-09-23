package com.iortatechnxt.finverse.reserves.service;

import com.iortatechnxt.finverse.reserves.domain.ReserveLineValues;
import com.iortatechnxt.finverse.reserves.domain.TakafulItem;
import com.iortatechnxt.finverse.reserves.domain.UprItem;
import java.util.List;

/**
 * Outcome of a reserve calculation at a valuation date.
 *
 * @param lines reserve lines (non-zero) by type and reporting unit
 * @param uprItems policy-level UPR (still unearned, or approved in the valuation month)
 * @param takafulItems takaful surplus of policies expiring in the valuation month
 * @param analyses chain-ladder analyses of the lines of business using that method
 * @param remarks calculation notes (missing parameters, modules not deployed)
 */
public record ValuationResult(
    List<ReserveLineValues> lines,
    List<UprItem> uprItems,
    List<TakafulItem> takafulItems,
    List<TriangleAnalysis> analyses,
    List<String> remarks) {

  /** Canonical constructor copying the lists. */
  public ValuationResult {
    lines = List.copyOf(lines);
    uprItems = List.copyOf(uprItems);
    takafulItems = List.copyOf(takafulItems);
    analyses = List.copyOf(analyses);
    remarks = List.copyOf(remarks);
  }
}
