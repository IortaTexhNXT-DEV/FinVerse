package com.iortatechnxt.brokerverse.csf.cases.report;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseStatistics.Criteria;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared parameters of the case reports (FR-CSF-059; FRS section 6.1): company, the creation date
 * range and the filters of the Case Dashboard, viewed and exported with {@code CSF_CASE_REPORT}.
 */
final class CaseReportSupport {

  static final String COMPANY = "companyId";
  static final String FROM = "fromDate";
  static final String TO = "toDate";
  static final String TYPE = "caseType";
  static final String PRIORITY = "priority";
  static final String STATUS = "status";
  static final String SEGMENT = "marketSegment";
  static final String UNIT = "fulfilmentUnit";

  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm", Locale.ENGLISH);
  private static final DateTimeFormatter DAY =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

  private CaseReportSupport() {}

  /**
   * The metadata of a case report.
   *
   * @param code code
   * @param title title
   * @param description description
   * @param withStatus whether the report filters on the status
   * @return metadata
   */
  static ReportMetadata metadata(
      String code, String title, String description, boolean withStatus) {
    List<ParameterSpec> specs = new ArrayList<>();
    specs.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
    specs.add(
        ParameterSpec.required(FROM, "Created From", ParameterType.DATE)
            .withDefault("MONTH_START"));
    specs.add(ParameterSpec.required(TO, "Created To", ParameterType.DATE).withDefault("TODAY"));
    specs.add(ParameterSpec.lookup(TYPE, "Case Type", CsfCaseCodeSources.TYPE, false));
    specs.add(ParameterSpec.lookup(PRIORITY, "Priority", CsfCaseCodeSources.PRIORITY, false));
    if (withStatus) {
      specs.add(ParameterSpec.lookup(STATUS, "Status", CsfCaseCodeSources.STATUS, false));
    }
    specs.add(ParameterSpec.lookup(SEGMENT, "Market Segment", CsfCaseCodeSources.SEGMENT, false));
    specs.add(ParameterSpec.lookup(UNIT, "Fulfilment Unit", CsfCaseCodeSources.UNIT, false));
    return new ReportMetadata(
        code,
        title,
        ReportCategory.CUSTOMER_SERVICE,
        description,
        specs,
        Permission.CSF_CASE_REPORT,
        Permission.CSF_CASE_REPORT,
        true);
  }

  /**
   * The criteria of the parameters.
   *
   * @param p parameters
   * @return criteria
   */
  static Criteria criteria(ReportParameters p) {
    return new Criteria(
        p.longValue(COMPANY),
        p.date(FROM),
        p.date(TO),
        p.optionalText(TYPE).orElse(null),
        p.optionalText(PRIORITY).orElse(null),
        p.optionalText(STATUS).orElse(null),
        p.optionalText(SEGMENT).orElse(null),
        p.optionalText(UNIT).orElse(null));
  }

  /**
   * A time as dd-MMM-yyyy HH:mm in the business zone.
   *
   * @param i time
   * @return text, null for null
   */
  static String time(Instant i) {
    return i == null ? null : TIME.format(i.atZone(BusinessClock.zone()));
  }

  /**
   * A date as dd-MMM-yyyy.
   *
   * @param d date
   * @return text, null for null
   */
  static String day(java.time.LocalDate d) {
    return d == null ? null : DAY.format(d);
  }
}
