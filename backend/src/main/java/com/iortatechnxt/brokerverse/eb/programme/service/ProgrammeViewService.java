package com.iortatechnxt.brokerverse.eb.programme.service;

import com.iortatechnxt.brokerverse.eb.domain.EbActivity;
import com.iortatechnxt.brokerverse.eb.domain.EbActivityRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbBor;
import com.iortatechnxt.brokerverse.eb.domain.EbBorRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbBorStatus;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbRenewalAdvice;
import com.iortatechnxt.brokerverse.eb.domain.EbRenewalAdviceRepository;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads a programme page (design 10.1): the programme with lines, contacts, cycles, renewal advices
 * and BOR status, and its activity log (History tab; TAT stamps, BRID-022, 024).
 */
@Service
@Transactional(readOnly = true)
public class ProgrammeViewService {

  private final EbRecords records;
  private final EbCycleRepository cycles;
  private final EbRenewalAdviceRepository advices;
  private final EbBorRepository bors;
  private final EbActivityRepository activities;

  /**
   * Creates the service.
   *
   * @param records programme look-up
   * @param cycles cycles
   * @param advices renewal advices
   * @param bors BOR versions
   * @param activities activity stamps
   */
  public ProgrammeViewService(
      EbRecords records,
      EbCycleRepository cycles,
      EbRenewalAdviceRepository advices,
      EbBorRepository bors,
      EbActivityRepository activities) {
    this.records = records;
    this.cycles = cycles;
    this.advices = advices;
    this.bors = bors;
    this.activities = activities;
  }

  /**
   * The programme page.
   *
   * @param companyId company
   * @param programmeId programme
   * @return the view
   */
  public ProgrammeView view(Long companyId, Long programmeId) {
    EbProgramme programme = records.programme(companyId, programmeId);
    List<EbCycle> all = cycles.findByProgrammeIdOrderByPolicyYearDescIdDesc(programme.getId());
    Map<Long, EbRenewalAdvice> sent =
        advices.findByProgrammeIdOrderBySentAtDesc(programme.getId()).stream()
            .collect(
                Collectors.toMap(EbRenewalAdvice::getCycleId, Function.identity(), (a, b) -> a));
    Map<Long, EbBorStatus> borStatus = new HashMap<>();
    for (EbBor bor : bors.findByProgrammeIdOrderByIdDesc(programme.getId())) {
      borStatus.putIfAbsent(bor.getCycleId(), bor.getStatus());
    }
    return ProgrammeView.of(programme, all, sent, borStatus);
  }

  /**
   * The activity log of a programme, newest first.
   *
   * @param companyId company
   * @param programmeId programme
   * @return stamps
   */
  public List<ActivityRow> activity(Long companyId, Long programmeId) {
    EbProgramme programme = records.programme(companyId, programmeId);
    Map<Long, String> cycleNos =
        cycles.findByProgrammeIdOrderByPolicyYearDescIdDesc(programme.getId()).stream()
            .collect(Collectors.toMap(EbCycle::getId, EbCycle::getCycleNo));
    List<EbActivity> stamps =
        activities.findByProgrammeIdOrderByReceivedAtAscIdAsc(programme.getId());
    return stamps.reversed().stream()
        .map(
            a ->
                new ActivityRow(
                    a.getId(),
                    a.getCycleId() == null ? null : cycleNos.get(a.getCycleId()),
                    a.getActivity().name(),
                    a.getReference(),
                    a.getReceivedAt(),
                    a.getReleasedAt(),
                    a.getActor(),
                    a.getRemarks()))
        .toList();
  }

  /**
   * An activity stamp as listed.
   *
   * @param id stamp
   * @param cycleNo cycle number, null for a programme activity
   * @param activity activity of the TAT annex
   * @param reference business reference
   * @param receivedAt received
   * @param releasedAt released, null while open
   * @param actor who performs it
   * @param remarks remarks
   */
  public record ActivityRow(
      Long id,
      String cycleNo,
      String activity,
      String reference,
      Instant receivedAt,
      Instant releasedAt,
      String actor,
      String remarks) {}
}
