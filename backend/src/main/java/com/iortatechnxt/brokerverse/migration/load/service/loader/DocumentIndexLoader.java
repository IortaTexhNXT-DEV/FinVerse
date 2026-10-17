package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.archive.service.ArchiveService;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Loader of the legacy document index (object H02; DATA_MIGRATION_DESIGN section 16): each file of
 * the transfer folder is read through the document source, checked against the size and SHA-256 of
 * the index and attached to its archive record as a legacy document.
 */
@Component
public class DocumentIndexLoader implements MigrationLoader {

  /** Entity of a legacy document in the cross-reference. */
  public static final String ENTITY = "Attachment";

  private static final String DOCUMENT_TYPE = "document_type";

  private final ArchiveService archive;

  /**
   * Creates the loader.
   *
   * @param archive archive
   */
  public DocumentIndexLoader(ArchiveService archive) {
    this.archive = archive;
  }

  @Override
  public String objectCode() {
    return "H02";
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    Map<String, String> v = unit.values();
    String fileName = Values.text(v.get("file_name"));
    long size =
        Values.decimal(v.get("file_size_bytes"))
            .orElseThrow(
                () -> new BusinessRuleException("MIG_DOCUMENT_SIZE", "The file size is missing"))
            .longValueExact();
    Attachment saved =
        archive.attach(
            ctx.companyId(),
            new ArchiveService.IndexedDocument(
                unit.sourceSystem(),
                Values.code(v.get("record_type")),
                Values.text(v.get("legacy_key")),
                fileName,
                size,
                Optional.ofNullable(Values.text(v.get("sha256"))).orElse(""),
                description(v, unit.main().getRawPayload())));
    return LoadOutcome.of(ENTITY, saved.getId(), fileName, null);
  }

  private static String description(Map<String, String> v, Map<String, String> raw) {
    List<String> parts = new ArrayList<>();
    String type = Values.text(v.get(DOCUMENT_TYPE));
    String legacy = Values.text(raw.get(DOCUMENT_TYPE));
    Optional.ofNullable(type).ifPresent(parts::add);
    if (legacy != null && !legacy.equals(type)) {
      parts.add("(" + legacy + ")");
    }
    Optional.ofNullable(Values.text(v.get("document_no"))).ifPresent(n -> parts.add("no. " + n));
    Optional.ofNullable(Values.text(v.get("document_date"))).ifPresent(d -> parts.add("of " + d));
    return String.join(" ", parts);
  }

  @Override
  public boolean reversible() {
    return true;
  }

  @Override
  public boolean compensate(KeyXref entry, LoadContext ctx) {
    archive.removeDocument(entry.getTargetId());
    return true;
  }
}
