package com.iortatechnxt.brokerverse.renewal.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.renewal.domain.CurrentDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.Disposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDispositionRepository;
import org.springframework.stereotype.Component;

/**
 * Writes a disposition (BRD 2.004; RENEWAL_DESIGN section 4.3): a new append-only row that
 * supersedes the previous one, the current disposition on the candidate and the audit entry. Every
 * source - user, matrix, upload, insurer, LAMD, system check - goes through here, so the online and
 * offline histories are one (BRRN.018).
 */
@Component
public class RenewalDispositions {

  private final RenewalDispositionRepository dispositions;
  private final AuditTrailService audit;

  /**
   * Creates the writer.
   *
   * @param dispositions disposition history
   * @param audit audit trail
   */
  public RenewalDispositions(RenewalDispositionRepository dispositions, AuditTrailService audit) {
    this.dispositions = dispositions;
    this.audit = audit;
  }

  /**
   * Records a disposition.
   *
   * @param candidate candidate
   * @param disposition code, reason, source, remarks and new invoice number
   * @param matrixVersion matrix version of a matrix decision, else null
   * @param ruleId matrix rule of a matrix decision, else null
   * @return the row
   */
  public Disposition record(
      RenewalCandidate candidate,
      CurrentDisposition disposition,
      Integer matrixVersion,
      Long ruleId) {
    Disposition saved =
        dispositions.save(new Disposition(candidate.getId(), disposition, matrixVersion, ruleId));
    dispositions.findByCandidateIdOrderByIdDesc(candidate.getId()).stream()
        .filter(d -> !d.getId().equals(saved.getId()) && d.getSupersededBy() == null)
        .forEach(d -> d.supersede(saved.getId()));
    candidate.dispose(disposition);
    audit.record(
        RenewalCodes.ENTITY,
        candidate.getRenewalRef(),
        AuditAction.UPDATE,
        summary(disposition, matrixVersion));
    return saved;
  }

  /**
   * The audit summary of a disposition in words: "Disposition For Renewal (Unit Sold), by User,
   * decision matrix version 3".
   *
   * @param disposition the disposition given
   * @param matrixVersion version of the decision matrix that proposed it, may be null
   * @return summary
   */
  static String summary(CurrentDisposition disposition, Integer matrixVersion) {
    return "Disposition "
        + disposition.code().label()
        + (disposition.reasonCode() == null
            ? ""
            : " (" + DisplayFormat.label(disposition.reasonCode()) + ")")
        + ", by "
        + disposition.source().label()
        + (matrixVersion == null ? "" : ", decision matrix version " + matrixVersion);
  }
}
