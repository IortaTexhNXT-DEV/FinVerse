package com.iortatechnxt.brokerverse.bulk.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRepository;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Stored rows of the bulk framework: their JSON values, the commit of one row in its own
 * transaction with its outcome category, re-validation before a reprocess and the committed rows
 * per outcome (BRQID.006).
 */
@Component
class BulkRowStore {

  private static final TypeReference<Map<String, String>> ROW_TYPE = new TypeReference<>() {};

  private final BulkRowRepository rows;
  private final ObjectMapper json;
  private final TransactionTemplate tx;
  private final TransactionTemplate rowTx;

  BulkRowStore(BulkRowRepository rows, ObjectMapper json, PlatformTransactionManager txManager) {
    this.rows = rows;
    this.json = json;
    this.tx = new TransactionTemplate(txManager);
    this.rowTx = new TransactionTemplate(txManager);
    this.rowTx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Commits one row in its own transaction and records the outcome or the failure.
   *
   * @param handler handler
   * @param record stored row
   * @param context run context
   * @return true when committed
   */
  boolean commit(BulkImportHandler handler, BulkRowRecord record, BulkContext context) {
    BulkRow row = new BulkRow(record.getRowNo(), read(record.getData()));
    try {
      BulkOutcome outcome = rowTx.execute(s -> handler.process(row, context));
      String reference = outcome == null ? null : outcome.reference();
      String category = outcome == null ? null : outcome.category();
      tx.executeWithoutResult(
          s -> rows.findById(record.getId()).ifPresent(r -> r.committed(reference, category)));
      return true;
    } catch (BusinessRuleException | ResourceNotFoundException e) {
      tx.executeWithoutResult(
          s -> rows.findById(record.getId()).ifPresent(r -> r.failed(e.getMessage())));
      return false;
    }
  }

  /**
   * Validates a failed row again before it is reprocessed; a row still invalid keeps the new
   * messages.
   *
   * @param handler handler
   * @param record stored row
   * @param context run context
   * @return true when valid
   */
  boolean revalidates(BulkImportHandler handler, BulkRowRecord record, BulkContext context) {
    List<String> errors =
        handler.validate(new BulkRow(record.getRowNo(), read(record.getData())), context);
    if (errors.isEmpty()) {
      return true;
    }
    tx.executeWithoutResult(
        s -> rows.findById(record.getId()).ifPresent(r -> r.failed(String.join("; ", errors))));
    return false;
  }

  /**
   * Committed rows per outcome category, in the handler's order; uncategorised rows count as
   * COMMITTED.
   *
   * @param handler handler
   * @param jobId job
   * @return count per category
   */
  Map<String, Long> outcomes(BulkImportHandler handler, Long jobId) {
    Map<String, Long> counts = new LinkedHashMap<>();
    handler.outcomeCategories().forEach(c -> counts.put(c, 0L));
    for (Object[] row : rows.countOutcomes(jobId, BulkRowStatus.COMMITTED)) {
      String category = row[0] == null ? BulkRowStatus.COMMITTED.name() : (String) row[0];
      counts.merge(category, (Long) row[1], Long::sum);
    }
    return counts;
  }

  /**
   * Values as JSON.
   *
   * @param values values, may be null
   * @return JSON, null for null
   */
  String write(Map<String, String> values) {
    if (values == null || values.isEmpty()) {
      return values == null ? null : "{}";
    }
    try {
      return json.writeValueAsString(values);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Values of stored JSON.
   *
   * @param data JSON, may be blank
   * @return values
   */
  Map<String, String> read(String data) {
    if (data == null || data.isBlank()) {
      return Map.of();
    }
    try {
      return json.readValue(data, ROW_TYPE);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
