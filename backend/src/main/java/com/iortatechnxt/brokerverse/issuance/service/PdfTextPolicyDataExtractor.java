package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.issuance.domain.ExtractionPattern;
import com.iortatechnxt.brokerverse.issuance.domain.ExtractionPatternRepository;
import com.iortatechnxt.brokerverse.issuance.service.PolicyTextParser.Rule;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link PolicyDataExtractor}: reads the text layer of the e-policy PDF (OpenPDF) and
 * applies the patterns of the insurer, then the default patterns (BRNB.104). A scanned PDF without
 * a text layer yields nothing and is completed by the reviewer; OCR is parked (Q24).
 */
@Component
public class PdfTextPolicyDataExtractor implements PolicyDataExtractor {

  private static final int MAX_PAGES = 50;

  private final ExtractionPatternRepository patterns;

  /**
   * Creates the extractor.
   *
   * @param patterns extraction patterns
   */
  public PdfTextPolicyDataExtractor(ExtractionPatternRepository patterns) {
    this.patterns = patterns;
  }

  @Override
  @Transactional(readOnly = true)
  public ExtractedPolicy extract(byte[] pdf, String insurerCode) {
    String text = text(pdf);
    if (text.isBlank()) {
      return ExtractedPolicy.NONE;
    }
    return PolicyTextParser.parse(text, rules(insurerCode));
  }

  /**
   * The patterns of an insurer followed by the defaults.
   *
   * @param insurerCode insurer, may be null
   * @return rules
   */
  List<Rule> rules(String insurerCode) {
    return patterns.findByActiveTrueOrderByPriorityAscIdAsc().stream()
        .filter(p -> p.getInsurerCode() == null || p.getInsurerCode().equals(insurerCode))
        .sorted(Comparator.comparing((ExtractionPattern p) -> p.getInsurerCode() == null))
        .map(p -> new Rule(p.getField(), p.getPattern(), p.getDateFormat()))
        .toList();
  }

  /**
   * The text of a PDF, page by page; empty when the file cannot be read.
   *
   * @param pdf bytes
   * @return text
   */
  static String text(byte[] pdf) {
    try (PdfReader reader = new PdfReader(pdf)) {
      PdfTextExtractor extractor = new PdfTextExtractor(reader);
      StringBuilder sb = new StringBuilder();
      int pages = Math.min(reader.getNumberOfPages(), MAX_PAGES);
      for (int page = 1; page <= pages; page++) {
        sb.append(extractor.getTextFromPage(page)).append('\n');
      }
      return sb.toString();
    } catch (IOException | RuntimeException e) {
      return "";
    }
  }
}
