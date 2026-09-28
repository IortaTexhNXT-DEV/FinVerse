package com.iortatechnxt.brokerverse.migration.mapping.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkFileReader;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.bulk.service.TextLayout;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import com.iortatechnxt.brokerverse.common.excel.GuidedSheet;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplate;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplateWriter;
import com.iortatechnxt.brokerverse.common.excel.GuidedWorkbook;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapEntry;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapEntry.EntryData;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapVersion;
import com.iortatechnxt.brokerverse.migration.mapping.domain.EntryAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;

/**
 * Excel export and import of a code map version (DATA_MIGRATION_DESIGN section 7: "map sets are
 * exportable to Excel and importable as a new DRAFT version"; the Data Steward edits in Excel). The
 * export is a guided sheet (title block, column guide above the header, the entries as rows); the
 * import reads it as it is, or a plain file with the headers in the first row.
 */
@Component
public class CodeMapExcel {

  private static final int MAX_SHEET_NAME = 31;

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
    List<GuideColumn> columns = columns();
    GuidedTemplate template =
        GuidedTemplate.single(
            "Code map " + version.getSetCode(),
            "The entries of code map "
                + version.getSetCode()
                + ": how each legacy code is translated into BIBS. Edit the entries and import the"
                + " file as a new draft version.",
            "The data steward of the code map",
            "Data Migration > Code Maps, the set's button Import Excel: the file becomes a new draft"
                + " version for approval.",
            List.of(
                "One row per legacy code (and qualifier); keep the header texts.",
                "Rows already in the file are the entries of the exported version."),
            new GuidedSheet(
                sheet(version.getSetCode()),
                "Entries of " + version.getSetCode(),
                "One row per legacy code.",
                columns,
                List.of()));
    try (GuidedWorkbook wb = GuidedTemplateWriter.open(template)) {
      int r = wb.firstDataRow(0);
      for (CodeMapEntry e : entries) {
        List<String> values =
            List.of(
                e.getSourceSystem(),
                e.getLegacyCode(),
                text(e.getLegacyDescription()),
                text(e.getQualifier()),
                text(e.getQualifierValue()),
                e.getAction().name(),
                text(e.getTargetCode()),
                text(e.getRemarks()));
        Row row = wb.sheet(0).createRow(r++);
        for (int c = 0; c < values.size(); c++) {
          Cell cell = row.createCell(GuidedWorkbook.sheetColumn(c));
          cell.setCellStyle(wb.dataStyle(Kind.TEXT));
          cell.setCellValue(values.get(c));
        }
      }
      return wb.bytes();
    }
  }

  private static List<GuideColumn> columns() {
    return List.of(
        GuideColumn.of(COLUMNS.get(0), Kind.TEXT, "Legacy system of the code").mandatory(),
        GuideColumn.of(COLUMNS.get(1), Kind.TEXT, "Code as stored in the legacy system")
            .mandatory(),
        GuideColumn.of(COLUMNS.get(2), Kind.TEXT, "Description of the code in the legacy system"),
        GuideColumn.of(COLUMNS.get(3), Kind.TEXT, "Field that qualifies the code, when any"),
        GuideColumn.of(COLUMNS.get(4), Kind.TEXT, "Value of the qualifier")
            .when("a qualifier is given"),
        GuideColumn.of(COLUMNS.get(5), Kind.TEXT, "What the load does with the code")
            .mandatory()
            .choices(
                List.of(
                    new Choice("MAP", "Map to the target code"),
                    new Choice("DEFAULT", "Map to the default target of the set"),
                    new Choice("REJECT", "Reject the row"),
                    new Choice("CREATE", "Create a new BIBS value"))),
        GuideColumn.of(COLUMNS.get(6), Kind.TEXT, "BIBS code the legacy code becomes")
            .when("action is MAP or CREATE"),
        GuideColumn.of(COLUMNS.get(7), Kind.TEXT, "Remarks"));
  }

  private static String sheet(String setCode) {
    String name = setCode.replaceAll("[\\\\/?*\\[\\]:]", "_");
    return name.length() > MAX_SHEET_NAME ? name.substring(0, MAX_SHEET_NAME) : name;
  }

  /**
   * Reads the entries of an imported file.
   *
   * @param fileName file name
   * @param content bytes
   * @return entries
   */
  public List<EntryData> read(String fileName, byte[] content) {
    ParsedFile parsed = reader.read(fileName, content, TextLayout.AUTO, COLUMNS);
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
