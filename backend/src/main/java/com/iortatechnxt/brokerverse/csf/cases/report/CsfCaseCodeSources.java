package com.iortatechnxt.brokerverse.csf.cases.report;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.csf.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseCodes;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.report.core.CodeSetSource;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The values offered by the filters of the case reports (FR-CSF-059): the case lists of values and
 * the case statuses.
 */
@Configuration(proxyBeanMethods = false)
public class CsfCaseCodeSources {

  /** Source of the case types. */
  public static final String TYPE = "csf.caseType";

  /** Source of the priorities. */
  public static final String PRIORITY = "csf.casePriority";

  /** Source of the market segments. */
  public static final String SEGMENT = "csf.caseSegment";

  /** Source of the fulfilment units. */
  public static final String UNIT = "csf.fulfilmentUnit";

  /** Source of the statuses. */
  public static final String STATUS = "csf.caseStatus";

  /**
   * Case types.
   *
   * @param lovs lists
   * @param clock clock
   * @return source
   */
  @Bean
  public CodeSetSource csfCaseTypeCodes(LovService lovs, Clock clock) {
    return new LovSource(TYPE, CsfCaseCodes.LOV_TYPE, lovs, clock);
  }

  /**
   * Priorities.
   *
   * @param lovs lists
   * @param clock clock
   * @return source
   */
  @Bean
  public CodeSetSource csfCasePriorityCodes(LovService lovs, Clock clock) {
    return new LovSource(PRIORITY, CsfCaseCodes.LOV_PRIORITY, lovs, clock);
  }

  /**
   * Market segments.
   *
   * @param lovs lists
   * @param clock clock
   * @return source
   */
  @Bean
  public CodeSetSource csfCaseSegmentCodes(LovService lovs, Clock clock) {
    return new LovSource(SEGMENT, CsfCaseCodes.LOV_SEGMENT, lovs, clock);
  }

  /**
   * Fulfilment units.
   *
   * @param lovs lists
   * @param clock clock
   * @return source
   */
  @Bean
  public CodeSetSource csfFulfilmentUnitCodes(LovService lovs, Clock clock) {
    return new LovSource(UNIT, CsfCaseCodes.LOV_UNIT, lovs, clock);
  }

  /**
   * Statuses.
   *
   * @return source
   */
  @Bean
  public CodeSetSource csfCaseStatusCodes() {
    return new CodeSetSource() {
      @Override
      public String source() {
        return STATUS;
      }

      @Override
      public List<CodeOption> options(Long companyId) {
        return Arrays.stream(CaseStatus.values())
            .map(s -> new CodeOption(s.name(), s.label()))
            .toList();
      }
    };
  }

  /** The active values of a list of values. */
  private record LovSource(String source, String list, LovService lovs, Clock clock)
      implements CodeSetSource {

    @Override
    public List<CodeOption> options(Long companyId) {
      return lovs.options(list, BusinessClock.today(clock)).stream()
          .map(e -> new CodeOption(e.code(), e.label()))
          .toList();
    }
  }
}
