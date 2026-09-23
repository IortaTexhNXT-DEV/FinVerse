package com.iortatechnxt.finverse.claims.service;

import com.iortatechnxt.finverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.finverse.accounting.service.BusinessEvent;
import com.iortatechnxt.finverse.claims.domain.Claim;
import com.iortatechnxt.finverse.claims.domain.CostType;
import com.iortatechnxt.finverse.claims.domain.EstimateLine;
import com.iortatechnxt.finverse.claims.domain.EstimateSide;
import com.iortatechnxt.finverse.claims.domain.MovementKind;
import com.iortatechnxt.finverse.claims.domain.MovementLine;
import com.iortatechnxt.finverse.claims.domain.MovementLineRepository;
import com.iortatechnxt.finverse.claims.domain.MovementSource;
import com.iortatechnxt.finverse.claims.domain.MovementValues;
import com.iortatechnxt.finverse.claims.domain.Recovery;
import com.iortatechnxt.finverse.claims.domain.ReserveChange;
import com.iortatechnxt.finverse.claims.domain.Settlement;
import com.iortatechnxt.finverse.claims.domain.ShareSplit;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItemValues;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accounts for approved claim documents inside the approval transaction, company share:
 *
 * <ul>
 *   <li>payment estimate change: CLAIM_RESERVE (RESERVE_CHANGE, signed: Dr change in claims
 *       reserves / Cr outstanding claims reserve);
 *   <li>settlement: CLAIM_SETTLEMENT (PAID_AMOUNT: Dr claims paid / Cr claims payable of the payee;
 *       RESERVE_RELEASE: Dr outstanding claims reserve / Cr change in claims reserves), a CREDIT
 *       open item {@value #SETTLEMENT_DOCUMENT} for the payee, paid by a payables payment voucher;
 *       when leading a coinsurance, CLAIM_COINSURANCE (COINSURER_SHARE: Dr due from coinsurers / Cr
 *       claims payable) and a DEBIT open item on the coinsurer;
 *   <li>recovery: CLAIM_RECOVERY (AMOUNT: Dr bank / Cr claims paid); when leading,
 *       CLAIM_COINSURANCE (COINSURER_RECOVERY: Dr bank / Cr due to coinsurers) and a CREDIT open
 *       item on the coinsurer.
 * </ul>
 *
 * Every document also becomes a {@link MovementLine}, and every listener is notified of it.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ClaimPostingService {

  /** Open item document type of claim settlements (contains CLAIM: payables pays it as CLAIM). */
  public static final String SETTLEMENT_DOCUMENT = "CLAIM_SETTLEMENT";

  /** Open item document type of the coinsurers' share of a settlement (recoverable). */
  public static final String COINSURER_SHARE_DOCUMENT = "COINSURER_CLAIM_SHARE";

  /** Open item document type of the coinsurers' share of a recovery (payable). */
  public static final String COINSURER_RECOVERY_DOCUMENT = "COINSURER_RECOVERY_SHARE";

  private static final String COINSURANCE_EVENT = "CLAIM_COINSURANCE";
  private static final String COINSURANCE_SUFFIX = ":COINS";
  private static final String BANK_ROLE = "BANK";
  private static final int MAX_NARRATION = 300;

  private final AccountingEventPublisher publisher;
  private final OpenItemService openItems;
  private final PartyService parties;
  private final MovementLineRepository lines;
  private final ClaimMovementNotifier notifier;
  private final ClaimSupport support;

  /**
   * Creates the service.
   *
   * @param publisher accounting engine
   * @param openItems party sub-ledger
   * @param parties coinsurer lookup
   * @param lines movement ledger
   * @param notifier kernel listeners
   * @param support rates
   */
  public ClaimPostingService(
      AccountingEventPublisher publisher,
      OpenItemService openItems,
      PartyService parties,
      MovementLineRepository lines,
      ClaimMovementNotifier notifier,
      ClaimSupport support) {
    this.publisher = publisher;
    this.openItems = openItems;
    this.parties = parties;
    this.lines = lines;
    this.notifier = notifier;
    this.support = support;
  }

  /**
   * Posts an approved estimate change (recovery estimates are memorandum: no journal).
   *
   * @param rc applied reserve change
   * @param date accounting date
   */
  public void postEstimate(ReserveChange rc, LocalDate date) {
    Claim c = rc.getClaim();
    if (rc.getChangeAmount().signum() == 0) {
      return;
    }
    String ref = reference(c, "RSV", rc.getId());
    String narration = narration(c, rc.label() + ": " + rc.getReason());
    BigDecimal ours = rc.getOurChange();
    if (rc.getSide() == EstimateSide.PAYMENT && ours.signum() != 0) {
      rc.recordJournal(
          publisher
              .publish(
                  event(
                      new EventSpec("CLAIM_RESERVE", ref, date, null, narration),
                      c,
                      Map.of("RESERVE_CHANGE", ours),
                      Map.of()))
              .getBatchNo());
    }
    record(
        new MovementValues(
            c,
            MovementKind.ESTIMATE,
            new EstimateLine(rc.getSide(), rc.getCostType(), rc.getChangeAmount()),
            ours,
            support.toBase(c, ours, date),
            date,
            MovementSource.RESERVE,
            rc.getId(),
            ref,
            rc.getJournalBatchNo(),
            narration));
  }

  /**
   * Posts an approved settlement and records the payee's payable.
   *
   * @param s settlement
   * @param split coinsurance split of the net amount
   * @param date accounting date
   */
  public void postSettlement(Settlement s, ShareSplit split, LocalDate date) {
    Claim c = s.getClaim();
    String ref = reference(c, "STL", s.getId());
    String narration = narration(c, s.label() + " to " + s.getPayee().getName());
    BigDecimal rate = support.rate(c, date);
    String batch =
        publisher
            .publish(
                event(
                    new EventSpec("CLAIM_SETTLEMENT", ref, date, s.getPayee().getCode(), narration),
                    c,
                    Map.of("PAID_AMOUNT", split.ours(), "RESERVE_RELEASE", split.ours()),
                    Map.of()))
            .getBatchNo();
    s.recordJournal(batch);
    openItems.record(
        item(
            c,
            new ItemSpec(
                s.getPayee(), ItemDirection.CREDIT, SETTLEMENT_DOCUMENT, s.getSettlementNo()),
            split.payable(),
            new PostingKey(date, rate, ref, batch, narration)));
    String coinsBatch = null;
    if (split.coinsurers().signum() != 0) {
      coinsBatch =
          postCoinsurance(
              c,
              new ItemSpec(
                  coinsurer(c), ItemDirection.DEBIT, COINSURER_SHARE_DOCUMENT, s.getSettlementNo()),
              Map.of("COINSURER_SHARE", split.coinsurers()),
              new PostingKey(date, rate, ref + COINSURANCE_SUFFIX, null, narration),
              Map.of());
    }
    s.recordSplit(split, rate, coinsBatch);
    record(
        new MovementValues(
            c,
            MovementKind.PAID,
            new EstimateLine(EstimateSide.PAYMENT, s.getCostType(), s.getNetAmount()),
            split.ours(),
            Money.convert(split.ours(), rate),
            date,
            MovementSource.SETTLEMENT,
            s.getId(),
            ref,
            batch,
            narration));
  }

  /**
   * Posts an approved recovery received into a bank account.
   *
   * @param r recovery
   * @param split coinsurance split of the amount received
   * @param date accounting date
   */
  public void postRecovery(Recovery r, ShareSplit split, LocalDate date) {
    Claim c = r.getClaim();
    String ref = reference(c, "REC", r.getId());
    String narration = narration(c, r.label() + " (" + r.getRecoveryType() + ")");
    BigDecimal rate = support.rate(c, date);
    Map<String, String> bank = Map.of(BANK_ROLE, r.getBankAccountCode());
    String batch =
        publisher
            .publish(
                event(
                    new EventSpec("CLAIM_RECOVERY", ref, date, null, narration),
                    c,
                    Map.of("AMOUNT", split.ours()),
                    bank))
            .getBatchNo();
    r.recordJournal(batch);
    String coinsBatch = null;
    if (split.coinsurers().signum() != 0) {
      coinsBatch =
          postCoinsurance(
              c,
              new ItemSpec(
                  coinsurer(c),
                  ItemDirection.CREDIT,
                  COINSURER_RECOVERY_DOCUMENT,
                  r.getRecoveryNo()),
              Map.of("COINSURER_RECOVERY", split.coinsurers()),
              new PostingKey(date, rate, ref + COINSURANCE_SUFFIX, null, narration),
              bank);
    }
    r.recordSplit(split, coinsBatch);
    record(
        new MovementValues(
            c,
            MovementKind.PAID,
            new EstimateLine(EstimateSide.RECOVERY, CostType.LOSS, r.getAmount()),
            split.ours(),
            Money.convert(split.ours(), rate),
            date,
            MovementSource.RECOVERY,
            r.getId(),
            ref,
            batch,
            narration));
  }

  /**
   * Stable, unique reference of a claim document movement.
   *
   * @param claim claim
   * @param kind RSV, STL or REC
   * @param documentId document id
   * @return e.g. {@code CLAIM:12:STL:5}
   */
  static String reference(Claim claim, String kind, Long documentId) {
    return "CLAIM:" + claim.getId() + ":" + kind + ":" + documentId;
  }

  private String postCoinsurance(
      Claim c,
      ItemSpec spec,
      Map<String, BigDecimal> amounts,
      PostingKey key,
      Map<String, String> accounts) {
    String batch =
        publisher
            .publish(
                event(
                    new EventSpec(
                        COINSURANCE_EVENT,
                        key.sourceReference(),
                        key.date(),
                        spec.party().getCode(),
                        key.narration()),
                    c,
                    amounts,
                    accounts))
            .getBatchNo();
    BigDecimal amount = amounts.values().iterator().next();
    openItems.record(
        item(
            c,
            spec,
            amount,
            new PostingKey(key.date(), key.rate(), key.sourceReference(), batch, key.narration())));
    return batch;
  }

  private void record(MovementValues values) {
    MovementLine line = lines.save(new MovementLine(values));
    notifier.publish(line);
  }

  private Party coinsurer(Claim c) {
    return parties.getByCode(c.getCompanyId(), c.getPolicy().getCoinsurerCode());
  }

  private static BusinessEvent event(
      EventSpec spec, Claim c, Map<String, BigDecimal> amounts, Map<String, String> accounts) {
    return new BusinessEvent(
        spec.eventType(),
        c.getCompanyId(),
        c.getBranchId(),
        spec.date(),
        c.getCurrency(),
        ClaimSupport.MODULE,
        spec.sourceReference(),
        c.getClaimNo(),
        spec.partyCode(),
        c.getPolicy().getBusinessLine(),
        null,
        spec.narration(),
        amounts,
        accounts);
  }

  private static OpenItemValues item(Claim c, ItemSpec spec, BigDecimal amount, PostingKey key) {
    Party party = spec.party();
    LocalDate due =
        spec.direction() == ItemDirection.CREDIT
            ? key.date()
            : key.date().plusDays(party.getCreditDays());
    return new OpenItemValues(
        c.getCompanyId(),
        c.getBranchId(),
        party.getId(),
        party.getCode(),
        spec.direction(),
        spec.documentType(),
        spec.documentNo(),
        key.date(),
        due,
        c.getCurrency(),
        amount,
        Money.convert(amount, key.rate()),
        ClaimSupport.MODULE,
        key.sourceReference(),
        key.batchNo(),
        key.narration());
  }

  private static String narration(Claim c, String text) {
    String full = "Claim " + c.getClaimNo() + ": " + text;
    return full.length() <= MAX_NARRATION ? full : full.substring(0, MAX_NARRATION);
  }

  /**
   * Accounting event header.
   *
   * @param eventType event type
   * @param sourceReference idempotency key
   * @param date value date
   * @param partyCode sub-ledger party, may be null
   * @param narration narration
   */
  private record EventSpec(
      String eventType,
      String sourceReference,
      LocalDate date,
      String partyCode,
      String narration) {}

  /**
   * Party side of an open item.
   *
   * @param party party
   * @param direction DEBIT (owes the company) or CREDIT (company owes)
   * @param documentType document type
   * @param documentNo document number
   */
  private record ItemSpec(
      Party party, ItemDirection direction, String documentType, String documentNo) {}

  /**
   * Posting facts shared by the open items of a document.
   *
   * @param date document date
   * @param rate exchange rate to base
   * @param sourceReference open item key
   * @param batchNo journal batch
   * @param narration narration
   */
  private record PostingKey(
      LocalDate date, BigDecimal rate, String sourceReference, String batchNo, String narration) {}
}
