package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.finreport.domain.ScheduleDefinition;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleDefinitionService;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleEngine;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleEngine.ScheduleOutput;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleEngine.ScheduleRequest;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * GL-SCHEDULE - the account schedule engine as a report (FRBS 3.2.0, Appendix A II-IV; design
 * section 10): runs the definition named by {@code schedule} ({@code GARD-*}, {@code SUBS-*},
 * {@code SCH-*}) for a company and period, so every schedule of the BDOI report pack is viewed and
 * exported (Excel, PDF, ODS, CSV) through the Report Centre, with the report archive and batches.
 */
@Component
public class AccountScheduleReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "GL-SCHEDULE";

  /** Parameter naming the schedule definition. */
  public static final String SCHEDULE = "schedule";

  private static final String COMPANY = "companyId";
  private static final String FROM = "fromDate";
  private static final String AS_OF = "asOf";
  private static final String BRANCH = "branchId";

  private final ScheduleDefinitionService definitions;
  private final ScheduleEngine engine;

  /**
   * Creates the report.
   *
   * @param definitions schedule definitions
   * @param engine schedule engine
   */
  public AccountScheduleReport(ScheduleDefinitionService definitions, ScheduleEngine engine) {
    this.definitions = definitions;
    this.engine = engine;
  }

  @Override
  public ReportMetadata metadata() {
    return ReportMetadata.frbs(
        CODE,
        "Account Schedule",
        "Schedule of the BDOI report pack run from its definition (GARD, subsidiaries, schedules and"
            + " ageing)",
        List.of(
            ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.required(SCHEDULE, "Schedule code", ParameterType.TEXT)
                .withDefault("SCH-PR-PHP"),
            ParameterSpec.optional(
                FROM, "Period from (default: first day of the month)", ParameterType.DATE),
            ParameterSpec.required(AS_OF, "As of", ParameterType.DATE).withDefault("TODAY"),
            ParameterSpec.optional(BRANCH, "Branch", ParameterType.BRANCH)));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    ScheduleDefinition def = definitions.get(p.text(SCHEDULE));
    ScheduleOutput out =
        engine.run(
            def,
            new ScheduleRequest(
                p.longValue(COMPANY),
                p.optionalDate(FROM).orElse(null),
                p.date(AS_OF),
                p.optionalLong(BRANCH).orElse(null)));
    TabularReportBuilder builder =
        TabularReportBuilder.of(p).columns(out.columns()).rows(out.rows()).presorted();
    out.notes().forEach(builder::note);
    ReportResult r = builder.build();
    return new ReportResult(
        CODE,
        def.getCode() + " " + def.getName(),
        r.parameterEcho(),
        r.columns(),
        r.rows(),
        r.notes());
  }
}
