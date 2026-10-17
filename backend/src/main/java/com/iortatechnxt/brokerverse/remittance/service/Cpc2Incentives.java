package com.iortatechnxt.brokerverse.remittance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveCriteria;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveValueBasis;
import com.iortatechnxt.brokerverse.catalog.service.IncentiveCriteriaService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * CPC2 incentive criteria for remittance (DIS 3.29.2; ACCOUNTING_DISBURSEMENT_DESIGN 12.2): the
 * active criteria of code {@code CPC2} maintained by TSU on the products matrix (catalog, PMADD07)
 * decide which invoices earn CPC2 (packaged Fire and Motor products, segment, insurer, on the
 * booking date) and at which rate. Only RATE criteria are applied; fixed amounts and rules wait for
 * the CPC2 definition (AQ24, OQ39, PQ04). The optional rule parameter {@code minimumPremium}
 * excludes invoices whose gross premium is below it.
 */
@Component
public class Cpc2Incentives {

  /** Code of the CPC2 criterion in the catalog. */
  public static final String CODE = "CPC2";

  private static final String MINIMUM_PREMIUM = "minimumPremium";

  private final IncentiveCriteriaService criteria;
  private final ObjectMapper json;

  /**
   * Creates the lookup.
   *
   * @param criteria catalog incentive criteria
   * @param json JSON (rule parameters)
   */
  public Cpc2Incentives(IncentiveCriteriaService criteria, ObjectMapper json) {
    this.criteria = criteria;
    this.json = json;
  }

  /**
   * The CPC2 criteria of a company, loaded once per extraction run.
   *
   * @param companyId company
   * @return criteria with a rate
   */
  public Table table(Long companyId) {
    return new Table(
        criteria.list(companyId).stream()
            .filter(c -> CODE.equals(c.getCode()))
            .filter(c -> c.getValueBasis() == IncentiveValueBasis.RATE && c.getValue() != null)
            .map(c -> new Row(c, minimumPremium(c.getRuleParams())))
            .toList());
  }

  private BigDecimal minimumPremium(String params) {
    if (params == null || params.isBlank()) {
      return null;
    }
    try {
      JsonNode node = json.readTree(params).get(MINIMUM_PREMIUM);
      return node == null || !node.isNumber() ? null : node.decimalValue();
    } catch (IOException ex) {
      return null;
    }
  }

  /**
   * One CPC2 criterion with its minimum premium.
   *
   * @param criterion catalog criterion
   * @param minimumPremium minimum gross premium, null for none
   */
  public record Row(IncentiveCriteria criterion, BigDecimal minimumPremium) {}

  /**
   * The CPC2 criteria of a company.
   *
   * @param rows criteria
   */
  public record Table(List<Row> rows) {

    /** Defensive copy. */
    public Table {
      rows = List.copyOf(rows);
    }

    /**
     * The CPC2 criterion an invoice earns, if any.
     *
     * @param invoice invoice
     * @return criterion code and rate
     */
    public Optional<Match> match(OpsInvoice invoice) {
      IncentiveCriteria.Facts facts =
          new IncentiveCriteria.Facts(
              invoice.getClassification().riskCode(),
              null,
              invoice.getClassification().segment(),
              null,
              invoice.getInsurerCode());
      return rows.stream()
          .filter(r -> r.criterion().appliesTo(facts, invoice.getBookingDate()))
          .filter(
              r ->
                  r.minimumPremium() == null
                      || invoice.getGrossPremium().compareTo(r.minimumPremium()) >= 0)
          .findFirst()
          .map(r -> new Match(r.criterion().getCode(), r.criterion().getValue()));
    }
  }

  /**
   * The CPC2 criterion of an invoice.
   *
   * @param code criterion code
   * @param rate rate in percent
   */
  public record Match(String code, BigDecimal rate) {}
}
