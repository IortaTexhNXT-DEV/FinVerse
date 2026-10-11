package com.iortatechnxt.brokerverse.renewal.dashboard.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.service.RenewalWorkingDays;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Processing dashboard of New Business and Renewal (BDOI Renewal FRS FRRN.003): the KPI cards
 * For Placement, For Booking, With and Without Policy, For Releasing, Released, Unreleased,
 * Directly Booked and Returned, in the Combined, New Business or Renewal view; the drill-down of
 * each card with the ageing in business days, the turnaround time status and the Assigned and
 * Unassigned tabs of For Placement; and the assignment of the Placement Processor.
 */
@Service
@Transactional(readOnly = true)
public class ProcessingDashboardService {

  /** Parameter: placement turnaround time of packaged accounts. */
  public static final String TAT_PACKAGED = "RNW_PLACEMENT_TAT_PACKAGED_DAYS";

  /** Parameter: placement turnaround time of non-package accounts. */
  public static final String TAT_NON_PACKAGE = "RNW_PLACEMENT_TAT_NON_PACKAGE_DAYS";

  /** Parameter: hour after which a placement counts from the next business day. */
  public static final String CUTOFF = "RNW_PLACEMENT_CUTOFF_HOUR";

  /** Card keys and labels in display order. */
  public static final List<String[]> CARDS =
      List.of(
          new String[] {"FOR_PLACEMENT", "For Placement"},
          new String[] {"FOR_BOOKING", "For Booking"},
          new String[] {"WITH_POLICY", "With Policy"},
          new String[] {"WITHOUT_POLICY", "Without Policy"},
          new String[] {"FOR_RELEASING", "For Releasing"},
          new String[] {"RELEASED", "Released Policy"},
          new String[] {"UNRELEASED", "Unreleased Policy"},
          new String[] {"DIRECTLY_BOOKED", "Directly Booked"},
          new String[] {"RETURNED", "Returned Accounts"});

  private static final int DEFAULT_TAT_PACKAGED = 3;
  private static final int DEFAULT_TAT = 10;
  private static final int DEFAULT_CUTOFF = 15;
  private static final String STAGE_PLACED = "PLACED";
  private static final String UNSUCCESSFUL = "UNSUCCESSFUL";
  private static final Set<String> SENT_STATUSES = Set.of(STAGE_PLACED, "POLICY_ISSUED", "BOOKED");
  private static final String PROCESSOR = "processor";
  private static final String POLICIES = "policy_numbers";
  private static final String TRANSMITTAL = "transmittal";
  private static final Map<String, Predicate<DashboardItem>> CARD_RULES = cardRules();

  private final ProcessingItems reader;
  private final RenewalWorkingDays workingDays;
  private final SystemParameterService parameters;
  private final NamedParameterJdbcTemplate jdbc;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param reader processing accounts
   * @param workingDays business-day calendar
   * @param parameters system parameters
   * @param jdbc SQL (assignment of the Placement Processor)
   * @param audit audit trail
   * @param currentUser user
   * @param clock clock
   */
  public ProcessingDashboardService(
      ProcessingItems reader,
      RenewalWorkingDays workingDays,
      SystemParameterService parameters,
      NamedParameterJdbcTemplate jdbc,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.reader = reader;
    this.workingDays = workingDays;
    this.parameters = parameters;
    this.jdbc = jdbc;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The KPI cards of the view.
   *
   * @param filter filters with the business type
   * @return cards with their counts, and the combined counts by business type
   */
  public Dashboard dashboard(DashboardFilter filter) {
    List<DashboardItem> items = items(filter);
    List<Card> cards = new ArrayList<>();
    for (String[] c : CARDS) {
      List<DashboardItem> in = items.stream().filter(card(c[0])).toList();
      long renewals = in.stream().filter(DashboardItem::renewal).count();
      cards.add(new Card(c[0], c[1], in.size(), in.size() - renewals, renewals));
    }
    Map<String, Long> workload = new LinkedHashMap<>();
    items.stream()
        .filter(i -> i.text(PROCESSOR) != null && !"BOOKED".equals(i.stage()))
        .map(i -> i.text(PROCESSOR))
        .sorted()
        .forEach(p -> workload.merge(p, 1L, Long::sum));
    return new Dashboard(filter.completed(BusinessClock.today(clock)), cards, workload);
  }

  /**
   * The accounts of a card (and of a tab of For Placement: ASSIGNED or UNASSIGNED).
   *
   * @param filter filters
   * @param card card key
   * @param tab tab or null
   * @return accounts with their ageing and turnaround status
   */
  public List<DashboardItem> drill(DashboardFilter filter, String card, String tab) {
    Predicate<DashboardItem> p = card(card);
    if ("ASSIGNED".equals(tab)) {
      p = p.and(i -> i.text(PROCESSOR) != null);
    } else if ("UNASSIGNED".equals(tab)) {
      p = p.and(i -> i.text(PROCESSOR) == null);
    }
    return items(filter).stream().filter(p).toList();
  }

  /**
   * Assigns the Placement Processor of accounts submitted for placement.
   *
   * @param companyId company
   * @param arns accounts
   * @param processor user name of the processor
   * @return accounts assigned
   */
  @Transactional
  public int assign(Long companyId, List<String> arns, String processor) {
    int n = 0;
    for (String arn : arns) {
      Map<String, Object> args =
          Map.of(
              "company",
              companyId,
              "arn",
              arn,
              PROCESSOR,
              processor,
              "at",
              java.sql.Timestamp.from(clock.instant()),
              "by",
              currentUser.username());
      n +=
          jdbc.update(
              "insert into rnw_processing_assignment (company_id, arn, processor, created_at, created_by)"
                  + " values (:company, :arn, :processor, :at, :by) on conflict (company_id, arn)"
                  + " do update set processor = :processor, updated_at = :at, updated_by = :by,"
                  + " version = rnw_processing_assignment.version + 1",
              args);
      audit.record(
          "Account", arn, AuditAction.UPDATE, "Placement Processor assigned: " + processor);
    }
    return n;
  }

  private List<DashboardItem> items(DashboardFilter filter) {
    LocalDate today = BusinessClock.today(clock);
    DashboardFilter f = filter.completed(today);
    List<DashboardItem> items = reader.items(f);
    Predicate<LocalDate> calendar = workingDays.calendar(f.companyId());
    Tat tat =
        new Tat(
            parameters.intValue(CUTOFF, DEFAULT_CUTOFF),
            parameters.intValue(TAT_PACKAGED, DEFAULT_TAT_PACKAGED),
            parameters.intValue(TAT_NON_PACKAGE, DEFAULT_TAT));
    items.forEach(i -> enrich(i, today, calendar, tat));
    return items;
  }

  private static void enrich(
      DashboardItem i, LocalDate today, Predicate<LocalDate> calendar, Tat tat) {
    i.put("ageing", RenewalWorkingDays.between(i.date("submitted_at"), today, calendar));
    int placementAgeing = placementAgeing(i, today, calendar, tat.cutoff());
    i.put("placement_ageing", placementAgeing);
    if (i.instant("placed_at") != null) {
      int limit = i.flag("packaged") ? tat.packaged() : tat.nonPackage();
      i.put("tat_status", placementAgeing > limit ? "Beyond TAT" : "Within TAT");
    }
    i.put("policy_status_name", policyStatus(i));
    i.put("transmittal", transmittal(i));
    LocalDate received = i.date("policy_received_at");
    LocalDate sent = i.date("transmitted_at");
    if (sent == null) {
      sent = i.date("dispatched_at");
    }
    if (received != null) {
      i.put("delivery_ageing", ChronoUnit.DAYS.between(received, sent == null ? today : sent));
    }
  }

  /**
   * Business days from the placement sent to the insurer (from the next business day when sent at
   * or after the cut-off hour) to the resolution date of a placement issue, the booking, or today.
   *
   * @param i account
   * @param today business date
   * @param calendar business days
   * @param cutoff cut-off hour
   * @return ageing
   */
  public static int placementAgeing(
      DashboardItem i, LocalDate today, Predicate<LocalDate> calendar, int cutoff) {
    if (i.instant("placed_at") == null) {
      return 0;
    }
    ZonedDateTime sent = i.instant("placed_at").atZone(BusinessClock.zone());
    LocalDate start = sent.toLocalDate();
    if (sent.getHour() >= cutoff) {
      start = start.plusDays(1);
      while (!calendar.test(start)) {
        start = start.plusDays(1);
      }
    }
    LocalDate end = today;
    if (i.flag("with_issue") && i.date("resolution_date") != null) {
      end = i.date("resolution_date");
    } else if (i.date("booked_at") != null) {
      end = i.date("booked_at");
    }
    return RenewalWorkingDays.between(start, end, calendar);
  }

  private static String policyStatus(DashboardItem i) {
    String s = i.text("policy_status");
    if (s != null) {
      return switch (s) {
        case "WITH_ISSUE" -> "With Issue";
        case "SPOILED" -> "Spoiled";
        case "CANCELED" -> "Canceled";
        case "RECEIVED" -> "Received";
        default -> "Pending";
      };
    }
    return i.text("policy_numbers") == null ? "Pending" : "Received";
  }

  private static String transmittal(DashboardItem i) {
    String t = i.text("transmittal_status");
    if (t != null) {
      return t;
    }
    if (i.instant("dispatched_at") != null) {
      return "SENT";
    }
    return "CONFIRMED".equals(i.text("epolicy_status")) ? "PENDING" : null;
  }

  /**
   * The accounts a card counts.
   *
   * @param key card key
   * @return predicate
   */
  static Predicate<DashboardItem> card(String key) {
    Predicate<DashboardItem> p = CARD_RULES.get(key);
    return p == null ? i -> false : p;
  }

  private static Map<String, Predicate<DashboardItem>> cardRules() {
    Map<String, Predicate<DashboardItem>> r = new HashMap<>();
    r.put("FOR_PLACEMENT", i -> "READY_FOR_PLACEMENT".equals(i.stage()));
    r.put("FOR_BOOKING", i -> STAGE_PLACED.equals(i.stage()) || "POLICY_ISSUED".equals(i.stage()));
    r.put("WITH_POLICY", i -> SENT_STATUSES.contains(i.stage()) && i.text(POLICIES) != null);
    r.put("WITHOUT_POLICY", i -> SENT_STATUSES.contains(i.stage()) && i.text(POLICIES) == null);
    r.put("FOR_RELEASING", i -> "PENDING".equals(i.get(TRANSMITTAL)));
    r.put("RELEASED", i -> "SENT".equals(i.get(TRANSMITTAL)));
    r.put("UNRELEASED", i -> UNSUCCESSFUL.equals(i.get(TRANSMITTAL)));
    r.put("DIRECTLY_BOOKED", i -> i.flag("direct_booking"));
    r.put("RETURNED", i -> "RETURNED_TO_MARKETING".equals(i.stage()));
    return r;
  }

  /**
   * Turnaround time settings.
   *
   * @param cutoff cut-off hour
   * @param packaged business days of packaged accounts
   * @param nonPackage business days of non-package accounts
   */
  record Tat(int cutoff, int packaged, int nonPackage) {}

  /**
   * A KPI card.
   *
   * @param key card key
   * @param label card name
   * @param count all accounts (Combined View)
   * @param newBusiness New Business accounts
   * @param renewal renewal accounts
   */
  public record Card(String key, String label, long count, long newBusiness, long renewal) {}

  /**
   * The processing dashboard.
   *
   * @param filter filters applied
   * @param cards KPI cards
   * @param workload open accounts per Placement Processor
   */
  public record Dashboard(DashboardFilter filter, List<Card> cards, Map<String, Long> workload) {}
}
