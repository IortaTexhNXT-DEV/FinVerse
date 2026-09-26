package com.iortatechnxt.brokerverse.screening;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Contact;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Identity;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.PersonName;
import com.iortatechnxt.brokerverse.crm.domain.ClientProfile;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.service.ClientOnboardingService;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.crm.service.KycDocumentService;
import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.AliasType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryValues;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistChange;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistDecisionService;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Test data of matching and risk profiling: unique clients of the demo company (or of a company of
 * their own with a copy of the demo matching criteria and risk rules) and invented watchlist
 * entries made ACTIVE through the maker-checker of the watchlist.
 */
@Component
public class ScreeningMatchingFixtures {

  public static final byte[] PDF =
      "%PDF-1.4\n% screening evidence\n%%EOF".getBytes(StandardCharsets.US_ASCII);

  private static final AtomicInteger SEQ = new AtomicInteger();
  private static final String LETTERS = "BCDFGHJKLMNPQRSTVWXZ";

  private final ClientService clients;
  private final ClientOnboardingService onboarding;
  private final KycDocumentService kyc;
  private final WatchlistService watchlists;
  private final WatchlistDecisionService decisions;
  private final ScreeningSetupFixtures setup;
  private final AsUser as;
  private final TestData data;
  private final JdbcTemplate jdbc;

  ScreeningMatchingFixtures(
      ClientService clients,
      ClientOnboardingService onboarding,
      KycDocumentService kyc,
      WatchlistService watchlists,
      WatchlistDecisionService decisions,
      ScreeningSetupFixtures setup,
      AsUser as,
      TestData data,
      JdbcTemplate jdbc) {
    this.clients = clients;
    this.onboarding = onboarding;
    this.kyc = kyc;
    this.watchlists = watchlists;
    this.decisions = decisions;
    this.setup = setup;
    this.as = as;
    this.data = data;
    this.jdbc = jdbc;
  }

  /**
   * An invented word of consonants and vowels, unique in the run (for names no list holds).
   *
   * @return capitalised word
   */
  public static String word() {
    long n = System.nanoTime() / 1000 + SEQ.incrementAndGet();
    StringBuilder b = new StringBuilder("Q");
    for (int i = 0; i < 6; i++) {
      b.append(LETTERS.charAt((int) (n % LETTERS.length())));
      b.append("aeiou".charAt((int) (n % 5)));
      n /= LETTERS.length();
    }
    return b.charAt(0) + b.substring(1).toLowerCase(Locale.ROOT);
  }

  /**
   * The demo company FVI (ACTIVE screening configuration of V1950).
   *
   * @return company id
   */
  public Long demoCompany() {
    return data.company().getId();
  }

  /**
   * A company of its own without screening configuration.
   *
   * @return company id
   */
  Long bareCompany() {
    return setup.company();
  }

  /**
   * A company of its own with a copy of the demo matching criteria and risk rules.
   *
   * @return company id
   */
  Long configuredCompany() {
    Long company = setup.company();
    for (String type : List.of("MATCH_CRITERIA", "RISK_RULES")) {
      jdbc.update(
          """
          insert into scr_config_version (company_id, config_type, scope, version_no, status,
              effective_from, change_note, created_at, created_by)
          values (?, ?, '', 1, 'ACTIVE', date '2026-01-01', 'Test copy', now(), 'SYSTEM')
          """,
          company,
          type);
    }
    jdbc.update(
        """
        insert into scr_match_rule (version_id, sort_order, list_type, subject_type, algorithm,
            threshold, match_fields, min_score_for_case, created_at, created_by)
        select n.id, r.sort_order, r.list_type, r.subject_type, r.algorithm, r.threshold,
               r.match_fields, r.min_score_for_case, now(), 'SYSTEM'
        from scr_match_rule r
        join scr_config_version o on o.id = r.version_id and o.config_type = 'MATCH_CRITERIA'
            and o.version_no = 1
        join org_company c on c.id = o.company_id and c.code = 'FVI'
        join scr_config_version n on n.company_id = ? and n.config_type = 'MATCH_CRITERIA'
        """,
        company);
    jdbc.update(
        """
        insert into scr_risk_category (version_id, code, name, tier, kyc_risk_rating, tags,
            case_type, requires_edd, created_at, created_by)
        select n.id, k.code, k.name, k.tier, k.kyc_risk_rating, k.tags, k.case_type,
               k.requires_edd, now(), 'SYSTEM'
        from scr_risk_category k
        join scr_config_version o on o.id = k.version_id and o.version_no = 1
        join org_company c on c.id = o.company_id and c.code = 'FVI'
        join scr_config_version n on n.company_id = ? and n.config_type = 'RISK_RULES'
        """,
        company);
    jdbc.update(
        """
        insert into scr_risk_rule (version_id, priority, category_code, condition_attr, operator,
            rule_values, created_at, created_by)
        select n.id, r.priority, r.category_code, r.condition_attr, r.operator, r.rule_values,
               now(), 'SYSTEM'
        from scr_risk_rule r
        join scr_config_version o on o.id = r.version_id and o.version_no = 1
        join org_company c on c.id = o.company_id and c.code = 'FVI'
        join scr_config_version n on n.company_id = ? and n.config_type = 'RISK_RULES'
        """,
        company);
    return company;
  }

  /**
   * An individual.
   *
   * @param first first name
   * @param last last name
   * @param birth birth date, may be {@code null}
   * @return details
   */
  public static ClientDetails person(String first, String last, LocalDate birth) {
    String tag = String.valueOf(System.nanoTime() % 1_000_000_000L);
    return new ClientDetails(
        ClientType.INDIVIDUAL,
        new PersonName(last, first, null, null, null),
        birth,
        new Identity(tin(), "PASSPORT", "S" + tag),
        new Contact(
            "screen" + tag + "@test-client.ph",
            "0998" + String.format("%07d", SEQ.incrementAndGet() % 10_000_000),
            null,
            "1 Test Street",
            "Makati",
            "Metro Manila",
            "1200"),
        "CBG",
        false,
        null);
  }

  private static String tin() {
    String d =
        String.format("%08d", (System.nanoTime() / 1000 + SEQ.incrementAndGet()) % 100_000_000L);
    return "8" + d.substring(0, 2) + "-" + d.substring(2, 5) + "-" + d.substring(5, 8) + "-000";
  }

  /**
   * A prospect created by ao (screened on registration).
   *
   * @param companyId company
   * @param details details
   * @return the client
   */
  public Client prospect(Long companyId, ClientDetails details) {
    return as.run(
        "ao",
        () ->
            clients.create(
                companyId,
                details,
                new ClientProfile("FILIPINO", "SINGLE", "Tester", "SALARY", "STANDARD")));
  }

  /**
   * A prospect of the demo company with verified KYC (so it has a KYC review date).
   *
   * @param details details
   * @return the client, reloaded
   */
  Client verified(ClientDetails details) {
    Client p = prospect(demoCompany(), details);
    for (String type : List.of("KYC_FORM", "VALID_ID")) {
      as.run("ao", () -> kyc.upload(p.getId(), type, type.toLowerCase(Locale.ROOT) + ".pdf", PDF));
    }
    as.run("ao", () -> onboarding.submitKyc(p.getId(), "documents complete"));
    as.run("mkttl", () -> onboarding.verifyKyc(p.getId(), "checked originals"));
    return clients.get(p.getId());
  }

  /**
   * An INTERNAL watchlist entry added by compoff and approved by compchk (ACTIVE, screened).
   *
   * @param name primary name
   * @param birth birth date, may be {@code null}
   * @return the entry id
   */
  public Long listed(String name, LocalDate birth) {
    WatchlistChange add =
        as.run("compoff", () -> watchlists.add(null, values(name, birth), "Test listing"));
    as.run("compchk", () -> decisions.approve(add.getId(), null));
    return add.getEntryId();
  }

  /**
   * Changes an entry through maker-checker (a new entry version).
   *
   * @param entryId entry
   * @param name primary name
   * @param birth new birth date
   */
  void change(Long entryId, String name, LocalDate birth) {
    WatchlistChange change =
        as.run(
            "compoff",
            () -> watchlists.change(entryId, values(name, birth), "Birth date corrected"));
    as.run("compchk", () -> decisions.approve(change.getId(), null));
  }

  private static EntryValues values(String name, LocalDate birth) {
    return new EntryValues(
        "INTERNAL",
        SubjectType.INDIVIDUAL,
        name,
        null,
        null,
        birth,
        "FILIPINO",
        null,
        null,
        null,
        List.of(new EntryValues.Alias(name + " Alias", AliasType.AKA)));
  }
}
