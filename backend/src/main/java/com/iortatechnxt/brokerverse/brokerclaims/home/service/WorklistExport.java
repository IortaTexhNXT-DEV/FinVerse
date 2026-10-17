package com.iortatechnxt.brokerverse.brokerclaims.home.service;

import com.iortatechnxt.brokerverse.brokerclaims.home.service.WorklistQuery.WorklistCriteria;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.WorklistQuery.WorklistRow;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Export of the filtered and ordered worklist to Excel and PDF as shown (FRS BRD-07 FRCLM.009.08,
 * OOTB point G-CLM-04): the rows and order of the screen, up to {@value #MAX_ROWS} claims.
 */
@Component
public class WorklistExport {

  /** Most claims in one export. */
  public static final int MAX_ROWS = 5000;

  /** Excel content type. */
  public static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private static final List<String> HEADERS =
      List.of(
          "Claim No.",
          "Claimant",
          "Assured",
          "Plate No.",
          "Status",
          "Follow-ups",
          "Last Follow-up",
          "Days Since Last Follow-up",
          "Age This Stage",
          "Age Overall",
          "Next Follow-up",
          "Handler",
          "Account Officer",
          "Claim Amount");
  private static final List<Integer> NUMBERS = List.of(5, 7, 8, 9, 13);

  private final WorklistQuery worklist;
  private final DocumentComposer composer;
  private final OrganizationService organization;
  private final Clock clock;

  /**
   * Creates the export.
   *
   * @param worklist worklist rows
   * @param composer Excel and PDF
   * @param organization company name of the PDF
   * @param clock date of the PDF
   */
  public WorklistExport(
      WorklistQuery worklist,
      DocumentComposer composer,
      OrganizationService organization,
      Clock clock) {
    this.worklist = worklist;
    this.composer = composer;
    this.organization = organization;
    this.clock = clock;
  }

  /**
   * The rows of the export.
   *
   * @param companyId company
   * @param criteria tab, filters and order of the screen
   * @return rows
   */
  public List<WorklistRow> rows(Long companyId, WorklistCriteria criteria) {
    return worklist.search(companyId, criteria, 0, MAX_ROWS).content();
  }

  /**
   * The worklist as an Excel sheet.
   *
   * @param companyId company
   * @param criteria criteria of the screen
   * @return XLSX bytes
   */
  public byte[] excel(Long companyId, WorklistCriteria criteria) {
    List<List<Object>> rows = new ArrayList<>();
    for (WorklistRow r : rows(companyId, criteria)) {
      rows.add(new ArrayList<>(cells(r)));
    }
    return composer.xlsx(new SheetSpec("Claims", HEADERS, rows));
  }

  /**
   * The worklist as a PDF.
   *
   * @param companyId company
   * @param criteria criteria of the screen
   * @return PDF bytes
   */
  public byte[] pdf(Long companyId, WorklistCriteria criteria) {
    List<List<String>> rows = new ArrayList<>();
    for (WorklistRow r : rows(companyId, criteria)) {
      rows.add(cells(r).stream().map(WorklistExport::text).toList());
    }
    DocumentSpec spec =
        new DocumentSpec(
            organization.getCompany(companyId).getName(),
            "CLAIMS WORKLIST",
            criteria.tab().name() + " as of " + DisplayFormat.date(BusinessClock.today(clock)),
            List.of(new Table(null, HEADERS, rows, NUMBERS)),
            List.of(),
            null);
    return composer.pdf(spec);
  }

  private static List<Object> cells(WorklistRow r) {
    List<Object> c = new ArrayList<>();
    c.add(r.claimNo());
    c.add(nz(r.cover().claimantName()));
    c.add(nz(r.cover().assuredName()));
    c.add(nz(r.followUps().plateNo()));
    c.add(nz(r.handling().statusLabel()));
    c.add(r.followUps().count());
    c.add(date(r.followUps().lastDate()));
    c.add(r.followUps().daysSinceLast());
    c.add(r.dates().ageThisStage());
    c.add(r.dates().ageOverall());
    c.add(date(r.dates().nextFollowUpDate()));
    c.add(nz(r.handling().handler()));
    c.add(nz(r.followUps().accountOfficer()));
    c.add(r.claimAmount() == null ? BigDecimal.ZERO : r.claimAmount());
    return c;
  }

  private static String text(Object value) {
    return value instanceof BigDecimal b ? DisplayFormat.amount(b) : String.valueOf(value);
  }

  private static String nz(String value) {
    return value == null ? "" : value;
  }

  private static String date(LocalDate value) {
    return value == null ? "" : DisplayFormat.date(value);
  }
}
