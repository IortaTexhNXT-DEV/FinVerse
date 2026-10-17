package com.iortatechnxt.brokerverse.payables.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import java.util.List;

/**
 * Parameter names and declarations shared by the payables reports. A blank From/To range bound
 * means "all" (finance reports spec 1.2).
 */
final class ReportParams {

  static final String PARTY_FROM = "partyFrom";
  static final String PARTY_TO = "partyTo";
  static final String MAIN_FROM = "mainAccountFrom";
  static final String MAIN_TO = "mainAccountTo";
  static final String BASIS = "orderBy";
  static final String CURRENCY_MODE = "currency";
  static final String PARTY_TYPE = "partyType";
  static final String DIVISION_FROM = "divisionFrom";
  static final String DIVISION_TO = "divisionTo";
  static final String DEPARTMENT_FROM = "departmentFrom";
  static final String DEPARTMENT_TO = "departmentTo";
  static final String BANK_FROM = "bankFrom";
  static final String BANK_TO = "bankTo";
  static final String DUE_DATE = "DUE_DATE";
  static final String DOCUMENT_DATE = "DOCUMENT_DATE";
  static final String BASE = "BASE";
  static final String FOREIGN = "FOREIGN";

  private ReportParams() {}

  static List<ParameterSpec> range(String fromName, String toName, String label) {
    return List.of(
        ParameterSpec.optional(fromName, label + " From", ParameterType.TEXT),
        ParameterSpec.optional(toName, label + " To", ParameterType.TEXT));
  }

  static ParameterSpec basis() {
    return ParameterSpec.select(
        BASIS, "Order By (ageing basis)", List.of(DUE_DATE, DOCUMENT_DATE), DUE_DATE);
  }

  static ParameterSpec currencyMode() {
    return ParameterSpec.select(CURRENCY_MODE, "Currency", List.of(BASE, FOREIGN), BASE);
  }

  static ParameterSpec partyType() {
    return ParameterSpec.select(
        PARTY_TYPE,
        "Creditor Type",
        List.of(
            CreditorLedger.VENDORS,
            CreditorLedger.ALL_CREDITORS,
            "SUPPLIER",
            "GARAGE",
            "SURVEYOR",
            "AGENT",
            "BROKER",
            "REINSURER"),
        CreditorLedger.VENDORS);
  }

  static List<ParameterSpec> divisionDepartmentBank() {
    return List.of(
        ParameterSpec.optional(DIVISION_FROM, "Division (Branch) From", ParameterType.TEXT),
        ParameterSpec.optional(DIVISION_TO, "Division (Branch) To", ParameterType.TEXT),
        ParameterSpec.optional(DEPARTMENT_FROM, "Department From", ParameterType.TEXT),
        ParameterSpec.optional(DEPARTMENT_TO, "Department To", ParameterType.TEXT),
        ParameterSpec.optional(BANK_FROM, "Bank Account From", ParameterType.TEXT),
        ParameterSpec.optional(BANK_TO, "Bank Account To", ParameterType.TEXT));
  }

  static String text(ReportParameters p, String name) {
    return p.optionalText(name).orElse(null);
  }

  static boolean inRange(String value, String from, String to) {
    String v = value == null ? "" : value;
    return (from == null || v.compareTo(from) >= 0) && (to == null || v.compareTo(to) <= 0);
  }
}
