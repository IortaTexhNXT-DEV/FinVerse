package com.iortatechnxt.brokerverse.renewal.letter.service;

import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.LetterType;
import com.iortatechnxt.brokerverse.renewal.domain.RaNotice;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The file names of the generated letters (FRRN.022.01, FRRN.023.02), by {@value #PARAMETER}: the
 * client's convention - {@code <Product>_RA_<First Notice|Last Notice>_<Reference>_MMDDYYYY.pdf}
 * for a Renewal Advice and {@code <Product>_<Letter>_<Reference>_MMDDYYYY.pdf} for the other
 * letters, the product being the letters of the risk code (MTR for MTR10) - or SYSTEM, the letter
 * number.
 */
@Component
public class LetterFileNames {

  /** Parameter: CLIENT or SYSTEM. */
  public static final String PARAMETER = "RNW_FILE_NAMES";

  private static final String PDF = ".pdf";
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMddyyyy");
  private static final Map<LetterType, String> CODES =
      Map.of(
          LetterType.NAL, "NAL",
          LetterType.NFR, "NRL",
          LetterType.NRNS_REMINDER, "Reminder",
          LetterType.NON_ACCEPTANCE, "NFR");

  private static final Map<String, String> ANNEX =
      Map.of(RenewalCodes.TEMPLATE_SFU, "SFU", RenewalCodes.TEMPLATE_RA_FFY, "Reminder");

  private final SystemParameterService parameters;

  /**
   * Creates the names.
   *
   * @param parameters setting
   */
  public LetterFileNames(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * The file name of a letter.
   *
   * @param c renewal account
   * @param type letter type
   * @param notice notice of a Renewal Advice, may be null
   * @param letterNo letter number
   * @param generatedOn generation date
   * @param templateCode template of the letter (SFU and FFY Reminder), may be null
   * @return file name
   */
  public String of(
      RenewalCandidate c,
      LetterType type,
      RaNotice notice,
      String letterNo,
      LocalDate generatedOn,
      String templateCode) {
    if ("SYSTEM".equals(parameters.text(PARAMETER, "CLIENT").strip())) {
      return letterNo + PDF;
    }
    if (RenewalCodes.TEMPLATE_SFU.equals(templateCode)) {
      return product(c) + "_SFU_" + c.getRenewalRef() + "_" + DATE.format(generatedOn) + ".pdf";
    }
    if (RenewalCodes.TEMPLATE_RA_FFY.equals(templateCode)) {
      return product(c)
          + "_Reminder_"
          + c.getRenewalRef()
          + "_"
          + DATE.format(generatedOn)
          + ".pdf";
    }
    return name(product(c), type, notice, c.getRenewalRef(), generatedOn);
  }

  /**
   * The client's file name of a letter.
   *
   * @param product product prefix
   * @param type letter type
   * @param notice notice of a Renewal Advice, may be null
   * @param reference reference number
   * @param generatedOn generation date
   * @return file name
   */
  static String name(
      String product, LetterType type, RaNotice notice, String reference, LocalDate generatedOn) {
    String letter =
        type == LetterType.RA
            ? "RA_" + (notice == RaNotice.SECOND ? "Last Notice" : "First Notice")
            : CODES.getOrDefault(type, type.name());
    return product + "_" + letter + "_" + reference + "_" + DATE.format(generatedOn) + PDF;
  }

  private static String product(RenewalCandidate c) {
    CandidateSnapshot.SnapshotProduct p = c.getSnapshot().product();
    String code = p == null || p.productCode() == null ? "RNW" : p.productCode();
    StringBuilder b = new StringBuilder();
    for (char ch : code.toCharArray()) {
      if (!Character.isLetter(ch)) {
        break;
      }
      b.append(ch);
    }
    return b.isEmpty() ? code : b.toString();
  }
}
