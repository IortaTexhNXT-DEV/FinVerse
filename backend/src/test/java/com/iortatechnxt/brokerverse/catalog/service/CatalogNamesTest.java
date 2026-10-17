package com.iortatechnxt.brokerverse.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.catalog.domain.CoverTypeRepository;
import com.iortatechnxt.brokerverse.catalog.domain.Coverage;
import com.iortatechnxt.brokerverse.catalog.domain.CoverageRepository;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfileRepository;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLineRepository;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Coverage names as users read them in the comparative and the documents of a package request. */
class CatalogNamesTest {

  private final CoverageRepository coverages = mock(CoverageRepository.class);
  private final CatalogNames names =
      new CatalogNames(
          mock(RiskProductRepository.class),
          mock(ProductLineRepository.class),
          mock(InsurerProfileRepository.class),
          mock(CoverTypeRepository.class),
          coverages);

  @Test
  void namesACoverageOfTheLineByItsCatalogName() {
    Coverage quake = mock(Coverage.class);
    when(quake.getName()).thenReturn("Earthquake Fire and Shock");
    when(coverages.findByLineCodeAndCode("PROPERTY", "EARTHQUAKE")).thenReturn(Optional.of(quake));
    assertThat(names.coverage("PROPERTY", "EARTHQUAKE")).isEqualTo("Earthquake Fire and Shock");
  }

  @Test
  void readsACoverageOutsideTheCatalogOfTheLineAsWordsNeverItsCode() {
    when(coverages.findByLineCodeAndCode("PROPERTY", "RIOT_STRIKE")).thenReturn(Optional.empty());
    assertThat(names.coverage("PROPERTY", "RIOT_STRIKE")).isEqualTo("Riot Strike");
    assertThat(names.coverage("PROPERTY", null)).isEmpty();
  }
}
