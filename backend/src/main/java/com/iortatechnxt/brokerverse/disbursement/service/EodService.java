package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.EventSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.OutputKind;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.VoucherStage;
import com.iortatechnxt.brokerverse.disbursement.domain.EodOutput;
import com.iortatechnxt.brokerverse.disbursement.domain.EodOutput.OutputFile;
import com.iortatechnxt.brokerverse.disbursement.domain.EodOutputRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.EodRun;
import com.iortatechnxt.brokerverse.disbursement.domain.EodRun.EodCounts;
import com.iortatechnxt.brokerverse.disbursement.domain.EodRunRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.Instrument;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherRepository;
import com.iortatechnxt.brokerverse.disbursement.service.DisbursementForms.FormFacts;
import com.iortatechnxt.brokerverse.disbursement.service.DisbursementForms.FormItem;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentService.Change;
import com.iortatechnxt.brokerverse.payables.service.NotificationRecord;
import com.iortatechnxt.brokerverse.payables.service.PaymentNotificationFormatter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-of-day processing of a business date (DIS 2.16.0-2.16.6, 3.26.3-3.26.6, 3.28.0-3.28.2): the
 * vouchers approved up to the end of the date and not yet processed are frozen in the run; credits
 * to account are extracted into the Direct Credit Transaction File (DCTF, forwarded to TPD for ACA,
 * AQ09); checks get their numbers and are printed in one batch; the ATD, MC / DD, credit ticket and
 * TT forms are generated; the day's vouchers are printed together; then the EOD reports are
 * produced. One run per company and date.
 */
@Service
@Transactional
public class EodService {

  private static final String TEXT = "text/plain";

  private final EodRunRepository runs;
  private final EodOutputRepository outputs;
  private final VoucherRepository vouchers;
  private final InstrumentService instruments;
  private final InstrumentActions actions;
  private final DisbursementForms forms;
  private final FormFactsReader facts;
  private final EodReports reports;
  private final EodConfirmations confirmations;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param runs runs
   * @param outputs outputs
   * @param vouchers vouchers
   * @param instruments instruments
   * @param actions printing
   * @param forms printed forms
   * @param facts form facts
   * @param reports EOD reports
   * @param confirmations payment confirmations
   * @param numbers run numbers
   * @param audit audit trail
   */
  @SuppressWarnings("java:S107") // constructor injection
  public EodService(
      EodRunRepository runs,
      EodOutputRepository outputs,
      VoucherRepository vouchers,
      InstrumentService instruments,
      InstrumentActions actions,
      DisbursementForms forms,
      FormFactsReader facts,
      EodReports reports,
      EodConfirmations confirmations,
      DocumentNumberService numbers,
      AuditTrailService audit) {
    this.runs = runs;
    this.outputs = outputs;
    this.vouchers = vouchers;
    this.instruments = instruments;
    this.actions = actions;
    this.forms = forms;
    this.facts = facts;
    this.reports = reports;
    this.confirmations = confirmations;
    this.numbers = numbers;
    this.audit = audit;
  }

  /**
   * Runs the end of day of a date (DIS 2.16.0).
   *
   * @param companyId company
   * @param businessDate business date
   * @return the run
   */
  public EodRun run(Long companyId, LocalDate businessDate) {
    if (runs.findByCompanyIdAndBusinessDate(companyId, businessDate).isPresent()) {
      throw new BusinessRuleException(
          "EOD_ALREADY_RUN", "The end of day of " + businessDate + " was already processed");
    }
    EodRun run =
        runs.save(
            new EodRun(
                companyId,
                numbers.next(DisbursementSettings.series("EOD", businessDate)),
                businessDate));
    List<FormItem> all = freeze(run);
    Map<DisbursementMode, List<FormItem>> byMode = new EnumMap<>(DisbursementMode.class);
    all.forEach(
        it -> byMode.computeIfAbsent(it.instrument().getMode(), m -> new ArrayList<>()).add(it));
    int credits = dctf(run, byMode.getOrDefault(DisbursementMode.CTA, List.of()));
    int checks = printed(run, DisbursementMode.CHECK, OutputKind.CHECKS, byMode);
    int formCount =
        printed(run, DisbursementMode.ATD, OutputKind.ATD, byMode)
            + printed(run, DisbursementMode.MC_DD, OutputKind.MC_DD, byMode)
            + printed(run, DisbursementMode.CREDIT_TICKET, OutputKind.CREDIT_TICKET, byMode)
            + printed(run, DisbursementMode.TT, OutputKind.TT, byMode);
    if (!all.isEmpty()) {
      save(
          run,
          OutputKind.VOUCHERS,
          "DV-BATCH",
          new OutputFile(
              run.getRunNo() + "_vouchers.pdf",
              DisbursementForms.PDF,
              forms.vouchers(run.getRunNo(), all)),
          all.size());
    }
    run.produced(
        new EodCounts(
            all.size(),
            checks,
            credits,
            formCount,
            0,
            all.size()
                + " DV(s): "
                + checks
                + " check(s), "
                + credits
                + " credit(s), "
                + formCount
                + " form(s)"));
    reports.produce(run);
    audit.record(
        DisbursementSettings.MODULE,
        run.getRunNo(),
        AuditAction.RUN,
        "End of day " + run.getMessage());
    return run;
  }

  private List<FormItem> freeze(EodRun run) {
    List<Voucher> day =
        vouchers.findByCompanyIdAndStageAndEodRunIdIsNullAndApprovedAtBeforeOrderByIdAsc(
            run.getCompanyId(),
            VoucherStage.APPROVED,
            run.getBusinessDate()
                .plusDays(1)
                .atStartOfDay(DisbursementSettings.MANILA)
                .toInstant());
    List<FormItem> all = new ArrayList<>();
    for (Voucher v : day) {
      v.inEod(run.getId());
      Instrument i = instruments.forVoucher(v.getId());
      i.inEod(run.getId());
      all.add(new FormItem(v, i, facts.of(v)));
    }
    return all;
  }

  private int dctf(EodRun run, List<FormItem> items) {
    if (items.isEmpty()) {
      return 0;
    }
    List<NotificationRecord> records = new ArrayList<>();
    for (FormItem it : items) {
      Instrument i = it.instrument();
      if (i.getStatus() == InstrumentStatus.PENDING) {
        instruments.advance(
            i, InstrumentStatus.EXTRACTED, new Change(EventSource.SYSTEM, run.getRunNo(), null));
      }
      Voucher v = it.voucher();
      FormFacts f = it.facts();
      records.add(
          new NotificationRecord(
              v.getDvNo(),
              null,
              run.getBusinessDate(),
              v.getPayeeCode(),
              f.payeeAccount() == null ? v.getPayeeName() : f.payeeAccount().getAccountName(),
              f.payeeAccount() == null ? "" : f.payeeAccount().getAccountNo(),
              v.getCurrency(),
              v.getNet()));
    }
    String fileName = "DCTF_" + run.getRunNo() + ".txt";
    String text = PaymentNotificationFormatter.dctf(fileName, run.getBusinessDate(), records);
    save(
        run,
        OutputKind.DCTF,
        "DCTF",
        new OutputFile(fileName, TEXT, text.getBytes(StandardCharsets.US_ASCII)),
        records.size());
    return records.size();
  }

  private int printed(
      EodRun run,
      DisbursementMode mode,
      OutputKind kind,
      Map<DisbursementMode, List<FormItem>> byMode) {
    List<FormItem> items = byMode.getOrDefault(mode, List.of());
    if (items.isEmpty()) {
      return 0;
    }
    List<FormItem> printed = new ArrayList<>();
    for (FormItem it : items) {
      Instrument i = it.instrument();
      if (i.getStatus() == InstrumentStatus.PENDING) {
        i =
            actions.print(
                it.voucher().getId(), new Change(EventSource.SYSTEM, run.getRunNo(), null));
      }
      printed.add(new FormItem(it.voucher(), i, it.facts()));
    }
    save(
        run,
        kind,
        mode.name(),
        new OutputFile(
            run.getRunNo() + "_" + mode.name().toLowerCase(Locale.ROOT) + ".pdf",
            DisbursementForms.PDF,
            forms.instruments(mode, run.getRunNo(), printed)),
        printed.size());
    return printed.size();
  }

  private void save(EodRun run, OutputKind kind, String code, OutputFile file, int count) {
    outputs.save(new EodOutput(run.getId(), kind, code, file, count));
  }

  /**
   * E-mails the payment confirmations of a run (DIS 2.7.12).
   *
   * @param id run
   * @return run
   */
  public EodRun confirm(Long id) {
    EodRun run = get(id);
    confirmations.confirm(run);
    return run;
  }

  /**
   * A run.
   *
   * @param id id
   * @return run
   */
  @Transactional(readOnly = true)
  public EodRun get(Long id) {
    return runs.findById(id).orElseThrow(() -> new ResourceNotFoundException("EOD run", id));
  }

  /**
   * Runs of a company, newest date first.
   *
   * @param companyId company
   * @param pageable page
   * @return runs
   */
  @Transactional(readOnly = true)
  public Page<EodRun> runs(Long companyId, Pageable pageable) {
    return runs.findByCompanyIdOrderByBusinessDateDesc(companyId, pageable);
  }

  /**
   * The outputs of a run.
   *
   * @param runId run
   * @return outputs
   */
  @Transactional(readOnly = true)
  public List<EodOutput> outputs(Long runId) {
    return outputs.findByEodRunIdOrderByIdAsc(runId);
  }

  /**
   * One output file.
   *
   * @param outputId output
   * @return output with content
   */
  @Transactional(readOnly = true)
  public EodOutput output(Long outputId) {
    return outputs
        .findById(outputId)
        .orElseThrow(() -> new ResourceNotFoundException("EOD output", outputId));
  }
}
