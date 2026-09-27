package com.iortatechnxt.brokerverse.renewal.candidate.service;

import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.renewal.check.service.CheckNames;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResult;
import com.iortatechnxt.brokerverse.renewal.domain.Disposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Downloads of the renewal lists and record page (FR-RN-013, 041): the record details as PDF and a
 * list as a spreadsheet with the columns of the grid.
 */
@Service
@Transactional(readOnly = true)
public class CandidateDocuments {

  /** Largest list export. */
  public static final int MAX_EXPORT = 20_000;

  private static final String PDF = "application/pdf";
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final List<String> HEADERS =
      List.of(
          "Renewal reference",
          "Status",
          "Bucket",
          "Disposition",
          "Expiring invoice",
          "Expiring ARN",
          "Policy number",
          "Client",
          "Risk code",
          "Product line",
          "Insurer",
          "Segment",
          "Branch",
          "Unit",
          "Account officer",
          "Assigned AO",
          "Assigned PO",
          "Expiry date",
          "Currency",
          "Gross premium",
          "Sum insured");

  private final CandidateQueryService queries;
  private final DocumentComposer composer;
  private final OrganizationService organization;

  /**
   * Creates the service.
   *
   * @param queries renewal reads
   * @param composer PDF and spreadsheet writer
   * @param organization companies
   */
  public CandidateDocuments(
      CandidateQueryService queries, DocumentComposer composer, OrganizationService organization) {
    this.queries = queries;
    this.composer = composer;
    this.organization = organization;
  }

  /**
   * The record details of a renewal as PDF.
   *
   * @param companyId company
   * @param renewalRef renewal reference
   * @return file
   */
  public MessageFile details(Long companyId, String renewalRef) {
    RenewalCandidate c = queries.get(companyId, renewalRef);
    boolean hide = queries.scope(companyId).hidePremium();
    CandidateSnapshot s = c.getSnapshot();
    List<Field> fields = new ArrayList<>();
    fields.add(new Field("Renewal reference", c.getRenewalRef()));
    fields.add(new Field("Status", c.getStage().label()));
    fields.add(new Field("Bucket", text(c.getBucket())));
    fields.add(new Field("Client", s.clientName()));
    fields.add(new Field("Expiring invoice", c.getExpiringInvoiceNo()));
    fields.add(new Field("Expiring ARN", c.getExpiringArn()));
    fields.add(new Field("Policy number", s.policyNo()));
    fields.add(new Field("Insurer", s.insurerCode()));
    fields.add(new Field("Expiry date", String.valueOf(c.getExpiryDate())));
    fields.add(new Field("Assigned AO", c.getAssignedAo()));
    fields.add(new Field("Assigned PO", c.getAssignedPo()));
    if (!hide && s.premium() != null) {
      fields.add(new Field("Gross premium", text(s.premium().grossPremium())));
      fields.add(new Field("Sum insured", text(s.premium().totalSumInsured())));
    }
    List<List<String>> checks =
        queries.latestResults(c).stream().map(CandidateDocuments::checkRow).toList();
    List<List<String>> dispositions =
        queries.dispositions(c).stream().map(CandidateDocuments::dispositionRow).toList();
    byte[] pdf =
        composer.pdf(
            new DocumentSpec(
                organization.getCompany(c.getCompanyId()).getName(),
                "Renewal Record",
                c.getRenewalRef(),
                List.of(
                    new Fields("Renewal", fields),
                    new Table(
                        "Checks", List.of("Check", "Outcome", "Severity", "Message"), checks, null),
                    new Table(
                        "Dispositions",
                        List.of("Disposition", "Reason", "Remarks", "Source", "By", "On"),
                        dispositions,
                        null)),
                List.of(),
                null));
    return new MessageFile(c.getRenewalRef() + ".pdf", PDF, pdf);
  }

  /**
   * A renewal list as a spreadsheet (premium columns empty for LAMD and Contact Center).
   *
   * @param filter criteria
   * @return file
   */
  public MessageFile export(CandidateFilter filter) {
    boolean hide = queries.scope(filter.companyId()).hidePremium();
    List<List<Object>> rows = new ArrayList<>();
    int page = 0;
    List<RenewalCandidate> chunk;
    do {
      chunk = queries.list(filter, page++, CandidateQueryService.MAX_CHUNK).getContent();
      chunk.forEach(c -> rows.add(exportRow(c, hide)));
    } while (chunk.size() == CandidateQueryService.MAX_CHUNK && rows.size() < MAX_EXPORT);
    byte[] xlsx = composer.xlsx(new SheetSpec("Renewals", HEADERS, rows));
    return new MessageFile("renewals.xlsx", XLSX, xlsx);
  }

  private static List<Object> exportRow(RenewalCandidate c, boolean hide) {
    CandidateSnapshot s = c.getSnapshot();
    var product = s.product();
    var sales = s.sales();
    var premium = hide ? null : s.premium();
    return Arrays.asList(
        c.getRenewalRef(),
        c.getStage().label(),
        text(c.getBucket()),
        text(c.getDisposition().code()),
        c.getExpiringInvoiceNo(),
        c.getExpiringArn(),
        s.policyNo(),
        s.clientName(),
        product == null ? null : product.productCode(),
        product == null ? null : product.lineCode(),
        s.insurerCode(),
        product == null ? null : product.segment(),
        sales == null ? null : sales.branchCode(),
        c.getOwnerUnit(),
        sales == null ? null : sales.accountOfficer(),
        c.getAssignedAo(),
        c.getAssignedPo(),
        c.getExpiryDate(),
        premium == null ? null : premium.currency(),
        premium == null ? null : premium.grossPremium(),
        premium == null ? null : premium.totalSumInsured());
  }

  private static List<String> checkRow(CheckResult r) {
    return List.of(
        CheckNames.of(r.getCheckCode()),
        r.getOutcome().label(),
        r.getSeverity().label(),
        Objects.toString(r.getMessage(), ""));
  }

  private static List<String> dispositionRow(Disposition d) {
    return List.of(
        d.getCode().label(),
        Objects.toString(d.getReasonCode(), ""),
        Objects.toString(d.getRemarks(), ""),
        d.getSource().label(),
        d.getCreatedBy(),
        String.valueOf(d.getCreatedAt()));
  }

  private static String text(Object value) {
    return value == null ? "" : value.toString();
  }
}
