package com.iortatechnxt.brokerverse.screening;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchAlgorithm;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchField;
import com.iortatechnxt.brokerverse.screening.config.domain.RiskAttribute;
import com.iortatechnxt.brokerverse.screening.config.domain.RuleOperator;
import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigVersionRef;
import com.iortatechnxt.brokerverse.screening.config.service.MatchCriteria;
import com.iortatechnxt.brokerverse.screening.config.service.RiskRules;
import com.iortatechnxt.brokerverse.screening.matching.service.EntryPool;
import com.iortatechnxt.brokerverse.screening.matching.service.NameKeys;
import com.iortatechnxt.brokerverse.screening.matching.service.NameNormaliser;
import com.iortatechnxt.brokerverse.screening.matching.service.NameScorer;
import com.iortatechnxt.brokerverse.screening.matching.service.PairMatcher;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningSubject;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskRuleEvaluator;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskRuleEvaluator.ClientFacts;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskRuleEvaluator.MatchFacts;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.service.ListedEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * Pure functions of matching and risk profiling (SNSRP-301, 302; FR-SS-031, 033): normalisation,
 * blocking keys, the three algorithms, the pair matcher with its attribute adjustments, the
 * in-memory entry pool, the risk rule evaluator and its combinations, and the safety of the demo
 * watchlist of V1951 against the demo clients.
 */
class ScreeningMatchingTest {

  private static final AtomicLong IDS = new AtomicLong(1000);
  private static final String SANCTION = "SANCTION";
  private static final String PEP = "PEP";
  private static final String INTERNAL = "INTERNAL";
  private static final String FILIPINO = "FILIPINO";

  /** The demo matching criteria of V1950 (FVI version 1). */
  private static final MatchCriteria DEMO =
      new MatchCriteria(
          new ConfigVersionRef(1L, ConfigType.MATCH_CRITERIA, null, 1, LocalDate.of(2026, 1, 1)),
          List.of(
              rule(10, SANCTION, SubjectType.INDIVIDUAL, MatchAlgorithm.EXACT, "1", "1", true),
              rule(
                  20,
                  SANCTION,
                  SubjectType.INDIVIDUAL,
                  MatchAlgorithm.PHONETIC,
                  "0.85",
                  "0.9",
                  true),
              rule(30, SANCTION, SubjectType.INDIVIDUAL, MatchAlgorithm.FUZZY, "0.85", "0.9", true),
              rule(40, SANCTION, SubjectType.ENTITY, MatchAlgorithm.EXACT, "1", "1", false),
              rule(50, SANCTION, SubjectType.ENTITY, MatchAlgorithm.FUZZY, "0.88", "0.92", false),
              rule(60, PEP, SubjectType.INDIVIDUAL, MatchAlgorithm.EXACT, "1", "1", true),
              rule(70, PEP, SubjectType.INDIVIDUAL, MatchAlgorithm.PHONETIC, "0.85", "0.9", true),
              rule(80, PEP, SubjectType.INDIVIDUAL, MatchAlgorithm.FUZZY, "0.88", "0.92", true),
              rule(90, PEP, SubjectType.ENTITY, MatchAlgorithm.FUZZY, "0.9", "0.94", false),
              rule(100, INTERNAL, SubjectType.INDIVIDUAL, MatchAlgorithm.EXACT, "1", "1", false),
              rule(
                  110,
                  INTERNAL,
                  SubjectType.INDIVIDUAL,
                  MatchAlgorithm.FUZZY,
                  "0.88",
                  "0.92",
                  false),
              rule(120, INTERNAL, SubjectType.ENTITY, MatchAlgorithm.FUZZY, "0.9", "0.94", false)));

  private static MatchCriteria.Rule rule(
      long id,
      String list,
      SubjectType type,
      MatchAlgorithm algorithm,
      String threshold,
      String caseScore,
      boolean birthDate) {
    Set<MatchField> fields =
        birthDate
            ? Set.of(MatchField.NAME, MatchField.ALIAS, MatchField.BIRTH_DATE)
            : Set.of(MatchField.NAME, MatchField.ALIAS);
    return new MatchCriteria.Rule(
        id, list, type, algorithm, new BigDecimal(threshold), fields, new BigDecimal(caseScore));
  }

  private static ListedEntry entry(
      String list, SubjectType type, String name, LocalDate birth, String... aliases) {
    return new ListedEntry(
        IDS.incrementAndGet(),
        "SRC",
        "REF",
        list,
        type,
        name,
        null,
        null,
        birth,
        null,
        null,
        List.of(aliases),
        1,
        EntryStatus.ACTIVE);
  }

  private static ScreeningSubject person(
      String first, String middle, String last, LocalDate birth) {
    String full =
        String.join(" ", middle == null ? List.of(first, last) : List.of(first, middle, last));
    List<String> names = new ArrayList<>(List.of(full));
    if (middle != null) {
      names.add(first + " " + last);
    }
    return new ScreeningSubject(
        IDS.incrementAndGet(),
        1L,
        "PR-T",
        last + ", " + first,
        "PROSPECT",
        SubjectType.INDIVIDUAL,
        names,
        birth,
        FILIPINO,
        Set.of(),
        "INDIVIDUAL",
        null,
        null,
        null,
        "STANDARD");
  }

  private static ScreeningSubject corporate(String name) {
    return new ScreeningSubject(
        IDS.incrementAndGet(),
        1L,
        "PR-C",
        name,
        "CONFIRMED",
        SubjectType.ENTITY,
        List.of(name),
        null,
        null,
        Set.of(),
        "CORPORATE",
        null,
        null,
        null,
        "STANDARD");
  }

  @Test
  void normalisesAccentsParticlesTitlesAndWordOrder() {
    assertThat(NameNormaliser.tokens("Juan Dela Cruz")).containsExactly("JUAN", "DELACRUZ");
    assertThat(NameNormaliser.tokens("juan de la CRUZ")).containsExactly("JUAN", "DELACRUZ");
    assertThat(NameNormaliser.tokens("Dr. José Peña-Núñez Jr."))
        .containsExactly("JOSE", "PENA", "NUNEZ");
    assertThat(NameNormaliser.tokens("Acme Trading Corp.")).containsExactly("ACME", "TRADING");
    assertThat(NameNormaliser.tokens("Von")).containsExactly("VON");
    assertThat(NameNormaliser.tokens(" ")).isEmpty();
    assertThat(NameNormaliser.tokens(null)).isEmpty();
    assertThat(NameNormaliser.identifier("123-456 789")).isEqualTo("123456789");
    assertThat(NameNormaliser.identifier(null)).isEmpty();
    assertThat(NameKeys.of("Dela Cruz, Juan").exact())
        .isEqualTo(NameKeys.of("Juan Dela Cruz").exact());
    assertThat(NameKeys.of("Juan Dela Cruz").tokenKeys()).containsExactly("JUAN", "DELACRUZ");
    assertThat(NameKeys.of("A" + "B".repeat(300)).exactKey()).hasSize(NameKeys.MAX_KEY);
  }

  @Test
  void scoresExactPhoneticAndFuzzy() {
    NameKeys juan = NameKeys.of("Juan de la Cruz");
    assertThat(NameScorer.score(MatchAlgorithm.EXACT, NameKeys.of("Juan Dela Cruz"), juan))
        .isEqualTo(1.0);
    assertThat(NameScorer.score(MatchAlgorithm.EXACT, NameKeys.of("Juan Cruz"), juan)).isZero();
    assertThat(
            NameScorer.score(
                MatchAlgorithm.PHONETIC, NameKeys.of("Jon Santoz"), NameKeys.of("John Santos")))
        .isEqualTo(1.0);
    assertThat(NameScorer.score(MatchAlgorithm.FUZZY, NameKeys.of("Juan Dela Crux"), juan))
        .isGreaterThan(0.9);
    assertThat(NameScorer.score(MatchAlgorithm.FUZZY, NameKeys.of("Juana Cruzado"), juan))
        .isLessThan(0.85);
    assertThat(NameScorer.score(MatchAlgorithm.PHONETIC, NameKeys.of("Juana Cruzado"), juan))
        .isLessThan(0.85);
    assertThat(NameScorer.score(MatchAlgorithm.FUZZY, NameKeys.of(""), juan)).isZero();
    assertThat(NameScorer.jaroWinkler("MARTHA", "MARHTA")).isBetween(0.96, 0.97);
    assertThat(NameScorer.jaroWinkler("ABC", "XYZ")).isZero();
    assertThat(NameScorer.jaroWinkler("", "XYZ")).isZero();
    assertThat(NameKeys.of("Juan Dela Cruz").blocksWith(juan)).isTrue();
    assertThat(NameKeys.of("Zyx Qwv").blocksWith(juan)).isFalse();
  }

  @Test
  void matchesThePairWithTheBestRuleAndTheAttributeAdjustments() {
    ListedEntry listed =
        entry(
            SANCTION,
            SubjectType.INDIVIDUAL,
            "Juan de la Cruz",
            LocalDate.of(1970, 2, 15),
            "Juanito Dela Cruz");
    PairMatcher.Hit exact =
        PairMatcher.match(person("Juan", null, "Dela Cruz", null), listed, DEMO).orElseThrow();
    assertThat(exact.rule().algorithm()).isEqualTo(MatchAlgorithm.EXACT);
    assertThat(exact.score()).isEqualByComparingTo("1");
    assertThat(exact.fields()).containsExactly(MatchField.NAME);
    assertThat(exact.reachesCase()).isTrue();

    PairMatcher.Hit sameBirth =
        PairMatcher.match(
                person("Juan", "Santos", "Dela Cruz", LocalDate.of(1970, 2, 15)), listed, DEMO)
            .orElseThrow();
    assertThat(sameBirth.fields())
        .containsExactlyInAnyOrder(MatchField.NAME, MatchField.BIRTH_DATE);
    assertThat(sameBirth.score()).isEqualByComparingTo("1");

    PairMatcher.Hit otherBirth =
        PairMatcher.match(person("Juan", null, "Dela Cruz", LocalDate.of(1985, 5, 1)), listed, DEMO)
            .orElseThrow();
    assertThat(otherBirth.rule().algorithm()).isEqualTo(MatchAlgorithm.PHONETIC);
    assertThat(otherBirth.score()).isEqualByComparingTo("0.9");

    PairMatcher.Hit alias =
        PairMatcher.match(person("Juanito", null, "Dela Cruz", null), listed, DEMO).orElseThrow();
    assertThat(alias.fields()).containsExactly(MatchField.ALIAS);

    assertThat(PairMatcher.match(person("Juana", null, "Cruzado", null), listed, DEMO)).isEmpty();
    assertThat(PairMatcher.match(corporate("Juan de la Cruz"), listed, DEMO)).isEmpty();
    ListedEntry adverse = entry("ADVERSE_MEDIA", SubjectType.INDIVIDUAL, "Juan de la Cruz", null);
    assertThat(PairMatcher.match(person("Juan", null, "Dela Cruz", null), adverse, DEMO)).isEmpty();
  }

  @Test
  void anIdSharedOrANationalityRaisesTheScoreWhenTheRuleComparesThem() {
    MatchCriteria withIds =
        new MatchCriteria(
            DEMO.version(),
            List.of(
                new MatchCriteria.Rule(
                    1L,
                    INTERNAL,
                    SubjectType.INDIVIDUAL,
                    MatchAlgorithm.FUZZY,
                    new BigDecimal("0.8"),
                    Set.of(MatchField.NAME, MatchField.ID, MatchField.NATIONALITY),
                    null)));
    ListedEntry listed =
        new ListedEntry(
            1L,
            "INTERNAL",
            "R",
            INTERNAL,
            SubjectType.INDIVIDUAL,
            "Pedro Invented Lagman",
            "Pedro",
            "Lagman",
            null,
            FILIPINO,
            "P-123 4567; X9",
            List.of(),
            2,
            EntryStatus.ACTIVE);
    ScreeningSubject subject =
        new ScreeningSubject(
            2L,
            1L,
            "C",
            "Lagman, Pedro",
            "CONFIRMED",
            SubjectType.INDIVIDUAL,
            List.of("Pedro Lagman"),
            null,
            FILIPINO,
            Set.of("P1234567"),
            "INDIVIDUAL",
            null,
            null,
            null,
            null);
    PairMatcher.Hit hit = PairMatcher.match(subject, listed, withIds).orElseThrow();
    assertThat(hit.fields()).contains(MatchField.ID, MatchField.NATIONALITY, MatchField.NAME);
    assertThat(hit.reachesCase()).isFalse();
    ScreeningSubject foreign =
        new ScreeningSubject(
            3L,
            1L,
            "C",
            "Lagman, Pedro",
            "CONFIRMED",
            SubjectType.INDIVIDUAL,
            List.of("Pedro Lagman"),
            null,
            "AMERICAN",
            Set.of(),
            "INDIVIDUAL",
            null,
            null,
            null,
            null);
    PairMatcher.Hit weaker = PairMatcher.match(foreign, listed, withIds).orElseThrow();
    assertThat(weaker.score()).isLessThan(hit.score());
  }

  @Test
  void theEntryPoolOffersOnlyEntriesSharingAKey() {
    ListedEntry juan = entry(SANCTION, SubjectType.INDIVIDUAL, "Juan de la Cruz", null);
    ListedEntry other = entry(SANCTION, SubjectType.INDIVIDUAL, "Zoltan Brakovic Invented", null);
    ListedEntry off =
        new ListedEntry(
            99L,
            "S",
            "R",
            SANCTION,
            SubjectType.INDIVIDUAL,
            "Juan Dela Cruz",
            null,
            null,
            null,
            null,
            null,
            List.of(),
            1,
            EntryStatus.INACTIVE);
    EntryPool pool = EntryPool.of(List.of(juan, other, off));
    assertThat(pool.size()).isEqualTo(2);
    assertThat(pool.isEmpty()).isFalse();
    assertThat(pool.candidates(person("Juan", null, "Dela Cruz", null))).containsExactly(juan);
    assertThat(pool.candidates(person("Nobody", null, "Qwxyz", null))).isEmpty();
    assertThat(EntryPool.of(List.of()).isEmpty()).isTrue();
  }

  private static RiskRules rules(List<RiskRules.Rule> rules) {
    return new RiskRules(
        new ConfigVersionRef(2L, ConfigType.RISK_RULES, null, 1, LocalDate.of(2026, 1, 1)),
        List.of(
            new RiskRules.Category(
                "HIGH_SANCTION",
                "High",
                1,
                "HIGH",
                Set.of("WATCHLIST_REVIEW"),
                "NAME_MATCH",
                false),
            new RiskRules.Category("PEP", "PEP", 2, "HIGH", Set.of("PEP"), "PEP", true),
            new RiskRules.Category(
                "CONFIRMED_SANCTION",
                "Confirmed",
                1,
                "HIGH",
                Set.of("WATCHLIST_REVIEW"),
                "HIGH_RISK",
                true),
            new RiskRules.Category("STATUS_ONLY", "Status", 3, "STANDARD", null, null, false),
            new RiskRules.Category("FOREIGN", "Foreign", 3, "STANDARD", null, null, false)),
        rules);
  }

  private static RiskRules.Rule risk(
      long id,
      int priority,
      String category,
      RiskAttribute attr,
      RuleOperator op,
      String... values) {
    return new RiskRules.Rule(id, priority, category, attr, op, Set.of(values));
  }

  private static final ClientFacts CLIENT =
      new ClientFacts("INDIVIDUAL", FILIPINO, null, null, "CBG", Set.of());

  @Test
  void theFirstRuleByPriorityDecidesAndTrueMatchesComeFirst() {
    RiskRules demo =
        rules(
            List.of(
                risk(
                    1,
                    10,
                    "HIGH_SANCTION",
                    RiskAttribute.MATCH_LIST_TYPE,
                    RuleOperator.EQ,
                    SANCTION),
                risk(2, 20, "PEP", RiskAttribute.PEP, RuleOperator.EQ, "TRUE"),
                risk(3, 30, "PEP", RiskAttribute.MATCH_LIST_TYPE, RuleOperator.EQ, PEP)));
    List<MatchFacts> matches =
        List.of(
            new MatchFacts(5L, SANCTION, "POTENTIAL"), new MatchFacts(7L, SANCTION, "TRUE_MATCH"));
    RiskRuleEvaluator.Qualification q =
        RiskRuleEvaluator.evaluate(demo, CLIENT, matches).orElseThrow();
    assertThat(q.category().code()).isEqualTo("HIGH_SANCTION");
    assertThat(q.matchId()).isEqualTo(7L);

    Optional<RiskRuleEvaluator.Qualification> pepTag =
        RiskRuleEvaluator.evaluate(
            demo,
            new ClientFacts("INDIVIDUAL", FILIPINO, null, null, null, Set.of("PEP")),
            List.of());
    assertThat(pepTag.orElseThrow().category().code()).isEqualTo("PEP");
    assertThat(pepTag.get().matchId()).isNull();
    assertThat(RiskRuleEvaluator.evaluate(demo, CLIENT, List.of())).isEmpty();
    assertThat(
            RiskRuleEvaluator.evaluate(demo, CLIENT, List.of(new MatchFacts(9L, PEP, "POTENTIAL")))
                .orElseThrow()
                .rule()
                .id())
        .isEqualTo(3L);
  }

  @Test
  void aMatchStatusRuleQualifiesTheOtherRulesOfItsCategory() {
    RiskRules combined =
        rules(
            List.of(
                risk(
                    1,
                    10,
                    "CONFIRMED_SANCTION",
                    RiskAttribute.MATCH_LIST_TYPE,
                    RuleOperator.EQ,
                    SANCTION),
                risk(
                    2,
                    20,
                    "CONFIRMED_SANCTION",
                    RiskAttribute.MATCH_STATUS,
                    RuleOperator.EQ,
                    "TRUE_MATCH"),
                risk(
                    3, 30, "STATUS_ONLY", RiskAttribute.MATCH_STATUS, RuleOperator.IN, "POTENTIAL"),
                risk(4, 40, "FOREIGN", RiskAttribute.NATIONALITY, RuleOperator.NOT_IN, FILIPINO)));
    assertThat(
            RiskRuleEvaluator.evaluate(
                    combined, CLIENT, List.of(new MatchFacts(1L, SANCTION, "POTENTIAL")))
                .orElseThrow()
                .category()
                .code())
        .isEqualTo("STATUS_ONLY");
    assertThat(
            RiskRuleEvaluator.evaluate(
                    combined, CLIENT, List.of(new MatchFacts(2L, SANCTION, "TRUE_MATCH")))
                .orElseThrow()
                .category()
                .code())
        .isEqualTo("CONFIRMED_SANCTION");
    assertThat(
            RiskRuleEvaluator.evaluate(
                combined, CLIENT, List.of(new MatchFacts(3L, PEP, "TRUE_MATCH"))))
        .isEmpty();
    ClientFacts foreigner =
        new ClientFacts("INDIVIDUAL", "AMERICAN", "Trader", "BUSINESS", "RETAIL", null);
    assertThat(
            RiskRuleEvaluator.evaluate(combined, foreigner, List.of())
                .orElseThrow()
                .category()
                .code())
        .isEqualTo("FOREIGN");
    RiskRules unknownCategory =
        rules(
            List.of(risk(9, 1, "NOPE", RiskAttribute.CLIENT_TYPE, RuleOperator.EQ, "INDIVIDUAL")));
    assertThat(RiskRuleEvaluator.evaluate(unknownCategory, CLIENT, List.of())).isEmpty();
    RiskRules byAttributes =
        rules(
            List.of(
                risk(5, 1, "FOREIGN", RiskAttribute.OCCUPATION, RuleOperator.EQ, "Trader"),
                risk(6, 2, "FOREIGN", RiskAttribute.SOURCE_OF_FUNDS, RuleOperator.EQ, "BUSINESS"),
                risk(7, 3, "FOREIGN", RiskAttribute.MARKET_SEGMENT, RuleOperator.EQ, "RETAIL")));
    assertThat(
            RiskRuleEvaluator.evaluate(byAttributes, foreigner, List.of())
                .orElseThrow()
                .rule()
                .id())
        .isEqualTo(5L);
  }

  /** V1951 entries against the demo clients of V981 / V983: only the intended pair matches. */
  @Test
  void theDemoWatchlistMatchesOnlyTheIntendedDemoClient() {
    List<ListedEntry> entries =
        List.of(
            entry(
                SANCTION,
                SubjectType.INDIVIDUAL,
                "Juan de la Cruz",
                LocalDate.of(1970, 2, 15),
                "Juanito Dela Cruz"),
            entry(
                SANCTION,
                SubjectType.INDIVIDUAL,
                "Jose Miguel Lopez Reyes",
                LocalDate.of(1979, 11, 2)),
            entry(
                SANCTION,
                SubjectType.INDIVIDUAL,
                "Zoltan Brakovic Invented",
                LocalDate.of(1962, 6, 1),
                "Zoli Brakovic"),
            entry(
                SANCTION,
                SubjectType.ENTITY,
                "Kestrel Maritime Fictional Ltd.",
                null,
                "Kestrel Shipping Fictional"),
            entry(
                SANCTION,
                SubjectType.INDIVIDUAL,
                "Orvald Nightingale Quist",
                LocalDate.of(1958, 1, 9)),
            entry(
                SANCTION,
                SubjectType.INDIVIDUAL,
                "Tamsin Vey Larkwood",
                LocalDate.of(1981, 12, 12)),
            entry(SANCTION, SubjectType.ENTITY, "Blackfen Trading Fictitious Corp.", null),
            entry(
                SANCTION,
                SubjectType.INDIVIDUAL,
                "Ignatius Ferro Mandalay",
                LocalDate.of(1966, 4, 4)),
            entry(
                SANCTION,
                SubjectType.INDIVIDUAL,
                "Casimir Delacroix Imaginario",
                LocalDate.of(1975, 10, 31)),
            entry(SANCTION, SubjectType.ENTITY, "Northwind Phantom Holdings", null),
            entry(
                PEP, SubjectType.INDIVIDUAL, "Marietta Sandoval Reyna", LocalDate.of(1968, 8, 18)),
            entry(
                PEP,
                SubjectType.INDIVIDUAL,
                "Honorato Buenaventura Lacsamana",
                LocalDate.of(1955, 3, 25),
                "Honorato B. Lacsamana"),
            entry(
                PEP, SubjectType.INDIVIDUAL, "Leonora Quimbo Invencion", LocalDate.of(1972, 9, 9)),
            entry(
                PEP,
                SubjectType.INDIVIDUAL,
                "Faustino Dimaculangan Ficticio",
                LocalDate.of(1960, 11, 11)),
            entry(
                PEP,
                SubjectType.INDIVIDUAL,
                "Rosalinda Macaraeg Imbento",
                LocalDate.of(1977, 5, 5)),
            entry(
                PEP,
                SubjectType.INDIVIDUAL,
                "Teodoro Villafuerte Kathangisip",
                LocalDate.of(1949, 7, 4)),
            entry(
                PEP, SubjectType.INDIVIDUAL, "Esperanza Magbanua Likha", LocalDate.of(1983, 2, 28)),
            entry(
                PEP, SubjectType.INDIVIDUAL, "Crisanto Padilla Gawain", LocalDate.of(1964, 12, 1)),
            entry(
                INTERNAL,
                SubjectType.INDIVIDUAL,
                "Gideon Farrow Pellucid",
                LocalDate.of(1987, 3, 3)),
            entry(
                INTERNAL,
                SubjectType.INDIVIDUAL,
                "Ysolde Marchetti Vane",
                LocalDate.of(1990, 10, 10),
                "Ysolde Vane"),
            entry(
                INTERNAL, SubjectType.ENTITY, "Hollowmere Assurance Brokers Fictional Inc.", null),
            entry(
                INTERNAL,
                SubjectType.INDIVIDUAL,
                "Percival Thorne Umbry",
                LocalDate.of(1971, 1, 21)),
            entry(
                INTERNAL,
                SubjectType.INDIVIDUAL,
                "Ondine Castellar Wray",
                LocalDate.of(1985, 6, 16)),
            entry(
                INTERNAL,
                SubjectType.INDIVIDUAL,
                "Aurelio Montclair Nox",
                LocalDate.of(1978, 8, 28)));
    ScreeningSubject intended = person("Jose Miguel", "Lopez", "Reyes", LocalDate.of(1979, 11, 2));
    List<ScreeningSubject> others =
        List.of(
            person("Maria Clara", "Reyes", "Santos", LocalDate.of(1984, 3, 12)),
            person("Maria Clara", "Reyes", "Santos", LocalDate.of(1984, 3, 14)),
            person("Antonio Luis", "Dizon", "Garcia", LocalDate.of(1990, 7, 21)),
            person("Carmela Isabel", "Santos", "Villanueva", LocalDate.of(1972, 1, 30)),
            person("Rafael Jose", "Castro", "Mendoza", LocalDate.of(1988, 5, 5)),
            person("Lorna Faye", null, "Aquino", null),
            person("Benjamin Tomas", "Ramos", "Cruz", LocalDate.of(1965, 9, 9)),
            person("Stephanie Ann", "Go", "Lim", LocalDate.of(1993, 12, 24)),
            person("Jose Antonio", null, "Reyes", LocalDate.of(1979, 11, 2)),
            person("Ana", "Cruz", "Villanueva", LocalDate.of(1991, 7, 21)),
            corporate("Pacific Harbor Logistics Inc."),
            corporate("Luzon Agri-Industrial Corp."),
            corporate("Bayside Builders Co."),
            corporate("Metro Dental Clinic Partners"));
    for (ScreeningSubject s : others) {
      for (ListedEntry e : entries) {
        assertThat(PairMatcher.match(s, e, DEMO))
            .as(s.names() + " vs " + e.primaryName())
            .isEmpty();
      }
    }
    List<String> hits =
        entries.stream()
            .filter(e -> PairMatcher.match(intended, e, DEMO).isPresent())
            .map(ListedEntry::primaryName)
            .toList();
    assertThat(hits).containsExactly("Jose Miguel Lopez Reyes");
  }
}
