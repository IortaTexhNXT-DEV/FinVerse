package com.iortatechnxt.finverse.closing.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.organization.domain.Company;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.subledger.domain.OpenItemRepository;
import com.iortatechnxt.finverse.subledger.domain.OpenItemStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revaluation of outstanding foreign currency open items (receivables and payables) at the CLOSING
 * rate, for information only (the GL is revalued at account level by {@link
 * FxRevaluationCalculator}; nothing is posted per open item).
 */
@Component
public class OpenItemRevaluation {

  private static final int WORK_SCALE = 10;

  private final OpenItemRepository openItems;
  private final OrganizationService organization;
  private final ClosingRates rates;

  /**
   * Creates the component.
   *
   * @param openItems open item repository
   * @param organization organization service
   * @param rates closing rate lookup
   */
  public OpenItemRevaluation(
      OpenItemRepository openItems, OrganizationService organization, ClosingRates rates) {
    this.openItems = openItems;
    this.organization = organization;
    this.rates = rates;
  }

  /**
   * Revalues outstanding foreign currency open items.
   *
   * @param companyId company
   * @param asOf date
   * @return revalued items (items without a closing rate are left out)
   */
  @Transactional(readOnly = true)
  public List<OpenItemLine> revalue(Long companyId, LocalDate asOf) {
    Company company = organization.getCompany(companyId);
    Map<String, Optional<BigDecimal>> closing = new HashMap<>();
    List<OpenItemLine> lines = new ArrayList<>();
    for (OpenItem i :
        openItems.findOutstanding(
            companyId, EnumSet.of(OpenItemStatus.OPEN, OpenItemStatus.PARTIALLY_SETTLED), asOf)) {
      if (!company.getBaseCurrency().equals(i.getCurrency())) {
        closing
            .computeIfAbsent(
                i.getCurrency(), c -> rates.closing(company.getBaseCurrency(), c, asOf))
            .ifPresent(rate -> lines.add(line(i, rate)));
      }
    }
    return lines;
  }

  private static OpenItemLine line(OpenItem i, BigDecimal rate) {
    BigDecimal outstanding = i.outstanding();
    BigDecimal booked =
        Money.round(
            i.getBaseAmount()
                .multiply(outstanding)
                .divide(i.getAmount(), WORK_SCALE, RoundingMode.HALF_EVEN));
    BigDecimal revalued = Money.convert(outstanding, rate);
    BigDecimal sign =
        i.getDirection() == ItemDirection.DEBIT ? BigDecimal.ONE : BigDecimal.ONE.negate();
    return new OpenItemLine(
        i.getPartyCode(),
        i.getDocumentNo(),
        i.getDirection(),
        i.getCurrency(),
        outstanding,
        booked,
        rate,
        revalued,
        revalued.subtract(booked).multiply(sign));
  }

  /**
   * Revalued open item.
   *
   * @param partyCode party
   * @param documentNo document
   * @param direction DEBIT (receivable) or CREDIT (payable)
   * @param currency currency
   * @param outstanding outstanding foreign currency amount
   * @param bookedBase outstanding base amount at the document rate
   * @param closingRate closing rate
   * @param revaluedBase outstanding at the closing rate
   * @param gainLoss unrealized gain (positive) or loss
   */
  public record OpenItemLine(
      String partyCode,
      String documentNo,
      ItemDirection direction,
      String currency,
      BigDecimal outstanding,
      BigDecimal bookedBase,
      BigDecimal closingRate,
      BigDecimal revaluedBase,
      BigDecimal gainLoss) {}
}
