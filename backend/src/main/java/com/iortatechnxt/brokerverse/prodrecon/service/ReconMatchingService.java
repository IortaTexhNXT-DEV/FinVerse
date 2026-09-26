package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycle;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.MatchMethod;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem.InsurerRow;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItemRepository;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconSide;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconMatcher.Field;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The matching engine of production reconciliation (PRCID.022-027/030): pairs each insurer line
 * with a booked invoice of the cycle on the configured keys, compares the eight criteria within the
 * tolerance, looks insurer-only production up in the ledger (booked since the extract) and among
 * the pre-booked accounts (PRCID.023), re-runs on demand or by job (PRCID.025), and lets the
 * handler pair or split items by hand.
 */
@Service
@Transactional
public class ReconMatchingService {

  private static final String ENTITY = "ReconItem";

  private final ReconItemRepository items;
  private final ReconCycleService cycles;
  private final InvoiceLedgerQueryService ledger;
  private final AccountQueryService accounts;
  private final ReconSettings settings;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param items items
   * @param cycles cycles
   * @param ledger Operations ledger
   * @param accounts accounts (pre-booked look-up)
   * @param settings tolerance and keys
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ReconMatchingService(
      ReconItemRepository items,
      ReconCycleService cycles,
      InvoiceLedgerQueryService ledger,
      AccountQueryService accounts,
      ReconSettings settings,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.items = items;
    this.cycles = cycles;
    this.ledger = ledger;
    this.accounts = accounts;
    this.settings = settings;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Takes one insurer line into its cycle (PRCID.009/022): paired with the booked item it matches,
   * else with an invoice booked since the extract, else kept as insurer-only production.
   *
   * @param cycleId cycle
   * @param row insurer line
   * @return the item's id and status
   */
  public String addInsurerLine(Long cycleId, InsurerRow row) {
    ReconCycle cycle = cycles.requireOpen(cycleId);
    List<ReconMatcher.MatchKey> keys = settings.keys();
    Optional<ReconItem> booked =
        items.findByCycleIdOrderByIdAsc(cycleId).stream()
            .filter(i -> i.getInvoiceNo() != null && i.getInsurer() == null)
            .filter(i -> ReconMatcher.pairs(keys, i.getBdoi(), row.side()))
            .findFirst();
    ReconItem item;
    if (booked.isPresent()) {
      item = booked.get();
      item.attachInsurer(withExtractFlag(row, item.getExtractLineId() != null));
      compare(item, MatchMethod.AUTO);
    } else {
      item = items.save(ReconItem.insurerOnly(cycleId, withExtractFlag(row, false)));
      resolve(cycle, item);
    }
    return item.getStatus() + " #" + item.getId();
  }

  private static InsurerRow withExtractFlag(InsurerRow row, boolean inExtract) {
    return new InsurerRow(
        row.uploadId(), row.rowNo(), row.side(), row.incentive(), row.remarks(), inExtract);
  }

  /**
   * Looks insurer-only production up again (PRCID.023/024): an invoice of the insurer booked since
   * (not yet an item of the cycle) pairs with it; otherwise a pre-booked account classifies it.
   *
   * @param cycle cycle
   * @param item insurer-only item
   */
  void resolve(ReconCycle cycle, ReconItem item) {
    Optional<OpsInvoice> invoice = bookedInvoice(cycle, item.getInsurer());
    if (invoice.isPresent()) {
      item.attachBdoi(ReconFacts.of(invoice.get(), null));
      compare(item, MatchMethod.AUTO);
      return;
    }
    item.unbooked(prebookedArn(cycle.getCompanyId(), item.getInsurer()));
  }

  private Optional<OpsInvoice> bookedInvoice(ReconCycle cycle, ReconSide side) {
    return Stream.of(side.referenceNo())
        .filter(Objects::nonNull)
        .map(String::strip)
        .map(ledger::find)
        .flatMap(Optional::stream)
        .filter(i -> i.getInsurerCode().equals(cycle.getInsurerCode()))
        .filter(i -> items.findByCycleIdAndInvoiceNo(cycle.getId(), i.getInvoiceNo()).isEmpty())
        .findFirst();
  }

  private String prebookedArn(Long companyId, ReconSide side) {
    return Stream.of(side.referenceNo(), side.policyNo(), side.pnNo())
        .filter(Objects::nonNull)
        .map(ref -> accounts.preBooked(companyId, ref))
        .flatMap(List::stream)
        .map(Account::getArn)
        .findFirst()
        .orElse(null);
  }

  private void compare(ReconItem item, MatchMethod method) {
    List<Field> differing =
        ReconMatcher.differences(item.getBdoi(), item.getInsurer(), settings.tolerance());
    item.paired(
        differing.stream().map(Field::name).toList(),
        method,
        clock.instant(),
        method == MatchMethod.AUTO ? CurrentUser.SYSTEM : currentUser.username());
  }

  /**
   * Pairs insurer-only production waiting for a newly booked invoice (PRCID.024).
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param invoiceNo new invoice
   * @param policyNo its policy number, may be null
   * @return items paired
   */
  public int invoiceBooked(Long companyId, String insurerCode, String invoiceNo, String policyNo) {
    Optional<OpsInvoice> invoice = ledger.find(invoiceNo);
    if (invoice.isEmpty()) {
      return 0;
    }
    int paired = 0;
    for (ReconItem item : items.waitingFor(companyId, insurerCode, invoiceNo, policyNo)) {
      if (items.findByCycleIdAndInvoiceNo(item.getCycleId(), invoiceNo).isEmpty()) {
        item.attachBdoi(ReconFacts.of(invoice.get(), null));
        compare(item, MatchMethod.AUTO);
        items.flush();
        paired++;
      }
    }
    return paired;
  }

  /**
   * Re-runs the matching of a cycle (PRCID.025): paired items are compared again with the current
   * tolerance (manual pairs are kept), insurer-only production is looked up again.
   *
   * @param cycleId cycle
   * @return items whose status changed
   */
  public int rematch(Long cycleId) {
    ReconCycle cycle = cycles.requireOpen(cycleId);
    int changed = 0;
    for (ReconItem item : items.findByCycleIdOrderByIdAsc(cycleId)) {
      var before = item.getStatus();
      if (item.isPaired() && item.getMatchMethod() != MatchMethod.MANUAL) {
        compare(item, MatchMethod.AUTO);
      } else if (item.getStatus().isInsurerOnly()) {
        resolve(cycle, item);
      }
      if (before != item.getStatus()) {
        changed++;
      }
    }
    cycle.matched(clock.instant());
    audit.record(
        ReconCycleService.ENTITY,
        cycle.getCycleNo(),
        AuditAction.RUN,
        "Automatch: " + changed + " item(s) changed");
    cycles.closeWhenSettled(cycle);
    return changed;
  }

  /**
   * Pairs an insurer-only item with a booked item of the same cycle by hand.
   *
   * @param bookedItemId item with the booked invoice and no insurer line
   * @param insurerItemId insurer-only item
   * @return the paired item
   */
  public ReconItem pair(Long bookedItemId, Long insurerItemId) {
    ReconItem booked = require(bookedItemId);
    ReconItem insurer = require(insurerItemId);
    cycles.requireOpen(booked.getCycleId());
    if (!booked.getCycleId().equals(insurer.getCycleId())
        || booked.getInvoiceNo() == null
        || booked.getInsurer() != null
        || insurer.getInvoiceNo() != null) {
      throw new BusinessRuleException(
          "RECON_PAIR_INVALID",
          "Pair a BDOI-only item with an insurer-only item of the same cycle");
    }
    booked.attachInsurer(
        new InsurerRow(
            insurer.getUploadId(),
            insurer.getInsRowNo() == null ? 0 : insurer.getInsRowNo(),
            insurer.getInsurer(),
            insurer.getInsurerIncentive(),
            insurer.getInsurerRemarks(),
            booked.getExtractLineId() != null));
    compare(booked, MatchMethod.MANUAL);
    items.delete(insurer);
    audit.record(
        ENTITY,
        booked.getId(),
        AuditAction.UPDATE,
        "Paired by hand with insurer line " + insurer.getInsurer().referenceNo());
    return booked;
  }

  /**
   * Splits a paired item: the insurer line becomes insurer-only production again.
   *
   * @param itemId paired item
   * @return the booked item, BDOI only again
   */
  public ReconItem split(Long itemId) {
    ReconItem item = require(itemId);
    ReconCycle cycle = cycles.requireOpen(item.getCycleId());
    if (!item.isPaired()) {
      throw new BusinessRuleException("RECON_SPLIT_INVALID", "Only a paired item can be split");
    }
    ReconItem insurer =
        items.save(
            ReconItem.insurerOnly(
                item.getCycleId(),
                new InsurerRow(
                    item.getUploadId(),
                    item.getInsRowNo() == null ? 0 : item.getInsRowNo(),
                    item.getInsurer(),
                    item.getInsurerIncentive(),
                    item.getInsurerRemarks(),
                    false)));
    insurer.unbooked(prebookedArn(cycle.getCompanyId(), insurer.getInsurer()));
    item.detachInsurer();
    audit.record(ENTITY, item.getId(), AuditAction.UPDATE, "Split from its insurer line");
    return item;
  }

  private ReconItem require(Long id) {
    return items.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }
}
