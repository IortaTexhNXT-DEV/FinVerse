package com.iortatechnxt.brokerverse.eb.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertCheck;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbComparative;
import com.iortatechnxt.brokerverse.eb.domain.EbComparativeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequestRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbInsurerRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbInsurerRequestRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbSoa;
import com.iortatechnxt.brokerverse.eb.domain.EbSoaRepository;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily EB checks (design 8.2; FR-EB-062): franchise decisions overdue ({@code
 * EB_FRANCHISE_OVERDUE}), franchise outcomes not advised to the client in time ({@code
 * EB_FRANCHISE_ADVICE_LATE}), insurer requests past due ({@code EB_PROPOSAL_OVERDUE}), comparatives
 * not presented by their due date ({@code EB_COMPARATIVE_LATE}) and SOAs not validated within
 * {@code EB_TAT_SOA_VALIDATION} working days ({@code EB_SOA_VALIDATION_LATE}). Each alert is
 * de-duplicated per record.
 */
@Component
public class EbAlertCheck implements AlertCheck {

  private final EbFranchiseRequestRepository franchises;
  private final EbInsurerRequestRepository requests;
  private final EbComparativeRepository comparatives;
  private final EbSoaRepository soas;
  private final EbParameters parameters;
  private final EbWorkingDays workingDays;

  /**
   * Creates the check.
   *
   * @param franchises franchise requests
   * @param requests insurer requests
   * @param comparatives comparatives
   * @param soas SOAs
   * @param parameters SOA validation TAT
   * @param workingDays working-day calendars
   */
  public EbAlertCheck(
      EbFranchiseRequestRepository franchises,
      EbInsurerRequestRepository requests,
      EbComparativeRepository comparatives,
      EbSoaRepository soas,
      EbParameters parameters,
      EbWorkingDays workingDays) {
    this.franchises = franchises;
    this.requests = requests;
    this.comparatives = comparatives;
    this.soas = soas;
    this.parameters = parameters;
    this.workingDays = workingDays;
  }

  @Override
  @Transactional(readOnly = true)
  public List<AlertSignal> evaluate(LocalDate asOf) {
    List<AlertSignal> signals = new ArrayList<>();
    for (EbFranchiseRequest f :
        franchises.findByStatusAndDueDateBeforeOrderByDueDateAscIdAsc(
            EbFranchiseRequest.Status.SUBMITTED, asOf)) {
      signals.add(
          signal(
              EbCodes.ALERT_FRANCHISE_OVERDUE,
              f.getCompanyId(),
              EbCodes.ENTITY_FRANCHISE,
              f.getFranchiseNo(),
              "Franchise request "
                  + f.getFranchiseNo()
                  + " to "
                  + f.getInsurerCode()
                  + " has no decision since "
                  + f.getDueDate()));
    }
    for (EbFranchiseRequest f :
        franchises.findByStatusInAndAdviceDueDateBeforeOrderByAdviceDueDateAscIdAsc(
            EnumSet.of(EbFranchiseRequest.Status.APPROVED, EbFranchiseRequest.Status.REJECTED),
            asOf)) {
      signals.add(
          signal(
              EbCodes.ALERT_FRANCHISE_ADVICE_LATE,
              f.getCompanyId(),
              EbCodes.ENTITY_FRANCHISE,
              f.getFranchiseNo(),
              "The client is not yet advised of franchise "
                  + f.getFranchiseNo()
                  + ", due "
                  + f.getAdviceDueDate()));
    }
    for (EbInsurerRequest r :
        requests.findByStatusAndDueDateBeforeOrderByDueDateAscIdAsc(
            EbInsurerRequest.Status.OPEN, asOf)) {
      signals.add(
          signal(
              EbCodes.ALERT_PROPOSAL_OVERDUE,
              r.getCompanyId(),
              EbCodes.ENTITY_REQUEST,
              r.getRequestNo(),
              "No proposal from "
                  + r.getInsurerCode()
                  + " on request "
                  + r.getRequestNo()
                  + ", due "
                  + r.getDueDate()));
    }
    comparatives(asOf, signals);
    soas(asOf, signals);
    return signals;
  }

  private void comparatives(LocalDate asOf, List<AlertSignal> signals) {
    for (EbComparative c :
        comparatives.findByStatusIn(
            EnumSet.of(
                EbComparative.Status.DRAFT,
                EbComparative.Status.FOR_APPROVAL,
                EbComparative.Status.THRESHOLD_APPROVAL,
                EbComparative.Status.APPROVED))) {
      if (c.getDueDate() != null && c.getDueDate().isBefore(asOf)) {
        signals.add(
            signal(
                EbCodes.ALERT_COMPARATIVE_LATE,
                c.getCompanyId(),
                EbCodes.ENTITY_COMPARATIVE,
                c.getComparativeNo(),
                "Comparative "
                    + c.getComparativeNo()
                    + " was due to the client on "
                    + c.getDueDate()));
      }
    }
  }

  private void soas(LocalDate asOf, List<AlertSignal> signals) {
    int tat = parameters.tatDays(TatActivity.SOA_VALIDATION);
    for (EbSoa s : soas.findByStatusOrderByReceivedOnAscIdAsc(EbSoa.Status.RECEIVED)) {
      LocalDate due = workingDays.plus(s.getCompanyId(), s.getReceivedOn(), tat);
      if (due.isBefore(asOf)) {
        signals.add(
            signal(
                EbCodes.ALERT_SOA_VALIDATION_LATE,
                s.getCompanyId(),
                EbCodes.ENTITY_SOA,
                s.getSoaNo(),
                "SOA "
                    + s.getSoaNo()
                    + " ("
                    + s.getInsurerSoaNo()
                    + ") received on "
                    + s.getReceivedOn()
                    + " is not validated"));
      }
    }
  }

  private static AlertSignal signal(
      String code, Long companyId, String entityType, String reference, String message) {
    return new AlertSignal(
        code,
        new AlertFacts(
            companyId, null, entityType, reference, message, null, code + ":" + reference));
  }
}
