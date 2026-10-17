package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Section;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalDocuments;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalDocuments.InsurerFileName;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.productmaint.service.TermsSources.Facts;
import com.iortatechnxt.brokerverse.productmaint.service.TermsTableService.OptionColumn;
import com.iortatechnxt.brokerverse.productmaint.service.TermsTableService.TermsTable;
import com.iortatechnxt.brokerverse.report.core.ExportFileNames;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The documents of the comparative table: the proposal slip of one insurer built from the Final
 * Terms for Proposal (FRPM.009.02, FRPM.013.01) and the comparative table with the fields chosen
 * for the client (PDF and Excel). File names follow BDOI's FRS when BDOI's file names apply.
 */
@Component
public class TermsDocuments {

  private static final String FIELD = "Field";
  private static final String QS_VALUE = "QS Value";
  private static final String FINAL_TERMS = "Final Terms for Proposal";

  private final DocumentComposer composer;
  private final OrganizationService organization;
  private final UserDirectory users;
  private final CurrentUser currentUser;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the builder.
   *
   * @param composer PDF and Excel composer
   * @param organization companies (letterhead)
   * @param users user names
   * @param currentUser current user (preparer)
   * @param parameters business parameters (file names)
   * @param clock clock
   */
  public TermsDocuments(
      DocumentComposer composer,
      OrganizationService organization,
      UserDirectory users,
      CurrentUser currentUser,
      SystemParameterService parameters,
      Clock clock) {
    this.composer = composer;
    this.organization = organization;
    this.users = users;
    this.currentUser = currentUser;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * The proposal slip of one insurer: the Final Terms for Proposal of the fields sent to the
   * client, with the insurer's own quotation options beside them.
   *
   * @param facts the record
   * @param table the comparative table
   * @param insurer insurer code and name
   * @param versionNo version of the insurer's slip
   * @return PDF file
   */
  public MessageFile proposalSlip(Facts facts, TermsTable table, String[] insurer, int versionNo) {
    List<OptionColumn> own =
        table.columns().stream().filter(c -> c.insurerCode().equals(insurer[0])).toList();
    List<String> headers = new ArrayList<>(List.of(FIELD, FINAL_TERMS));
    own.forEach(c -> headers.add("Option " + c.optionNo()));
    List<List<String>> rows = new ArrayList<>();
    for (String key : table.clientFields()) {
      List<String> row = new ArrayList<>();
      row.add(TermsField.valueOf(key).label());
      row.add(text(table.finalTerms().get(key)));
      own.forEach(c -> row.add(text(c.values().get(key))));
      rows.add(row);
    }
    LocalDate today = BusinessClock.today(clock);
    List<Section> sections =
        List.of(
            new Fields(
                "Proposal",
                List.of(
                    new Field("Client", facts.clientName()),
                    new Field("Reference No.", facts.reference()),
                    new Field("Insurer", insurer[1]),
                    new Field("Version", String.valueOf(versionNo)),
                    new Field("Date", DisplayFormat.date(today)))),
            new Table("Proposed terms", headers, rows, List.of()));
    byte[] pdf =
        composer.pdf(
            new DocumentSpec(
                company(facts),
                "Proposal Slip",
                facts.reference() + " / " + insurer[1],
                sections,
                List.of(
                    DocumentSpec.signature(
                        "Prepared by", users.displayName(currentUser.username()))),
                "Proposal slip version " + versionNo));
    String fileName =
        ExportFileNames.bdoi(parameters)
            ? ExportFileNames.dated(
                new InsurerFileName("ProposalSlip", facts.reference(), insurer[1]).stem()
                    + "_"
                    + versionNo,
                today,
                "pdf")
            : "PS_" + facts.reference() + "_" + insurer[0] + "_v" + versionNo + ".pdf";
    return new MessageFile(fileName, ProposalDocuments.PDF, pdf);
  }

  /**
   * The comparative table with the fields chosen for the client, as PDF or Excel.
   *
   * @param facts the record
   * @param table the comparative table
   * @param excel true for Excel, false for PDF
   * @return file
   */
  public MessageFile comparative(Facts facts, TermsTable table, boolean excel) {
    List<OptionColumn> columns =
        table.selectedInsurers().isEmpty()
            ? table.columns()
            : table.columns().stream()
                .filter(c -> table.selectedInsurers().contains(c.insurerCode()))
                .toList();
    List<String> headers = new ArrayList<>(List.of(FIELD, QS_VALUE));
    columns.forEach(c -> headers.add(c.insurerName() + " - Option " + c.optionNo()));
    headers.add(FINAL_TERMS);
    List<List<String>> rows = new ArrayList<>();
    for (String key : table.clientFields()) {
      List<String> row = new ArrayList<>();
      row.add(TermsField.valueOf(key).label());
      row.add(text(table.qsValues().get(key)));
      columns.forEach(c -> row.add(text(c.values().get(key))));
      row.add(text(table.finalTerms().get(key)));
      rows.add(row);
    }
    List<String> response = new ArrayList<>(List.of("Insurer Response", ""));
    columns.forEach(c -> response.add(TermsAnswers.label(c.answer(), c.otherAnswer())));
    response.add("");
    rows.add(response);
    String ext = excel ? "xlsx" : "pdf";
    String fileName =
        ExportFileNames.bdoi(parameters)
            ? ExportFileNames.dated(
                "ComparativeTable_" + facts.reference(), BusinessClock.today(clock), ext)
            : facts.reference() + "_comparative." + ext;
    if (excel) {
      List<List<Object>> cells = rows.stream().map(r -> List.<Object>copyOf(r)).toList();
      return new MessageFile(
          fileName,
          ProposalDocuments.XLSX,
          composer.xlsx(new SheetSpec("Comparative Table", headers, cells)));
    }
    byte[] pdf =
        composer.pdf(
            new DocumentSpec(
                company(facts),
                "Comparative Table",
                facts.reference(),
                List.of(new Table(null, headers, rows, List.of())),
                List.of("Prepared by"),
                facts.clientName()));
    return new MessageFile(fileName, ProposalDocuments.PDF, pdf);
  }

  private String company(Facts facts) {
    return organization.getCompany(facts.companyId()).getName();
  }

  private static String text(String value) {
    return value == null ? "" : value;
  }
}
