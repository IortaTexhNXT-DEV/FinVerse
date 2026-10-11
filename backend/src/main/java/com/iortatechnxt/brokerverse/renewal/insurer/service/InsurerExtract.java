package com.iortatechnxt.brokerverse.renewal.insurer.service;

import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoiceRepository;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfileRepository;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.renewal.check.service.OutstandingPremiumCheck;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotPremium;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotProduct;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotSales;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResult;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResultRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The 28 columns of the insurer extract (FR-RN-070; BRD 3.009.1.4.1-28): one row per renewal,
 * frozen in the batch line when the batch is built so the file sent is the file stored.
 */
@Component
public class InsurerExtract {

  /** Column headers in order. */
  public static final List<String> HEADERS =
      List.of(
          "Insurance Company",
          "Invoice No",
          "Assured Name",
          "Risk Code",
          "Inception Date",
          "Expiry Date",
          "Sum Insured",
          "Premium Amount",
          "Commission Rate",
          "AR Client (Unpaid Balance)",
          "Is Mortgaged",
          "Premium Rate (per cover)",
          "Policy No",
          "Risk Description",
          "Encoder Name",
          "Unit Head",
          "Business Origin",
          "Account Type",
          "Region",
          "Area",
          "Branch",
          "Account Officer Name",
          "Remarks",
          "BIPD for Motor",
          "Seating Capacity for Motor",
          "Auto Personal Accident for Motor",
          "Sum Insured (per cover)",
          "Packaged / Non-Packaged");

  private static final SnapshotPremium NO_PREMIUM =
      new SnapshotPremium(null, null, null, null, null, null);
  private static final SnapshotProduct NO_PRODUCT =
      new SnapshotProduct(null, null, null, null, null, null);
  private static final SnapshotSales NO_SALES =
      new SnapshotSales(null, null, null, null, null, null);
  private static final String YES = "Yes";
  private static final String NO = "No";
  private static final String SEP = "; ";

  private final InsurerProfileRepository insurers;
  private final AccountQueryService accounts;
  private final BookedInvoiceRepository bookings;
  private final CheckResultRepository results;
  private final AppUserRepository users;

  /**
   * Creates the extract.
   *
   * @param insurers insurers
   * @param accounts expiring accounts
   * @param bookings booked invoices (encoder)
   * @param results check results (unpaid balance)
   * @param users user names
   */
  public InsurerExtract(
      InsurerProfileRepository insurers,
      AccountQueryService accounts,
      BookedInvoiceRepository bookings,
      CheckResultRepository results,
      AppUserRepository users) {
    this.insurers = insurers;
    this.accounts = accounts;
    this.bookings = bookings;
    this.results = results;
    this.users = users;
  }

  /**
   * The row of a renewal.
   *
   * @param c renewal
   * @return 28 values, as text
   */
  public List<String> row(RenewalCandidate c) {
    CandidateSnapshot s = c.getSnapshot();
    SnapshotPremium premium = s.premium() == null ? NO_PREMIUM : s.premium();
    SnapshotProduct product = s.product() == null ? NO_PRODUCT : s.product();
    SnapshotSales sales = s.sales() == null ? NO_SALES : s.sales();
    List<RiskItemData> items = items(c);
    return Arrays.asList(
        insurerName(c.getCompanyId(), s.insurerCode()),
        c.getExpiringInvoiceNo(),
        assured(s),
        product.productCode(),
        text(s.inceptionDate()),
        text(s.expiryDate()),
        text(premium.totalSumInsured()),
        text(premium.grossPremium()),
        text(premium.commissionRate()),
        unpaid(c),
        mortgaged(s),
        join(items.stream().map(RiskItemData::rate).toList()),
        s.policyNo(),
        product.productName(),
        encoder(c),
        name(sales.unitHead()),
        product.businessOrigin(),
        product.accountType(),
        sales.regionCode(),
        sales.departmentCode(),
        sales.branchCode(),
        name(sales.accountOfficer()),
        c.getDisposition().remarks(),
        join(items.stream().map(RiskItemData::biLimit).toList()),
        seats(items),
        null,
        join(items.stream().map(RiskItemData::sumInsured).toList()),
        s.packaged() ? "Packaged" : "Non-Packaged");
  }

  private static String seats(List<RiskItemData> items) {
    return items.stream()
        .map(RiskItemData::vehicle)
        .filter(v -> v != null && v.seatingCapacity() != null)
        .map(v -> v.seatingCapacity().toString())
        .collect(Collectors.joining(SEP));
  }

  private static String mortgaged(CandidateSnapshot s) {
    return s.mortgage() != null && s.mortgage().mortgaged() ? YES : NO;
  }

  private static String assured(CandidateSnapshot s) {
    return s.client() == null ? s.clientName() : first(s.client().assuredName(), s.clientName());
  }

  private List<RiskItemData> items(RenewalCandidate c) {
    if (c.getExpiringArn() == null) {
      return List.of();
    }
    AccountDraft d = accounts.draftOf(c.getExpiringArn());
    return d.items();
  }

  private String insurerName(Long companyId, String code) {
    return code == null
        ? null
        : insurers
            .findByCompanyIdAndPartyCode(companyId, code)
            .map(InsurerProfile::getName)
            .orElse(code);
  }

  private String unpaid(RenewalCandidate c) {
    if (c.getLastCheckRunId() == null) {
      return null;
    }
    return results.findByRunIdOrderByIdAsc(c.getLastCheckRunId()).stream()
        .filter(r -> OutstandingPremiumCheck.CODE.equals(r.getCheckCode()))
        .map(CheckResult::getDetail)
        .filter(java.util.Objects::nonNull)
        .findFirst()
        .orElse("0.00");
  }

  private String encoder(RenewalCandidate c) {
    return Optional.ofNullable(c.getExpiringInvoiceNo())
        .flatMap(bookings::findByInvoiceNo)
        .map(BookedInvoice::getBookedBy)
        .map(this::name)
        .orElse(null);
  }

  private String name(String username) {
    return username == null
        ? null
        : users.findByUsernameIgnoreCase(username).map(AppUser::getFullName).orElse(username);
  }

  private static String join(List<BigDecimal> values) {
    return values.stream()
        .filter(v -> v != null)
        .map(BigDecimal::toPlainString)
        .collect(Collectors.joining(SEP));
  }

  private static String first(String a, String b) {
    return a != null ? a : b;
  }

  private static String text(Object value) {
    return value == null ? null : DisplayFormat.value(value);
  }
}
