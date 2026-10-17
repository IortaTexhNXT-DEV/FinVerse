package com.iortatechnxt.brokerverse.eb.comparative.service;

import com.iortatechnxt.brokerverse.eb.domain.EbProposal;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalFactor;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalItem;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalLine;
import com.iortatechnxt.brokerverse.eb.domain.EbTorItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * The rows of a comparative (BRID-010; FR-EB-041), kept as its JSON snapshot: one column per
 * validated proposal; per benefit line the premium and TSI of each proposal, its plans and the
 * lowest premium; per TOR item what each proposal offers and whether it deviates; per capability
 * factor each proposal's value and rating.
 *
 * @param proposals the proposals compared (columns)
 * @param lines premium per benefit line
 * @param items answers per TOR item
 * @param factors capability factors
 */
public record ComparativeMatrix(
    List<Column> proposals, List<LineRow> lines, List<ItemRow> items, List<FactorRow> factors) {

  /**
   * Builds the matrix.
   *
   * @param validated validated proposals, one per insurer
   * @param benefitLines benefit lines compared
   * @param torItems items of the TOR released, may be empty
   * @param names labels of the insurers, benefit lines and factors
   * @return matrix
   */
  public static ComparativeMatrix of(
      List<EbProposal> validated,
      List<String> benefitLines,
      List<EbTorItem> torItems,
      Labels names) {
    List<Column> columns = validated.stream().map(p -> Column.of(p, names)).toList();
    List<LineRow> lines = benefitLines.stream().map(l -> LineRow.of(l, validated, names)).toList();
    List<ItemRow> items = torItems.stream().map(i -> ItemRow.of(i, validated, names)).toList();
    Map<String, FactorRow> factors = new LinkedHashMap<>();
    for (EbProposal p : validated) {
      for (EbProposalFactor f : p.getFactors()) {
        factors
            .computeIfAbsent(
                f.getFactorCode(),
                c -> new FactorRow(c, names.factor().apply(c), new LinkedHashMap<>()))
            .ratings()
            .put(p.getId().toString(), new Rating(f.getFactorValue(), f.getRating()));
      }
    }
    return new ComparativeMatrix(columns, lines, items, List.copyOf(factors.values()));
  }

  /**
   * A proposal compared.
   *
   * @param proposalId proposal
   * @param proposalNo number
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param kind kind
   * @param versionNo version
   * @param currency currency
   * @param validUntil validity
   * @param terms terms and additional benefits
   * @param exclusions exclusions
   * @param totalPremium annual premium of every line
   */
  public record Column(
      Long proposalId,
      String proposalNo,
      String insurerCode,
      String insurerName,
      String kind,
      int versionNo,
      String currency,
      LocalDate validUntil,
      String terms,
      String exclusions,
      BigDecimal totalPremium) {

    static Column of(EbProposal p, Labels names) {
      BigDecimal total =
          p.getLines().stream()
              .map(EbProposalLine::getAnnualPremium)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      return new Column(
          p.getId(),
          p.getProposalNo(),
          p.getInsurerCode(),
          names.insurer().apply(p.getInsurerCode()),
          p.getKind().name(),
          p.getVersionNo(),
          p.getCurrency(),
          p.getValidUntil(),
          p.getTerms(),
          p.getExclusions(),
          total);
    }
  }

  /**
   * A benefit line compared.
   *
   * @param benefitLine benefit line
   * @param label its label
   * @param offers what each proposal offers, by proposal id
   * @param lowestProposalId proposal of the lowest premium, null when none offers the line
   * @param lowestPremium lowest premium, null when none
   */
  public record LineRow(
      String benefitLine,
      String label,
      Map<String, Offer> offers,
      Long lowestProposalId,
      BigDecimal lowestPremium) {

    static LineRow of(String line, List<EbProposal> validated, Labels names) {
      Map<String, Offer> offers = new LinkedHashMap<>();
      validated.stream()
          .filter(p -> p.offers(line))
          .forEach(p -> offers.put(p.getId().toString(), Offer.of(p, line)));
      Optional<EbProposal> lowest =
          validated.stream()
              .filter(p -> p.offers(line))
              .min(Comparator.comparing((EbProposal p) -> p.premiumOf(line)));
      return new LineRow(
          line,
          names.line().apply(line),
          offers,
          lowest.map(EbProposal::getId).orElse(null),
          lowest.map(p -> p.premiumOf(line)).orElse(null));
    }
  }

  /**
   * What a proposal offers on a benefit line.
   *
   * @param annualPremium annual premium of the line
   * @param sumInsured TSI of the line
   * @param plans premium per plan
   */
  public record Offer(BigDecimal annualPremium, BigDecimal sumInsured, List<Plan> plans) {

    static Offer of(EbProposal p, String line) {
      return new Offer(
          p.premiumOf(line),
          p.sumInsuredOf(line),
          p.getLines().stream()
              .filter(l -> l.getBenefitLine().equals(line))
              .map(
                  l ->
                      new Plan(
                          l.getPlanCode(),
                          l.getPlanName(),
                          l.getMembers(),
                          l.getPremiumRate(),
                          l.getAnnualPremium(),
                          l.getSumInsured()))
              .toList());
    }
  }

  /**
   * The premium of a plan.
   *
   * @param planCode plan
   * @param planName plan description
   * @param members members
   * @param premiumRate premium per member
   * @param annualPremium annual premium
   * @param sumInsured TSI
   */
  public record Plan(
      String planCode,
      String planName,
      Integer members,
      BigDecimal premiumRate,
      BigDecimal annualPremium,
      BigDecimal sumInsured) {}

  /**
   * A TOR item compared.
   *
   * @param torItemId TOR item
   * @param benefitLine benefit line label
   * @param planCode plan, may be null
   * @param description item
   * @param requirement requirement
   * @param answers what each proposal offers, by proposal id
   */
  public record ItemRow(
      Long torItemId,
      String benefitLine,
      String planCode,
      String description,
      String requirement,
      Map<String, Answer> answers) {

    static ItemRow of(EbTorItem item, List<EbProposal> validated, Labels names) {
      Map<String, Answer> answers = new LinkedHashMap<>();
      for (EbProposal p : validated) {
        p.getItems().stream()
            .filter(a -> a.getTorItemId().equals(item.getId()))
            .findFirst()
            .map(Answer::of)
            .ifPresent(a -> answers.put(p.getId().toString(), a));
      }
      return new ItemRow(
          item.getId(),
          names.line().apply(item.getBenefitLine()),
          item.getPlanCode(),
          item.getDescription(),
          item.getRequirement(),
          answers);
    }
  }

  /**
   * A proposal's answer to a TOR item.
   *
   * @param offeredValue what is offered
   * @param deviation whether it deviates
   * @param remark remark
   */
  public record Answer(String offeredValue, boolean deviation, String remark) {

    static Answer of(EbProposalItem item) {
      return new Answer(item.getOfferedValue(), item.isDeviation(), item.getRemark());
    }
  }

  /**
   * A capability factor compared.
   *
   * @param factorCode factor
   * @param label label
   * @param ratings each proposal's value and rating, by proposal id
   */
  public record FactorRow(String factorCode, String label, Map<String, Rating> ratings) {}

  /**
   * A proposal's value and rating of a factor.
   *
   * @param value value
   * @param rating rating 1 to 5
   */
  public record Rating(String value, Integer rating) {}

  /**
   * Labels of the codes shown.
   *
   * @param insurer insurer name of a code
   * @param line label of a benefit line
   * @param factor label of a capability factor
   */
  public record Labels(
      Function<String, String> insurer,
      Function<String, String> line,
      Function<String, String> factor) {}
}
