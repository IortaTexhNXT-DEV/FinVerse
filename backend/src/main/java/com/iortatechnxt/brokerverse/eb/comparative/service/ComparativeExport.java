package com.iortatechnxt.brokerverse.eb.comparative.service;

import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.eb.domain.EbComparative;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.eb.service.EbTemplates;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The comparative as PDF (sent to the client, printed) and as an Excel workbook with the same
 * content (FR-EB-041): premium per benefit line and insurer with the recommendation, the answers to
 * the TOR items and the capability factors.
 */
@Component
@Transactional(readOnly = true)
public class ComparativeExport {

  private static final String RECOMMENDED = "Recommended";

  /** Room left for the sheet name after its number (Excel allows 31 characters). */
  private static final int SHEET_NAME = 25;

  private final EbComparativeService comparatives;
  private final EbRecords records;
  private final EbParties parties;
  private final EbTemplates templates;

  /**
   * Creates the export.
   *
   * @param comparatives snapshot reader
   * @param records programme look-up
   * @param parties AO name
   * @param templates PDF composer
   */
  public ComparativeExport(
      EbComparativeService comparatives,
      EbRecords records,
      EbParties parties,
      EbTemplates templates) {
    this.comparatives = comparatives;
    this.records = records;
    this.parties = parties;
    this.templates = templates;
  }

  /**
   * The comparative as PDF.
   *
   * @param comparative comparative
   * @return PDF bytes
   */
  public byte[] pdf(EbComparative comparative) {
    EbProgramme programme =
        records.programme(comparative.getCompanyId(), comparative.getProgrammeId());
    List<DocumentSpec.Section> sections = new ArrayList<>();
    sections.add(
        new DocumentSpec.Fields(
            "Programme",
            List.of(
                new DocumentSpec.Field("Client", programme.getClientName()),
                new DocumentSpec.Field("Programme", programme.getName()),
                new DocumentSpec.Field(
                    "Comparative",
                    comparative.getComparativeNo() + " version " + comparative.getVersionNo()))));
    tables(comparative)
        .forEach(
            t ->
                sections.add(
                    new DocumentSpec.Table(t.heading(), t.headers(), t.rows(), List.of())));
    if (comparative.getSummary() != null && !comparative.getSummary().isBlank()) {
      sections.add(new DocumentSpec.Text("Recommendation", comparative.getSummary()));
    }
    return templates.pdf(
        comparative.getCompanyId(),
        new EbTemplates.Heading("COMPARATIVE ANALYSIS", comparative.getComparativeNo(), null),
        sections,
        List.of(parties.aoName(programme)));
  }

  /**
   * The comparative of a company as an Excel workbook.
   *
   * @param companyId company
   * @param comparativeId comparative
   * @return XLSX bytes
   */
  public byte[] xlsx(Long companyId, Long comparativeId) {
    return xlsx(comparatives.require(companyId, comparativeId));
  }

  /**
   * The comparative as an Excel workbook, one sheet per table.
   *
   * @param comparative comparative
   * @return XLSX bytes
   */
  public byte[] xlsx(EbComparative comparative) {
    try (XSSFWorkbook book = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      int n = 1;
      for (Grid grid : tables(comparative)) {
        Sheet sheet = book.createSheet(n++ + " " + safe(grid.heading()));
        Row header = sheet.createRow(0);
        for (int c = 0; c < grid.headers().size(); c++) {
          header.createCell(c).setCellValue(grid.headers().get(c));
        }
        for (int r = 0; r < grid.rows().size(); r++) {
          Row row = sheet.createRow(r + 1);
          List<String> cells = grid.rows().get(r);
          for (int c = 0; c < cells.size(); c++) {
            row.createCell(c).setCellValue(cells.get(c));
          }
        }
      }
      book.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String safe(String heading) {
    String clean = heading.replaceAll("[\\\\/?*\\[\\]:]", " ");
    return clean.length() > SHEET_NAME ? clean.substring(0, SHEET_NAME) : clean;
  }

  /**
   * The tables of the comparative.
   *
   * @param comparative comparative
   * @return tables in order
   */
  List<Grid> tables(EbComparative comparative) {
    ComparativeMatrix matrix = comparatives.matrix(comparative);
    List<Grid> grids = new ArrayList<>();
    for (ComparativeMatrix.LineRow line : matrix.lines()) {
      grids.add(premiums(comparative, matrix, line));
    }
    if (!matrix.items().isEmpty()) {
      grids.add(items(matrix));
    }
    if (!matrix.factors().isEmpty()) {
      grids.add(factors(matrix));
    }
    return grids;
  }

  private static Grid premiums(
      EbComparative comparative, ComparativeMatrix matrix, ComparativeMatrix.LineRow line) {
    Long recommended =
        comparative
            .line(line.benefitLine())
            .map(EbComparative.Line::getRecommendedProposalId)
            .orElse(null);
    List<List<String>> rows = new ArrayList<>();
    for (ComparativeMatrix.Column column : matrix.proposals()) {
      ComparativeMatrix.Offer offer = line.offers().get(column.proposalId().toString());
      if (offer == null) {
        continue;
      }
      rows.add(
          List.of(
              column.insurerName(),
              column.proposalNo() + " v" + column.versionNo(),
              amount(offer.annualPremium()),
              amount(offer.sumInsured()),
              String.valueOf(offer.plans().size()),
              column.proposalId().equals(line.lowestProposalId()) ? "Lowest" : "",
              column.proposalId().equals(recommended) ? RECOMMENDED : ""));
    }
    return new Grid(
        line.label(),
        List.of("Insurer", "Proposal", "Annual premium", "TSI", "Plans", "Lowest", RECOMMENDED),
        rows);
  }

  private static Grid items(ComparativeMatrix matrix) {
    List<String> headers = new ArrayList<>(List.of("Benefit line", "Item", "Requirement"));
    matrix.proposals().forEach(c -> headers.add(c.insurerName()));
    List<List<String>> rows = new ArrayList<>();
    for (ComparativeMatrix.ItemRow item : matrix.items()) {
      List<String> row =
          new ArrayList<>(List.of(item.benefitLine(), item.description(), item.requirement()));
      for (ComparativeMatrix.Column c : matrix.proposals()) {
        ComparativeMatrix.Answer a = item.answers().get(c.proposalId().toString());
        row.add(a == null ? "-" : a.offeredValue() + (a.deviation() ? " (deviation)" : ""));
      }
      rows.add(row);
    }
    return new Grid("Coverage", headers, rows);
  }

  private static Grid factors(ComparativeMatrix matrix) {
    List<String> headers = new ArrayList<>(List.of("Factor"));
    matrix.proposals().forEach(c -> headers.add(c.insurerName()));
    List<List<String>> rows = new ArrayList<>();
    for (ComparativeMatrix.FactorRow factor : matrix.factors()) {
      List<String> row = new ArrayList<>(List.of(factor.label()));
      for (ComparativeMatrix.Column c : matrix.proposals()) {
        ComparativeMatrix.Rating r = factor.ratings().get(c.proposalId().toString());
        row.add(r == null ? "-" : text(r));
      }
      rows.add(row);
    }
    return new Grid("Capability", headers, rows);
  }

  private static String text(ComparativeMatrix.Rating r) {
    String value = r.value() == null ? "" : r.value();
    return r.rating() == null ? value : (value + " (" + r.rating() + "/5)").strip();
  }

  private static String amount(BigDecimal value) {
    return value == null
        ? ""
        : new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
            .format(value);
  }

  /**
   * A table of text cells.
   *
   * @param heading heading
   * @param headers column headers
   * @param rows rows
   */
  record Grid(String heading, List<String> headers, List<List<String>> rows) {}
}
