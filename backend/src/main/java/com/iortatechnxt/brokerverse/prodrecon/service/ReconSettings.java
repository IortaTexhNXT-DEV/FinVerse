package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.prodrecon.service.ReconMatcher.MatchKey;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Business parameters of production reconciliation: the tolerance ({@code RECON_TOLERANCE},
 * PRCID.026), the pairing keys ({@code RECON_MATCH_KEYS}, PRCID.022/027, OQ30) and the file naming
 * pattern of the register ({@code PRODRECON_FILE_PATTERN}, PRCID.004, OQ29).
 */
@Component
public class ReconSettings {

  /** Default naming pattern while BDOI's convention is parked (OQ29). */
  public static final String DEFAULT_PATTERN = "<INSURER>_PRODREG_<yyyyMM>_<seq>";

  private static final BigDecimal DEFAULT_TOLERANCE = new BigDecimal("1.00");
  private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final SystemParameterService parameters;

  /**
   * Creates the settings.
   *
   * @param parameters business parameters
   */
  public ReconSettings(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * The amount tolerance.
   *
   * @return tolerance, 1.00 when unset
   */
  public BigDecimal tolerance() {
    String value = parameters.text("RECON_TOLERANCE", "").strip();
    return value.isEmpty() ? DEFAULT_TOLERANCE : new BigDecimal(value);
  }

  /**
   * The pairing keys in order.
   *
   * @return keys; invoice then policy number when unset
   */
  public List<MatchKey> keys() {
    List<MatchKey> keys = new ArrayList<>();
    for (String item : parameters.items("RECON_MATCH_KEYS")) {
      keys.add(MatchKey.valueOf(item.strip().toUpperCase(Locale.ROOT)));
    }
    return keys.isEmpty() ? List.of(MatchKey.INVOICE_NO, MatchKey.POLICY_NO) : keys;
  }

  /**
   * The file name of a register.
   *
   * @param insurerCode insurer
   * @param month production month
   * @param seq sequence of the extract (unique extract number)
   * @param today today
   * @return file name ending in .xlsx
   */
  public String fileName(String insurerCode, LocalDate month, String seq, LocalDate today) {
    String pattern = parameters.text("PRODRECON_FILE_PATTERN", "").strip();
    String name =
        (pattern.isEmpty() ? DEFAULT_PATTERN : pattern)
            .replace("<INSURER>", insurerCode)
            .replace("<yyyyMM>", MONTH.format(month))
            .replace("<yyyyMMdd>", DAY.format(today))
            .replace("<seq>", seq)
            .replaceAll("[^A-Za-z0-9_.-]", "_");
    return name.toLowerCase(Locale.ROOT).endsWith(".xlsx") ? name : name + ".xlsx";
  }
}
