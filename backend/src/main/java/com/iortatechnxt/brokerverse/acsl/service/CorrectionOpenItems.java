package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.acsl.domain.Correction;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionLine;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalLine;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.subledger.domain.ItemDirection;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItemValues;
import com.iortatechnxt.brokerverse.subledger.service.OpenItemService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Open items of a posted correction (ACSL 2.9.0, ACCOUNTING_DISBURSEMENT_DESIGN 6 row 18): every
 * party line on a control account records an open item of its party (debit line, debit item) in the
 * correction's transaction; the debit and credit items of the same party are then matched, so a
 * reversal and its re-post to the same party leave nothing open while a change of party moves the
 * balance from one party to the other.
 */
@Component
public class CorrectionOpenItems {

  /** Document type of the open items of a correction. */
  public static final String DOCUMENT_TYPE = "ACSL_CORRECTION";

  private final OpenItemService openItems;
  private final PartyService parties;
  private final CorrectionJournals journals;

  /**
   * Creates the helper.
   *
   * @param openItems open-item sub-ledger
   * @param parties parties
   * @param journals accounts
   */
  public CorrectionOpenItems(
      OpenItemService openItems, PartyService parties, CorrectionJournals journals) {
    this.openItems = openItems;
    this.parties = parties;
    this.journals = journals;
  }

  /**
   * Records and matches the open items of a posted correction.
   *
   * @param c correction
   * @param batch its posted journal
   * @param today document date
   * @return number of open items recorded
   */
  public int record(Correction c, JournalBatch batch, LocalDate today) {
    Map<String, List<OpenItem>> byParty = new LinkedHashMap<>();
    List<CorrectionLine> lines = c.getLines();
    List<JournalLine> posted = batch.getLines();
    int count = 0;
    for (int i = 0; i < lines.size(); i++) {
      CorrectionLine l = lines.get(i);
      if (l.getPartyCode() != null
          && journals.account(c.getCompanyId(), l.getAccountCode()).isControlAccount()) {
        OpenItem item = openItems.record(values(c, l, posted.get(i), batch, today));
        byParty.computeIfAbsent(l.getPartyCode(), k -> new ArrayList<>()).add(item);
        count++;
      }
    }
    byParty.values().forEach(items -> match(items, today));
    return count;
  }

  private OpenItemValues values(
      Correction c, CorrectionLine l, JournalLine posted, JournalBatch batch, LocalDate today) {
    Party party = party(c.getCompanyId(), l.getPartyCode());
    return new OpenItemValues(
        c.getCompanyId(),
        c.getBranchId(),
        party.getId(),
        party.getCode(),
        l.getSide() == BalanceSide.DEBIT ? ItemDirection.DEBIT : ItemDirection.CREDIT,
        DOCUMENT_TYPE,
        c.getCorrectionNo(),
        today,
        today,
        c.getCurrency(),
        l.getAmount(),
        posted.getBaseAmount(),
        Acsl.MODULE,
        "ACS:" + c.getCorrectionNo() + ":" + l.getLineNo(),
        batch.getBatchNo(),
        l.getNarration());
  }

  private void match(List<OpenItem> items, LocalDate today) {
    for (OpenItem debit : items) {
      if (debit.getDirection() != ItemDirection.DEBIT) {
        continue;
      }
      for (OpenItem credit : items) {
        boolean open = debit.outstanding().signum() > 0 && credit.outstanding().signum() > 0;
        if (credit.getDirection() == ItemDirection.CREDIT && open) {
          openItems.match(debit.getId(), credit.getId(), null, today);
        }
      }
    }
  }

  private Party party(Long companyId, String code) {
    try {
      return parties.getByCode(companyId, code);
    } catch (ResourceNotFoundException e) {
      throw new BusinessRuleException(
          "ACSL_PARTY_UNKNOWN", "Party " + code + " of a correction line is not maintained", e);
    }
  }
}
