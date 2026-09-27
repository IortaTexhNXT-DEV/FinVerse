package com.iortatechnxt.brokerverse.eb.home.service;

import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChange;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChangeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbSoa;
import com.iortatechnxt.brokerverse.eb.domain.EbSoaRepository;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeQuery;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeQuery.ProgrammeCriteria;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemQuery;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The counts of EB Home (design 10.1; FR-EB-062): renewal advices due, open cycles per work stage
 * (awaiting feedback, franchise, proposals, sign-off, threshold approval, with client) and pending
 * items past due, member changes open and SOAs to validate.
 */
@Service
@Transactional(readOnly = true)
public class EbHomeService {

  private final ProgrammeQuery programmes;
  private final EbCycleRepository cycles;
  private final TrackedItemQuery items;
  private final EbMemberChangeRepository changes;
  private final EbSoaRepository soas;

  /**
   * Creates the service.
   *
   * @param programmes programme work list
   * @param cycles cycles per stage
   * @param items pending items
   * @param changes member changes
   * @param soas SOAs
   */
  public EbHomeService(
      ProgrammeQuery programmes,
      EbCycleRepository cycles,
      TrackedItemQuery items,
      EbMemberChangeRepository changes,
      EbSoaRepository soas) {
    this.programmes = programmes;
    this.cycles = cycles;
    this.items = items;
    this.changes = changes;
    this.soas = soas;
  }

  /**
   * The tile counts of a company, by tile id.
   *
   * @param companyId company
   * @return counts
   */
  public Map<String, Long> counts(Long companyId) {
    Map<EbCycleStage, Long> stages = new EnumMap<>(EbCycleStage.class);
    List<Object[]> rows = cycles.countOpenByStage(companyId);
    for (Object[] row : rows) {
      stages.put((EbCycleStage) row[0], ((Number) row[1]).longValue());
    }
    Map<String, Long> counts = new LinkedHashMap<>();
    counts.put(
        "raDue",
        programmes.count(
            companyId,
            new ProgrammeCriteria(ProgrammeQuery.Tab.RENEWAL_DUE, null, null, null, null)));
    counts.put("awaitingFeedback", stages.getOrDefault(EbCycleStage.RA_SENT, 0L));
    counts.put("franchisePending", stages.getOrDefault(EbCycleStage.FRANCHISE, 0L));
    counts.put("proposalsOutstanding", stages.getOrDefault(EbCycleStage.PROPOSALS, 0L));
    counts.put("comparativesToSignOff", stages.getOrDefault(EbCycleStage.FOR_SIGNOFF, 0L));
    counts.put("thresholdApprovals", stages.getOrDefault(EbCycleStage.THRESHOLD_APPROVAL, 0L));
    counts.put("withClient", stages.getOrDefault(EbCycleStage.WITH_CLIENT, 0L));
    counts.put(
        "memberChangesOpen",
        changes.countByCompanyIdAndStatusIn(
            companyId,
            EnumSet.of(
                EbMemberChange.Status.CAPTURED,
                EbMemberChange.Status.RELAYED,
                EbMemberChange.Status.BILLED,
                EbMemberChange.Status.VALIDATED)));
    counts.put("soaToValidate", soas.countByCompanyIdAndStatus(companyId, EbSoa.Status.RECEIVED));
    counts.put("pendingItemsOverdue", items.overdue(companyId));
    return counts;
  }
}
