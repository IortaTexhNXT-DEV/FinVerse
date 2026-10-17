package com.iortatechnxt.brokerverse.crm;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.bulk.service.BulkUpload;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

@IntegrationTest
class ClientBulkIT {

  private static final String HEADER =
      "Client Code,Client Type,Last Name,First Name,Middle Name,Corporate Name,Birth Date,TIN,"
          + "ID Type,ID Number,E-mail,Mobile,Address,City,Province,Postal Code,Market Segment,"
          + "Bank Client,Bank CIF,Nationality,Source of Funds,Risk Rating\n";

  private static final List<String> COLUMNS = List.of(HEADER.trim().split(","));

  @Autowired private CrmFixtures fx;
  @Autowired private BulkService bulk;
  @Autowired private ClientService clients;
  @Autowired private ClientRepository repository;
  @Autowired private AsUser as;

  @Test
  void createsUpdatesByCodeOrKeyAndReportsEveryProblem() {
    Client byCode = fx.prospect(CrmFixtures.person());
    ClientDetails keyed = CrmFixtures.person();
    Client byTin = fx.prospect(keyed);
    String newTin = CrmFixtures.tin();
    String newLast = "Bulk" + CrmFixtures.word(6);
    String newMobile = "0998" + CrmFixtures.digits(7);
    String csv =
        HEADER
            + row(
                Map.ofEntries(
                    Map.entry("Client Type", "individual"),
                    Map.entry("Last Name", newLast),
                    Map.entry("First Name", "Ana"),
                    Map.entry("Birth Date", "1991-02-03"),
                    Map.entry("TIN", newTin),
                    Map.entry("ID Type", "passport"),
                    Map.entry("ID Number", "B" + CrmFixtures.digits(8)),
                    Map.entry("E-mail", "ana@bulk.ph"),
                    Map.entry("Mobile", "09171112222"),
                    Map.entry("Address", "1 Bulk St"),
                    Map.entry("City", "Pasig"),
                    Map.entry("Market Segment", "cbg"),
                    Map.entry("Bank Client", "Y"),
                    Map.entry("Nationality", "filipino"),
                    Map.entry("Source of Funds", "salary"),
                    Map.entry("Risk Rating", "standard")))
            + row(
                Map.of(
                    "Client Code",
                    byCode.getProspectCode(),
                    "Client Type",
                    "INDIVIDUAL",
                    "Mobile",
                    newMobile))
            + row(Map.of("Client Type", "INDIVIDUAL", "TIN", "123456"))
            + row(Map.of("Client Type", "PERSON", "Last Name", "X", "First Name", "Y"))
            + row(
                Map.of(
                    "Client Code", "PR-1900-000001",
                    "Client Type", "INDIVIDUAL",
                    "Last Name", "X",
                    "First Name", "Y"))
            + row(
                Map.of(
                    "Client Type", "INDIVIDUAL",
                    "Last Name", keyed.name().lastName(),
                    "First Name", keyed.name().firstName(),
                    "TIN", keyed.identity().tin(),
                    "Market Segment", "RETAIL"));
    BulkJob job =
        as.run(
            "ao",
            () ->
                bulk.upload(
                    new BulkUpload(
                        fx.company(),
                        ClientBulkHandler.CODE,
                        "clients.csv",
                        csv.getBytes(StandardCharsets.UTF_8),
                        Map.of())));
    List<BulkRowRecord> rows = bulk.rows(job.getId(), null, Pageable.ofSize(20)).getContent();
    assertThat(rows.get(0).getMessages()).isNullOrEmpty();
    assertThat(rows.get(1).getMessages()).isNullOrEmpty();
    assertThat(rows.get(2).getMessages())
        .contains("Enter the Last Name and First Name of an individual client")
        .contains("Enter the TIN as 000-000-000-000");
    assertThat(rows.get(3).getMessages()).contains("Client Type must be INDIVIDUAL or CORPORATE");
    assertThat(rows.get(4).getMessages()).contains("Client code PR-1900-000001 does not exist");
    assertThat(rows.get(5).getMessages()).isNullOrEmpty();
    assertThat(job.getValidRows()).isEqualTo(3);

    BulkJob done = as.run("ao", () -> bulk.commit(job.getId()));
    assertThat(done.getCommittedRows()).isEqualTo(3);
    Client created =
        repository.findByCompanyIdAndTin(fx.company(), newTin).stream().findFirst().orElseThrow();
    assertThat(created.getLastName()).isEqualTo(newLast);
    assertThat(created.getIdType()).isEqualTo("PASSPORT");
    assertThat(created.isBankClient()).isTrue();
    assertThat(created.profile().riskRating()).isEqualTo("STANDARD");

    Client updated = clients.get(byCode.getId());
    assertThat(updated.getMobile()).isEqualTo(newMobile);
    assertThat(updated.getTin()).isEqualTo(byCode.getTin());
    assertThat(updated.getEmail()).isEqualTo(byCode.getEmail());
    assertThat(clients.get(byTin.getId()).getMarketSegment()).isEqualTo("RETAIL");
  }

  private static String row(Map<String, String> values) {
    return String.join(",", COLUMNS.stream().map(c -> values.getOrDefault(c, "")).toList()) + "\n";
  }
}
