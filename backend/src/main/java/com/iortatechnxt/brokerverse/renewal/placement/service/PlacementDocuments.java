package com.iortatechnxt.brokerverse.renewal.placement.service;

import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfileRepository;
import com.iortatechnxt.brokerverse.common.security.UserDisplayNames;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotPremium;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotProduct;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotSales;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalPlacement;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * The placement documents (FRRN.029.02, Annexes T and U): the placement slip of a renewal account
 * with one insurer and the consolidated placement file of an insurer, with the fields of the
 * placement file layout; and the Insurance Advice of a mortgaged account (FRRN.032.01, Annex W).
 */
@Component
public class PlacementDocuments {

  /** Columns of the consolidated placement file (Annex U). */
  public static final List<String> FILE_HEADERS =
      List.of(
          "Branch",
          "Assured Name",
          "Address",
          "Reference No",
          "Unit Head",
          "Account Officer",
          "Bank Branch",
          "Department",
          "Market Segment",
          "Business Type",
          "Account Type",
          "Insurer",
          "Insurer Branch Location",
          "Type of Policy",
          "Previous Policy Number",
          "Insurer Package Name",
          "Package / Cover Option",
          "Risk Code",
          "Risk Information Details",
          "Items Insured",
          "Location of Risk (LOR)",
          "Construction",
          "Occupancy",
          "Boundaries",
          "Basis of Valuation",
          "Mortgagee",
          "Sum Insured",
          "Inception",
          "Expiry",
          "Basic Premium",
          "Documentary Stamps",
          "VAT / Premium Tax",
          "FST",
          "LGT",
          "Other Charges",
          "Validation Fee / CTPL Fee",
          "Total Premium",
          "Commission Rate",
          "Commission Amount",
          "Loan PL No",
          "EOPT TIN & Branch Code",
          "Name of Taxpayer",
          "Share (%)");

  private static final SnapshotProduct NO_PRODUCT =
      new SnapshotProduct(null, null, null, null, null, null);
  private static final SnapshotSales NO_SALES =
      new SnapshotSales(null, null, null, null, null, null);
  private static final SnapshotPremium NO_PREMIUM =
      new SnapshotPremium(null, null, null, null, null, null);
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final DocumentComposer composer;
  private final InsurerProfileRepository insurers;
  private final UserDisplayNames users;
  private final OrganizationService organization;

  /**
   * Creates the documents.
   *
   * @param composer PDF and workbook rendering
   * @param insurers insurer names
   * @param users user names
   * @param organization letterhead
   */
  public PlacementDocuments(
      DocumentComposer composer,
      InsurerProfileRepository insurers,
      UserDisplayNames users,
      OrganizationService organization) {
    this.composer = composer;
    this.insurers = insurers;
    this.users = users;
    this.organization = organization;
  }

  /**
   * The placement slip of a renewal account with one insurer.
   *
   * @param c renewal
   * @param p placement
   * @return PDF
   */
  public byte[] slip(RenewalCandidate c, RenewalPlacement p) {
    List<Object> values = row(c, p);
    List<Field> fields = new ArrayList<>();
    for (int i = 0; i < FILE_HEADERS.size(); i++) {
      String value = text(values.get(i));
      if (!value.isEmpty()) {
        fields.add(new Field(FILE_HEADERS.get(i), value));
      }
    }
    return composer.pdf(
        new DocumentSpec(
            organization.getCompany(c.getCompanyId()).getName(),
            "Placement Slip",
            c.getRenewalRef(),
            List.of(
                new Fields("Renewal account", fields),
                new Text(
                    "Request",
                    "Please place the renewal of the policy above with the share shown and send"
                        + " your approval or rejection with the reference number.")),
            List.of("Prepared by", "Approved by"),
            null));
  }

  /**
   * The consolidated placement file of an insurer.
   *
   * @param lines renewal accounts and their placement with the insurer
   * @return workbook
   */
  public byte[] file(List<Line> lines) {
    List<List<Object>> rows = new ArrayList<>();
    lines.forEach(l -> rows.add(row(l.candidate(), l.placement())));
    return composer.xlsx(new SheetSpec("Placement File", FILE_HEADERS, rows));
  }

  /**
   * The Insurance Advice of a mortgaged renewal account.
   *
   * @param c renewal
   * @param placements its current placements
   * @param versionNo version
   * @return PDF
   */
  public byte[] advice(RenewalCandidate c, List<RenewalPlacement> placements, int versionNo) {
    CandidateSnapshot s = c.getSnapshot();
    LocalDate inception = c.getExpiryDate();
    List<Field> fields = new ArrayList<>();
    fields.add(new Field("Mortgagee", s.mortgage() == null ? "" : s.mortgage().bank()));
    fields.add(new Field("Assured", s.clientName()));
    fields.add(new Field("Reference Number", c.getRenewalRef()));
    fields.add(new Field("Previous Policy Number", s.policyNo()));
    fields.add(
        new Field("Period of Insurance", DisplayFormat.period(inception, inception.plusYears(1))));
    fields.add(new Field("PN Numbers", Objects.toString(s.pnNos(), "")));
    placements.forEach(
        p ->
            fields.add(
                new Field(
                    insurer(c.getCompanyId(), p.getInsurerCode()),
                    p.getSharePercent().stripTrailingZeros().toPlainString()
                        + "% - sum insured "
                        + DisplayFormat.value(p.getSumInsured()))));
    return composer.pdf(
        new DocumentSpec(
            organization.getCompany(c.getCompanyId()).getName(),
            "Insurance Advice",
            c.getRenewalRef() + " v" + versionNo,
            List.of(
                new Fields("Insurance", fields),
                new Text(
                    "Advice",
                    "This is to advise that the renewal of the insurance of the mortgaged property"
                        + " above has been placed with the insurer(s) shown.")),
            List.of("Authorized signatory"),
            null));
  }

  private List<Object> row(RenewalCandidate c, RenewalPlacement p) {
    CandidateSnapshot s = c.getSnapshot();
    SnapshotProduct product = s.product() == null ? NO_PRODUCT : s.product();
    SnapshotSales sales = s.sales() == null ? NO_SALES : s.sales();
    SnapshotPremium premium = s.premium() == null ? NO_PREMIUM : s.premium();
    LocalDate inception = c.getExpiryDate();
    return Arrays.asList(
        sales.branchCode(),
        s.clientName(),
        "",
        c.getRenewalRef(),
        name(sales.unitHead()),
        name(c.getAssignedAo() == null ? sales.accountOfficer() : c.getAssignedAo()),
        "",
        sales.departmentCode(),
        product.segment(),
        "Renewal",
        product.accountType(),
        insurer(c.getCompanyId(), p.getInsurerCode()),
        "",
        product.lineCode(),
        s.policyNo(),
        product.productName(),
        s.legacyPackageCode(),
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        s.mortgage() == null ? "" : s.mortgage().bank(),
        p.getSumInsured(),
        inception,
        inception.plusYears(1),
        share(premium.basicPremium(), p.getSharePercent()),
        "",
        "",
        "",
        "",
        "",
        "",
        p.getPremium(),
        premium.commissionRate(),
        commission(p.getPremium(), premium.commissionRate()),
        s.pnNos(),
        "",
        s.clientName(),
        p.getSharePercent());
  }

  private static BigDecimal share(BigDecimal amount, BigDecimal percent) {
    return amount == null
        ? null
        : amount.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
  }

  private static BigDecimal commission(BigDecimal premium, BigDecimal rate) {
    return premium == null || rate == null
        ? null
        : premium.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
  }

  private static String text(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof LocalDate d) {
      return DisplayFormat.date(d);
    }
    return value instanceof BigDecimal b ? DisplayFormat.value(b) : value.toString();
  }

  private String insurer(Long companyId, String code) {
    return insurers
        .findByCompanyIdAndPartyCode(companyId, code)
        .map(InsurerProfile::getName)
        .orElse(code);
  }

  private String name(String username) {
    return username == null ? "" : Objects.toString(users.displayName(username), username);
  }

  /**
   * A renewal account in a placement file.
   *
   * @param candidate renewal
   * @param placement its placement with the insurer
   */
  public record Line(RenewalCandidate candidate, RenewalPlacement placement) {}
}
