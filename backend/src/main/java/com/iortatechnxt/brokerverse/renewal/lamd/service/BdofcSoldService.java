package com.iortatechnxt.brokerverse.renewal.lamd.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.rules.service.ReevaluationService;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BDOFC and BDOSOLD reports (FRRN.012.06 to FRRN.012.09): each line is saved and matched to the
 * open renewal accounts on the PN; a matched renewal is evaluated again by the sanitation rules
 * (check {@code BDOFC_SOLD}).
 */
@Service
@Transactional
public class BdofcSoldService {

  private static final String INSERT =
      "insert into rnw_bdofc_line (company_id, job_no, row_no, report_kind, location, pn_no,"
          + " assured_name, inception_date, expiry_date, remarks, candidate_id, matching_status,"
          + " processing_status, reason, created_at, created_by) values (:companyId, :jobNo,"
          + " :rowNo, :kind, :location, :pn, :assured, :inception, :expiry, :remarks,"
          + " :candidateId, :matching, 'SUCCESS', :reason, :at, :by)";

  private final LamdService lamd;
  private final ReevaluationService reevaluation;
  private final NamedParameterJdbcTemplate jdbc;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param lamd open renewals by PN
   * @param reevaluation checks
   * @param jdbc lines
   * @param audit audit trail
   * @param currentUser uploading user
   * @param clock clock
   */
  public BdofcSoldService(
      LamdService lamd,
      ReevaluationService reevaluation,
      NamedParameterJdbcTemplate jdbc,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.lamd = lamd;
    this.reevaluation = reevaluation;
    this.jdbc = jdbc;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Saves and matches one line.
   *
   * @param companyId company
   * @param jobNo upload
   * @param rowNo row of the file
   * @param kind BDOFC or BDOSOLD
   * @param line the line
   * @return outcome with the matching status as category
   */
  public BulkOutcome record(Long companyId, String jobNo, int rowNo, String kind, Line line) {
    List<RenewalCandidate> matches = lamd.openByPn(companyId, line.pn());
    RenewalCandidate c = matches.size() == 1 ? matches.get(0) : null;
    String reason =
        c == null
            ? (matches.isEmpty()
                ? "No open renewal account with PN " + line.pn()
                : "PN " + line.pn() + " matches several renewal accounts")
            : c.getRenewalRef() + ": evaluated again";
    Map<String, Object> a = new HashMap<>();
    a.put("companyId", companyId);
    a.put("jobNo", jobNo);
    a.put("rowNo", rowNo);
    a.put("kind", kind);
    a.put("location", line.location());
    a.put("pn", line.pn().strip());
    a.put("assured", line.assuredName());
    a.put("inception", line.inception());
    a.put("expiry", line.expiry());
    a.put("remarks", line.remarks());
    a.put("candidateId", c == null ? null : c.getId());
    a.put("matching", c == null ? "UNMATCHED" : "MATCHED");
    a.put("reason", reason);
    a.put("at", Timestamp.from(clock.instant()));
    a.put("by", currentUser.optionalUsername().orElse("SYSTEM"));
    jdbc.update(INSERT, a);
    if (c != null) {
      reevaluation.reevaluate(c, CheckTrigger.UPLOAD);
      audit.record(
          RenewalCodes.ENTITY,
          c.getRenewalRef(),
          AuditAction.UPDATE,
          kind + " report: " + line.remarks());
    }
    return new BulkOutcome(reason, c == null ? "UNMATCHED" : "MATCHED");
  }

  /**
   * A line of a BDOFC or BDOSOLD report.
   *
   * @param location BDOI location
   * @param pn PN number
   * @param assuredName assured's name
   * @param inception inception date
   * @param expiry expiry date
   * @param remarks remarks
   */
  public record Line(
      String location,
      String pn,
      String assuredName,
      LocalDate inception,
      LocalDate expiry,
      String remarks) {}
}
