package com.iortatechnxt.brokerverse.renewal.marketing.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import com.iortatechnxt.brokerverse.renewal.candidate.service.AccountHistoryService;
import com.iortatechnxt.brokerverse.renewal.domain.ClosedAs;
import com.iortatechnxt.brokerverse.renewal.domain.CurrentDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.DispositionSource;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.renewal.service.RenewalBatch;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalDispositions;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The disposition of a renewal by the Marketing AO (FR-RN-042, 043, 046, 047): refused until the
 * user opened the account history; Not for Renewal needs a reason, "Booked to New Invoice" the new
 * invoice number and "Transfer to Another Marketing Unit" opens a transfer. Each disposition is a
 * new row; pushing sends the account to the Team Leader, and a renewal tagged Not for Renewal is
 * re-opened until its expiry plus {@code RNW_REOPEN_DAYS}.
 */
@Service
@Transactional
public class RenewalDispositionService {

  private final RenewalRecords records;
  private final RenewalDispositions dispositions;
  private final AccountHistoryService history;
  private final TransferService transfers;
  private final RemarkService remarks;
  private final RenewalFlow flow;
  private final RenewalParameters parameters;
  private final RenewalNotices notices;
  private final RenewalBatch batch;
  private final OpsInvoiceRepository invoices;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param dispositions disposition history
   * @param history account history (gate)
   * @param transfers transfers
   * @param remarks remarks
   * @param flow workflow
   * @param parameters parameters
   * @param notices notifications
   * @param batch batch runner
   * @param invoices ledger invoices
   * @param lovs lists of values
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public RenewalDispositionService(
      RenewalRecords records,
      RenewalDispositions dispositions,
      AccountHistoryService history,
      TransferService transfers,
      RemarkService remarks,
      RenewalFlow flow,
      RenewalParameters parameters,
      RenewalNotices notices,
      RenewalBatch batch,
      OpsInvoiceRepository invoices,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.records = records;
    this.dispositions = dispositions;
    this.history = history;
    this.transfers = transfers;
    this.remarks = remarks;
    this.flow = flow;
    this.parameters = parameters;
    this.notices = notices;
    this.batch = batch;
    this.invoices = invoices;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Saves the disposition of a renewal (it is pushed separately).
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param input disposition
   * @return the renewal
   */
  public RenewalCandidate save(Long companyId, String renewalRef, Input input) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    RenewalRecords.requireStage(c, RenewalStage.FOR_DISPOSITION);
    RenewalRecords.requireUnlocked(c);
    requireDisposer(c);
    if (!history.viewedByCurrentUser(c)) {
      throw new BusinessRuleException(
          "HISTORY_NOT_VIEWED", "Open the Account History before you give the disposition");
    }
    if (input.code() == null) {
      throw new BusinessRuleException("RNW_DISPOSITION_REQUIRED", "Select the disposition");
    }
    String text = optionalText(input.remarks());
    String reason = reason(input);
    if (RenewalCodes.REASON_TRANSFER.equals(reason)) {
      transfers.request(c, new TransferService.Request(input.receivingUnit(), null, text));
      return c;
    }
    String newInvoice = newInvoice(reason, input.newInvoiceNo());
    dispositions.record(
        c,
        new CurrentDisposition(input.code(), reason, DispositionSource.USER, text, newInvoice),
        null,
        null);
    if (text != null) {
      remarks.add(c, text);
    }
    return c;
  }

  /**
   * Pushes dispositioned renewals to the Team Leader ("Review in progress"); the Returned flag
   * clears.
   *
   * @param companyId company
   * @param refs renewals
   * @return pushed and refused renewals
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public BatchOutcome push(Long companyId, List<String> refs) {
    return batch.run(refs, ref -> pushOne(records.get(companyId, ref)));
  }

  private void pushOne(RenewalCandidate c) {
    RenewalRecords.requireStage(c, RenewalStage.FOR_DISPOSITION);
    RenewalRecords.requireUnlocked(c);
    requireDisposer(c);
    List<String> missing = missing(c.getDisposition());
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "RNW_DISPOSITION_INCOMPLETE",
          "Complete the mandatory fields: " + String.join(", ", missing));
    }
    c.getFlags().setReturned(false);
    flow.act(c, "push", TransitionNote.comment("Disposition " + c.getDisposition().code().label()));
    flow.assign(c, null);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.SUBMIT,
        "Pushed to the Team Leader: " + c.getDisposition().code().label());
  }

  /**
   * Re-opens a renewal tagged Not for Renewal (FR-RN-047).
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param reason reason
   * @return the renewal
   */
  public RenewalCandidate reopen(Long companyId, String renewalRef, String reason) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    String text = RemarkService.requireText(reason, "Enter the reason for re-opening");
    boolean closedNotRenewed =
        c.getStage() == RenewalStage.CLOSED && c.getClosedAs() == ClosedAs.NOT_RENEWED;
    if (c.getStage() != RenewalStage.LETTER_PENDING && !closedNotRenewed) {
      throw new BusinessRuleException(
          "RNW_REOPEN_STAGE", "Only a renewal tagged Not for Renewal can be re-opened");
    }
    LocalDate limit = c.getExpiryDate().plusDays(parameters.reopenDays());
    if (BusinessClock.today(clock).isAfter(limit)) {
      throw new BusinessRuleException(
          "RNW_REOPEN_LATE",
          "Renewal "
              + c.getRenewalRef()
              + " can no longer be re-opened (expired on "
              + c.getExpiryDate()
              + ")");
    }
    flow.act(c, "reopen", TransitionNote.comment(text));
    c.reopen();
    remarks.add(c, "Re-opened: " + text);
    audit.record(RenewalCodes.ENTITY, c.getRenewalRef(), AuditAction.REOPEN, text);
    List<String> owners = new ArrayList<>();
    owners.add(c.getAssignedAo());
    notices.users(
        owners,
        RenewalCodes.EVENT_RETURNED,
        c,
        new RenewalNotices.Text(
            c.getRenewalRef() + " re-opened", "Give a new disposition: " + text));
    if (c.getOwnerUnit() != null) {
      notices.teamLeaders(
          companyId,
          c.getOwnerUnit(),
          RenewalCodes.EVENT_RETURNED,
          c,
          new RenewalNotices.Text(c.getRenewalRef() + " re-opened", text));
    }
    return c;
  }

  private void requireDisposer(RenewalCandidate c) {
    boolean assigned = CurrentUser.sameUser(c.getAssignedAo(), currentUser.username());
    boolean leader = currentUser.hasAuthority(RenewalCodes.REVIEW);
    if (!assigned && !leader) {
      throw new BusinessRuleException(
          "RNW_NOT_ASSIGNED", "Renewal " + c.getRenewalRef() + " is not assigned to you");
    }
  }

  private String reason(Input input) {
    String reason =
        input.reasonCode() == null || input.reasonCode().isBlank()
            ? null
            : input.reasonCode().strip();
    if (input.code() != RenewalDisposition.NOT_FOR_RENEWAL) {
      return null;
    }
    if (reason == null) {
      throw new BusinessRuleException(
          "RNW_NONRENEWAL_REASON", "Select the reason for Not for Renewal");
    }
    lovs.requireValid(RenewalCodes.LOV_NONRENEWAL_REASON, reason, BusinessClock.today(clock));
    return reason;
  }

  private String newInvoice(String reason, String invoiceNo) {
    if (reason == null || !parameters.invoiceNoReason(reason)) {
      return null;
    }
    String value = invoiceNo == null ? "" : invoiceNo.strip();
    if (value.isEmpty()) {
      throw new BusinessRuleException("RNW_NEW_INVOICE", "Enter the new invoice number");
    }
    if (!invoices.existsByInvoiceNo(value)) {
      throw new BusinessRuleException("RNW_NEW_INVOICE", "Invoice " + value + " does not exist");
    }
    return value;
  }

  private List<String> missing(CurrentDisposition d) {
    List<String> missing = new ArrayList<>();
    if (d == null || d.code() == null) {
      missing.add("Disposition");
      return missing;
    }
    if (d.code() == RenewalDisposition.NOT_FOR_RENEWAL && d.reasonCode() == null) {
      missing.add("Reason for Not for Renewal");
    }
    boolean invoiceNeeded = d.reasonCode() != null && parameters.invoiceNoReason(d.reasonCode());
    if (invoiceNeeded && d.newInvoiceNo() == null) {
      missing.add("New invoice no.");
    }
    return missing;
  }

  private static String optionalText(String remarks) {
    return Optional.ofNullable(remarks)
        .filter(r -> !r.isBlank())
        .map(r -> RemarkService.requireText(r, ""))
        .orElse(null);
  }

  /**
   * A disposition.
   *
   * @param code disposition
   * @param reasonCode reason for Not for Renewal (list RNW_NONRENEWAL_REASON)
   * @param newInvoiceNo new invoice number (Booked to New Invoice)
   * @param receivingUnit receiving unit (Transfer to Another Marketing Unit)
   * @param remarks remarks
   */
  public record Input(
      RenewalDisposition code,
      String reasonCode,
      String newInvoiceNo,
      String receivingUnit,
      String remarks) {}
}
