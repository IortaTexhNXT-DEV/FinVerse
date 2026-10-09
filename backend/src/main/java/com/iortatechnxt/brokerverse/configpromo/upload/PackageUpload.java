package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.catalog.domain.PackageInsurerRole;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSetupService;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSpec;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * PM-06 Packages: one row per package version of a packaged risk code. The row becomes a DRAFT
 * version of the product (the package set-up of MBS), which is then validated and released on
 * Products as usual. The ManCom sign-off reference identifies the version: a row whose reference
 * already has a draft replaces the draft; a reference already released changes nothing.
 *
 * <p>The cells with several entries (coverages, insurers, insurer terms) hold one entry per line of
 * the cell, the values of an entry separated by semicolons.
 */
@Component
// The package tab holds a whole product version in one row.
@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity"})
public class PackageUpload extends ConfigUploadHandler {

  static final String RISK = "Risk code";
  static final String NAME = "Package name / Scope / Client code";
  static final String DATES = "Effective from / Package start / Package end / Anniversary";
  static final String TERMS = "Package rate % / Minimum premium / Commission % / Package TSI limit";
  static final String BASIS = "Computation basis";
  static final String COVERAGES = "Coverage code / Included / Optional / Limit / Deductible";
  static final String INSURERS = "Insurer code / Role / Share % / Rate % / Minimum premium";
  static final String INSURER_TERMS =
      "Insurer terms: insurer / coverage / limit / deductible / clauses";
  static final String SIGNOFF = "ManCom sign-off reference / Signed package slip";

  private static final List<String> SCOPES =
      List.of("GENERIC", "CLIENT-SPECIFIC", "CLIENT_SPECIFIC");
  private static final String ENTRY_SEPARATOR = "\\r?\\n|\\|";
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int THREE_PARTS = 3;
  private static final int FOUR_PARTS = 4;
  private static final int FIVE_PARTS = 5;
  private static final int FOURTH = 3;
  private static final int FIFTH = 4;
  private static final int MAX_SUMMARY = 300;

  private final PackageSetupService packages;

  /**
   * Creates the handler.
   *
   * @param db database support
   * @param packages package set-up of the catalog
   */
  public PackageUpload(UploadSupport db, PackageSetupService packages) {
    super(db);
    this.packages = packages;
  }

  @Override
  public String code() {
    return "CFG_PACKAGE";
  }

  @Override
  public String templateId() {
    return "PM-06";
  }

  @Override
  public String title() {
    return "Packages";
  }

  @Override
  public String screen() {
    return "Product Maintenance > Products";
  }

  @Override
  public String permission() {
    return "PRODUCT_MAINTAIN";
  }

  /** The draft versions are set up by MBS: a second package maintainer approves the upload. */
  @Override
  public String approvePermission() {
    return "PRODUCT_MAINTAIN";
  }

  @Override
  public String filledBy() {
    return "TSU; MBS";
  }

  @Override
  public List<String> rules() {
    List<String> rules = new ArrayList<>(super.rules());
    rules.add(
        "Each row becomes a draft version of the package, validated and released on Products.");
    rules.add(
        "Coverages, insurers and insurer terms: one entry per line of the cell, values separated by ;.");
    return rules;
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(RISK, "Risk code of the packaged product", "MTR30").master("product"),
        BulkColumn.required(
            NAME, "Name; Generic or Client-specific; client code", "Motor Fleet Plus; Generic"),
        BulkColumn.required(
                DATES,
                "Effective from; package start; package end; anniversary",
                "01-Jan-2028; 01-Jan-2028; 31-Dec-2028; -")
            .format("dd-MMM-yyyy; dd-MMM-yyyy; dd-MMM-yyyy; dd-MMM-yyyy"),
        BulkColumn.required(
            TERMS,
            "Rate %; minimum premium; commission %; TSI limit",
            "1.2000; 5000.00; 15; 5000000.00"),
        BulkColumn.required(
            BASIS, "How the premium is computed", "Comprehensive rate on the vehicle value"),
        BulkColumn.required(
                COVERAGES,
                "Coverage; included Y/N; optional Y/N; limit; deductible",
                "OD_THEFT; Y; N; -; 2000.00")
            .format("One coverage per line"),
        BulkColumn.required(
                INSURERS,
                "Insurer; Panel, Lead or Participant; share %; rate %; minimum premium",
                "INS-MGIC; Panel; -; 1.2000; -")
            .format("One insurer per line"),
        BulkColumn.required(
                INSURER_TERMS,
                "Insurer; coverage; limit; deductible; clauses separated by commas",
                "INS-MGIC; OD_THEFT; -; 2000.00; FLEET_REPAIR")
            .format("One term per line"),
        BulkColumn.required(SIGNOFF, "ManCom sign-off reference", "MC-2026-017"));
  }

  @Override
  protected Set<String> multiline() {
    return Set.of(COVERAGES, INSURERS, INSURER_TERMS);
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(RISK) + "|" + signoff(row);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    Optional<String> line =
        db.text("select line_code from cat_product where code = ? and packaged", row.text(RISK));
    if (line.isEmpty()) {
      errors.add(error(RISK, row.text(RISK) + " is not a packaged product"));
      return errors;
    }
    List<String> name = UploadCells.parts(row.text(NAME), THREE_PARTS);
    if (name.get(1) != null && !SCOPES.contains(name.get(1).toUpperCase(Locale.ROOT))) {
      errors.add(error(NAME, "the scope is Generic or Client-specific"));
    }
    List<String> dates = UploadCells.parts(row.text(DATES), FOUR_PARTS);
    for (int i = 0; i < FOUR_PARTS; i++) {
      if ((i < FOURTH || dates.get(i) != null) && UploadCells.date(dates.get(i)) == null) {
        errors.add(
            error(
                DATES,
                "give the effective from, package start and package end dates as dd-MMM-yyyy"));
        break;
      }
    }
    List<String> terms = UploadCells.parts(row.text(TERMS), FOUR_PARTS);
    if (UploadCells.number(terms.get(1)) == null || UploadCells.number(terms.get(2)) == null) {
      errors.add(error(TERMS, "give the minimum premium and the commission %"));
    }
    validateCoverages(row, line.get(), errors);
    validateInsurers(row, context, errors);
    validateTerms(row, line.get(), errors);
    Optional<String> open =
        db.text(
            "select coalesce(mancom_signoff_ref, '') from cat_product_version where product_code = ?"
                + " and status in ('DRAFT', 'FOR_VALIDATION') and coalesce(mancom_signoff_ref, '') <> ?",
            row.text(RISK),
            signoff(row));
    open.ifPresent(o -> errors.add(error(RISK, "another version of the package is being set up")));
    return errors;
  }

  private void validateCoverages(BulkRow row, String line, List<String> errors) {
    for (String entry : entries(row.text(COVERAGES))) {
      List<String> p = UploadCells.parts(entry, FIVE_PARTS);
      if (p.get(0) == null || !coverage(line, p.get(0))) {
        errors.add(error(COVERAGES, p.get(0) + " is not a coverage of line " + line));
      } else if (UploadCells.yes(p.get(1)).isEmpty() || UploadCells.yes(p.get(2)).isEmpty()) {
        errors.add(error(COVERAGES, p.get(0) + ": give included and optional as Y or N"));
      }
    }
  }

  private void validateInsurers(BulkRow row, BulkContext context, List<String> errors) {
    BigDecimal shares = BigDecimal.ZERO;
    boolean coInsured = false;
    for (String entry : entries(row.text(INSURERS))) {
      List<String> p = UploadCells.parts(entry, FIVE_PARTS);
      if (!db.exists(
          "select 1 from cat_insurer where company_id = ? and party_code = ?",
          context.companyId(),
          p.get(0))) {
        errors.add(error(INSURERS, p.get(0) + " is not an insurer"));
      }
      PackageInsurerRole role = role(p.get(1));
      if (role == null) {
        errors.add(error(INSURERS, p.get(0) + ": the role is Panel, Lead or Participant"));
      } else if (role != PackageInsurerRole.PANEL) {
        coInsured = true;
        BigDecimal share = UploadCells.number(p.get(2));
        shares = shares.add(share == null ? BigDecimal.ZERO : share);
      }
    }
    if (coInsured && shares.compareTo(HUNDRED) != 0) {
      errors.add(
          error(
              INSURERS,
              "the shares of the lead and the participants total " + shares + ", not 100"));
    }
  }

  private void validateTerms(BulkRow row, String line, List<String> errors) {
    List<String> insurers =
        entries(row.text(INSURERS)).stream().map(e -> UploadCells.parts(e, 1).get(0)).toList();
    for (String entry : entries(row.text(INSURER_TERMS))) {
      List<String> p = UploadCells.parts(entry, FIVE_PARTS);
      if (!insurers.contains(p.get(0))) {
        errors.add(
            error(INSURER_TERMS, p.get(0) + " has terms but is not an insurer of the package"));
      }
      if (p.get(1) == null || !coverage(line, p.get(1))) {
        errors.add(error(INSURER_TERMS, p.get(1) + " is not a coverage of line " + line));
      }
      for (String clause : clauses(p.get(FIFTH))) {
        if (!db.exists("select 1 from cat_clause where code = ?", clause)) {
          errors.add(error(INSURER_TERMS, clause + " is not a clause of the clause library"));
        }
      }
    }
  }

  private boolean coverage(String line, String code) {
    return db.exists("select 1 from cat_coverage where line_code = ? and code = ?", line, code);
  }

  private static List<String> entries(String cell) {
    if (cell == null) {
      return List.of();
    }
    return Arrays.stream(cell.split(ENTRY_SEPARATOR))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  private static List<String> clauses(String cell) {
    if (cell == null) {
      return List.of();
    }
    return Arrays.stream(cell.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  private static PackageInsurerRole role(String text) {
    if (text == null) {
      return null;
    }
    try {
      return PackageInsurerRole.valueOf(text.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static String signoff(BulkRow row) {
    String ref = UploadCells.parts(row.text(SIGNOFF), 1).get(0);
    return ref == null ? "" : ref;
  }

  private Optional<Map<String, Object>> version(BulkRow row) {
    return db
        .rows(
            "select version_no, status from cat_product_version where product_code = ?"
                + " and mancom_signoff_ref = ? order by version_no desc",
            row.text(RISK),
            signoff(row))
        .stream()
        .findFirst();
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return version(row).isPresent() ? UploadSupport.UPDATE : UploadSupport.ADD;
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    Optional<Map<String, Object>> existing = version(row);
    if (existing.isPresent() && !"DRAFT".equals(existing.get().get("status"))) {
      return row.text(RISK) + " v" + existing.get().get("version_no") + " unchanged";
    }
    PackageSpec spec = spec(row, context);
    if (existing.isPresent()) {
      int versionNo = ((Number) existing.get().get("version_no")).intValue();
      packages.updateDraftVersion(row.text(RISK), versionNo, spec);
      return row.text(RISK) + " v" + versionNo;
    }
    return row.text(RISK) + " v" + packages.createDraftVersion(spec).versionNo();
  }

  private static PackageSpec spec(BulkRow row, BulkContext context) {
    List<String> name = UploadCells.parts(row.text(NAME), THREE_PARTS);
    List<String> d = UploadCells.parts(row.text(DATES), FOUR_PARTS);
    List<String> t = UploadCells.parts(row.text(TERMS), FOUR_PARTS);
    List<PackageSpec.Coverage> coverages = coverages(row);
    List<PackageSpec.Insurer> insurers = insurers(row);
    List<PackageSpec.InsurerTerm> terms = insurerTerms(row);
    String summary =
        "Package "
            + (name.get(0) == null ? "" : name.get(0))
            + (name.get(1) == null ? "" : ", " + name.get(1))
            + (name.get(2) == null ? "" : " for client " + name.get(2))
            + " (upload "
            + context.jobNo()
            + ")";
    return new PackageSpec(
        context.companyId(),
        row.text(RISK),
        null,
        null,
        new PackageSpec.RateScheme(
            UploadCells.number(t.get(0)),
            UploadCells.number(t.get(1)),
            UploadCells.number(t.get(2)),
            UploadCells.number(t.get(FOURTH)),
            row.text(BASIS)),
        new PackageSpec.PackageDates(
            UploadCells.date(d.get(0)),
            UploadCells.date(d.get(1)),
            UploadCells.date(d.get(2)),
            UploadCells.date(d.get(FOURTH))),
        coverages,
        insurers,
        terms,
        new PackageSpec.Origin(
            null,
            signoff(row),
            summary.length() > MAX_SUMMARY ? summary.substring(0, MAX_SUMMARY) : summary));
  }

  private static List<PackageSpec.Coverage> coverages(BulkRow row) {
    List<PackageSpec.Coverage> coverages = new ArrayList<>();
    int order = 0;
    for (String entry : entries(row.text(COVERAGES))) {
      List<String> p = UploadCells.parts(entry, FIVE_PARTS);
      order += 10;
      coverages.add(
          new PackageSpec.Coverage(
              p.get(0),
              UploadCells.yes(p.get(1)).orElse(Boolean.FALSE),
              UploadCells.yes(p.get(2)).orElse(Boolean.FALSE),
              UploadCells.number(p.get(FOURTH)),
              null,
              deductible(p.get(FIFTH)),
              order));
    }
    return coverages;
  }

  private static List<PackageSpec.Insurer> insurers(BulkRow row) {
    List<PackageSpec.Insurer> insurers = new ArrayList<>();
    for (String entry : entries(row.text(INSURERS))) {
      List<String> p = UploadCells.parts(entry, FIVE_PARTS);
      insurers.add(
          new PackageSpec.Insurer(
              p.get(0),
              role(p.get(1)),
              UploadCells.number(p.get(2)),
              UploadCells.number(p.get(FOURTH)),
              UploadCells.number(p.get(FIFTH)),
              null));
    }
    return insurers;
  }

  private static List<PackageSpec.InsurerTerm> insurerTerms(BulkRow row) {
    List<PackageSpec.InsurerTerm> terms = new ArrayList<>();
    for (String entry : entries(row.text(INSURER_TERMS))) {
      List<String> p = UploadCells.parts(entry, FIVE_PARTS);
      terms.add(
          new PackageSpec.InsurerTerm(
              p.get(0),
              p.get(1),
              true,
              UploadCells.number(p.get(2)),
              null,
              deductible(p.get(FOURTH)),
              clauses(p.get(FIFTH)),
              null));
    }
    return terms;
  }

  /** A deductible: an amount, a percentage ("10%") or a wording. */
  private static PackageSpec.Deductible deductible(String text) {
    if (text == null) {
      return null;
    }
    if (text.endsWith("%") && UploadCells.number(text.substring(0, text.length() - 1)) != null) {
      return new PackageSpec.Deductible(
          null, UploadCells.number(text.substring(0, text.length() - 1)), null);
    }
    BigDecimal amount = UploadCells.number(text);
    return amount != null
        ? new PackageSpec.Deductible(amount, null, null)
        : new PackageSpec.Deductible(null, null, text);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    List<Map<String, String>> rows = new ArrayList<>();
    for (Map<String, Object> v :
        db.rows(
            "select distinct on (v.product_code) v.id, v.product_code, p.name, v.effective_from,"
                + " v.package_start_date, v.package_end_date, v.anniversary_date, v.default_rate,"
                + " v.minimum_premium, v.default_commission_rate, v.max_sum_insured, v.rating_basis_note,"
                + " v.mancom_signoff_ref from cat_product_version v join cat_product p on p.code = v.product_code"
                + " where p.packaged and v.status in ('RELEASED', 'DRAFT', 'FOR_VALIDATION')"
                + " and v.mancom_signoff_ref is not null"
                + " order by v.product_code, v.version_no desc")) {
      Long id = ((Number) v.get("id")).longValue();
      rows.add(
          exportRow(
              RISK, v.get("product_code"),
              NAME, v.get("name"),
              DATES,
                  UploadCells.join(
                      v.get("effective_from"),
                      v.get("package_start_date"),
                      v.get("package_end_date"),
                      v.get("anniversary_date")),
              TERMS,
                  UploadCells.join(
                      v.get("default_rate"),
                      v.get("minimum_premium"),
                      v.get("default_commission_rate"),
                      v.get("max_sum_insured")),
              BASIS, v.get("rating_basis_note"),
              COVERAGES, coverageLines(id),
              INSURERS, insurerLines(id),
              INSURER_TERMS, termLines(id),
              SIGNOFF, v.get("mancom_signoff_ref")));
    }
    return rows;
  }

  private static final String DEDUCTIBLE =
      "coalesce(cast(deductible_amount as varchar), deductible_percent || '%', deductible_text)";

  private String coverageLines(Long id) {
    return lines(
        db.rows(
            "select coverage_code, included, optional, limit_amount, "
                + DEDUCTIBLE
                + " as deductible from cat_package_coverage where version_id = ?"
                + " order by sort_order, coverage_code",
            id),
        "coverage_code",
        "included",
        "optional",
        "limit_amount",
        "deductible");
  }

  private String insurerLines(Long id) {
    return lines(
        db.rows(
            "select insurer_code, initcap(role) as role, share_percent, rate, minimum_premium"
                + " from cat_package_insurer where version_id = ? order by insurer_code",
            id),
        "insurer_code",
        "role",
        "share_percent",
        "rate",
        "minimum_premium");
  }

  private String termLines(Long id) {
    return lines(
        db.rows(
            "select insurer_code, coverage_code, limit_amount, "
                + DEDUCTIBLE
                + " as deductible, replace(clause_codes, ' ', '') as clauses"
                + " from cat_package_insurer_term where version_id = ? order by insurer_code, coverage_code",
            id),
        "insurer_code",
        "coverage_code",
        "limit_amount",
        "deductible",
        "clauses");
  }

  private static String lines(List<Map<String, Object>> rows, String... columns) {
    List<String> lines = new ArrayList<>();
    for (Map<String, Object> r : rows) {
      lines.add(UploadCells.join(Arrays.stream(columns).map(r::get).toArray()));
    }
    return lines.isEmpty() ? null : String.join("\n", lines);
  }
}
