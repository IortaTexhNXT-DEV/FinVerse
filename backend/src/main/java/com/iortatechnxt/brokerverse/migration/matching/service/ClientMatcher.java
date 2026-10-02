package com.iortatechnxt.brokerverse.migration.matching.service;

import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.matching.domain.ClientMatch;
import com.iortatechnxt.brokerverse.migration.matching.domain.ClientMatch.Decision;
import com.iortatechnxt.brokerverse.migration.matching.domain.ClientMatchRepository;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Client matching and deduplication (BRID 2.1; DATA_MIGRATION_DESIGN section 9; FR-DM-031): the
 * staged clients of every source system are compared with each other and with the clients already
 * in BIBS on the keys K1-K7; a pair scoring at least {@code MIG_CLIENT_MATCH_AUTO} merges
 * automatically, a pair from {@code MIG_CLIENT_MATCH_REVIEW} goes to the Data Steward's review
 * queue, a lower score is a new client. Pairs build clusters; a cluster becomes one BIBS client.
 * Decisions of the Data Steward survive a new validation of the batch.
 */
@Service
@Transactional
public class ClientMatcher {

  private static final int HARD = 100;
  private static final int PERSON = 95;
  private static final int CORPORATE_REG = 95;
  private static final int CORPORATE = 90;
  private static final int SOFT = 40;
  private static final int SIMILAR = 70;
  private static final double SIMILARITY = 0.85;

  private final ClientMatchRepository matches;
  private final Survivorship survivorship;
  private final MigrationParameters parameters;
  private final JdbcTemplate jdbc;

  /**
   * Creates the matcher.
   *
   * @param matches match pairs
   * @param survivorship survivor values
   * @param parameters thresholds
   * @param jdbc JDBC (clients already in BIBS)
   */
  public ClientMatcher(
      ClientMatchRepository matches,
      Survivorship survivorship,
      MigrationParameters parameters,
      JdbcTemplate jdbc) {
    this.matches = matches;
    this.survivorship = survivorship;
    this.parameters = parameters;
    this.jdbc = jdbc;
  }

  /**
   * Matches the client rows of a batch.
   *
   * @param batch batch
   * @param rows staged client rows (layout C01)
   * @param mapped mapped values of a row
   * @param sources source system by extract id
   * @return pairs waiting for review
   */
  public int match(
      MigBatch batch,
      List<StageRow> rows,
      Function<Long, Map<String, String>> mapped,
      Map<Long, String> sources) {
    if (rows.isEmpty()) {
      return 0;
    }
    matches.deleteByBatchIdAndDecisionIn(
        batch.getId(), EnumSet.of(Decision.AUTO_MERGE, Decision.REVIEW));
    Map<String, ClientMatch> decided = new HashMap<>();
    for (ClientMatch m : matches.findByBatchIdOrderByClusterNoAscIdAsc(batch.getId())) {
      decided.put(pairKey(m.getLeftKey(), m.getRightKey()), m);
    }
    List<Candidate> candidates = new ArrayList<>();
    for (StageRow r : rows) {
      Map<String, String> v = mapped.apply(r.getId());
      String source = sources.getOrDefault(r.getExtractId(), "");
      candidates.add(
          new Candidate(
              r.getId(),
              source + ":" + r.getLegacyKey(),
              null,
              MatchKeys.of(v == null ? r.getRawPayload() : v)));
    }
    candidates.addAll(existingClients(batch.getCompanyId()));
    int auto = parameters.clientMatchAuto();
    int review = parameters.clientMatchReview();
    List<ClientMatch> pairs = new ArrayList<>(decided.values());
    for (Pair p : pairs(candidates)) {
      if (p.score() < review || decided.containsKey(pairKey(p.left().key(), p.right().key()))) {
        continue;
      }
      Decision d = p.score() >= auto ? Decision.AUTO_MERGE : Decision.REVIEW;
      pairs.add(
          matches.save(
              new ClientMatch(
                  batch.getId(),
                  new ClientMatch.Side(p.left().rowId(), p.left().key(), null),
                  new ClientMatch.Side(p.right().rowId(), p.right().key(), p.right().clientCode()),
                  p.score(),
                  String.join(", ", p.keys()),
                  d)));
    }
    cluster(pairs, rows, mapped, sources);
    return (int) pairs.stream().filter(m -> m.getDecision() == Decision.REVIEW).count();
  }

  private void cluster(
      List<ClientMatch> pairs,
      List<StageRow> rows,
      Function<Long, Map<String, String>> mapped,
      Map<Long, String> sources) {
    UnionFind uf = new UnionFind();
    pairs.stream()
        .filter(ClientMatch::merges)
        .forEach(m -> uf.union(m.getLeftKey(), m.getRightKey()));
    Map<String, Integer> numbers = new LinkedHashMap<>();
    Map<String, StageRow> byKey = new HashMap<>();
    rows.forEach(
        r -> byKey.put(sources.getOrDefault(r.getExtractId(), "") + ":" + r.getLegacyKey(), r));
    for (ClientMatch m : pairs) {
      String root = uf.find(m.getLeftKey());
      int no = numbers.computeIfAbsent(root, k -> numbers.size() + 1);
      if (!m.merges()) {
        m.survivor(no, null, null);
        continue;
      }
      List<Survivorship.Member> members = new ArrayList<>();
      for (String key : uf.members(root)) {
        StageRow r = byKey.get(key);
        if (r != null) {
          members.add(
              new Survivorship.Member(
                  key, key.substring(0, key.indexOf(':')), mapped.apply(r.getId())));
        }
      }
      Survivorship.Result result = survivorship.merge(members);
      m.survivor(no, result.survivorKey(), result.lostValuesJson());
    }
  }

  private static List<Pair> pairs(List<Candidate> candidates) {
    Map<String, List<Candidate>> blocks = new HashMap<>();
    for (Candidate c : candidates) {
      for (String b : c.blocks()) {
        blocks.computeIfAbsent(b, k -> new ArrayList<>()).add(c);
      }
    }
    Map<String, Pair> out = new LinkedHashMap<>();
    for (List<Candidate> block : blocks.values()) {
      pairsOf(block, out);
    }
    return new ArrayList<>(out.values());
  }

  /** Every pair of a block with at least one legacy record, the best score per pair. */
  private static void pairsOf(List<Candidate> block, Map<String, Pair> out) {
    for (int i = 0; i < block.size(); i++) {
      for (int j = i + 1; j < block.size(); j++) {
        pairOf(block.get(i), block.get(j)).ifPresent(p -> keepBest(out, p));
      }
    }
  }

  private static Optional<Pair> pairOf(Candidate a, Candidate b) {
    if (a.rowId() == null && b.rowId() == null) {
      return Optional.empty();
    }
    return Optional.of(a.rowId() == null ? score(b, a) : score(a, b));
  }

  private static void keepBest(Map<String, Pair> out, Pair p) {
    out.merge(
        pairKey(p.left().key(), p.right().key()), p, (x, y) -> x.score() >= y.score() ? x : y);
  }

  private static Pair score(Candidate a, Candidate b) {
    MatchKeys x = a.keys();
    MatchKeys y = b.keys();
    Set<String> keys = new TreeSet<>();
    int hard = hardScore(x, y, keys);
    int soft =
        keyScore(same(x.email(), y.email()), keys, "K6 e-mail", SOFT)
            + keyScore(same(x.mobile(), y.mobile()), keys, "K6 mobile", SOFT);
    boolean similarBlock = same(x.birthDate(), y.birthDate()) || same(x.city(), y.city());
    int similar =
        keyScore(
            similarBlock && MatchKeys.similarity(x.name(), y.name()) >= SIMILARITY,
            keys,
            "K7",
            SIMILAR);
    return new Pair(
        a, b, Math.min(HARD, Math.max(hard, Math.max(soft, similar))), List.copyOf(keys));
  }

  /** The best of the hard keys K1 to K5. */
  private static int hardScore(MatchKeys x, MatchKeys y, Set<String> keys) {
    int hard = keyScore(same(x.tin(), y.tin()), keys, "K1", HARD);
    hard = Math.max(hard, keyScore(same(x.id(), y.id()), keys, "K2", HARD));
    hard = Math.max(hard, keyScore(same(x.person(), y.person()), keys, "K3", PERSON));
    if (same(x.corporate(), y.corporate())) {
      boolean reg = same(x.registration(), y.registration());
      hard = Math.max(hard, add(keys, "K4", reg ? CORPORATE_REG : CORPORATE));
    }
    return Math.max(hard, keyScore(same(x.cif(), y.cif()), keys, "K5", HARD));
  }

  private static int keyScore(boolean matched, Set<String> keys, String key, int score) {
    return matched ? add(keys, key, score) : 0;
  }

  private static int add(Set<String> keys, String key, int score) {
    keys.add(key);
    return score;
  }

  private static boolean same(String a, String b) {
    return a != null && a.equals(b);
  }

  private static String pairKey(String a, String b) {
    return a.compareTo(b) < 0 ? a + "~" + b : b + "~" + a;
  }

  private List<Candidate> existingClients(Long companyId) {
    List<Candidate> out = new ArrayList<>();
    jdbc.query(
        "select coalesce(client_code, prospect_code) as code, tin, id_type, id_number, last_name, first_name,"
            + " birth_date, corporate_name, email, mobile, bank_cif, city from crm_client"
            + " where company_id = ? and status <> 'INACTIVE'",
        rs -> {
          Map<String, String> v = new HashMap<>();
          for (String c :
              List.of(
                  "tin",
                  "id_type",
                  "id_number",
                  "last_name",
                  "first_name",
                  "corporate_name",
                  "email",
                  "mobile",
                  "bank_cif",
                  "city")) {
            v.put(c, rs.getString(c));
          }
          v.put(
              "birth_date",
              rs.getDate("birth_date") == null ? null : rs.getDate("birth_date").toString());
          String code = rs.getString("code");
          out.add(new Candidate(null, "BIBS:" + code, code, MatchKeys.of(v)));
        },
        companyId);
    return out;
  }

  private record Candidate(Long rowId, String key, String clientCode, MatchKeys keys) {

    List<String> blocks() {
      List<String> b = new ArrayList<>();
      addBlock(b, "T", keys.tin());
      addBlock(b, "I", keys.id());
      addBlock(b, "P", keys.person());
      addBlock(b, "C", keys.corporate());
      addBlock(b, "F", keys.cif());
      addBlock(b, "E", keys.email());
      addBlock(b, "M", keys.mobile());
      addBlock(b, "B", keys.birthDate());
      addBlock(
          b,
          "Y",
          keys.city() == null || keys.name() == null
              ? null
              : keys.city() + "|" + keys.name().charAt(0));
      return b;
    }

    private static void addBlock(List<String> b, String kind, String value) {
      if (value != null) {
        b.add(kind + ":" + value);
      }
    }
  }

  private record Pair(Candidate left, Candidate right, int score, List<String> keys) {}

  /** Clusters of merged keys. */
  private static final class UnionFind {
    private final Map<String, String> parent = new HashMap<>();

    String find(String x) {
      parent.putIfAbsent(x, x);
      String p = parent.get(x);
      if (!p.equals(x)) {
        p = find(p);
        parent.put(x, p);
      }
      return p;
    }

    void union(String a, String b) {
      String ra = find(a);
      String rb = find(b);
      if (!ra.equals(rb)) {
        parent.put(rb, ra);
      }
    }

    List<String> members(String root) {
      List<String> out = new ArrayList<>();
      for (String k : new ArrayList<>(parent.keySet())) {
        if (find(k).equals(root)) {
          out.add(k);
        }
      }
      return out;
    }
  }
}
