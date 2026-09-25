package com.iortatechnxt.brokerverse.collections.demo;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.AssignmentKind;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.ItemStatus;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.disposition.domain.PrDispositionRepository;
import com.iortatechnxt.brokerverse.collections.disposition.service.EffortService;
import com.iortatechnxt.brokerverse.collections.disposition.service.EffortService.EffortCommand;
import com.iortatechnxt.brokerverse.collections.disposition.service.PrDispositionService;
import com.iortatechnxt.brokerverse.collections.disposition.service.PrDispositionService.DispositionCommand;
import com.iortatechnxt.brokerverse.collections.files.service.CollectionFiles;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentService;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentService.ItemSelection;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentService.Reassign;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistQueryService;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistRefreshService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.demo.DemoUsers;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Demo start-up of Collections (demo profile only, idempotent), after the booking and Operations
 * runners have put the demo invoices in the ledger: the team lead ({@code clxtl}) refreshes the
 * worklist, which lists the unpaid demo invoices and assigns them by the V1900 rules; the handler
 * ({@code clxhandler}) logs an effort and records dispositions of every kind in turn on the open
 * accounts - coordinate further, DP PR for reversal (to Commission), PR 2307 for reversal and check
 * pick-up (to Cashiering), a later one superseding an earlier one on the same account and
 * withdrawing its hand-off; the team lead reassigns one account temporarily and publishes the daily
 * files, the weekly files of this week (available next Monday) and the monthly files (available on
 * the first working day).
 */
@Component
@Profile("demo")
@Order(97)
public class CollectionsDemoData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(CollectionsDemoData.class);
  private static final String LEAD = "clxtl";
  private static final String HANDLER = "clxhandler";
  private static final int PICKUP_IN_DAYS = 3;
  private static final int TEMPORARY_DAYS = 14;
  private static final int STEPS = 4;

  private final CompanyRepository companies;
  private final WorklistRefreshService refresh;
  private final WorklistQueryService worklist;
  private final PrDispositionService dispositions;
  private final PrDispositionRepository dispositionRows;
  private final EffortService efforts;
  private final AssignmentService assignments;
  private final CollectionFiles files;
  private final DemoUsers users;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param companies companies
   * @param refresh worklist refresh
   * @param worklist worklist reads
   * @param dispositions dispositions
   * @param dispositionRows dispositions (idempotency)
   * @param efforts efforts
   * @param assignments reassignment
   * @param files Collections files
   * @param users demo sign-in
   * @param clock clock
   */
  public CollectionsDemoData(
      CompanyRepository companies,
      WorklistRefreshService refresh,
      WorklistQueryService worklist,
      PrDispositionService dispositions,
      PrDispositionRepository dispositionRows,
      EffortService efforts,
      AssignmentService assignments,
      CollectionFiles files,
      DemoUsers users,
      Clock clock) {
    this.companies = companies;
    this.refresh = refresh;
    this.worklist = worklist;
    this.dispositions = dispositions;
    this.dispositionRows = dispositionRows;
    this.efforts = efforts;
    this.assignments = assignments;
    this.files = files;
    this.users = users;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    LocalDate today = LocalDate.now(clock);
    for (Company company : companies.findAll()) {
      Long id = company.getId();
      var outcome = users.as(LEAD, () -> refresh.refreshAll(id, today));
      LOG.info("Collections demo refresh of {}: {}", company.getCode(), outcome.message());
      if (dispositionRows.count() == 0) {
        storyline(id, today);
      }
    }
  }

  private void storyline(Long companyId, LocalDate today) {
    List<CollectionItem> open =
        worklist
            .search(
                companyId,
                WorklistFilter.of(ItemStatus.OPEN),
                PageRequest.of(0, STEPS, Sort.by("id")))
            .getContent();
    try {
      for (int step = 0; !open.isEmpty() && step < STEPS; step++) {
        step(companyId, step, open.get(step % open.size()), today);
      }
      users.run(LEAD, () -> files.daily(companyId, today));
      users.run(
          LEAD,
          () ->
              files.weekly(companyId, today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))));
      users.run(LEAD, () -> files.monthly(companyId, firstWorkingDayOfNextMonth(today)));
      LOG.info("Collections demo: {} account(s) worked", open.size());
    } catch (BusinessRuleException | ResourceNotFoundException ex) {
      LOG.warn("Collections demo storyline skipped: {}", ex.getMessage());
    }
  }

  private void step(Long companyId, int index, CollectionItem item, LocalDate today) {
    String no = item.getInvoiceNo();
    switch (index) {
      case 0 -> {
        users.run(
            HANDLER,
            () ->
                efforts.log(
                    companyId,
                    new EffortCommand(
                        List.of(no),
                        "CALL",
                        null,
                        "PHONE",
                        "Client treasury",
                        "Client asked for the statement of account")));
        dispose(
            companyId,
            no,
            "COORDINATE_FURTHER",
            "Awaiting the client's payment schedule",
            Map.of());
        users.run(
            LEAD,
            () ->
                assignments.reassign(
                    companyId,
                    new ItemSelection(List.of(no), null),
                    new Reassign(
                        "mkthandler",
                        AssignmentKind.TEMPORARY,
                        today.plusDays(TEMPORARY_DAYS),
                        "Handler on leave")));
      }
      case 1 ->
          dispose(
              companyId, no, "DP_PR_FOR_REVERSAL", "Client paid the insurer directly", Map.of());
      case 2 ->
          dispose(
              companyId,
              no,
              "PR2307_FOR_REVERSAL",
              "BIR 2307 received by e-mail",
              Map.of("path", "CERTIFICATE", "certificateNo", "2307-2026-0917"));
      default ->
          dispose(
              companyId,
              no,
              "FOR_CHECK_PICKUP",
              "Client asked for check pick-up",
              Map.of(
                  "pickupDate", today.plusDays(PICKUP_IN_DAYS).toString(),
                  "pickupAddress", "21 Paseo de Roxas, Makati",
                  "contactPerson", "Treasury desk",
                  "checkNo", "CHK-100245",
                  "checkBank", "BDO",
                  "amount", item.getFigures().netOutstanding().toPlainString()));
    }
  }

  private void dispose(
      Long companyId, String invoiceNo, String code, String remarks, Map<String, String> details) {
    users.run(
        HANDLER,
        () ->
            dispositions.record(
                companyId,
                new DispositionCommand(List.of(invoiceNo), code, remarks, null, details)));
  }

  private static LocalDate firstWorkingDayOfNextMonth(LocalDate today) {
    LocalDate d = today.plusMonths(1).withDayOfMonth(1);
    while (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
      d = d.plusDays(1);
    }
    return d;
  }
}
