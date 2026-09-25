package com.iortatechnxt.brokerverse.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Vehicle;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.catalog.domain.RateOverride;
import com.iortatechnxt.brokerverse.catalog.service.CatalogKind;
import com.iortatechnxt.brokerverse.catalog.service.CatalogRecords;
import com.iortatechnxt.brokerverse.catalog.service.RateSchemeExceptionService;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery.Purpose;
import com.iortatechnxt.brokerverse.catalog.service.RatingService;
import com.iortatechnxt.brokerverse.catalog.service.RatingService.Rating;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSetupService;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.service.QuotationAcceptanceService;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDispatchService;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDispatchService.EmailRequest;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft.DraftItem;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft.Terms;
import com.iortatechnxt.brokerverse.quotation.service.QuotationQueryService;
import com.iortatechnxt.brokerverse.quotation.service.QuotationService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The rate scheme that prices a transaction (BRPM.007): new business on the current released
 * version, renewals and endorsements on their version, a non-current version or rate only with an
 * approved exception, and the version stored on quotations and accounts.
 */
@IntegrationTest
class RatingSchemeIT {

  private static final String CLIENT = "CL-DEMO-A001";
  private static final BigDecimal NEW_RATE = new BigDecimal("1.75");

  @Autowired private PackageFixtures fx;
  @Autowired private PackageSetupService setup;
  @Autowired private RatingService rating;
  @Autowired private RateSchemeExceptionService exceptions;
  @Autowired private CatalogRecords records;
  @Autowired private QuotationService quotations;
  @Autowired private QuotationDispatchService dispatch;
  @Autowired private QuotationAcceptanceService acceptance;
  @Autowired private QuotationQueryService quotationQueries;
  @Autowired private AccountQueryService accounts;
  @Autowired private ClientService clients;
  @Autowired private DocumentService documents;
  @Autowired private AsUser as;

  /** A package with version 1 sold since 2020 and version 2 released today at 1.75 %. */
  private String twoVersions() {
    String code = fx.released();
    fx.backdate(code, 1);
    as.run("mbs", () -> setup.createDraftVersion(fx.spec(code, false, fx.today(), NEW_RATE)));
    fx.release(code, 2);
    return code;
  }

  private RateOverride approvedException(
      String code, Integer version, BigDecimal rate, String ref) {
    RateOverride requested =
        as.run(
            "ao",
            () ->
                exceptions.request(
                    new RateOverride.Request(
                        code, null, version, rate, ref, "Client keeps last year's terms", null)));
    as.run(
        "approver", () -> records.authorize(CatalogKind.RATE_SCHEME_EXCEPTION, requested.getId()));
    return requested;
  }

  @Test
  void newBusinessUsesTheCurrentVersionAndAnOldOneOnlyWithAnException() {
    String code = twoVersions();
    Rating current = rating.rate(fx.query(code, null, null));
    assertThat(current.schemeVersion()).isEqualTo(2);
    assertThat(current.schemeRate()).isEqualByComparingTo(NEW_RATE);
    assertThat(current.breakdown().items().get(0).ratePercent()).isEqualByComparingTo(NEW_RATE);

    RatingQuery old = fx.query(code, null, null).withScheme(Purpose.NEW_BUSINESS, 1, null);
    assertThatThrownBy(() -> rating.rate(old))
        .extracting("code")
        .isEqualTo("RATE_SCHEME_NOT_CURRENT");

    String ref = "QT-TEST-" + code;
    RateOverride pending =
        as.run(
            "ao",
            () ->
                exceptions.request(
                    new RateOverride.Request(
                        code, null, 1, null, ref, "Renewal-like terms", null)));
    assertThatThrownBy(
            () -> rating.rate(old.withScheme(Purpose.NEW_BUSINESS, 1, pending.getReferenceNo())))
        .extracting("code")
        .isEqualTo("RATE_SCHEME_NOT_CURRENT");
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () -> records.authorize(CatalogKind.RATE_SCHEME_EXCEPTION, pending.getId())))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    as.run("approver", () -> records.authorize(CatalogKind.RATE_SCHEME_EXCEPTION, pending.getId()));
    Rating onOld = rating.rate(old.withScheme(Purpose.NEW_BUSINESS, 1, pending.getReferenceNo()));
    assertThat(onOld.schemeVersion()).isEqualTo(1);
    assertThat(onOld.overrideRef()).isEqualTo(pending.getReferenceNo());
    assertThat(onOld.schemeDeviation()).isTrue();
    assertThat(exceptions.latestApproved(ref, code)).isEqualTo(pending.getReferenceNo());
  }

  @Test
  void anItemRateOtherThanTheSchemeNeedsAnExceptionAndThePanelIsEnforced() {
    String code = twoVersions();
    RatingQuery manual = fx.query(code, null, new BigDecimal("2.10"));
    assertThatThrownBy(() -> rating.rate(manual))
        .extracting("code")
        .isEqualTo("RATE_SCHEME_NOT_CURRENT");
    Rating draft = rating.rateDraft(manual);
    assertThat(draft.schemeDeviation()).isTrue();
    RateOverride approved =
        approvedException(code, null, new BigDecimal("2.10"), "ARN-TEST-" + code);
    Rating accepted =
        rating.rate(manual.withScheme(Purpose.NEW_BUSINESS, null, approved.getReferenceNo()));
    assertThat(accepted.overrideRef()).isEqualTo(approved.getReferenceNo());

    Rating lac = rating.rate(fx.query(code, "INS-LAC", null));
    assertThat(lac.schemeRate()).isEqualByComparingTo("1.50");
    assertThatThrownBy(() -> rating.rate(fx.query(code, "INS-VMI", null)))
        .extracting("code")
        .isEqualTo("INSURER_NOT_ON_PACKAGE");
  }

  @Test
  void renewalsAndEndorsementsUseTheirOwnVersion() {
    String code = twoVersions();
    Rating renewal = rating.rate(fx.query(code, null, null).withScheme(Purpose.RENEWAL, 1, null));
    assertThat(renewal.schemeVersion()).isEqualTo(1);
    assertThat(renewal.schemeRate()).isEqualByComparingTo(PackageFixtures.RATE);
    Rating endorsement =
        rating.rate(fx.query(code, null, null).withScheme(Purpose.ENDORSEMENT, 1, null));
    assertThat(endorsement.schemeVersion()).isEqualTo(1);
    RatingQuery legacy =
        new RatingQuery(
            fx.company(),
            code,
            null,
            null,
            List.of(new RatingQuery.Item("Endt", new BigDecimal("1000"), null, null, null)),
            false,
            null,
            LocalDate.of(2021, 3, 1),
            LocalDate.of(2022, 3, 1),
            null,
            true,
            LocalDate.of(2021, 3, 1));
    assertThat(rating.rate(legacy).schemeVersion()).isEqualTo(1);
    Rating plain = rating.rate(fx.query("MOP07", null, new BigDecimal("0.2")));
    assertThat(plain.schemeVersion()).isNull();
  }

  @Test
  void quotationAndAccountsCarryTheVersionAndSubmissionNeedsTheException() {
    String code = fx.released();
    Long clientId = clients.requireByCode(fx.company(), CLIENT).getId();
    Quotation q =
        as.run(
            "ao",
            () -> quotations.create(fx.company(), draft(clientId, code, new BigDecimal("2.00"))));
    assertThat(q.getProductVersionNo()).isEqualTo(1);
    assertThat(quotationQueries.content(quotationQueries.get(q.getId())).schemeDeviation())
        .isTrue();
    assertThatThrownBy(() -> as.run("ao", () -> quotations.submit(q.getId(), null)))
        .extracting("code")
        .isEqualTo("RATE_SCHEME_NOT_CURRENT");
    RateOverride approved =
        approvedException(code, null, new BigDecimal("2.00"), q.getQuotationNo());
    as.run("ao", () -> quotations.submit(q.getId(), null));
    assertThat(quotationQueries.get(q.getId()).getRateOverrideRef())
        .isEqualTo(approved.getReferenceNo());
    assertThat(quotationQueries.getByArn(q.getArn()).productVersionNo()).isEqualTo(1);

    as.run("mkttl", () -> quotations.approve(q.getId(), "ok"));
    as.run(
        "ao",
        () ->
            dispatch.send(
                q.getId(),
                new EmailRequest(
                    List.of("client@example.ph"),
                    List.of(),
                    "Your quotation",
                    "Dear client",
                    null)));
    as.run(
        "ao",
        () ->
            documents.upload(
                new AttachmentTarget("Quotation", String.valueOf(q.getId())),
                List.of(
                    new UploadedFile(
                        "acceptance.pdf", "%PDF-1.4 test".getBytes(StandardCharsets.US_ASCII))),
                new UploadOptions("CLIENT_ACCEPTANCE", false, null, null)));
    as.run("ao", () -> acceptance.accept(q.getId(), null, "accepted"));
    as.run("ao", () -> acceptance.createAccounts(q.getId(), null));
    Account account = accounts.requireByArn(q.getArn());
    assertThat(account.getProductVersionNo()).isEqualTo(1);
    assertThat(account.getRateOverrideRef()).isEqualTo(approved.getReferenceNo());
  }

  private static QuotationDraft draft(Long clientId, String code, BigDecimal rate) {
    String id = PackageFixtures.code();
    RiskItemData vehicle =
        new RiskItemData(
            null,
            new BigDecimal("900000"),
            rate,
            null,
            null,
            new Vehicle(
                "R" + id, null, "RE" + id, "RC" + id, "Toyota", "Vios", 2025, "SEDAN", null, 5),
            null,
            null);
    return new QuotationDraft(
        clientId,
        code,
        "CBG",
        "EMAIL",
        null,
        null,
        new Terms(
            "INS-MGIC",
            "MKT",
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2027, 11, 1),
            null,
            false,
            null,
            "Scheme test"),
        List.of(new DraftItem(1, vehicle)));
  }
}
