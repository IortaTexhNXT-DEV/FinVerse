package com.iortatechnxt.brokerverse.renewal.insurer.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponseCode;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.service.UploadValues;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insurer dispositions (FRRN.013.01, FRRN.013.02): a record of an insurer disposition file is
 * matched to its renewal account on the renewal reference, the expiring reference or the expiring
 * policy number; its insurer's approval, one of BDOI's nine values, is recorded as the insurer
 * response of the BRD it maps to: Renew As Is / Approved goes on to the Renewal Advice (Clean, For
 * Renewal), Total Loss closes the account Not for Renewal (Non-Renewable), every other value
 * returns it to Marketing for a disposition (Review, For Disposition).
 */
@Service
@Transactional
public class InsurerDispositionService {

  private static final String MAP =
      "select code, label, outcome, brd_response, bucket, disposition from"
          + " rnw_insurer_disposition_map where active order by sort_order";

  private static final String FIND =
      "select id, case when renewal_ref = :ref then 'RENEWAL_REF' when expiring_arn = :exp or"
          + " expiring_invoice_no = :exp then 'EXPIRING_REF' else 'EXPIRING_POLICY' end as on_key"
          + " from rnw_candidate where company_id = :companyId and stage not in ('RENEWED',"
          + " 'CLOSED') and (renewal_ref = :ref or expiring_arn = :exp or expiring_invoice_no ="
          + " :exp or expiring_policy_no = :policy) order by id desc";

  private static final String INSERT =
      "insert into rnw_insurer_disposition_line (company_id, job_no, row_no, candidate_id,"
          + " disposition_code, insurer_code, insurer_branch, insurer_remarks, matched_on,"
          + " created_at, created_by) values (:companyId, :jobNo, :rowNo, :candidateId, :code,"
          + " :insurer, :branch, :remarks, :on, :at, :by)";

  private final NamedParameterJdbcTemplate jdbc;
  private final RenewalCandidateRepository candidates;
  private final RenewalInsurerResponseService responses;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param jdbc dispositions and matching
   * @param candidates renewals
   * @param responses insurer responses of the BRD
   * @param currentUser uploading user
   * @param clock clock
   */
  public InsurerDispositionService(
      NamedParameterJdbcTemplate jdbc,
      RenewalCandidateRepository candidates,
      RenewalInsurerResponseService responses,
      CurrentUser currentUser,
      Clock clock) {
    this.jdbc = jdbc;
    this.candidates = candidates;
    this.responses = responses;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The insurer dispositions with their response, bucket and renewal disposition.
   *
   * @return values in their order
   */
  @Transactional(readOnly = true)
  public List<Value> values() {
    return jdbc.query(
        MAP,
        Map.of(),
        (rs, i) ->
            new Value(
                rs.getString("code"),
                rs.getString("label"),
                rs.getString("outcome"),
                rs.getString("brd_response"),
                rs.getString("bucket"),
                rs.getString("disposition")));
  }

  /**
   * The value an insurer's approval names, by its code or label.
   *
   * @param typed value of the file
   * @return the value
   */
  @Transactional(readOnly = true)
  public Optional<Value> value(String typed) {
    return values().stream()
        .filter(
            v ->
                UploadValues.names(typed, v.code(), v.label())
                    || "Approved".equals(typed == null ? null : typed.strip())
                        && "RENEW_AS_IS".equals(v.code()))
        .findFirst();
  }

  /**
   * The open renewal account of a record.
   *
   * @param companyId company
   * @param keys references of the record
   * @return the renewal and the key it matched on
   */
  @Transactional(readOnly = true)
  public Optional<Match> find(Long companyId, Keys keys) {
    Map<String, Object> a = new HashMap<>();
    a.put("companyId", companyId);
    a.put("ref", blank(keys.renewalRef()));
    a.put("exp", blank(keys.expiringRef()));
    a.put("policy", blank(keys.policyNo()));
    List<Match> found =
        jdbc.query(
            FIND,
            a,
            (rs, i) ->
                new Match(
                    candidates.findById(rs.getLong("id")).orElseThrow(), rs.getString("on_key")));
    return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
  }

  /**
   * Records the disposition of a record and applies it to its renewal account.
   *
   * @param companyId company
   * @param source upload and row
   * @param match the renewal account matched
   * @param record the disposition and the insurer details
   * @return outcome
   */
  public BulkOutcome apply(Long companyId, Source source, Match match, Record record) {
    RenewalCandidate c = match.candidate();
    Value v = record.value();
    Map<String, Object> a = new HashMap<>();
    a.put("companyId", companyId);
    a.put("jobNo", source.jobNo());
    a.put("rowNo", source.rowNo());
    a.put("candidateId", c.getId());
    a.put("code", v.code());
    a.put("insurer", record.insurer());
    a.put("branch", record.insurerBranch());
    a.put("remarks", record.remarks());
    a.put("on", match.on());
    a.put("at", Timestamp.from(clock.instant()));
    a.put("by", currentUser.optionalUsername().orElse("SYSTEM"));
    jdbc.update(INSERT, a);
    InsurerResponseCode code =
        "RENEW".equals(v.outcome()) ? InsurerResponseCode.RENEW_AS_IS : InsurerResponseCode.REJECT;
    String remarks = v.label() + (record.remarks() == null ? "" : ": " + record.remarks());
    InsurerResponse r =
        responses.record(
            c,
            new InsurerResponse.Content(
                code,
                null,
                null,
                code == InsurerResponseCode.REJECT ? record.tsi() : null,
                code == InsurerResponseCode.REJECT ? record.rate() : null,
                null,
                null,
                remarks),
            new RenewalInsurerResponseService.Source(
                "UPLOAD", source.jobNo(), source.rowNo(), source.policyNo()),
            "REVIEW".equals(v.outcome()));
    return new BulkOutcome(
        c.getRenewalRef() + ": " + v.label() + (r.isLatestValid() ? "" : " (not applied)"),
        "MATCHED");
  }

  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * An insurer disposition.
   *
   * @param code code
   * @param label label
   * @param outcome RENEW, REVIEW or NOT_RENEWED
   * @param brdResponse response of the BRD it maps to
   * @param bucket bucket
   * @param disposition renewal disposition
   */
  public record Value(
      String code,
      String label,
      String outcome,
      String brdResponse,
      String bucket,
      String disposition) {}

  /**
   * The references of a record.
   *
   * @param renewalRef renewal reference number
   * @param expiringRef expiring reference number
   * @param policyNo expiring policy number
   */
  public record Keys(String renewalRef, String expiringRef, String policyNo) {}

  /**
   * A renewal account matched.
   *
   * @param candidate renewal
   * @param on RENEWAL_REF, EXPIRING_REF or EXPIRING_POLICY
   */
  public record Match(RenewalCandidate candidate, String on) {}

  /**
   * Where a record comes from.
   *
   * @param jobNo upload
   * @param rowNo row
   * @param policyNo expiring policy number of the record
   */
  public record Source(String jobNo, int rowNo, String policyNo) {}

  /**
   * The disposition of a record.
   *
   * @param value insurer disposition
   * @param insurer insurer
   * @param insurerBranch insurer's branch
   * @param remarks insurer remarks
   * @param tsi total sum insured
   * @param rate premium rate
   */
  public record Record(
      Value value,
      String insurer,
      String insurerBranch,
      String remarks,
      BigDecimal tsi,
      BigDecimal rate) {}
}
