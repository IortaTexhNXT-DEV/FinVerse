package com.iortatechnxt.brokerverse.submitted.fee.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.UnappliedDispositionChanged;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedFilter;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedView;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests.Action;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests.DispositionRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests.DispositionTicket;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHandlingFee;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHandlingFeeRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handling fees (BRIDSP-31; FRS FR-SP-070; design section 3.6): the billed records, the tagger that
 * matches the open unapplied payments - CLPC payments on the PN, OTC payments on the location
 * reference - against the billed records and asks Cashiering to recognise a matched payment as
 * handling-fee income ({@code RECOGNIZE_INCOME}, income type HANDLING_FEE), the manual tag of the
 * UPP handler, and the answer of Cashiering (APPLIED with the official receipt). A payment matching
 * several records is left for the UPP handler.
 */
@Service
@Transactional
public class HandlingFeeService {

  /** Requesting module on the disposition request. */
  public static final String SOURCE = "SUBMITTED";

  /** Income type of the request. */
  public static final String INCOME_TYPE = "HANDLING_FEE";

  private static final int PAGE = 500;
  private static final String CLPC = "CLPC";

  private final SbmHandlingFeeRepository fees;
  private final UnappliedDirectory directory;
  private final UnappliedDispositionRequests requests;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param fees handling fees
   * @param directory unapplied payments (Cashiering)
   * @param requests disposition requests (Cashiering)
   * @param numbers document numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public HandlingFeeService(
      SbmHandlingFeeRepository fees,
      UnappliedDirectory directory,
      UnappliedDispositionRequests requests,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.fees = fees;
    this.directory = directory;
    this.requests = requests;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records a billed handling fee.
   *
   * @param companyId company
   * @param bill policy, keys, amount and billing date
   * @param bulkJobNo upload, may be null
   * @return the record
   */
  public SbmHandlingFee bill(Long companyId, SbmHandlingFee.Bill bill, String bulkJobNo) {
    if (blank(bill.pnNo()) && blank(bill.locationRef())) {
      throw new BusinessRuleException(
          "SBM_FEE_KEY_REQUIRED", "Enter the PN number or the location reference");
    }
    if (bill.amount() == null || bill.amount().signum() <= 0) {
      throw new BusinessRuleException("SBM_FEE_AMOUNT", "The amount must be above zero");
    }
    SbmHandlingFee fee =
        fees.save(
            new SbmHandlingFee(
                companyId,
                numbers.next("SBF-" + BusinessClock.today(clock).getYear()),
                bill,
                bulkJobNo));
    audit.record("SubmittedHandlingFee", fee.getFeeNo(), AuditAction.CREATE, "Handling fee billed");
    return fee;
  }

  /**
   * The tagger of a company: matches the open unapplied payments with the billed records.
   *
   * @param companyId company
   * @return what the tagger did
   */
  public Tagging tag(Long companyId) {
    if (fees.countByCompanyIdAndStatus(companyId, SbmHandlingFee.BILLED) == 0) {
      return new Tagging(0, List.of());
    }
    int tagged = 0;
    List<String> ambiguous = new ArrayList<>();
    Page<UnappliedView> page;
    int n = 0;
    do {
      page = directory.open(companyId, UnappliedFilter.all(), PageRequest.of(n++, PAGE));
      for (UnappliedView item : page.getContent()) {
        List<SbmHandlingFee> matches = matches(companyId, item);
        if (matches.size() == 1) {
          request(matches.get(0), item);
          tagged++;
        } else if (matches.size() > 1) {
          ambiguous.add(item.unappliedRef());
        }
      }
    } while (page.hasNext());
    return new Tagging(tagged, ambiguous);
  }

  /**
   * Payments the tagger could not tag alone: open unapplied payments matching several records.
   *
   * @param companyId company
   * @return matches per payment
   */
  @Transactional(readOnly = true)
  public List<Ambiguous> ambiguous(Long companyId) {
    List<Ambiguous> out = new ArrayList<>();
    for (UnappliedView item :
        directory.open(companyId, UnappliedFilter.all(), PageRequest.of(0, PAGE)).getContent()) {
      List<SbmHandlingFee> matches = matches(companyId, item);
      if (matches.size() > 1) {
        out.add(new Ambiguous(item, matches));
      }
    }
    return out;
  }

  /**
   * Tags a payment by hand (UPP handler).
   *
   * @param feeId billed record
   * @param unappliedRef unapplied payment
   * @return the record
   */
  public SbmHandlingFee tagByHand(Long feeId, String unappliedRef) {
    SbmHandlingFee fee = get(feeId);
    UnappliedView item =
        directory
            .find(unappliedRef)
            .filter(i -> i.companyId().equals(fee.getCompanyId()))
            .orElseThrow(() -> new ResourceNotFoundException("Unapplied payment", unappliedRef));
    if (!fees.findByUnappliedRef(unappliedRef).stream()
        .filter(
            f ->
                !SbmHandlingFee.BILLED.equals(f.getStatus())
                    && !SbmHandlingFee.CANCELLED.equals(f.getStatus()))
        .toList()
        .isEmpty()) {
      throw new BusinessRuleException(
          "SBM_PAYMENT_TAGGED", "Payment " + unappliedRef + " is already tagged");
    }
    request(fee, item);
    return fee;
  }

  private List<SbmHandlingFee> matches(Long companyId, UnappliedView item) {
    String ref = item.reference();
    if (blank(ref)) {
      return List.of();
    }
    String key = ref.strip();
    return CLPC.equals(item.channel()) || CLPC.equals(item.paymentType())
        ? fees.findByCompanyIdAndPnNoAndStatus(companyId, key, SbmHandlingFee.BILLED)
        : fees.findByCompanyIdAndLocationRefAndStatus(companyId, key, SbmHandlingFee.BILLED);
  }

  private void request(SbmHandlingFee fee, UnappliedView item) {
    fee.tag(item.unappliedRef(), item.channel(), currentUser.username(), clock.instant());
    DispositionTicket ticket =
        requests.request(
            new DispositionRequest(
                fee.getCompanyId(),
                item.unappliedRef(),
                Action.RECOGNIZE_INCOME,
                null,
                fee.getAmount().min(item.balance()),
                currentUser.username(),
                SOURCE,
                "HF:" + fee.getFeeNo(),
                "Handling fee " + fee.getFeeNo(),
                INCOME_TYPE));
    if (ticket.status() == UnappliedDispositionRequests.Status.REJECTED) {
      fee.refused(ticket.message());
    } else {
      fee.ticket(ticket.reference(), ticket.message());
    }
    audit.record(
        "SubmittedHandlingFee",
        fee.getFeeNo(),
        AuditAction.UPDATE,
        "Tagged " + item.unappliedRef() + ": " + ticket.status());
  }

  /**
   * The answer of Cashiering: an executed request applies the record with its official receipt.
   *
   * @param event disposition change
   */
  public void answered(UnappliedDispositionChanged event) {
    if (!SOURCE.equals(event.source())
        || event.sourceRef() == null
        || !event.sourceRef().startsWith("HF:")) {
      return;
    }
    fees.findByFeeNo(event.sourceRef().substring(3))
        .ifPresent(
            fee -> {
              fee.ticket(event.cashieringRef(), event.message());
              if ("APPLIED".equals(event.status())) {
                fee.applied(event.documentNo(), clock.instant());
              } else if ("REJECTED".equals(event.status())) {
                fee.refused(event.message());
              }
            });
  }

  /**
   * Cancels a billed record.
   *
   * @param id record
   * @param reason reason
   * @return the record
   */
  public SbmHandlingFee cancel(Long id, String reason) {
    if (blank(reason)) {
      throw new BusinessRuleException("SBM_REASON_REQUIRED", "Enter the reason");
    }
    SbmHandlingFee fee = get(id);
    fee.cancel(reason.strip());
    return fee;
  }

  /**
   * A record.
   *
   * @param id record
   * @return record
   */
  @Transactional(readOnly = true)
  public SbmHandlingFee get(Long id) {
    return fees.findById(id).orElseThrow(() -> new ResourceNotFoundException("Handling fee", id));
  }

  /**
   * Records in some statuses.
   *
   * @param companyId company
   * @param statuses statuses
   * @param pageable page
   * @return records
   */
  @Transactional(readOnly = true)
  public Page<SbmHandlingFee> list(Long companyId, List<String> statuses, Pageable pageable) {
    return fees.findByCompanyIdAndStatusInOrderByIdDesc(companyId, statuses, pageable);
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  /**
   * What the tagger did.
   *
   * @param tagged payments tagged
   * @param ambiguous payments matching several records
   */
  public record Tagging(int tagged, List<String> ambiguous) {

    /** Defensive copy. */
    public Tagging {
      ambiguous = List.copyOf(ambiguous);
    }
  }

  /**
   * A payment matching several records.
   *
   * @param payment unapplied payment
   * @param fees billed records it matches
   */
  public record Ambiguous(UnappliedView payment, List<SbmHandlingFee> fees) {

    /** Defensive copy. */
    public Ambiguous {
      fees = List.copyOf(fees);
    }
  }
}
