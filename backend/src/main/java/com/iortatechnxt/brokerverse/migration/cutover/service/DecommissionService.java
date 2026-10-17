package com.iortatechnxt.brokerverse.migration.cutover.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.migration.cutover.domain.DecommissionItem;
import com.iortatechnxt.brokerverse.migration.cutover.domain.DecommissionItem.Template;
import com.iortatechnxt.brokerverse.migration.cutover.domain.DecommissionItemRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The legacy decommissioning checklists (DATA_MIGRATION_DESIGN 17.6): per legacy system the
 * milestone "Legacy system decommissioned", and once for BIBS the milestone "Legacy context closed"
 * whose criteria are measured - no open legacy invoice, no legacy unapplied balance, the legacy
 * control accounts and migration clearing at zero. Each criterion is met with its evidence and
 * signed.
 */
@Service
@Transactional
public class DecommissionService {

  /** Milestone of a legacy system. */
  public static final String SYSTEM = "SYSTEM";

  /** Milestone of the legacy context in BIBS. */
  public static final String CONTEXT = "CONTEXT";

  /** System code of the legacy context checklist. */
  public static final String LEGACY_CONTEXT = "LEGACY";

  private static final String ENTITY = "MigDecommission";

  private static final List<Template> SYSTEM_TEMPLATE =
      List.of(
          new Template(
              "FINAL_EXTRACTS",
              "Final extracts reconciled",
              "The final extracts of the system are loaded and reconciled (counts and hash totals)"),
          new Template(
              "FINAL_TRUEUP",
              "Final true-up reconciled",
              "For a system holding the GL: the final true-up reconciled to the audited FY2027 TB"
                  + " and the legacy GL locked"),
          new Template(
              "ARCHIVE",
              "Archive reconciled",
              "The archive objects of the system are loaded and reconciled against legacy"),
          new Template(
              "INQUIRY",
              "Legacy Inquiry verified",
              "Legacy Inquiry verified by Audit and Compliance on the archive of the system"),
          new Template(
              "NO_OPEN_ITEM",
              "No open item needs the system",
              "No open inquiry, claim or item needs the legacy system"),
          new Template(
              "CLAIMS_TAIL",
              "Claims tail covered",
              "The last legacy-booked policy expired plus the claims reporting tail"),
          new Template(
              "ACCESS_LOGS",
              "Access logs archived",
              "The access logs of the legacy system are exported to the archive"),
          new Template(
              "RETENTION",
              "Retention covered",
              "The retention obligations of the records are covered"),
          new Template(
              "OWNERS",
              "Owners signed",
              "Sign-off by the system owner, Compliance and Comptrollership"));

  private static final List<Template> CONTEXT_TEMPLATE =
      List.of(
          new Template(
              "NO_LEGACY_INVOICE",
              "No open legacy invoice",
              "Every legacy invoice is paid, reversed, written off or remitted"),
          new Template(
              "NO_LEGACY_UPP",
              "No legacy UPP balance",
              "Every legacy unapplied payment is applied, refunded or reclassified"),
          new Template(
              "LEGACY_ACCOUNTS_ZERO",
              "Legacy accounts at zero",
              "The legacy control accounts and Migration Clearing are at 0.00"),
          new Template(
              "CHART_DECISION",
              "Chart decision recorded",
              "Comptrollership decides whether the legacy accounts are closed in the chart"));

  private static final Set<String> MEASURED =
      Set.of("NO_LEGACY_INVOICE", "NO_LEGACY_UPP", "LEGACY_ACCOUNTS_ZERO");

  private static final String OPEN_INVOICES =
      "select count(distinct i.id) from ops_invoice i join ops_invoice_component c"
          + " on c.invoice_id = i.id where i.company_id = ? and i.ledger_context = 'LEGACY'"
          + " and not i.cancelled and c.balance <> 0";

  private static final String OPEN_UPP =
      "select count(*) from csh_unapplied where company_id = ? and ledger_context = 'LEGACY'"
          + " and balance > 0";

  private static final String LEGACY_ACCOUNTS =
      "select coalesce(sum(abs(t.balance)), 0) from (select sum(e.debit_fc - e.credit_fc) as balance"
          + " from gl_ledger_entry e join coa_account a on a.id = e.account_id"
          + " where e.company_id = ? and (a.code like '1215%' or a.code in ('1216', '2206', '2222',"
          + " '2223') or a.code like 'LGC-%') group by a.code, e.currency) t";

  private final DecommissionItemRepository items;
  private final JdbcTemplate jdbc;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param items checklist items
   * @param jdbc JDBC (measured criteria)
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public DecommissionService(
      DecommissionItemRepository items,
      JdbcTemplate jdbc,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.items = items;
    this.jdbc = jdbc;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The checklists of a company, the legacy context measured again.
   *
   * @param companyId company
   * @return items by system and milestone
   */
  public List<DecommissionItem> checklists(Long companyId) {
    List<DecommissionItem> all =
        items.findByCompanyIdOrderBySystemCodeAscMilestoneAscIdAsc(companyId);
    if (all.stream().noneMatch(i -> LEGACY_CONTEXT.equals(i.getSystemCode()))) {
      CONTEXT_TEMPLATE.forEach(
          t -> items.save(new DecommissionItem(companyId, LEGACY_CONTEXT, CONTEXT, t)));
      all = items.findByCompanyIdOrderBySystemCodeAscMilestoneAscIdAsc(companyId);
    }
    measure(companyId, all);
    return all;
  }

  /**
   * Opens the checklist of a legacy system.
   *
   * @param companyId company
   * @param systemCode EBIX, QPS, ISYS or CMS
   * @return its items
   */
  public List<DecommissionItem> open(Long companyId, String systemCode) {
    String system = systemCode.strip().toUpperCase(Locale.ROOT);
    boolean exists =
        items.findByCompanyIdOrderBySystemCodeAscMilestoneAscIdAsc(companyId).stream()
            .anyMatch(i -> system.equals(i.getSystemCode()));
    if (exists) {
      throw new BusinessRuleException(
          "MIG_DECOMMISSION_EXISTS", "The checklist of " + system + " is already open");
    }
    List<DecommissionItem> created =
        SYSTEM_TEMPLATE.stream()
            .map(t -> items.save(new DecommissionItem(companyId, system, SYSTEM, t)))
            .toList();
    audit.record(ENTITY, system, AuditAction.CREATE, "Decommissioning checklist opened");
    return created;
  }

  /**
   * Records the status and evidence of a criterion.
   *
   * @param id item
   * @param status MET, SIGNED or NOT_APPLICABLE (or OPEN)
   * @param evidence evidence
   * @return the item
   */
  public DecommissionItem update(Long id, DecommissionItem.Status status, String evidence) {
    DecommissionItem item =
        items.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    if (status != DecommissionItem.Status.OPEN && (evidence == null || evidence.isBlank())) {
      throw new BusinessRuleException(
          "MIG_EVIDENCE_REQUIRED", "Give the evidence of the criterion");
    }
    item.update(status, evidence, currentUser.username(), clock.instant());
    audit.record(
        ENTITY,
        item.getSystemCode() + ":" + item.getCriterion(),
        AuditAction.UPDATE,
        status + ": " + evidence);
    return item;
  }

  private void measure(Long companyId, List<DecommissionItem> all) {
    Map<String, Boolean> met =
        Map.of(
            "NO_LEGACY_INVOICE", zero(jdbc.queryForObject(OPEN_INVOICES, Integer.class, companyId)),
            "NO_LEGACY_UPP", zero(jdbc.queryForObject(OPEN_UPP, Integer.class, companyId)),
            "LEGACY_ACCOUNTS_ZERO", legacyBalance(companyId).signum() == 0);
    for (DecommissionItem item : all) {
      boolean measured =
          LEGACY_CONTEXT.equals(item.getSystemCode()) && MEASURED.contains(item.getCriterion());
      if (measured && item.getStatus() != DecommissionItem.Status.SIGNED) {
        boolean ok = met.get(item.getCriterion());
        item.update(
            ok ? DecommissionItem.Status.MET : DecommissionItem.Status.OPEN,
            ok ? "Measured on " + clock.instant() : null,
            currentUser.username(),
            clock.instant());
      }
    }
  }

  private static boolean zero(Integer n) {
    return n == null || n == 0;
  }

  private BigDecimal legacyBalance(Long companyId) {
    BigDecimal b = jdbc.queryForObject(LEGACY_ACCOUNTS, BigDecimal.class, companyId);
    return b == null ? BigDecimal.ZERO : b;
  }
}
