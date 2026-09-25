package com.iortatechnxt.brokerverse.frbs.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.RunStage;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeItem;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeItemRepository;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLineRepository;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRun;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRunRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads service-fee runs, lines and invoices (FRBS 2.10.0-2.10.2). */
@Service
@Transactional(readOnly = true)
public class ServiceFeeQueryService {

  private final ServiceFeeRunRepository runs;
  private final ServiceFeeLineRepository lines;
  private final ServiceFeeItemRepository items;

  /**
   * Creates the service.
   *
   * @param runs runs
   * @param lines lines
   * @param items invoices
   */
  public ServiceFeeQueryService(
      ServiceFeeRunRepository runs,
      ServiceFeeLineRepository lines,
      ServiceFeeItemRepository items) {
    this.runs = runs;
    this.lines = lines;
    this.items = items;
  }

  /**
   * Searches runs.
   *
   * @param companyId company
   * @param stage stage, null for all
   * @param q run number contains, blank for all
   * @param pageable page
   * @return runs
   */
  public Page<ServiceFeeRun> search(Long companyId, RunStage stage, String q, Pageable pageable) {
    String pattern =
        q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    return runs.search(companyId, stage, pattern, pageable);
  }

  /**
   * Runs per stage (work list tabs).
   *
   * @param companyId company
   * @return count per stage, every stage present
   */
  public Map<RunStage, Long> counts(Long companyId) {
    Map<RunStage, Long> counts = new EnumMap<>(RunStage.class);
    for (RunStage s : RunStage.values()) {
      counts.put(s, 0L);
    }
    for (Object[] row : runs.countByStage(companyId)) {
      counts.put((RunStage) row[0], ((Number) row[1]).longValue());
    }
    return counts;
  }

  /**
   * One run.
   *
   * @param id run
   * @return run
   */
  public ServiceFeeRun get(Long id) {
    return runs.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Service-fee run", id));
  }

  /**
   * The lines of a run.
   *
   * @param runId run
   * @return lines
   */
  public List<ServiceFeeLine> lines(Long runId) {
    return lines.findByRunIdOrderByLineNoAsc(get(runId).getId());
  }

  /**
   * The invoices of a run.
   *
   * @param runId run
   * @return invoices
   */
  public List<ServiceFeeItem> items(Long runId) {
    return items.findByRunIdOrderByInvoiceNoAsc(get(runId).getId());
  }
}
