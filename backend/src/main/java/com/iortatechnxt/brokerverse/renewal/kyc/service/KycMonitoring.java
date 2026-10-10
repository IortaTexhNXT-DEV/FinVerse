package com.iortatechnxt.brokerverse.renewal.kyc.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.service.KycReviewPolicy;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KYC monitoring of the renewal accounts (FRRN.039): the accounts whose client is due for KYC
 * review are identified by configurable criteria - the review date against the due window and the
 * upcoming days, the market segments, bank or non-bank clients and the products - and tagged KYC
 * Due, KYC Upcoming or KYC Not Required; users record the review activities, follow-up notes and
 * the completion (Pending Follow-Up, KYC Completed), and every status is kept in the history.
 */
@Service
public class KycMonitoring {

  /** Status: review due within the due window. */
  public static final String DUE = "KYC_DUE";

  /** Status: review within the upcoming days. */
  public static final String UPCOMING = "KYC_UPCOMING";

  /** Status: no review needed. */
  public static final String NOT_REQUIRED = "KYC_NOT_REQUIRED";

  /** Status: follow-up pending. */
  public static final String FOLLOW_UP = "PENDING_FOLLOW_UP";

  /** Status: review completed. */
  public static final String COMPLETED = "KYC_COMPLETED";

  /** Labels of the statuses. */
  public static final Map<String, String> LABELS =
      Map.of(
          DUE, "KYC Due",
          UPCOMING, "KYC Upcoming",
          NOT_REQUIRED, "KYC Not Required",
          FOLLOW_UP, "Pending Follow-Up",
          COMPLETED, "KYC Completed");

  private static final Set<String> MANUAL = Set.of(FOLLOW_UP, COMPLETED);
  private static final String SYSTEM = "SYSTEM";
  private static final int UPCOMING_DAYS = 60;

  private final RenewalRecords records;
  private final RenewalCandidateRepository candidates;
  private final ClientRepository clients;
  private final KycReviewPolicy policy;
  private final RenewalParameters renewal;
  private final SystemParameterService parameters;
  private final NamedParameterJdbcTemplate jdbc;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the monitoring.
   *
   * @param records renewals
   * @param candidates open renewals
   * @param clients KYC review dates
   * @param policy KYC due window
   * @param renewal segments of the KYC flag
   * @param parameters upcoming days, client types and products
   * @param jdbc statuses and history
   * @param currentUser user
   * @param clock clock
   */
  public KycMonitoring(
      RenewalRecords records,
      RenewalCandidateRepository candidates,
      ClientRepository clients,
      KycReviewPolicy policy,
      RenewalParameters renewal,
      SystemParameterService parameters,
      NamedParameterJdbcTemplate jdbc,
      CurrentUser currentUser,
      Clock clock) {
    this.records = records;
    this.candidates = candidates;
    this.clients = clients;
    this.policy = policy;
    this.renewal = renewal;
    this.parameters = parameters;
    this.jdbc = jdbc;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Refreshes the KYC status of the open renewal accounts of a company.
   *
   * @param companyId company
   * @return accounts whose status changed
   */
  @Transactional
  public int refresh(Long companyId) {
    int changed = 0;
    List<Long> ids =
        jdbc.queryForList(
            "select id from rnw_candidate where company_id = :companyId"
                + " and stage not in ('RENEWED', 'CLOSED')",
            Map.of("companyId", companyId),
            Long.class);
    for (RenewalCandidate c : candidates.findAllById(ids)) {
      changed += refresh(c) ? 1 : 0;
    }
    return changed;
  }

  /**
   * Refreshes the KYC status of one account; a recorded follow-up or completion stays while the
   * review date is unchanged.
   *
   * @param c renewal
   * @return whether the status changed
   */
  public boolean refresh(RenewalCandidate c) {
    Optional<Client> client = client(c);
    LocalDate due = client.map(Client::getKycReviewDue).orElse(null);
    Map<String, Object> current = current(c.getId());
    String status = (String) current.get("kyc_status");
    LocalDate previousDue = date(current.get("kyc_review_due"));
    if (MANUAL.contains(status) && Objects.equals(due, previousDue)) {
      return false;
    }
    String next = automatic(c, client, due);
    if (next.equals(status) && Objects.equals(due, previousDue)) {
      return false;
    }
    save(c, next, due, "Identified by the KYC criteria", null, SYSTEM);
    return true;
  }

  /**
   * Records a KYC review activity with its status.
   *
   * @param companyId company
   * @param ref renewal
   * @param status new status
   * @param activity activity
   * @param remarks remarks or follow-up note
   */
  @Transactional
  public void record(Long companyId, String ref, String status, String activity, String remarks) {
    if (!LABELS.containsKey(status)) {
      throw new BusinessRuleException("RNW_KYC_STATUS", "Select the KYC status");
    }
    if ((activity == null || activity.isBlank()) && (remarks == null || remarks.isBlank())) {
      throw new BusinessRuleException("RNW_KYC_ACTIVITY", "Enter the activity or the remarks");
    }
    RenewalCandidate c = records.get(companyId, ref);
    LocalDate due = client(c).map(Client::getKycReviewDue).orElse(null);
    save(c, status, due, blank(activity), blank(remarks), currentUser.username());
  }

  /**
   * The KYC status and history of an account, newest first.
   *
   * @param companyId company
   * @param ref renewal
   * @return status and history
   */
  @Transactional(readOnly = true)
  public Monitoring of(Long companyId, String ref) {
    RenewalCandidate c = records.get(companyId, ref);
    Map<String, Object> current = current(c.getId());
    List<Map<String, Object>> history =
        jdbc.queryForList(
            "select status, activity, remarks, created_by, created_at from rnw_kyc_activity"
                + " where candidate_id = :id order by id desc",
            Map.of("id", c.getId()));
    return new Monitoring(
        (String) current.get("kyc_status"), date(current.get("kyc_review_due")), history);
  }

  /**
   * The counts of the KYC Monitoring Dashboard.
   *
   * @param companyId company
   * @return count per status
   */
  @Transactional(readOnly = true)
  public Map<String, Object> dashboard(Long companyId) {
    return jdbc.queryForMap(
        "select count(*) filter (where kyc_status = 'KYC_DUE') as due,"
            + " count(*) filter (where kyc_status = 'KYC_UPCOMING') as upcoming,"
            + " count(*) filter (where kyc_status = 'KYC_COMPLETED') as completed,"
            + " count(*) filter (where kyc_status = 'PENDING_FOLLOW_UP') as follow_up"
            + " from rnw_candidate where company_id = :companyId"
            + " and stage not in ('RENEWED', 'CLOSED')",
        Map.of("companyId", companyId));
  }

  /**
   * The accounts of a KYC status (drill-down of the dashboard).
   *
   * @param companyId company
   * @param status status
   * @return accounts
   */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> accounts(Long companyId, String status) {
    return jdbc.queryForList(
        "select renewal_ref as \"renewalRef\", client_name as \"clientName\","
            + " kyc_review_due as \"kycReviewDue\", kyc_status as \"status\","
            + " assigned_ao as \"accountOfficer\", expiry_date as \"expiryDate\""
            + " from rnw_candidate where company_id = :companyId and kyc_status = :status"
            + " and stage not in ('RENEWED', 'CLOSED') order by kyc_review_due nulls last",
        Map.of("companyId", companyId, "status", status));
  }

  private String automatic(RenewalCandidate c, Optional<Client> client, LocalDate due) {
    if (due == null || !monitored(c, client)) {
      return NOT_REQUIRED;
    }
    LocalDate today = BusinessClock.today(clock);
    if (!due.isAfter(policy.dueHorizon(today))) {
      return DUE;
    }
    int upcoming = parameters.intValue("RNW_KYC_UPCOMING_DAYS", UPCOMING_DAYS);
    return due.isAfter(today.plusDays(upcoming)) ? NOT_REQUIRED : UPCOMING;
  }

  private boolean monitored(RenewalCandidate c, Optional<Client> client) {
    CandidateSnapshot.SnapshotProduct p = c.getSnapshot().product();
    if (p == null || !renewal.kycApplies(p.segment())) {
      return false;
    }
    List<String> products = parameters.items("RNW_KYC_PRODUCTS");
    if (!products.isEmpty() && !products.contains(p.productCode())) {
      return false;
    }
    String type = client.map(k -> k.isBankClient() ? "BANK" : "NON_BANK").orElse("NON_BANK");
    return parameters.items("RNW_KYC_CLIENT_TYPES").contains(type);
  }

  private void save(
      RenewalCandidate c,
      String status,
      LocalDate due,
      String activity,
      String remarks,
      String user) {
    jdbc.update(
        "update rnw_candidate set kyc_status = :status, kyc_review_due = :due where id = :id",
        new MapSqlParameterSource()
            .addValue("status", status)
            .addValue("due", due)
            .addValue("id", c.getId()));
    jdbc.update(
        "insert into rnw_kyc_activity (company_id, candidate_id, status, activity, remarks,"
            + " created_at, created_by) values (:company, :candidate, :status, :activity,"
            + " :remarks, :at, :user)",
        new MapSqlParameterSource()
            .addValue("company", c.getCompanyId())
            .addValue("candidate", c.getId())
            .addValue("status", status)
            .addValue("activity", activity)
            .addValue("remarks", remarks)
            .addValue("at", java.sql.Timestamp.from(clock.instant()))
            .addValue("user", user));
  }

  private Map<String, Object> current(Long id) {
    return jdbc.queryForMap(
        "select kyc_status, kyc_review_due from rnw_candidate where id = :id", Map.of("id", id));
  }

  private Optional<Client> client(RenewalCandidate c) {
    Long id = c.getSnapshot().client() == null ? null : c.getSnapshot().client().clientId();
    return id == null ? Optional.empty() : clients.findById(id);
  }

  private static LocalDate date(Object value) {
    return value instanceof java.sql.Date d ? d.toLocalDate() : (LocalDate) value;
  }

  private static String blank(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }

  /**
   * The KYC monitoring of an account.
   *
   * @param status current status
   * @param kycReviewDue review date of the client
   * @param history statuses and activities, newest first
   */
  public record Monitoring(
      String status, LocalDate kycReviewDue, List<Map<String, Object>> history) {

    /** Defensive copy. */
    public Monitoring {
      history = List.copyOf(history);
    }
  }
}
