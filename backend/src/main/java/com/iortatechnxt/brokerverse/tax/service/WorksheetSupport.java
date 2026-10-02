package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.tax.domain.NormalBalance;
import com.iortatechnxt.brokerverse.tax.domain.TaxCode;
import com.iortatechnxt.brokerverse.tax.domain.TaxCodeRepository;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.domain.TaxType;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/** Shared helpers of the worksheet builders: sums and the ledger reconciliation. */
@Component
public class WorksheetSupport {

  private final TaxCodeRepository codes;
  private final TaxSourceQueries queries;

  /**
   * Creates the helper.
   *
   * @param codes tax codes (GL accounts per tax type)
   * @param queries ledger queries
   */
  public WorksheetSupport(TaxCodeRepository codes, TaxSourceQueries queries) {
    this.codes = codes;
    this.queries = queries;
  }

  /**
   * GL accounts of the authorized tax codes of a type.
   *
   * @param companyId company
   * @param type tax type
   * @return distinct account codes in code order
   */
  public List<String> accounts(Long companyId, TaxType type) {
    return codes.findByCompanyIdOrderByTaxTypeAscCodeAsc(companyId).stream()
        .filter(c -> c.getTaxType() == type && c.getRecordStatus() == RecordStatus.ACTIVE)
        .map(TaxCode::getGlAccountCode)
        .distinct()
        .sorted()
        .toList();
  }

  /**
   * Reconciles a documents total with the posted movement of the GL accounts of a tax type.
   *
   * @param companyId company
   * @param type tax type whose accounts are read
   * @param description what is reconciled
   * @param perDocuments documents total
   * @param period period
   * @param side natural side of the accounts
   * @return control, empty when no tax code of the type is configured
   */
  public Optional<LedgerControl> control(
      Long companyId,
      TaxType type,
      String description,
      BigDecimal perDocuments,
      TaxPeriod period,
      NormalBalance side) {
    List<String> accounts = accounts(companyId, type);
    if (accounts.isEmpty()) {
      return Optional.empty();
    }
    BigDecimal ledger =
        accounts.stream()
            .map(a -> queries.ledgerMovement(companyId, a, period.from(), period.to()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return Optional.of(
        new LedgerControl(
            String.join(",", accounts),
            description,
            Money.round(perDocuments),
            Money.round(side.present(ledger))));
  }

  /**
   * Sums a field of documents.
   *
   * @param items items
   * @param field field
   * @param <T> item type
   * @return sum rounded to centavos
   */
  public static <T> BigDecimal sum(Collection<T> items, Function<T, BigDecimal> field) {
    return Money.round(
        items.stream().map(field).map(Money::nz).reduce(BigDecimal.ZERO, BigDecimal::add));
  }
}
