package com.iortatechnxt.brokerverse.screening.str.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent.EventFacts;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseAccess;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseCodes;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseMover;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseTimeline;
import com.iortatechnxt.brokerverse.screening.str.domain.StrRepository;
import com.iortatechnxt.brokerverse.screening.str.domain.StrStatus;
import com.iortatechnxt.brokerverse.screening.str.domain.SuspiciousTransactionReport;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records the AMLC filing of an extracted STR (SNSRP-706; FR-SS-072): filing on the AMLC portal
 * stays manual; the Compliance Officer records the unique AMLC reference and the filing date (not
 * before the extraction, not in the future), the STR becomes FILED and the case closes (action
 * {@code filed}).
 */
@Service
@Transactional
public class StrFilingService {

  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
  private static final int MAX_REFERENCE = 60;

  private final StrRepository strs;
  private final ScreeningCaseRepository cases;
  private final CaseAccess access;
  private final CaseMover mover;
  private final CaseTimeline timeline;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param strs STRs
   * @param cases cases
   * @param access case access
   * @param mover case transitions
   * @param timeline case timeline
   * @param audit audit trail
   * @param clock clock
   */
  public StrFilingService(
      StrRepository strs,
      ScreeningCaseRepository cases,
      CaseAccess access,
      CaseMover mover,
      CaseTimeline timeline,
      AuditTrailService audit,
      Clock clock) {
    this.strs = strs;
    this.cases = cases;
    this.access = access;
    this.mover = mover;
    this.timeline = timeline;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Records the filing and closes the case.
   *
   * @param strId the STR
   * @param reference the AMLC reference
   * @param filedOn the filing date
   * @return the STR
   */
  public SuspiciousTransactionReport file(Long strId, String reference, LocalDate filedOn) {
    SuspiciousTransactionReport str =
        strs.findById(strId).orElseThrow(() -> new ResourceNotFoundException("STR", strId));
    ScreeningCase c =
        cases
            .findById(str.getCaseId())
            .orElseThrow(() -> new ResourceNotFoundException(CaseCodes.ENTITY, str.getCaseId()));
    access.requireActor(c, "record the filing", EnumSet.of(CaseStage.STR_EXTRACTION));
    if (str.getStatus() != StrStatus.EXTRACTED) {
      throw new BusinessRuleException(
          "SCR_STR_NOT_EXTRACTED",
          "STR " + str.getStrNo() + " is " + str.getStatus() + ", not extracted");
    }
    String ref = requireReference(reference);
    requireDate(str, filedOn);
    if (strs.existsByAmlcReferenceIgnoreCase(ref)) {
      throw new BusinessRuleException(
          "SCR_AMLC_REFERENCE_USED",
          "The AMLC reference " + ref + " is already recorded on another STR");
    }
    str.filed(ref, filedOn, access.user());
    mover.act(c, "filed", TransitionNote.comment("AMLC " + ref + " filed " + filedOn));
    timeline.record(
        c,
        CaseEventType.FILED,
        EventFacts.move(
            CaseStage.STR_EXTRACTION.name(),
            CaseStage.CLOSED.name(),
            null,
            "AMLC " + ref + " filed " + filedOn));
    audit.record(
        CaseCodes.ENTITY,
        c.getCaseNo(),
        AuditAction.CLOSE,
        "STR " + str.getStrNo() + " filed, AMLC " + ref);
    return str;
  }

  private static String requireReference(String reference) {
    if (reference == null || reference.isBlank()) {
      throw new BusinessRuleException("SCR_AMLC_REFERENCE_REQUIRED", "Enter the AMLC reference");
    }
    String ref = reference.strip();
    if (ref.length() > MAX_REFERENCE) {
      throw new BusinessRuleException(
          "SCR_AMLC_REFERENCE_TOO_LONG", "The AMLC reference is limited to 60 characters");
    }
    return ref;
  }

  private void requireDate(SuspiciousTransactionReport str, LocalDate filedOn) {
    if (filedOn == null) {
      throw new BusinessRuleException("SCR_FILING_DATE_REQUIRED", "Enter the filing date");
    }
    LocalDate extracted = LocalDate.ofInstant(str.getExtractedAt(), MANILA);
    if (filedOn.isBefore(extracted)) {
      throw new BusinessRuleException(
          "SCR_FILING_BEFORE_EXTRACTION", "The filing date cannot be before the extraction date");
    }
    if (filedOn.isAfter(LocalDate.now(clock.withZone(MANILA)))) {
      throw new BusinessRuleException(
          "SCR_FILING_DATE_FUTURE", "The filing date cannot be in the future");
    }
  }
}
