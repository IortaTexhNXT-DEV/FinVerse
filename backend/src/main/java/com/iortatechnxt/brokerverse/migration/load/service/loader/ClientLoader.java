package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientProfile;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import com.iortatechnxt.brokerverse.crm.service.MigratedClient;
import com.iortatechnxt.brokerverse.crm.service.MigratedClientRegistration;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.migration.matching.domain.ClientMatch;
import com.iortatechnxt.brokerverse.migration.matching.domain.ClientMatchRepository;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Loader of the client master (objects C01 with C02; DATA_MIGRATION_DESIGN sections 9 and 10): the
 * legacy records of one cluster of the client matching load as one BIBS client, registered through
 * {@link MigratedClientRegistration} as a confirmed client that keeps its legacy KYC status, with
 * every legacy key of the cluster pointing to it; a cluster matched to a client already in BIBS
 * loads onto that client. Delta rows update a loaded client; a rolled-back batch deactivates its
 * clients.
 */
@Component
public class ClientLoader implements MigrationLoader {

  /** Entity type of a client in the cross-reference. */
  public static final String ENTITY = "Client";

  private static final String BIBS_PREFIX = "BIBS:";
  private static final String TIN = "tin";
  private static final String EMAIL = "email";
  private static final String SEGMENT = "market_segment";
  private static final int TIN_SHORT = 9;
  private static final int TIN_LONG = 12;
  private static final int TIN_GROUP = 3;
  private static final Set<ClientMatch.Decision> MERGED =
      EnumSet.of(ClientMatch.Decision.AUTO_MERGE, ClientMatch.Decision.MERGE);

  private final MigratedClientRegistration registration;
  private final ClientRepository clients;
  private final ClientMatchRepository matches;

  /**
   * Creates the loader.
   *
   * @param registration migrated client registration
   * @param clients clients (read)
   * @param matches client matching results
   */
  public ClientLoader(
      MigratedClientRegistration registration,
      ClientRepository clients,
      ClientMatchRepository matches) {
    this.registration = registration;
    this.clients = clients;
    this.matches = matches;
  }

  @Override
  public String objectCode() {
    return "C01";
  }

  @Override
  public List<String> childLayouts() {
    return List.of("C02");
  }

  /**
   * Merges the units of each cluster of merged pairs into the unit of its survivor; the other
   * legacy keys become aliases of the loaded client.
   */
  @Override
  public List<LoadUnit> group(List<LoadUnit> units) {
    if (units.isEmpty()) {
      return units;
    }
    Long batchId = units.get(0).main().getBatchId();
    Clusters clusters = new Clusters();
    List<ClientMatch> merged = matches.findByBatchIdAndDecisionIn(batchId, MERGED);
    merged.forEach(m -> clusters.union(m.getLeftKey(), m.getRightKey()));
    merged.stream()
        .filter(m -> m.getSurvivorKey() != null)
        .forEach(m -> clusters.survivor(m.getLeftKey(), m.getSurvivorKey()));
    Map<String, LoadUnit> byKey = new LinkedHashMap<>();
    units.forEach(u -> byKey.put(u.sourceSystem() + ":" + u.legacyKey(), u));
    Map<String, List<LoadUnit>> groups = new LinkedHashMap<>();
    for (Map.Entry<String, LoadUnit> e : byKey.entrySet()) {
      groups.computeIfAbsent(clusters.find(e.getKey()), k -> new ArrayList<>()).add(e.getValue());
    }
    List<LoadUnit> out = new ArrayList<>();
    for (Map.Entry<String, List<LoadUnit>> g : groups.entrySet()) {
      out.add(merge(g.getValue(), clusters.survivorOf(g.getKey())));
    }
    return out;
  }

  private static LoadUnit merge(List<LoadUnit> members, String survivor) {
    LoadUnit main =
        members.stream()
            .filter(u -> (u.sourceSystem() + ":" + u.legacyKey()).equals(survivor))
            .findFirst()
            .orElse(members.get(0));
    if (members.size() == 1) {
      return main;
    }
    Map<String, List<StageRow>> children = new HashMap<>(main.children());
    List<LoadUnit.Alias> aliases = new ArrayList<>();
    for (LoadUnit u : members) {
      if (!u.legacyKey().equals(main.legacyKey())
          || !u.sourceSystem().equals(main.sourceSystem())) {
        aliases.add(new LoadUnit.Alias(u.sourceSystem(), u.legacyKey(), u.hash()));
        children.merge(LoadUnit.MERGED, List.of(u.main()), ClientLoader::concat);
        u.children().forEach((k, v) -> children.merge(k, v, ClientLoader::concat));
        fillBlanks(main.values(), u.values());
      }
    }
    return main.with(children, aliases);
  }

  private static List<StageRow> concat(List<StageRow> a, List<StageRow> b) {
    List<StageRow> all = new ArrayList<>(a);
    all.addAll(b);
    return all;
  }

  private static void fillBlanks(Map<String, String> into, Map<String, String> from) {
    from.forEach(
        (k, v) -> {
          if (Values.blank(into.get(k)) && !Values.blank(v)) {
            into.put(k, v);
          }
        });
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    Optional<Client> existing = matchedBibsClient(unit, ctx);
    if (existing.isPresent()) {
      Client c = existing.get();
      return new LoadOutcome(
          new KeyXref.Target(ENTITY, c.getId(), c.getCode(), c.getVersion()),
          "Matched to BIBS client " + c.getCode());
    }
    Client c = registration.register(request(unit, ctx));
    return LoadOutcome.of(ENTITY, c.getId(), c.getCode(), c.getVersion());
  }

  private Optional<Client> matchedBibsClient(LoadUnit unit, LoadContext ctx) {
    String key = unit.sourceSystem() + ":" + unit.legacyKey();
    return matches.findByBatchIdAndDecisionIn(ctx.batch().getId(), MERGED).stream()
        .filter(m -> key.equals(m.getLeftKey()) && m.getRightClientCode() != null)
        .findFirst()
        .flatMap(m -> clients.findByCode(ctx.companyId(), m.getRightClientCode()));
  }

  @Override
  public Optional<LoadOutcome> update(LoadUnit unit, KeyXref entry, LoadContext ctx) {
    Client c = clients.findById(entry.getTargetId()).orElse(null);
    if (c == null || !c.getRecordOrigin().isMigrated()) {
      return Optional.empty();
    }
    Client updated = registration.update(c.getId(), request(unit, ctx));
    return Optional.of(
        LoadOutcome.of(ENTITY, updated.getId(), updated.getCode(), updated.getVersion()));
  }

  private static MigratedClient request(LoadUnit unit, LoadContext ctx) {
    Map<String, String> v = unit.values();
    ClientType type =
        "C".equalsIgnoreCase(Values.code(v.get("client_type")))
            ? ClientType.CORPORATE
            : ClientType.INDIVIDUAL;
    ClientDetails details =
        new ClientDetails(
            type,
            new ClientDetails.PersonName(
                Values.text(v.get("last_name")),
                Values.text(v.get("first_name")),
                Values.text(v.get("middle_name")),
                Values.text(v.get("suffix")),
                Values.text(v.get("corporate_name"))),
            Values.date(v.get("birth_date")).orElse(null),
            new ClientDetails.Identity(
                tin(v.get(TIN)), Values.text(v.get("id_type")), Values.text(v.get("id_number"))),
            new ClientDetails.Contact(
                Values.text(v.get(EMAIL)),
                Values.text(v.get("mobile")),
                Values.text(v.get("phone")),
                Values.text(v.get("address_line")),
                Values.text(v.get("city")),
                Values.text(v.get("province")),
                Values.text(v.get("postal_code"))),
            Values.text(v.get(SEGMENT)),
            Values.flag(v.get("bank_client_flag")),
            Values.text(v.get("bank_cif")));
    ClientProfile profile =
        new ClientProfile(
            Values.text(v.get("nationality")),
            null,
            null,
            Values.text(v.get("source_of_funds")),
            Values.text(v.get("risk_rating")));
    return new MigratedClient(
        ctx.companyId(),
        details,
        profile,
        kyc(v),
        RecordOrigin.migrated(unit.sourceSystem(), unit.legacyKey(), ctx.batchNo()));
  }

  /** The BIBS TIN format 000-000-000-000 (a 9-digit legacy TIN gets the head-office branch 000). */
  static String tin(String value) {
    String digits = value == null ? "" : value.replaceAll("\\D", "");
    if (digits.length() == TIN_SHORT) {
      digits = digits + "000";
    }
    if (digits.length() != TIN_LONG) {
      return Values.text(value);
    }
    List<String> groups = new ArrayList<>();
    for (int at = 0; at < TIN_LONG; at += TIN_GROUP) {
      groups.add(digits.substring(at, at + TIN_GROUP));
    }
    return String.join("-", groups);
  }

  private static MigratedClient.Kyc kyc(Map<String, String> v) {
    String status = Values.code(v.get("kyc_status"));
    KycStatus kyc;
    try {
      kyc = status == null ? KycStatus.NOT_STARTED : KycStatus.valueOf(status);
    } catch (IllegalArgumentException e) {
      kyc = KycStatus.NOT_STARTED;
    }
    return new MigratedClient.Kyc(
        kyc,
        Values.date(v.get("kyc_verified_date"))
            .map(d -> d.atTime(LocalTime.NOON).atZone(BusinessClock.zone()).toInstant())
            .orElse(null),
        Values.date(v.get("kyc_review_due")).orElse(null));
  }

  @Override
  public List<String> reconciledColumns() {
    return List.of(EMAIL, SEGMENT);
  }

  @Override
  public Map<String, String> readBack(KeyXref entry) {
    return clients
        .findById(entry.getTargetId())
        .map(
            c -> {
              Map<String, String> m = new HashMap<>();
              m.put(EMAIL, c.getEmail() == null ? null : c.getEmail().toLowerCase(Locale.ROOT));
              m.put(SEGMENT, c.getMarketSegment());
              return m;
            })
        .orElse(Map.of());
  }

  @Override
  public boolean reversible() {
    return true;
  }

  @Override
  public boolean changedSinceLoad(KeyXref entry) {
    return clients
        .findById(entry.getTargetId())
        .map(c -> !Long.valueOf(c.getVersion()).equals(entry.getTargetVersion()))
        .orElse(false);
  }

  @Override
  public boolean compensate(KeyXref entry, LoadContext ctx) {
    Optional<Client> c = clients.findById(entry.getTargetId());
    if (c.isEmpty() || !c.get().getRecordOrigin().isMigrated()) {
      // A legacy key matched to a client created in BIBS: nothing to undo on the client.
      return true;
    }
    registration.rollback(c.get().getId(), ctx.batchNo());
    return true;
  }

  /** Union-find over the keys of merged pairs, with the survivor of each cluster. */
  private static final class Clusters {
    private final Map<String, String> parent = new HashMap<>();
    private final Map<String, String> survivors = new HashMap<>();

    String find(String key) {
      String p = parent.getOrDefault(key, key);
      if (p.equals(key)) {
        return key;
      }
      String root = find(p);
      parent.put(key, root);
      return root;
    }

    void union(String a, String b) {
      if (a.startsWith(BIBS_PREFIX) || b.startsWith(BIBS_PREFIX)) {
        return;
      }
      String ra = find(a);
      String rb = find(b);
      if (!ra.equals(rb)) {
        parent.put(rb, ra);
      }
    }

    void survivor(String member, String key) {
      survivors.put(find(member), key);
    }

    String survivorOf(String root) {
      return survivors.getOrDefault(find(root), root);
    }
  }
}
