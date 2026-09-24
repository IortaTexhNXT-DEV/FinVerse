package com.iortatechnxt.brokerverse.account;

import static com.iortatechnxt.brokerverse.account.AccountFixtures.draft;
import static com.iortatechnxt.brokerverse.account.AccountFixtures.token;
import static com.iortatechnxt.brokerverse.account.AccountFixtures.vehicle;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountCreateBulkHandler;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.AccountUpdateBulkHandler;
import com.iortatechnxt.brokerverse.account.service.FfyTaggingBulkHandler;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.bulk.service.BulkUpload;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.support.AsUser;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

@com.iortatechnxt.brokerverse.support.IntegrationTest
class AccountBulkIT {

  @Autowired private BulkService bulk;
  @Autowired private AccountCreateBulkHandler createHandler;
  @Autowired private AccountUpdateBulkHandler updateHandler;
  @Autowired private FfyTaggingBulkHandler ffyHandler;
  @Autowired private AccountService accounts;
  @Autowired private AccountQueryService queries;
  @Autowired private AccountFixtures fx;
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
    Map<String, String> row = new java.util.LinkedHashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      row.put(keyValues[i], keyValues[i + 1]);
    }
    return row;
  }

  private BulkJob upload(BulkImportHandler handler, Map<String, String> params, byte[] content) {
    return as.run(
        "ao",
        () ->
            bulk.upload(
                new BulkUpload(fx.company(), handler.code(), "accounts.csv", content, params)));
  }

  private List<BulkRowRecord> rows(BulkJob job) {
    return bulk.rows(job.getId(), null, Pageable.ofSize(50)).getContent();
  }

  @Test
  void accountsAreCreatedForExistingClientsAndNewProspects() {
    Client client = fx.confirmed("CL-DEMO-A002");
    Account existing =
        as.run(
            "ao",
            () ->
                accounts.createDraft(
                    NewAccount.direct(
                        fx.company(),
                        draft(
                            client.getId(), "MTR10", "CBG", List.of(vehicle(token(), "500000"))))));
    String existingPlate = existing.getItems().get(0).getPlateNo();
    String a = token();
    String b = token();
    String name = "Bulkson" + token() + ", Ramon";
    byte[] file =
        csv(
            createHandler,
            List.of(
                row(
                    "Client Code",
                    "CL-DEMO-A002",
                    "Period From",
                    "2026-10-01",
                    "Period To",
                    "2027-10-01",
                    "Sum Insured",
                    "800000",
                    "Plate No",
                    "pl-" + a,
                    "Engine No",
                    "en " + a,
                    "Chassis No",
                    "ch" + a,
                    "Make",
                    "Toyota",
                    "Model",
                    "Innova",
                    "Year Model",
                    "2025",
                    "Quotation Ref",
                    "QT-" + a),
                row(
                    "Client Name",
                    name,
                    "Birth Date",
                    "1988-02-03",
                    "Period From",
                    "2026-10-01",
                    "Period To",
                    "2027-10-01",
                    "Sum Insured",
                    "700000",
                    "Plate No",
                    "pl" + b,
                    "Engine No",
                    "en" + b,
                    "Chassis No",
                    "ch" + b,
                    "Make",
                    "Nissan",
                    "Model",
                    "Almera",
                    "Year Model",
                    "2024"),
                row(
                    "Client Code",
                    "CL-DEMO-A002",
                    "Period From",
                    "2026-10-01",
                    "Period To",
                    "2027-10-01",
                    "Sum Insured",
                    "800000",
                    "Plate No",
                    existingPlate,
                    "Engine No",
                    "zz" + a,
                    "Chassis No",
                    "yy" + a,
                    "Make",
                    "Toyota",
                    "Model",
                    "Innova",
                    "Year Model",
                    "2025"),
                row(
                    "Client Code",
                    "CL-DEMO-A002",
                    "Period From",
                    "2026-10-01",
                    "Period To",
                    "2027-10-01",
                    "Sum Insured",
                    "800000",
                    "Plate No",
                    "qq" + a,
                    "Make",
                    "Toyota",
                    "Model",
                    "Innova"),
                row(
                    "Client Name",
                    "Nobody Individual",
                    "Birth Date",
                    "1990-01-01",
                    "Period From",
                    "2026-10-01",
                    "Period To",
                    "2027-10-01",
                    "Sum Insured",
                    "1",
                    "Plate No",
                    "rr" + a,
                    "Engine No",
                    "rr" + a,
                    "Chassis No",
                    "rr" + a,
                    "Make",
                    "X",
                    "Model",
                    "Y",
                    "Year Model",
                    "2020"),
                row(
                    "Client Code",
                    "NO-SUCH-CLIENT",
                    "Period From",
                    "2026-10-01",
                    "Period To",
                    "2027-10-01",
                    "Sum Insured",
                    "1",
                    "Plate No",
                    "ss" + a)));
    BulkJob job = upload(createHandler, row("product", "MTR10", "segment", "CBG"), file);
    List<BulkRowRecord> rows = rows(job);
    assertThat(job.getValidRows()).isEqualTo(2);
    assertThat(rows.get(2).getMessages()).contains(existing.getArn()).contains("plate number");
    assertThat(rows.get(3).getMessages()).contains("Engine").contains("Year model");
    assertThat(rows.get(4).getMessages()).contains("Last, First");
    assertThat(rows.get(5).getMessages()).contains("NO-SUCH-CLIENT");

    BulkJob done = as.run("ao", () -> bulk.commit(job.getId()));
    assertThat(done.getCommittedRows()).isEqualTo(2);
    List<BulkRowRecord> committed = rows(done);
    Account first = queries.requireByArn(committed.get(0).getResultRef());
    assertThat(first.getQuotationRef()).isEqualTo("QT-" + a);
    assertThat(first.getSourceChannel()).isEqualTo("UPLOAD");
    assertThat(first.getItems().get(0).getPlateNo()).isEqualTo("PL" + a);
    assertThat(first.getPremium().isRated()).isTrue();
    Account second = queries.requireByArn(committed.get(1).getResultRef());
    assertThat(second.getClientName()).isEqualTo(name);
    assertThat(second.getClientCode()).startsWith("PR-");
    assertThat(second.getStatus()).isEqualTo(AccountStatus.DRAFT);

    BulkJob withSubmit = upload(createHandler, row("product", "MTR10", "submit", "Y"), file);
    assertThat(rows(withSubmit).get(0).getMessages()).contains("needs documents");
  }

  @Test
  void accountsAreSubmittedDirectlyWhenNoDocumentIsMandatory() {
    String a = token();
    byte[] file =
        csv(
            createHandler,
            List.of(
                row(
                    "Client Code",
                    "CL-DEMO-A003",
                    "Market Segment",
                    "CORBANK",
                    "Period From",
                    "2026-11-01",
                    "Period To",
                    "2027-11-01",
                    "Sum Insured",
                    "3000000",
                    "Description",
                    "Store operations " + a,
                    "PN Numbers",
                    "PN-" + a + "; PN2-" + a,
                    "Direct Payment",
                    "Y")));
    BulkJob job = upload(createHandler, row("product", "CGL01", "submit", "Y"), file);
    assertThat(job.getValidRows()).isEqualTo(1);
    BulkJob done = as.run("ao", () -> bulk.commit(job.getId()));
    Account account = queries.requireByArn(rows(done).get(0).getResultRef());
    assertThat(account.getStatus()).isEqualTo(AccountStatus.SUBMITTED);
    assertThat(account.getPnNumbers()).containsExactly("PN-" + a, "PN2-" + a);
    assertThat(account.getPaymentArrangement().name()).isEqualTo("DIRECT_TO_INSURER");

    byte[] update =
        csv(
            updateHandler,
            List.of(
                row(
                    "ARN",
                    account.getArn(),
                    "Sum Insured",
                    "3500000",
                    "Contact Email",
                    "ops@example.ph"),
                row("ARN", "ARN-1999-000001")));
    BulkJob updateJob = upload(updateHandler, row(), update);
    assertThat(updateJob.getValidRows()).isEqualTo(1);
    assertThat(rows(updateJob).get(1).getMessages()).contains("ARN-1999-000001");
    as.run("proc", () -> bulk.commit(updateJob.getId()));
    Account updated = queries.get(account.getId());
    assertThat(updated.getTotalSumInsured()).isEqualByComparingTo("3500000");
    assertThat(updated.getContact().email()).isEqualTo("ops@example.ph");
    assertThat(updated.getPnNumbers()).hasSize(2);
  }

  @Test
  void draftsAreUpdatedAndSubmittedByUploadAndFfyIsTaggedBySerialNumber() {
    Client client = fx.confirmed("CL-DEMO-A001");
    String id = token();
    Account account =
        as.run(
            "ao",
            () ->
                accounts.createDraft(
                    NewAccount.direct(
                        fx.company(),
                        draft(client.getId(), "MTR22", "CBG", List.of(vehicle(id, "900000"))))));
    fx.attach(account.getId(), "IDF", "ao");
    byte[] update =
        csv(
            updateHandler,
            List.of(
                row(
                    "ARN",
                    account.getArn(),
                    "Period From",
                    "2026-11-01",
                    "Period To",
                    "2027-11-01",
                    "Rate %",
                    "1.5",
                    "Loan Application No",
                    "AL-" + id,
                    "Mortgagee Bank",
                    "BDO_AUTO_LOANS",
                    "Submit",
                    "Y")));
    BulkJob job = upload(updateHandler, row(), update);
    assertThat(job.getValidRows()).isEqualTo(1);
    as.run("ao", () -> bulk.commit(job.getId()));
    Account submitted = queries.get(account.getId());
    assertThat(submitted.getStatus()).isEqualTo(AccountStatus.SUBMITTED);
    assertThat(submitted.getPeriodFrom()).isEqualTo(LocalDate.of(2026, 11, 1));
    assertThat(submitted.getItems().get(0).getRate()).isEqualByComparingTo("1.5");
    assertThat(submitted.getLoanApplicationNo()).isEqualTo("AL-" + id);

    byte[] ffy =
        csv(
            ffyHandler,
            List.of(
                row("Vehicle Identifier", "c-" + id.toLowerCase(), "FFY Start", "2026-11-01"),
                row("Vehicle Identifier", "NOPE" + id, "FFY Start", "2026-11-01"),
                row("Vehicle Identifier", "E" + id, "Action", "SOMETHING")));
    BulkJob ffyJob = upload(ffyHandler, row(), ffy);
    assertThat(ffyJob.getValidRows()).isEqualTo(1);
    assertThat(rows(ffyJob).get(1).getMessages()).contains("No live account");
    as.run("ao", () -> bulk.commit(ffyJob.getId()));
    assertThat(queries.get(account.getId()).getFreeFirstYear().end())
        .isEqualTo(LocalDate.of(2027, 10, 31));

    byte[] cancel =
        csv(
            ffyHandler,
            List.of(
                row(
                    "Vehicle Identifier",
                    "P" + id,
                    "Action",
                    "cancel",
                    "Cancel Reason",
                    "TAGGED_IN_ERROR")));
    BulkJob cancelJob = upload(ffyHandler, row(), cancel);
    assertThat(cancelJob.getValidRows()).isEqualTo(1);
    as.run("ao", () -> bulk.commit(cancelJob.getId()));
    assertThat(queries.get(account.getId()).getFreeFirstYear().active()).isFalse();
  }
}
