package com.iortatechnxt.brokerverse.renewal.candidate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.iortatechnxt.brokerverse.report.core.CodeSetSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** The filter and dialog lists of Renewal offer units and segments by name, not by code. */
class RenewalCodeSetsTest {

  private static String queryOf(String key) {
    NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    CodeSetSource source =
        new RenewalCodeSets()
            .renewalColumnSources(jdbc).sources().stream()
                .filter(s -> s.source().equals(key))
                .findFirst()
                .orElseThrow();
    source.options(1L);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).query(sql.capture(), anyMap(), ArgumentMatchers.<RowMapper<Object>>any());
    return sql.getValue();
  }

  @Test
  void unitsAreOfferedByTheirNames() {
    for (String key : new String[] {"renewal.unit", "renewal.region", "renewal.department"}) {
      assertThat(queryOf(key)).contains("cat_sales_unit").contains("coalesce(s.name");
    }
  }

  @Test
  void segmentsAreOfferedByTheirLabels() {
    assertThat(queryOf("renewal.segment")).contains("MARKET_SEGMENT").contains("l.label");
  }
}
