package com.iortatechnxt.brokerverse.migration.mapping.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkFileReader;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.common.service.Workbooks;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapEntry;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapEntry.EntryData;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapVersion;
import com.iortatechnxt.brokerverse.migration.mapping.domain.EntryAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Excel export and import of a code map version (DATA_MIGRATION_DESIGN section 7: "map sets are
 * exportable to Excel and importable as a new DRAFT version"; the Data Steward edits in Excel).
 */
@Component
public class CodeMapExcel {

  /** Columns of the code map sheet. */
  public static final List<String> COLUMNS =
      List.of(
          "source_system",
          "legacy_code",
          "legacy_description",
          "qualifier",
          "qualifier_value",
          "action",
          "target_code",
          "remarks");

  private final BulkFileReader reader;

  /**
   * Creates the helper.
   *
   * @param reader file reader
   */
  public CodeMapExcel(BulkFileReader reader) {
    this.reader = reader;
  }

  /**
   * The entries of a version as a workbook.
   *
   * @param version version
   * @param entries entries
   * @return XLSX bytes
   */
  public byte[] export(CodeMapVersion version, List<CodeMapEntry> entries) {
    List<List<String>> rows =
        entries.stream()
            .map(
                e ->
                    List.of(
                        e.getSourceSystem(),
                        e.getLegacyCode(),
                        text(e.getLegacyDescription()),
                        e.getQualifier(),
                        e.getQualifierValue(),
                        e.getAction().name(),
                        text(e.getTargetCode()),
                        text(e.getRemarks())))
            .toList();
    try (Workbooks wb = Workbooks.create()) {
      return wb.sheet(sheet(version.getSetCode()), COLUMNS, rows).bytes();
    }
  }

  private static String sheet(String setCode) {
    String name = setCode.replaceAll("[\\\\/?*\\[\\]:]", "_");
    return name.length() > 31 ? name.substring(0, 31) : name;
  }

  /**
   * Reads the entries of an imported file.
   *
   * @param fileName file name
   * @param content bytes
   * @return entries
   */
  public List<EntryData> read(String fileName, byte[] content) {
    ParsedFile parsed = reader.read(fileName, content);
    if (!parsed.headers().containsAll(List.of("source_system", "legacy_code", "action"))) {
      throw new BusinessRuleException(
          "MIG_MAP_IMPORT_LAYOUT",
          "The file must have the columns source_system, legacy_code and action");
    }
    List<EntryData> out = new ArrayList<>();
    for (ParsedFile.RawRow row : parsed.rows()) {
      Map<String, String> v = row.values();
      out.add(
          new EntryData(
              v.get("source_system"),
              v.get("legacy_code"),
              v.get("legacy_description"),
              v.get("qualifier"),
              v.get("qualifier_value"),
              action(row.rowNo(), v.get("action")),
              v.get("target_code"),
              v.get("remarks")));
    }
    return out;
  }

  private static EntryAction action(int rowNo, String raw) {
    try {
      return EntryAction.valueOf(raw == null ? "" : raw.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new BusinessRuleException(
          "MIG_MAP_IMPORT_ACTION",
          "Row " + rowNo + ": the action must be MAP, DEFAULT, REJECT or CREATE",
          e);
    }
  }

  private static String text(String v) {
    return v == null ? "" : v;
  }
}
