package com.iortatechnxt.brokerverse.eb.report;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.report.core.CodeSetSource;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The Employee Benefits teams offered by the Team filter of the EB reports: the usable values of
 * the list {@value #LIST}, maintained per client on the list of values screen.
 */
@Component
public class EbTeamCodes implements CodeSetSource {

  /** Key of the source. */
  public static final String SOURCE = "eb.team";

  /** List of values of the teams. */
  public static final String LIST = "EB_TEAM";

  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the source.
   *
   * @param lovs lists of values
   * @param clock clock
   */
  public EbTeamCodes(LovService lovs, Clock clock) {
    this.lovs = lovs;
    this.clock = clock;
  }

  @Override
  public String source() {
    return SOURCE;
  }

  @Override
  public List<CodeOption> options(Long companyId) {
    return lovs.options(LIST, BusinessClock.today(clock)).stream()
        .map(e -> new CodeOption(e.code(), e.label()))
        .toList();
  }
}
