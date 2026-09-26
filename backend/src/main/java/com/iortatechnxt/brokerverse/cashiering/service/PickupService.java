package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentChannel;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PickupStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentIntake;
import com.iortatechnxt.brokerverse.cashiering.domain.PickupRequest;
import com.iortatechnxt.brokerverse.cashiering.domain.PickupRequest.PickupDetails;
import com.iortatechnxt.brokerverse.cashiering.domain.PickupRequestRepository;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeResult;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeTarget;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.service.port.CollectionFeed;
import com.iortatechnxt.brokerverse.opsledger.service.port.FeedItem;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The check pick-up queue (CSHID.009, OQ01/OQ13): checks tagged "for check pick-up" by Collection
 * arrive through the {@code CollectionFeed} port (manual entry or upload until the Collection
 * interface exists), are filtered by pick-up date, and "Print ARs" allocates the AR numbers of the
 * requests due in one transaction; requests not yet due are left in the queue.
 */
@Service
@Transactional
public class PickupService {

  /** Collection feed of pick-up requests. */
  public static final String FEED = "COLLECTION_CHECK_PICKUP";

  private static final String ENTITY = "PickupRequest";
  private static final LocalDate EARLIEST = LocalDate.of(1900, 1, 1);
  private static final LocalDate LATEST = LocalDate.of(9999, 12, 31);

  private final PickupRequestRepository requests;
  private final PaymentIntakeService intake;
  private final CollectionFeed collection;
  private final CashieringSettings settings;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests pick-up requests
   * @param intake payment intake
   * @param collection Collection feed port
   * @param settings settings
   * @param audit audit trail
   * @param clock clock
   */
  public PickupService(
      PickupRequestRepository requests,
      PaymentIntakeService intake,
      CollectionFeed collection,
      CashieringSettings settings,
      AuditTrailService audit,
      Clock clock) {
    this.requests = requests;
    this.intake = intake;
    this.collection = collection;
    this.settings = settings;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Queues a request (manual entry of a Collection tag).
   *
   * @param companyId company
   * @param branchId branch that prints the AR
   * @param details request
   * @return the request
   */
  public PickupRequest create(Long companyId, Long branchId, PickupDetails details) {
    settings.branch(companyId, branchId);
    if (requests.existsByCompanyIdAndCollectionRef(companyId, details.collectionRef())) {
      throw new BusinessRuleException(
          "PICKUP_DUPLICATE",
          "Collection reference " + details.collectionRef() + " is already queued");
    }
    if (details.amount() == null || details.amount().signum() <= 0) {
      throw new BusinessRuleException("PICKUP_AMOUNT", "The check amount must be above zero");
    }
    PickupRequest saved =
        requests.save(new PickupRequest(companyId, branchId, details, clock.instant()));
    audit.record(
        ENTITY, saved.getCollectionRef(), AuditAction.CREATE, "Pick-up " + details.pickupDate());
    return saved;
  }

  /**
   * Pulls the pending requests of the Collection system (empty with the manual adapter).
   *
   * @param companyId company
   * @return requests queued
   */
  public int importPending(Long companyId) {
    int queued = 0;
    Long branch = settings.headOffice(companyId).getId();
    for (FeedItem item : collection.pending(companyId, FEED)) {
      if (!requests.existsByCompanyIdAndCollectionRef(companyId, item.key())) {
        create(companyId, branch, details(item.key(), item.fields()));
        queued++;
      }
    }
    return queued;
  }

  /**
   * Requests of the queue.
   *
   * @param companyId company
   * @param status status
   * @param from pick-up date from, null for open
   * @param to pick-up date to, null for open
   * @param pageable page
   * @return requests by pick-up date
   */
  @Transactional(readOnly = true)
  public Page<PickupRequest> list(
      Long companyId, PickupStatus status, LocalDate from, LocalDate to, Pageable pageable) {
    return requests.search(
        companyId, status, from == null ? EARLIEST : from, to == null ? LATEST : to, pageable);
  }

  /**
   * Prints the ARs of the selected requests that are due, in one transaction (CSHID.009).
   *
   * @param ids requests
   * @return receipt ids of the ARs issued
   */
  public List<Long> printArs(Collection<Long> ids) {
    LocalDate today = LocalDate.now(clock);
    List<Long> receipts = new ArrayList<>();
    for (PickupRequest r : requests.findByIdInOrderByIdAsc(ids)) {
      if (r.getStatus() != PickupStatus.FOR_PICKUP || r.getPickupDate().isAfter(today)) {
        continue;
      }
      IntakeResult result =
          intake.receive(
              new IntakeTarget(r.getCompanyId(), r.getBranchId(), "OTC", ReceiptSource.PICKUP),
              new PaymentIntake(
                  PaymentChannel.PICKUP,
                  r.getCollectionRef(),
                  r.getCollectionRef(),
                  null,
                  r.getReference(),
                  List.of(),
                  new PaymentIntake.Payor(r.getClientCode(), r.getPayorName()),
                  r.getAssuredName(),
                  new PaymentIntake.Money(r.getAmount(), r.getCurrency(), today),
                  new PaymentIntake.Tender(
                      PaymentMode.CHECK, r.getCheckNo(), r.getCheckBank(), null, false)));
      r.printed(result.payment().getId(), result.receipt().getReceiptNo(), clock.instant());
      receipts.add(result.receipt().getId());
    }
    if (receipts.isEmpty()) {
      throw new BusinessRuleException(
          "PICKUP_NOTHING_DUE", "None of the selected requests is due for printing");
    }
    return receipts;
  }

  /**
   * Cancels a queued request.
   *
   * @param id request
   * @return the request
   */
  public PickupRequest cancel(Long id) {
    PickupRequest r =
        requests.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    r.cancel();
    audit.record(ENTITY, r.getCollectionRef(), AuditAction.UPDATE, "Cancelled");
    return r;
  }

  /**
   * Request details from Collection fields.
   *
   * @param key Collection reference
   * @param f fields: reference, clientCode, payorName, pickupDate, amount, currency, checkNo,
   *     checkBank, requestor
   * @return details
   */
  static PickupDetails details(String key, Map<String, String> f) {
    try {
      return new PickupDetails(
          key,
          f.get("reference"),
          f.get("clientCode"),
          f.get("payorName"),
          f.get("assuredName"),
          LocalDate.parse(f.get("pickupDate")),
          f.getOrDefault("requestor", "Collection"),
          new BigDecimal(f.get("amount")),
          f.getOrDefault("currency", "PHP"),
          f.get("checkNo"),
          f.get("checkBank"));
    } catch (RuntimeException ex) {
      throw new BusinessRuleException(
          "PICKUP_RECORD_INVALID",
          "Pick-up record " + key + " is incomplete: " + ex.getMessage(),
          ex);
    }
  }
}
