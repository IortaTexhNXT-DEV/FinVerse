package com.iortatechnxt.brokerverse.csf;

import static com.iortatechnxt.brokerverse.csf.CsfFixtures.AGENT;
import static com.iortatechnxt.brokerverse.csf.CsfFixtures.MANAGEMENT;
import static com.iortatechnxt.brokerverse.csf.CsfFixtures.SUPERVISOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.csf.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.Category;
import com.iortatechnxt.brokerverse.csf.cases.report.CsfCaseSummaryReport;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseCalendar;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseQueryService;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseQueryService.Queue;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseService;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseService.LogRequest;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseSetupService;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseStatistics;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseStatistics.Criteria;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseViews.CaseRow;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

/**
 * Case management of the Customer Servicing Facility (FR-CSF-050, FR-CSF-051, FR-CSF-052,
 * FR-CSF-053, FR-CSF-054, FR-CSF-055, FR-CSF-056, FR-CSF-057, FR-CSF-058, FR-CSF-059, FR-CSF-060,
 * FR-CSF-061): logging with the mandatory fields, the duplicate rule and Resolve and Close; routing
 * to the fulfilment unit, resolution, return, CSR action and re-endorsement; reassignment,
 * escalation and change of category by the supervisor; notes, follow-up cases and the history; the
 * access of the fulfilment units; the dashboard and the Case Summary on the same figures.
 */
@IntegrationTest
class CsfCaseIT {

  private static final String FULFILMENT = "csffu";

  @Autowired private CsfFixtures fx;
  @Autowired private CsfCaseService cases;
  @Autowired private CsfCaseQueryService queries;
  @Autowired private CsfCaseSetupService setup;
  @Autowired private CsfCaseStatistics statistics;
  @Autowired private CsfCaseSummaryReport summary;
  @Autowired private CsfCaseCalendar calendar;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Clock clock;
  @Autowired private AsUser as;
  @Autowired private Api api;

  private static Category category(String type, String nature, String unit) {
    return new Category("PHONE", type, nature, "MOTOR", "CONSUMER_1", unit, "NORMAL");
  }

  private CsfCase log(Client c, String type, String nature, String unit) {
    return as.run(
        AGENT,
        () ->
            cases.log(
                fx.company(),
                new LogRequest(
                    c.getId(),
                    null,
                    null,
                    null,
                    category(type, nature, unit),
                    "Subject " + nature,
                    "Description of the request",
                    false,
                    null,
                    null)));
  }

  @Test
  void aCaseGetsItsIdAndDueDateAndADuplicateIsRefused() {
    Client c = fx.client(true);
    CsfCase logged = log(c, "INQUIRY", "OR_REQUEST", "PROCESSING");
    LocalDate today = BusinessClock.today(clock);
    assertThat(logged.caseNo()).matches("CASE-\\d{4}-\\d{6}");
    assertThat(logged.status()).isEqualTo(CaseStatus.WITH_FULFILMENT);
    assertThat(logged.tatDays()).isEqualTo(3);
    assertThat(logged.dueDate())
        .isEqualTo(CsfCaseCalendar.plus(today, 3, calendar.of(fx.company())));
    assertThatThrownBy(() -> log(c, "REQUEST", "OR_REQUEST", "PROCESSING"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining(logged.caseNo());
    assertThatThrownBy(
            () ->
                as.run(
                    AGENT,
                    () ->
                        cases.log(
                            fx.company(),
                            new LogRequest(
                                c.getId(),
                                null,
                                null,
                                null,
                                new Category("PHONE", "", "", "MOTOR", "CONSUMER_1", "", null),
                                "S",
                                "D",
                                false,
                                null,
                                null))))
        .hasMessageContaining("case type, nature of transaction, fulfilment unit");
    assertThat(as.run(MANAGEMENT, () -> queries.history(fx.company(), logged.id())))
        .extracting(h -> h.user())
        .contains(AGENT);
  }

  @Test
  void anInquiryAnsweredOnTheCallIsClosedByCsrWithItsResolution() {
    Client c = fx.client(true);
    CsfCase closed =
        as.run(
            AGENT,
            () ->
                cases.log(
                    fx.company(),
                    new LogRequest(
                        c.getId(),
                        null,
                        null,
                        null,
                        category("INQUIRY", "COVERAGE_INQUIRY", "PROCESSING"),
                        "Coverage",
                        "Asked about the coverage",
                        true,
                        "Explained the coverage",
                        null)));
    assertThat(closed.status()).isEqualTo(CaseStatus.CLOSED_BY_CSR);
    assertThat(closed.handling().resolution()).isEqualTo("Explained the coverage");
    assertThatThrownBy(() -> as.run(AGENT, () -> cases.addNote(closed.id(), "late note")))
        .hasMessageContaining("Closed by CSR");

    CsfCase followUp = as.run(AGENT, () -> cases.followUp(closed.id(), "Client sends a document"));
    assertThat(followUp.caseNo()).isNotEqualTo(closed.caseNo());
    assertThat(followUp.parentCaseId()).isEqualTo(closed.id());
    assertThat(as.run(AGENT, () -> queries.record(fx.company(), closed.id())).linked())
        .singleElement()
        .satisfies(l -> assertThat(l.caseNo()).isEqualTo(followUp.caseNo()));
  }

  @Test
  void theFulfilmentUnitResolvesReturnsAndAsksForCsrAction() {
    Client c = fx.client(true);
    CsfCase first = log(c, "REQUEST", "OR_REQUEST", "PROCESSING");
    assertThat(as.run(FULFILMENT, () -> queries.queue(fx.company(), Queue.UNIT, null, null)))
        .extracting(CaseRow::caseNo)
        .contains(first.caseNo());
    CsfCase resolved =
        as.run(FULFILMENT, () -> cases.resolve(first.id(), "Receipt sent", "REQUEST_COMPLETED"));
    assertThat(resolved.status()).isEqualTo(CaseStatus.CLOSED_BY_FULFILMENT);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where lower(recipient) = ? and title like ?",
                Long.class,
                AGENT,
                first.caseNo() + "%"))
        .isPositive();

    CsfCase second = log(c, "PROBLEM", "BILLING_INQUIRY", "PROCESSING");
    as.run(FULFILMENT, () -> cases.returnToAgent(second.id(), "INCOMPLETE_DETAILS", "Dates"));
    assertThat(as.run(AGENT, () -> queries.queue(fx.company(), Queue.RETURNED, null, null)))
        .extracting(CaseRow::caseNo)
        .contains(second.caseNo());
    assertThatThrownBy(
            () ->
                as.run(
                    FULFILMENT, () -> cases.returnToAgent(second.id(), "INCOMPLETE_DETAILS", "")))
        .hasMessageContaining("Returned to Agent");
    as.run(AGENT, () -> cases.reendorse(second.id(), "Dates added"));
    CsfCase action = as.run(FULFILMENT, () -> cases.requestCsrAction(second.id(), "Call back"));
    assertThat(action.status()).isEqualTo(CaseStatus.CSR_ACTION_REQUIRED);
    CsfCase closed =
        as.run(AGENT, () -> cases.closeByCsr(second.id(), "Called the client", "RESOLVED"));
    assertThat(closed.status()).isEqualTo(CaseStatus.CLOSED_BY_CSR);
    assertThat(as.run(AGENT, () -> queries.notes(fx.company(), second.id())))
        .extracting(n -> n.author())
        .contains(AGENT, FULFILMENT);
  }

  @Test
  void aMemberOfAnotherUnitDoesNotSeeTheCase() {
    Client c = fx.client(true);
    CsfCase other = log(c, "REQUEST", "OR_REQUEST", "RENEWAL");
    assertThat(as.run(FULFILMENT, () -> queries.queue(fx.company(), Queue.ALL, null, null)))
        .extracting(CaseRow::caseNo)
        .doesNotContain(other.caseNo());
    assertThatThrownBy(() -> as.run(FULFILMENT, () -> cases.resolve(other.id(), "x", "RESOLVED")))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void theSupervisorReassignsEscalatesAndRecategorises() throws Exception {
    Client c = fx.client(true);
    CsfCase logged = log(c, "INQUIRY", "RENEWAL_INQUIRY", "PROCESSING");
    assertThatThrownBy(() -> as.run(AGENT, () -> cases.escalate(logged.id(), "PAST_TAT", null)))
        .isInstanceOf(AccessDeniedException.class);
    as.run(SUPERVISOR, () -> cases.reassign(logged.id(), "csfagent2", null, "Leave"));
    assertThat(as.run("csfagent2", () -> queries.queue(fx.company(), Queue.MINE, null, null)))
        .extracting(CaseRow::caseNo)
        .contains(logged.caseNo());
    assertThatThrownBy(() -> as.run(SUPERVISOR, () -> cases.escalate(logged.id(), "", null)))
        .hasMessageContaining("escalation reason");
    CsfCase escalated =
        as.run(SUPERVISOR, () -> cases.escalate(logged.id(), "DUE_FOR_RESOLUTION", "Urgent"));
    assertThat(escalated.escalation().escalated()).isTrue();
    assertThat(escalated.status()).isEqualTo(CaseStatus.WITH_FULFILMENT);
    CsfCase changed =
        as.run(
            SUPERVISOR,
            () -> cases.changeCategory(logged.id(), "COMPLAINT", "RENEWAL_INQUIRY", "Angry"));
    LocalDate created = BusinessClock.dateOf(logged.createdAt());
    assertThat(changed.dueDate())
        .isEqualTo(CsfCaseCalendar.plus(created, 6, calendar.of(fx.company())));
    assertThat(as.run(MANAGEMENT, () -> queries.history(fx.company(), logged.id())))
        .extracting(h -> h.summary())
        .anyMatch(s -> s.startsWith("Reassigned"))
        .anyMatch(s -> s.startsWith("Escalated"))
        .anyMatch(s -> s.startsWith("Category changed"));

    api.doGet(AGENT, "/api/v1/csf/cases/dashboard?companyId=" + fx.company())
        .andExpect(status().isForbidden());
    api.doPost(
            AGENT,
            "/api/v1/csf/cases/" + logged.id() + "/escalate?companyId=" + fx.company(),
            Json.of("reason", "PAST_TAT"))
        .andExpect(status().isForbidden());
  }

  @Test
  void theDashboardAndTheCaseSummaryShowTheSameFigures() throws Exception {
    Client c = fx.client(true);
    log(c, "REQUEST", "REFUND_REQUEST", "PROCESSING");
    LocalDate today = BusinessClock.today(clock);
    Criteria k = new Criteria(fx.company(), today, today, null, null, null, null, null);
    CsfCaseStatistics.Dashboard d = as.run(MANAGEMENT, () -> statistics.dashboard(k));
    assertThat(d.created()).isPositive();
    ReportResult r =
        summary.generate(
            ReportParameters.validate(
                summary.metadata(),
                Map.of(
                    "companyId", fx.company().toString(),
                    "fromDate", today.toString(),
                    "toDate", today.toString()),
                clock));
    assertThat(r.rows())
        .anySatisfy(
            row -> {
              assertThat(row.cells().get("measure")).isEqualTo("Cases created");
              assertThat(row.cells().get("value")).isEqualTo(String.valueOf(d.created()));
            });
    JsonNode body =
        api.read(
            api.doGet(
                    MANAGEMENT,
                    "/api/v1/csf/cases/dashboard?companyId="
                        + fx.company()
                        + "&from="
                        + today
                        + "&to="
                        + today)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").isNumber()));
    assertThat(body.get("created").asLong()).isGreaterThanOrEqualTo(d.created());
  }

  @Test
  void theRoutingTableProposesTheMostSpecificUnit() {
    assertThat(setup.propose("COMPLAINT", "CORPORATE", "OR_REQUEST")).contains("CLAIMS");
    assertThat(setup.propose("INQUIRY", "CORPORATE", "OR_REQUEST")).contains("PROCESSING");
    assertThat(List.of(setup.propose("INQUIRY", "", "CLAIM_STATUS").orElseThrow()))
        .containsExactly("CLAIMS");
  }
}
