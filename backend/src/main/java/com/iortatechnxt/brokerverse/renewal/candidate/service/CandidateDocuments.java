package com.iortatechnxt.brokerverse.renewal.candidate.service;

import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfileRepository;
import com.iortatechnxt.brokerverse.common.security.UserDisplayNames;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.renewal.check.service.CheckNames;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotPremium;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotProduct;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotSales;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResult;
import com.iortatechnxt.brokerverse.renewal.domain.Disposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
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
          "Classification",
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

  private static final SnapshotProduct NO_PRODUCT =
      new SnapshotProduct(null, null, null, null, null, null);
  private static final SnapshotSales NO_SALES =
      new SnapshotSales(null, null, null, null, null, null);
  private static final SnapshotPremium NO_PREMIUM =
      new SnapshotPremium(null, null, null, null, null, null);

  private final CandidateQueryService queries;
  private final DocumentComposer composer;
  private final OrganizationService organization;
  private final InsurerProfileRepository insurers;
  private final UserDisplayNames users;
  private final LovService lovs;

  /**
   * Creates the service.
   *
   * @param queries renewal reads
   * @param composer PDF and spreadsheet writer
   * @param organization companies
   * @param insurers insurers (names)
   * @param users user names
   * @param lovs lists of values (reasons)
   */
  public CandidateDocuments(
      CandidateQueryService queries,
      DocumentComposer composer,
      OrganizationService organization,
      InsurerProfileRepository insurers,
      UserDisplayNames users,
      LovService lovs) {
    this.queries = queries;
    this.composer = composer;
    this.organization = organization;
    this.insurers = insurers;
    this.users = users;
    this.lovs = lovs;
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
    fields.add(new Field("Classification", c.getBucket() == null ? "" : c.getBucket().label()));
    fields.add(new Field("Client", s.clientName()));
    fields.add(new Field("Expiring invoice", c.getExpiringInvoiceNo()));
    fields.add(new Field("Expiring ARN", c.getExpiringArn()));
    fields.add(new Field("Policy number", s.policyNo()));
    fields.add(new Field("Insurer", insurer(c.getCompanyId(), s.insurerCode())));
    fields.add(new Field("Expiry date", DisplayFormat.date(c.getExpiryDate())));
    fields.add(new Field("Account Officer", name(c.getAssignedAo())));
    fields.add(new Field("Processing Officer", name(c.getAssignedPo())));
    if (!hide && s.premium() != null) {
      fields.add(new Field("Gross premium", DisplayFormat.value(s.premium().grossPremium())));
      fields.add(new Field("Sum insured", DisplayFormat.value(s.premium().totalSumInsured())));
    }
    List<List<String>> checks =
        queries.latestResults(c).stream().map(CandidateDocuments::checkRow).toList();
    List<List<String>> dispositions =
        queries.dispositions(c).stream().map(this::dispositionRow).toList();
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
    byte[] xlsx = composer.xlsx(new SheetSpec("Renewals", HEADERS, rows(filter)));
    return new MessageFile("renewals.xlsx", XLSX, xlsx);
  }

  /**
   * The rows of a renewal list export (premium columns empty for LAMD and Contact Center).
   *
   * @param filter criteria
   * @return rows in the order of {@link #headers()}
   */
  public List<List<Object>> rows(CandidateFilter filter) {
    boolean hide = queries.scope(filter.companyId()).hidePremium();
    List<List<Object>> rows = new ArrayList<>();
    int page = 0;
    List<RenewalCandidate> chunk;
    do {
      chunk = queries.list(filter, page++, CandidateQueryService.MAX_CHUNK).getContent();
      chunk.forEach(c -> rows.add(exportRow(c, hide)));
    } while (chunk.size() == CandidateQueryService.MAX_CHUNK && rows.size() < MAX_EXPORT);
    return rows;
  }

  /**
   * The columns of a renewal list export.
   *
   * @return headers
   */
  public static List<String> headers() {
    return HEADERS;
  }

  private List<Object> exportRow(RenewalCandidate c, boolean hide) {
    CandidateSnapshot s = c.getSnapshot();
    SnapshotProduct product = s.product() == null ? NO_PRODUCT : s.product();
    SnapshotSales sales = s.sales() == null ? NO_SALES : s.sales();
    SnapshotPremium premium = hide || s.premium() == null ? NO_PREMIUM : s.premium();
    return Arrays.asList(
        c.getRenewalRef(),
        c.getStage().label(),
        c.getBucket() == null ? "" : c.getBucket().label(),
        c.getDisposition().code() == null ? "" : c.getDisposition().code().label(),
        c.getExpiringInvoiceNo(),
        c.getExpiringArn(),
        s.policyNo(),
        s.clientName(),
        product.productCode(),
        product.lineCode(),
        insurer(c.getCompanyId(), s.insurerCode()),
        product.segment(),
        sales.branchCode(),
        c.getOwnerUnit(),
        name(sales.accountOfficer()),
        name(c.getAssignedAo()),
        name(c.getAssignedPo()),
        c.getExpiryDate(),
        premium.currency(),
        premium.grossPremium(),
        premium.totalSumInsured());
  }

  private static List<String> checkRow(CheckResult r) {
    return List.of(
        CheckNames.of(r.getCheckCode()),
        r.getOutcome().label(),
        r.getSeverity().label(),
        Objects.toString(r.getMessage(), ""));
  }

  private List<String> dispositionRow(Disposition d) {
    return List.of(
        d.getCode().label(),
        Objects.toString(lovs.label(RenewalCodes.LOV_NONRENEWAL_REASON, d.getReasonCode()), ""),
        Objects.toString(d.getRemarks(), ""),
        d.getSource().label(),
        name(d.getCreatedBy()),
        DisplayFormat.dateTime(d.getCreatedAt()));
  }

  private String insurer(Long companyId, String code) {
    return code == null
        ? ""
        : insurers
            .findByCompanyIdAndPartyCode(companyId, code)
            .map(InsurerProfile::getName)
            .orElse(code);
  }

  private String name(String username) {
    return username == null ? "" : Objects.toString(users.displayName(username), username);
  }
}
