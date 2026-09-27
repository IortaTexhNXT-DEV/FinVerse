package com.iortatechnxt.brokerverse.migration.load.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.migration.load.api.dto.XrefResponse;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXrefRepository;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Legacy key cross-reference: finds the BIBS record loaded from a legacy key, or the legacy key of
 * a migrated BIBS record, with the batch that loaded it. Used by the Batches screen and by the
 * legacy block of the migrated records in the other modules.
 */
@RestController
@RequestMapping("/api/v1/migration/xref")
@Transactional(readOnly = true)
public class XrefController {

  private static final int MAX_PAGE = 200;

  private final KeyXrefRepository xrefs;
  private final MigBatchRepository batches;

  /**
   * Creates the controller.
   *
   * @param xrefs cross-references
   * @param batches batches
   */
  public XrefController(KeyXrefRepository xrefs, MigBatchRepository batches) {
    this.xrefs = xrefs;
    this.batches = batches;
  }

  /**
   * Searches by legacy key or BIBS code.
   *
   * @param q key fragment
   * @param page page
   * @param size size
   * @return entries
   */
  @GetMapping
  @PreAuthorize("hasAnyAuthority('MIG_VIEW', 'LEGACY_INQUIRY_VIEW')")
  public PageResponse<XrefResponse> search(
      @RequestParam String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    Map<Long, String> nos = new HashMap<>();
    return PageResponse.of(
        xrefs.findByLegacyKeyLikeIgnoreCaseOrTargetCodeLikeIgnoreCaseOrderByIdDesc(
            like, like, PageRequest.of(page, Math.min(size, MAX_PAGE))),
        x -> XrefResponse.from(x, batchNo(nos, x.getBatchId())));
  }

  /**
   * The legacy keys of a BIBS record.
   *
   * @param entity record type
   * @param code record code
   * @return live entries
   */
  @GetMapping("/of")
  @PreAuthorize("isAuthenticated()")
  public List<XrefResponse> of(@RequestParam String entity, @RequestParam String code) {
    Map<Long, String> nos = new HashMap<>();
    return xrefs.findByTargetEntityAndTargetCodeAndRolledBackAtIsNull(entity, code).stream()
        .map(x -> XrefResponse.from(x, batchNo(nos, x.getBatchId())))
        .toList();
  }

  private String batchNo(Map<Long, String> cache, Long id) {
    return id == null
        ? null
        : cache.computeIfAbsent(
            id, k -> batches.findById(k).map(MigBatch::getBatchNo).orElse(null));
  }
}
