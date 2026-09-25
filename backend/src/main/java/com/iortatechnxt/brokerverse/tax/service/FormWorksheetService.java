package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificate;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.service.BirOutputQueries.FormLineDef;
import com.iortatechnxt.brokerverse.tax.service.BirOutputQueries.Movement;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes the new BIR form worksheets 0619-F, 1603, 1702-Q and 1702 (FRBS 3.2.0, Appendix A VII;
 * formats AQ07) from their line definitions (V703): ledger movements of account prefixes, sums of
 * lines, a rate (system parameter) on a line, and the tax of the certificates received (SAWT).
 */
@Service
@Transactional(readOnly = true)
public class FormWorksheetService {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int MONEY = 2;

  private final BirOutputQueries queries;
  private final ReceivedCertificateService certificates;
  private final SystemParameterService parameters;

  /**
   * Creates the service.
   *
   * @param queries ledger and definitions
   * @param certificates received certificates
   * @param parameters rates
   */
  public FormWorksheetService(
      BirOutputQueries queries,
      ReceivedCertificateService certificates,
      SystemParameterService parameters) {
    this.queries = queries;
    this.certificates = certificates;
    this.parameters = parameters;
  }

  /**
   * The lines of a form for a period.
   *
   * @param companyId company
   * @param formCode form ({@code 0619F}, {@code 1603}, {@code 1702Q}, {@code 1702})
   * @param period period of the ledger figures
   * @param creditPeriod period of the certificates credited
   * @return lines with their amounts
   */
  public List<LineValue> compute(
      Long companyId, String formCode, TaxPeriod period, TaxPeriod creditPeriod) {
    List<FormLineDef> defs = queries.formLines(formCode);
    if (defs.isEmpty()) {
      throw new BusinessRuleException("FORM_NOT_DEFINED", "Form " + formCode + " has no lines");
    }
    BigDecimal credits =
        certificates.inPeriod(companyId, creditPeriod).stream()
            .map(ReceivedCertificate::getTaxTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return calculate(
        defs,
        d -> {
          Movement m = queries.movement(companyId, d.selector(), period.from(), period.to());
          return "ACCOUNT_CREDITS".equals(d.kind())
              ? m.credit().subtract(m.debit())
              : m.debit().subtract(m.credit());
        },
        credits,
        key -> new BigDecimal(parameters.text(key, "0")));
  }

  /**
   * Evaluates the lines in order (pure; a line refers only to earlier lines).
   *
   * @param defs line definitions
   * @param accounts ledger figure of an account line
   * @param certificates tax of the certificates received
   * @param rates rate in percent of a parameter
   * @return lines with their amounts
   */
  public static List<LineValue> calculate(
      List<FormLineDef> defs,
      Function<FormLineDef, BigDecimal> accounts,
      BigDecimal certificates,
      Function<String, BigDecimal> rates) {
    Map<Integer, BigDecimal> values = new HashMap<>();
    List<LineValue> out = new ArrayList<>();
    for (FormLineDef d : defs) {
      String note = null;
      BigDecimal amount;
      switch (d.kind()) {
        case "ACCOUNT_CREDITS", "ACCOUNT_DEBITS" -> {
          if (d.selector().isEmpty()) {
            amount = BigDecimal.ZERO;
            note = "No account mapped yet (AQ07)";
          } else {
            amount = accounts.apply(d);
            note = "Accounts " + String.join(", ", d.selector());
          }
        }
        case "LINES" -> amount = sumOf(d.operand(), values);
        case "RATE" -> {
          String[] parts = d.operand().split(":");
          BigDecimal rate = rates.apply(parts[1].trim());
          amount =
              value(values, parts[0])
                  .max(BigDecimal.ZERO)
                  .multiply(rate)
                  .divide(HUNDRED, MONEY, RoundingMode.HALF_UP);
          note = rate.stripTrailingZeros().toPlainString() + "% (" + parts[1].trim() + ")";
        }
        case "CERTIFICATES" -> {
          amount = certificates;
          note = "Certificates received in the period";
        }
        default ->
            throw new BusinessRuleException("FORM_LINE_KIND", "Unknown line kind " + d.kind());
      }
      BigDecimal scaled = amount.setScale(MONEY, RoundingMode.HALF_UP);
      values.put(d.lineNo(), scaled);
      out.add(new LineValue(d.lineNo(), d.label(), scaled, note));
    }
    return out;
  }

  private static BigDecimal sumOf(String operand, Map<Integer, BigDecimal> values) {
    BigDecimal total = BigDecimal.ZERO;
    for (String ref : operand.split(",")) {
      String r = ref.trim();
      boolean minus = r.startsWith("-");
      BigDecimal v = value(values, minus ? r.substring(1) : r);
      total = minus ? total.subtract(v) : total.add(v);
    }
    return total;
  }

  private static BigDecimal value(Map<Integer, BigDecimal> values, String line) {
    return values.getOrDefault(Integer.valueOf(line.trim()), BigDecimal.ZERO);
  }

  /**
   * A computed line.
   *
   * @param lineNo line number
   * @param label label
   * @param amount amount
   * @param note how it was obtained
   */
  public record LineValue(int lineNo, String label, BigDecimal amount, String note) {}
}
