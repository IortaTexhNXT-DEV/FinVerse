package com.iortatechnxt.brokerverse.currency.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Currency}. */
public interface CurrencyRepository extends JpaRepository<Currency, String> {

  /**
   * Lists currencies ordered by code.
   *
   * @return currencies
   */
  List<Currency> findAllByOrderByCode();
}
