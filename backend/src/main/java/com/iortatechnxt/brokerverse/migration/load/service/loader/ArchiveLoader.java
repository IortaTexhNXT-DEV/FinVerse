package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.migration.archive.domain.ArchiveRecord;
import com.iortatechnxt.brokerverse.migration.archive.domain.ArchiveRecordRepository;
import com.iortatechnxt.brokerverse.migration.archive.service.ArchiveService;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Loader of the archive records (object H01, also used for the archived objects C04, P02 and G02;
 * DATA_MIGRATION_DESIGN section 16): each closed transaction or history record is kept read-only in
 * {@code mig_archive_record} with its search keys, dates, amount and status, and the other legacy
 * columns as labelled values, for the Legacy Inquiry.
 */
@Component
public class ArchiveLoader implements MigrationLoader {

  private static final String AMOUNT = "amount";
  private static final String DETAIL = "detail_json";

  private final ArchiveService archive;
  private final ArchiveRecordRepository records;
  private final ObjectMapper json;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param archive archive
   * @param records archive records (read)
   * @param json JSON reader of the legacy details
   * @param currentUser current user
   * @param clock clock
   */
  public ArchiveLoader(
      ArchiveService archive,
      ArchiveRecordRepository records,
      ObjectMapper json,
      CurrentUser currentUser,
      Clock clock) {
    this.archive = archive;
    this.records = records;
    this.json = json;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Override
  public String objectCode() {
    return "H01";
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    Map<String, String> v = unit.values();
    String type = Values.code(v.get("record_type"));
    String key = Values.text(v.get("legacy_key"));
    ArchiveRecord record =
        archive.record(
            ctx.companyId(),
            new ArchiveRecord.Keys(
                unit.sourceSystem(),
                type,
                key,
                Values.code(v.get("legacy_client_no")),
                Values.text(v.get("client_name")),
                Values.text(v.get("policy_no")),
                Values.text(v.get("invoice_no")),
                Values.text(v.get("receipt_no")),
                Values.text(v.get("claim_no"))),
            new ArchiveRecord.Facts(
                Values.date(v.get("document_date"))
                    .orElseThrow(
                        () ->
                            new BusinessRuleException(
                                "MIG_DATE_REQUIRED", "The record has no document date")),
                Values.date(v.get("period_from")).orElse(null),
                Values.date(v.get("period_to")).orElse(null),
                Values.code(v.get("currency")),
                Values.decimal(v.get(AMOUNT)).orElse(null),
                Values.text(v.get("status")),
                details(v.get(DETAIL))),
            new ArchiveRecord.Load(
                ctx.batch().getId(), unit.hash(), currentUser.username(), clock.instant()));
    return LoadOutcome.of(MigrationCodes.ENTITY_ARCHIVE, record.getId(), type + " " + key, null);
  }

  private Map<String, String> details(String text) {
    Map<String, String> out = new LinkedHashMap<>();
    if (Values.blank(text)) {
      return out;
    }
    try {
      JsonNode node = json.readTree(text);
      if (!node.isObject()) {
        throw new BusinessRuleException(
            "MIG_ARCHIVE_DETAIL", "The legacy details must be an object of label and value");
      }
      node.properties().forEach(e -> out.put(e.getKey(), e.getValue().asText()));
      return out;
    } catch (JsonProcessingException e) {
      throw new BusinessRuleException(
          "MIG_ARCHIVE_DETAIL",
          "The legacy details are not valid JSON: " + e.getOriginalMessage(),
          e);
    }
  }

  @Override
  public List<String> reconciledColumns() {
    return List.of(AMOUNT);
  }

  @Override
  public Map<String, String> readBack(KeyXref entry) {
    return records
        .findById(entry.getTargetId())
        .map(
            r -> {
              Map<String, String> m = new HashMap<>();
              if (r.getAmount() != null) {
                m.put(AMOUNT, r.getAmount().toPlainString());
              }
              return m;
            })
        .orElse(Map.of());
  }

  @Override
  public boolean reversible() {
    return true;
  }

  @Override
  public boolean compensate(KeyXref entry, LoadContext ctx) {
    archive.rollBackRecord(entry.getTargetId());
    return true;
  }

  @Override
  public Optional<BigDecimal> targetTotal(AmountMeasure measure, List<KeyXref> loaded) {
    if (!"H01".equals(measure.layoutCode()) || !AMOUNT.equals(measure.column())) {
      return Optional.empty();
    }
    BigDecimal total = BigDecimal.ZERO;
    for (ArchiveRecord r :
        records.findAllById(loaded.stream().map(KeyXref::getTargetId).toList())) {
      boolean currency = measure.currency() == null || measure.currency().equals(r.getCurrency());
      if (currency && r.getAmount() != null) {
        total = total.add(r.getAmount());
      }
    }
    return Optional.of(total);
  }
}
