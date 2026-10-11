package com.iortatechnxt.brokerverse.renewal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.alert.service.AlertCheck.AlertSignal;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfileRepository;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProductRepository;
import com.iortatechnxt.brokerverse.catalog.domain.SalesUnit;
import com.iortatechnxt.brokerverse.catalog.domain.SalesUnitRepository;
import com.iortatechnxt.brokerverse.common.security.UserDisplayNames;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.renewal.alert.RenewalAlertCheck;
import com.iortatechnxt.brokerverse.renewal.candidate.api.CandidateRowMapper;
import com.iortatechnxt.brokerverse.renewal.candidate.api.dto.CandidateDtos.CandidateRow;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateDocuments;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateQueryService;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.BucketHistoryRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotClient;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotMortgage;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResultRepository;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatch;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatchRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.insurer.service.InsurerExtract;
import com.iortatechnxt.brokerverse.renewal.letter.service.LetterContent;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.renewal.service.RenewalScope.Scope;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The renewal documents, letters and alerts in words: dates as dd-MMM-yyyy, insurers and users by
 * name, the classification by its label (screen standards).
 */
class RenewalDocumentWordsTest {

  private static final LocalDate EXPIRY = LocalDate.of(2027, 11, 15);

  private final InsurerProfileRepository insurers = mock(InsurerProfileRepository.class);

  RenewalDocumentWordsTest() {
    InsurerProfile mgic = mock(InsurerProfile.class);
    when(mgic.getName()).thenReturn("Mabuhay General Insurance Corp.");
    when(insurers.findByCompanyIdAndPartyCode(anyLong(), eq("INS-MGIC")))
        .thenReturn(Optional.of(mgic));
  }

  private static RenewalCandidate candidate() {
    RenewalCandidate c =
        new RenewalCandidate(
            1L,
            "RNW-2027-000001",
            new RenewalCandidate.Origin(CandidateSource.BIBS_INVOICE, "BI-1", null, null, null),
            new CandidateSnapshot(
                "POL-1",
                null,
                null,
                null,
                new SnapshotClient(null, null, "Maria Clara Santos", "Maria Clara Santos", null),
                null,
                null,
                "INS-MGIC",
                new SnapshotMortgage(false, null),
                false,
                EXPIRY.minusYears(1),
                EXPIRY,
                null,
                null,
                null),
            null);
    ReflectionTestUtils.setField(c, "id", 9L);
    c.evaluated(Bucket.REVIEW, 1, null, Instant.now());
    c.assignAo("ao");
    return c;
  }

  @Test
  void theAccountDetailsNameTheInsurerTheOfficerAndTheClassification() {
    CandidateQueryService queries = mock(CandidateQueryService.class);
    DocumentComposer composer = mock(DocumentComposer.class);
    OrganizationService organization = mock(OrganizationService.class);
    UserDisplayNames users = mock(UserDisplayNames.class);
    Company company = mock(Company.class);
    when(company.getName()).thenReturn("BDO Insurance and Reinsurance Brokers, Inc.");
    when(organization.getCompany(1L)).thenReturn(company);
    when(users.displayName("ao")).thenReturn("Aileen Account Officer");
    RenewalCandidate c = candidate();
    when(queries.get(1L, "RNW-2027-000001")).thenReturn(c);
    when(queries.scope(1L)).thenReturn(new Scope(null, null, "ao", true));
    when(queries.latestResults(c)).thenReturn(List.of());
    when(queries.dispositions(c)).thenReturn(List.of());
    ArgumentCaptor<DocumentSpec> spec = ArgumentCaptor.forClass(DocumentSpec.class);
    when(composer.pdf(spec.capture())).thenReturn(new byte[0]);

    new CandidateDocuments(queries, composer, organization, insurers, users, mock(LovService.class))
        .details(1L, "RNW-2027-000001");

    Map<String, String> fields =
        ((Fields) spec.getValue().sections().get(0))
            .fields().stream()
                .collect(Collectors.toMap(Field::label, f -> f.value() == null ? "" : f.value()));
    assertThat(fields)
        .containsEntry("Classification", "Review")
        .containsEntry("Insurer", "Mabuhay General Insurance Corp.")
        .containsEntry("Expiry date", "15-Nov-2027")
        .containsEntry("Account Officer", "Aileen Account Officer")
        .doesNotContainKey("Bucket");
  }

  @Test
  void theAccountDetailsDownloadIsTheWorkbookRenewalAccountDetails() {
    CandidateQueryService queries = mock(CandidateQueryService.class);
    DocumentComposer composer = mock(DocumentComposer.class);
    UserDisplayNames users = mock(UserDisplayNames.class);
    when(users.displayName("ao")).thenReturn("Aileen Account Officer");
    RenewalCandidate c = candidate();
    when(queries.get(1L, "RNW-2027-000001")).thenReturn(c);
    when(queries.scope(1L)).thenReturn(new Scope(null, null, "ao", true));
    ArgumentCaptor<SheetSpec> sheet = ArgumentCaptor.forClass(SheetSpec.class);
    when(composer.xlsx(sheet.capture())).thenReturn(new byte[0]);

    var file =
        new CandidateDocuments(
                queries,
                composer,
                mock(OrganizationService.class),
                insurers,
                users,
                mock(LovService.class))
            .detailsSheet(1L, "RNW-2027-000001");

    assertThat(file.fileName()).isEqualTo("Renewal Account Details.xlsx");
    assertThat(sheet.getValue().rows())
        .contains(
            List.of("Account Officer", "Aileen Account Officer"),
            List.of("Effective expiry", "15-Nov-2027"));
  }

  @Test
  void theInsurerExtractShowsItsDatesAsTheScreensDo() {
    InsurerExtract extract =
        new InsurerExtract(
            insurers,
            null,
            mock(com.iortatechnxt.brokerverse.booking.domain.BookedInvoiceRepository.class),
            null,
            null);

    List<String> row = extract.row(candidate());

    assertThat(row.get(0)).isEqualTo("Mabuhay General Insurance Corp.");
    assertThat(row.get(4)).isEqualTo("15-Nov-2026");
    assertThat(row.get(5)).isEqualTo("15-Nov-2027");
  }

  @Test
  void theRenewalPeriodOfTheLetterIsADate() {
    OrganizationService organization = mock(OrganizationService.class);
    Company company = mock(Company.class);
    when(company.getBaseCurrency()).thenReturn("PHP");
    when(organization.getCompany(1L)).thenReturn(company);
    LetterContent content =
        new LetterContent(
            null,
            null,
            organization,
            null,
            insurers,
            mock(com.iortatechnxt.brokerverse.security.domain.AppUserRepository.class),
            mock(LovService.class),
            null,
            new com.iortatechnxt.brokerverse.renewal.letter.service.AnnexTemplates(
                mock(com.iortatechnxt.brokerverse.account.domain.AccountRepository.class)));

    Map<String, Object> values = content.values(candidate(), EXPIRY.minusMonths(3));

    assertThat(DocTemplateService.fill("{{renewalFrom}}", values)).isEqualTo("15-Nov-2027");
    assertThat(DocTemplateService.fill("{{expiryDate}}", values)).isEqualTo("15-Nov-2027");
    assertThat(LetterContent.fields(values))
        .filteredOn(f -> f.label().equals("Expiry date"))
        .singleElement()
        .extracting(f -> f.value())
        .isEqualTo("15-Nov-2027");
  }

  @Test
  void anOverdueInsurerBatchAlertNamesTheInsurerAndTheDueDate() {
    RenewalCandidateRepository candidates = mock(RenewalCandidateRepository.class);
    InsurerBatchRepository batches = mock(InsurerBatchRepository.class);
    InsurerBatch batch =
        new InsurerBatch(1L, "RIB-2027-000001", "INS-MGIC", EXPIRY, EXPIRY.plusDays(30));
    ReflectionTestUtils.setField(batch, "replyDue", LocalDate.of(2027, 8, 10));
    when(candidates.findByStageIn(anyCollection())).thenReturn(List.of());
    when(batches.findByStatusInAndReplyDueBefore(anyCollection(), any()))
        .thenReturn(List.of(batch));

    List<AlertSignal> signals =
        new RenewalAlertCheck(
                candidates,
                mock(BucketHistoryRepository.class),
                batches,
                mock(RenewalParameters.class),
                insurers,
                mock(OrganizationService.class))
            .evaluate(LocalDate.of(2027, 8, 20));

    assertThat(signals)
        .singleElement()
        .extracting(s -> s.facts().message())
        .isEqualTo(
            "Insurer Mabuhay General Insurance Corp. has not answered batch RIB-2027-000001"
                + " due 10-Aug-2027");
  }

  @Test
  void aRenewalRowNamesTheInsurerTheUnitAndTheProductForEveryUser() {
    SalesUnitRepository units = mock(SalesUnitRepository.class);
    SalesUnit unit = mock(SalesUnit.class);
    when(unit.getName()).thenReturn("Corporate Marketing Team 1");
    when(units.findByCompanyIdAndCode(1L, "T-CORP1")).thenReturn(Optional.of(unit));
    RiskProductRepository products = mock(RiskProductRepository.class);
    RiskProduct product = mock(RiskProduct.class);
    when(product.getName()).thenReturn("Comprehensive General Liability");
    when(products.findByCode("CGL01")).thenReturn(Optional.of(product));
    RenewalCandidate c = candidate();
    ReflectionTestUtils.setField(c, "ownerUnit", "T-CORP1");
    ReflectionTestUtils.setField(
        c,
        "snapshot",
        new CandidateSnapshot(
            "POL-1",
            null,
            null,
            null,
            new SnapshotClient(null, null, "Maria Clara Santos", "Maria Clara Santos", null),
            new CandidateSnapshot.SnapshotProduct("CGL01", null, "LIABILITY", null, null, null),
            null,
            "INS-MGIC",
            new SnapshotMortgage(false, null),
            false,
            EXPIRY.minusYears(1),
            EXPIRY,
            null,
            null,
            null));
    CandidateRowMapper mapper =
        new CandidateRowMapper(
            mock(CheckResultRepository.class),
            java.time.Clock.systemUTC(),
            insurers,
            units,
            products);

    CandidateRow row = mapper.rows(List.of(c), new Scope(null, null, "contactc", true)).get(0);

    assertThat(row.names().insurer()).isEqualTo("Mabuhay General Insurance Corp.");
    assertThat(row.names().ownerUnit()).isEqualTo("Corporate Marketing Team 1");
    assertThat(row.names().product()).isEqualTo("Comprehensive General Liability");
  }
}
