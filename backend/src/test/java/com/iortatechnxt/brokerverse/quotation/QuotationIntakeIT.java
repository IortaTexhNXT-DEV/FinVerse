package com.iortatechnxt.brokerverse.quotation;

import static com.iortatechnxt.brokerverse.quotation.QuotationFixtures.motor;
import static com.iortatechnxt.brokerverse.quotation.QuotationFixtures.token;
import static com.iortatechnxt.brokerverse.quotation.QuotationFixtures.vehicle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.bulk.service.BulkUpload;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationRequest;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationStatus;
import com.iortatechnxt.brokerverse.quotation.domain.RequestStatus;
import com.iortatechnxt.brokerverse.quotation.service.IncomingQuotationRequest;
import com.iortatechnxt.brokerverse.quotation.service.QuotationAcceptanceBulkHandler;
import com.iortatechnxt.brokerverse.quotation.service.QuotationCreateBulkHandler;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDispatchService;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDispatchService.EmailRequest;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft.DraftItem;
import com.iortatechnxt.brokerverse.quotation.service.QuotationQueryService;
import com.iortatechnxt.brokerverse.quotation.service.QuotationRequestBulkHandler;
import com.iortatechnxt.brokerverse.quotation.service.QuotationRequestIntakeJob;
import com.iortatechnxt.brokerverse.quotation.service.QuotationRequestService;
import com.iortatechnxt.brokerverse.quotation.service.QuotationService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@IntegrationTest
class QuotationIntakeIT {

  @Autowired private QuotationRequestService requests;
  @Autowired private QuotationRequestIntakeJob intakeJob;
  @Autowired private QuotationService quotations;
  @Autowired private QuotationQueryService queries;
  @Autowired private QuotationDispatchService dispatch;
  @Autowired private QuotationCreateBulkHandler createHandler;
  @Autowired private QuotationAcceptanceBulkHandler acceptanceHandler;
  @Autowired private QuotationRequestBulkHandler requestHandler;
  @Autowired private BulkService bulk;
  @Autowired private QuotationFixtures fx;
  @Autowired private AsUser as;

  private static byte[] csv(BulkImportHandler handler, List<Map<String, String>> rows) {
    List<String> headers = handler.columns().stream().map(BulkColumn::header).toList();
    StringBuilder sb = new StringBuilder(String.join(",", headers)).append('\n');
    for (Map<String, String> row : rows) {
      sb.append(
              headers.stream()
                  .map(h -> "\"" + row.getOrDefault(h, "") + "\"")
                  .collect(Collectors.joining(",")))
          .append('\n');
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static Map<String, String> row(String... keyValues) {
    Map<String, String> row = new LinkedHashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      row.put(keyValues[i], keyValues[i + 1]);
    }
    return row;
  }

  private List<BulkRowRecord> commit(
      BulkImportHandler handler, Map<String, String> params, List<Map<String, String>> rows) {
    BulkJob job =
        as.run(
            "ao",
            () ->
                bulk.upload(
                    new BulkUpload(
                        fx.company(), handler.code(), "rows.csv", csv(handler, rows), params)));
    as.run("ao", () -> bulk.commit(job.getId()));
    return bulk.rows(job.getId(), null, Pageable.ofSize(50)).getContent();
  }

  @Test
  void requestsAreCapturedGivenAProspectQuotedOrClosed() {
    String ref = "MAIL-" + token();
    QuotationRequest request =
        as.run(
            "ao",
            () ->
                requests.receive(
                    fx.company(),
                    new IncomingQuotationRequest(
                        "EMAIL",
                        ref,
                        null,
                        null,
                        "Requester" + token() + ", Ana",
                        "ana@example.ph",
                        "09171234567",
                        "MTR10",
                        "CBG",
                        "Comprehensive cover for a new sedan")));
    assertThat(request.getRequestNo()).matches("REQ-\\d{4}-\\d{6}");
    assertThat(request.getStatus()).isEqualTo(RequestStatus.NEW);
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        requests.receive(
                            fx.company(),
                            new IncomingQuotationRequest(
                                "EMAIL", ref, null, null, "X", null, null, null, null, "x"))))
        .extracting("code")
        .isEqualTo("QUOTATION_REQUEST_DUPLICATE");

    QuotationRequest withProspect = as.run("ao", () -> requests.createProspect(request.getId()));
    assertThat(withProspect.getClientId()).isNotNull();
    Quotation q =
        as.run(
            "ao",
            () ->
                quotations.create(
                    fx.company(),
                    new QuotationDraft(
                        withProspect.getClientId(),
                        "MTR10",
                        "CBG",
                        "EMAIL",
                        request.getId(),
                        null,
                        motor(null, false).terms(),
                        List.of(new DraftItem(1, vehicle("950000"))))));
    QuotationRequest quoted = requests.get(request.getId());
    assertThat(quoted.getStatus()).isEqualTo(RequestStatus.QUOTED);
    assertThat(quoted.getQuotationId()).isEqualTo(q.getId());
    assertThat(queries.get(q.getId()).getRequestId()).isEqualTo(request.getId());
    assertThatThrownBy(() -> as.run("ao", () -> requests.close(request.getId(), "dup")))
        .extracting("code")
        .isEqualTo("QUOTATION_REQUEST_CLOSED");

    QuotationRequest other =
        as.run(
            "ao",
            () ->
                requests.receive(
                    fx.company(),
                    new IncomingQuotationRequest(
                        "EMAIL",
                        null,
                        null,
                        "CL-2026-900001",
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Fire cover")));
    assertThat(as.run("ao", () -> requests.close(other.getId(), "Client withdrew")).getStatus())
        .isEqualTo(RequestStatus.CLOSED);
    assertThat(
            requests
                .search(
                    fx.company(), RequestStatus.CLOSED, other.getRequestNo(), PageRequest.of(0, 5))
                .getContent())
        .hasSize(1);
    assertThat(intakeJob.execute(LocalDate.now()).itemsProcessed()).isZero();
    assertThat(intakeJob.cron()).isEqualTo("-");
  }

  @Test
  void bulkQuotationsRequestsAndAcceptancesAreProcessedRowByRow() {
    String a = token();
    String prospect = "Bulkquote" + token() + ", Rina";
    List<BulkRowRecord> created =
        commit(
            createHandler,
            Map.of("product", "MTR10", "segment", "CBG"),
            List.of(
                row(
                    "Client Code",
                    "CL-2026-900001",
                    "Period From",
                    "2026-11-01",
                    "Period To",
                    "2027-11-01",
                    "Sum Insured",
                    "800000",
                    "Plate No",
                    "bq-" + a,
                    "Engine No",
                    "be" + a,
                    "Chassis No",
                    "bc" + a,
                    "Make",
                    "Toyota",
                    "Model",
                    "Vios",
                    "Year Model",
                    "2025",
                    "Direct Payment",
                    "Y"),
                row(
                    "Client Name",
                    prospect,
                    "Birth Date",
                    "1990-01-01",
                    "Period From",
                    "2026-11-01",
                    "Period To",
                    "2027-11-01",
                    "Sum Insured",
                    "600000",
                    "Plate No",
                    "bp" + a),
                row(
                    "Client Code",
                    "CL-NOPE-" + a,
                    "Period From",
                    "2026-11-01",
                    "Period To",
                    "2027-11-01",
                    "Sum Insured",
                    "600000")));
    assertThat(created)
        .extracting(BulkRowRecord::getStatus)
        .containsExactly(BulkRowStatus.COMMITTED, BulkRowStatus.COMMITTED, BulkRowStatus.INVALID);
    String arn = created.get(0).getResultRef().split(" / ")[1];
    Quotation q = queries.get(queries.getByArn(arn).id());
    assertThat(q.isDirectPayment()).isTrue();
    assertThat(q.getSourceChannel()).isEqualTo("UPLOAD");

    as.run("ao", () -> quotations.submit(q.getId(), null));
    as.run("mkttl", () -> quotations.approve(q.getId(), null));
    as.run(
        "ao",
        () ->
            dispatch.send(
                q.getId(), new EmailRequest(List.of("c@example.ph"), null, "Q", "Body", null)));
    List<BulkRowRecord> accepted =
        commit(
            acceptanceHandler,
            Map.of("createAccounts", "Y"),
            List.of(
                row("ARN", arn.toLowerCase(), "Remarks", "accepted by phone"),
                row("ARN", "ARN-1999-000001")));
    assertThat(accepted)
        .extracting(BulkRowRecord::getStatus)
        .containsExactly(BulkRowStatus.COMMITTED, BulkRowStatus.INVALID);
    Quotation converted = queries.get(q.getId());
    assertThat(converted.getStatus()).isEqualTo(QuotationStatus.CONVERTED);
    assertThat(converted.getAccountArns()).containsExactly(arn);

    String hls = "HLS-" + token();
    List<BulkRowRecord> received =
        commit(
            requestHandler,
            Map.of(),
            List.of(
                row(
                    "Channel",
                    "HLS",
                    "Source Reference",
                    hls,
                    "Prospect Name",
                    prospect,
                    "Product Code",
                    "PAR01",
                    "Requested Cover",
                    "House and lot, fire"),
                row("Channel", "HLS", "Requested Cover", "No client given")));
    assertThat(received)
        .extracting(BulkRowRecord::getStatus)
        .containsExactly(BulkRowStatus.COMMITTED, BulkRowStatus.INVALID);
    assertThat(requests.known("HLS", hls)).isTrue();
  }
}
