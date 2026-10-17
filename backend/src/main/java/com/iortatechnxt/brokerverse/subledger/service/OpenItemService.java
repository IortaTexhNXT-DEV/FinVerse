package com.iortatechnxt.brokerverse.subledger.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.subledger.domain.ItemDirection;
import com.iortatechnxt.brokerverse.subledger.domain.ItemMatch;
import com.iortatechnxt.brokerverse.subledger.domain.ItemMatchRepository;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItemRepository;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItemStatus;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItemValues;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Party sub-ledger: records open items, matches (knocks off) debits against credits and answers
 * outstanding-balance questions. Called by business modules in the same transaction as their GL
 * posting, so the sub-ledger always agrees with the control accounts.
 */
@Service
@Transactional
public class OpenItemService {

  private static final String ENTITY = "OpenItem";
  private static final Set<OpenItemStatus> UNSETTLED =
      EnumSet.of(OpenItemStatus.OPEN, OpenItemStatus.PARTIALLY_SETTLED);

  private final OpenItemRepository items;
  private final ItemMatchRepository matches;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param items item repository
   * @param matches match repository
   * @param audit audit trail
   */
  public OpenItemService(
      OpenItemRepository items, ItemMatchRepository matches, AuditTrailService audit) {
    this.items = items;
    this.matches = matches;
    this.audit = audit;
  }

  /**
   * Records a new open item.
   *
   * @param values item values (amounts positive)
   * @return saved item
   */
  public OpenItem record(OpenItemValues values) {
    if (values.amount().signum() <= 0 || values.baseAmount().signum() < 0) {
      throw new BusinessRuleException("INVALID_OPEN_ITEM", "Open item amounts must be positive");
    }
    if (values.sourceReference() != null
        && items.existsByCompanyIdAndSourceModuleAndSourceReference(
            values.companyId(), values.sourceModule(), values.sourceReference())) {
      throw new BusinessRuleException(
          "DUPLICATE_OPEN_ITEM", "Open item already recorded for " + values.sourceReference());
    }
    return items.save(new OpenItem(values));
  }

  /**
   * Gets an item.
   *
   * @param id id
   * @return item
   */
  @Transactional(readOnly = true)
  public OpenItem get(Long id) {
    return items.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Matches a DEBIT item against a CREDIT item of the same party and currency.
   *
   * @param debitItemId debit item
   * @param creditItemId credit item
   * @param amount amount to match (null = largest possible)
   * @param date matching date
   * @return match record
   */
  public ItemMatch match(Long debitItemId, Long creditItemId, BigDecimal amount, LocalDate date) {
    OpenItem debit = get(debitItemId);
    OpenItem credit = get(creditItemId);
    validatePair(debit, credit);
    BigDecimal value = amount != null ? amount : debit.outstanding().min(credit.outstanding());
    debit.settle(value);
    credit.settle(value);
    ItemMatch saved = matches.save(new ItemMatch(debit.getId(), credit.getId(), value, date));
    audit.record(
        ENTITY,
        debit.getDocumentNo(),
        AuditAction.UPDATE,
        "Matched " + value + " against " + credit.getDocumentNo());
    return saved;
  }

  /**
   * Undoes a match: both items get the matched amount back as outstanding and the match record is
   * removed. Used when the document that settled an item is cancelled (cancelled receipt, bounced
   * cheque). The audit trail keeps the history of the removed match.
   *
   * @param matchId match to undo
   * @param reason why the match is undone (audit)
   * @return the removed match
   */
  public ItemMatch unmatch(Long matchId, String reason) {
    ItemMatch match =
        matches
            .findById(matchId)
            .orElseThrow(() -> new ResourceNotFoundException("ItemMatch", matchId));
    OpenItem debit = get(match.getDebitItemId());
    OpenItem credit = get(match.getCreditItemId());
    debit.unsettle(match.getAmount());
    credit.unsettle(match.getAmount());
    matches.delete(match);
    audit.record(
        ENTITY,
        debit.getDocumentNo(),
        AuditAction.UPDATE,
        "Unmatched " + match.getAmount() + " against " + credit.getDocumentNo() + ": " + reason);
    return match;
  }

  /**
   * Undoes every match of an item (see {@link #unmatch(Long, String)}).
   *
   * @param itemId item whose matches are undone
   * @param reason why the matches are undone (audit)
   * @return the removed matches
   */
  public List<ItemMatch> unmatchAll(Long itemId, String reason) {
    return matchesOf(get(itemId)).stream().map(m -> unmatch(m.getId(), reason)).toList();
  }

  /**
   * Matches a CREDIT item against the party's oldest DEBIT items (first in, first out), or the
   * reverse for a DEBIT item. Used for automatic allocation of receipts and payments.
   *
   * @param itemId item to allocate
   * @param date matching date
   * @return matches created
   */
  public List<ItemMatch> allocateFifo(Long itemId, LocalDate date) {
    OpenItem source = get(itemId);
    List<OpenItem> targets =
        items
            .findByCompanyIdAndPartyIdOrderByDocumentDateAscIdAsc(
                source.getCompanyId(), source.getPartyId())
            .stream()
            .filter(i -> i.getDirection() == source.getDirection().opposite())
            .filter(i -> UNSETTLED.contains(i.getStatus()))
            .filter(i -> i.getCurrency().equals(source.getCurrency()))
            .sorted(Comparator.comparing(OpenItem::getDueDate))
            .toList();
    List<ItemMatch> created = new ArrayList<>();
    for (OpenItem target : targets) {
      if (source.outstanding().signum() == 0) {
        break;
      }
      BigDecimal value = source.outstanding().min(target.outstanding());
      boolean sourceIsDebit = source.getDirection() == ItemDirection.DEBIT;
      created.add(
          match(
              sourceIsDebit ? source.getId() : target.getId(),
              sourceIsDebit ? target.getId() : source.getId(),
              value,
              date));
    }
    return created;
  }

  /**
   * Lists a party's items (statement of account).
   *
   * @param companyId company
   * @param partyId party
   * @return items oldest first
   */
  @Transactional(readOnly = true)
  public List<OpenItem> partyItems(Long companyId, Long partyId) {
    return items.findByCompanyIdAndPartyIdOrderByDocumentDateAscIdAsc(companyId, partyId);
  }

  /**
   * Lists items not yet settled, documented on or before a date.
   *
   * @param companyId company
   * @param asOf date
   * @return outstanding items
   */
  @Transactional(readOnly = true)
  public List<OpenItem> outstanding(Long companyId, LocalDate asOf) {
    return items.findOutstanding(companyId, UNSETTLED, asOf);
  }

  /**
   * Lists the matches of an item.
   *
   * @param item item
   * @return matches
   */
  @Transactional(readOnly = true)
  public List<ItemMatch> matchesOf(OpenItem item) {
    return item.getDirection() == ItemDirection.DEBIT
        ? matches.findByDebitItemIdIn(List.of(item.getId()))
        : matches.findByCreditItemIdIn(List.of(item.getId()));
  }

  private static void validatePair(OpenItem debit, OpenItem credit) {
    if (debit.getDirection() != ItemDirection.DEBIT
        || credit.getDirection() != ItemDirection.CREDIT) {
      throw new BusinessRuleException(
          "INVALID_MATCH", "A debit item must be matched with a credit item");
    }
    if (!Objects.equals(debit.getPartyId(), credit.getPartyId())) {
      throw new BusinessRuleException("INVALID_MATCH", "Items belong to different parties");
    }
    if (!debit.getCurrency().equals(credit.getCurrency())) {
      throw new BusinessRuleException("INVALID_MATCH", "Items are in different currencies");
    }
  }
}
