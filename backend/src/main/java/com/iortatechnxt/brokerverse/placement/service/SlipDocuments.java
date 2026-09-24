package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.RiskItem;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.placement.service.InsurerDirectory.PlacementAddress;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Renders placement documents in the BDOI layout (BRNB.069/072): the placement slip as PDF (letter
 * with the merged template and the risks) and as XLSX (one row per account for the insurer's
 * systems), and the hold cover request as PDF.
 */
@Component
public class SlipDocuments {

  private static final String DEFAULT_COMPANY = "BDO Insurance and Reinsurance Brokers, Inc.";
  private static final List<String> SIGNATURES = List.of("Prepared by", "Approved by");
  private static final List<String> RISK_HEADERS =
      List.of("ARN", "Insured", "Product", "Period", "Sum insured", "Gross premium");
  private static final List<Integer> AMOUNT_COLUMNS = List.of(4, 5);
  private static final List<String> SHEET_HEADERS =
      List.of(
          "Slip No.",
          "ARN",
          "Client Code",
          "Insured",
          "Product",
          "Line",
          "Cover Type",
          "Period From",
          "Period To",
          "Sum Insured",
          "Net Premium",
          "Gross Premium",
          "Mortgagee Bank",
          "Risk Items");

  private final DocumentComposer composer;
  private final CompanyRepository companies;

  /**
   * Creates the renderer.
   *
   * @param composer document composer
   * @param companies companies (letterhead)
   */
  public SlipDocuments(DocumentComposer composer, CompanyRepository companies) {
    this.composer = composer;
    this.companies = companies;
  }

  /**
   * The placement slip as PDF.
   *
   * @param header slip number, insurer and template text
   * @param accounts accounts on the slip
   * @return PDF bytes
   */
  public byte[] slipPdf(SlipHeader header, List<Account> accounts) {
    BigDecimal total =
        accounts.stream()
            .map(a -> Objects.requireNonNullElse(a.getPremium().grossPremium(), BigDecimal.ZERO))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    List<Field> facts =
        List.of(
            new Field("Slip", header.slipNo()),
            new Field(
                "Insurer", header.address().insurerName() + " (" + header.insurerCode() + ")"),
            new Field("Branch", header.address().branchName()),
            new Field("Accounts", String.valueOf(accounts.size())),
            new Field("Total gross premium", total.toPlainString()));
    List<List<String>> risks = accounts.stream().map(SlipDocuments::riskRow).toList();
    return composer.pdf(
        new DocumentSpec(
            companyName(header.companyId()),
            header.text().title(),
            header.slipNo(),
            List.of(
                new Fields("Placement", facts),
                new Text(null, header.text().text()),
                new Table("Risks", RISK_HEADERS, risks, AMOUNT_COLUMNS)),
            SIGNATURES,
            header.text().versionTag()));
  }

  /**
   * The placement slip as XLSX.
   *
   * @param slipNo slip number
   * @param accounts accounts on the slip
   * @return XLSX bytes
   */
  public byte[] slipXlsx(String slipNo, List<Account> accounts) {
    List<List<Object>> rows =
        accounts.stream()
            .map(
                a ->
                    Arrays.<Object>asList(
                        slipNo,
                        a.getArn(),
                        a.getClientCode(),
                        a.getClientName(),
                        a.getProductCode(),
                        a.getLineCode(),
                        a.getCoverTypeCode(),
                        a.getPeriodFrom(),
                        a.getPeriodTo(),
                        a.getTotalSumInsured(),
                        a.getPremium().netPremium(),
                        a.getPremium().grossPremium(),
                        a.getMortgageeBank(),
                        items(a)))
            .toList();
    return composer.xlsx(new SheetSpec("Placement slip", SHEET_HEADERS, rows));
  }

  /**
   * The hold cover request as PDF (BRNB.072).
   *
   * @param header request reference, insurer and template text
   * @param account account
   * @param start first day of the hold cover
   * @param expiry last day of the hold cover
   * @return PDF bytes
   */
  public byte[] holdCoverPdf(
      SlipHeader header, Account account, LocalDate start, LocalDate expiry) {
    List<Field> facts =
        List.of(
            new Field("ARN", account.getArn()),
            new Field("Insured", account.getClientName()),
            new Field("Product", account.getProductCode()),
            new Field("Insurer", header.address().insurerName()),
            new Field("Hold cover", start + " to " + expiry),
            new Field("Sum insured", account.getTotalSumInsured().toPlainString()));
    return composer.pdf(
        new DocumentSpec(
            companyName(header.companyId()),
            header.text().title(),
            header.slipNo(),
            List.of(new Text(null, header.text().text()), new Fields("Risk", facts)),
            SIGNATURES,
            header.text().versionTag()));
  }

  private static List<String> riskRow(Account a) {
    return List.of(
        a.getArn(),
        a.getClientName(),
        a.getProductCode(),
        a.getPeriodFrom() + " to " + a.getPeriodTo(),
        a.getTotalSumInsured().toPlainString(),
        a.getPremium().grossPremium() == null ? "" : a.getPremium().grossPremium().toPlainString());
  }

  private static String items(Account a) {
    return a.getItems().stream().map(RiskItem::label).collect(Collectors.joining("; "));
  }

  private String companyName(Long companyId) {
    return companies.findById(companyId).map(Company::getName).orElse(DEFAULT_COMPANY);
  }

  /**
   * Header of a placement document.
   *
   * @param companyId company (letterhead)
   * @param slipNo document reference
   * @param insurerCode insurer party code
   * @param address insurer and branch names
   * @param text merged template
   */
  public record SlipHeader(
      Long companyId,
      String slipNo,
      String insurerCode,
      PlacementAddress address,
      MergedText text) {}
}
