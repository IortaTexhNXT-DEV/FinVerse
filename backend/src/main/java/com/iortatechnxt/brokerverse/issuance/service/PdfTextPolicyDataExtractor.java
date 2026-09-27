package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.issuance.domain.ExtractionPattern;
import com.iortatechnxt.brokerverse.issuance.domain.ExtractionPatternRepository;
import com.iortatechnxt.brokerverse.issuance.service.PolicyTextParser.Rule;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link PolicyDataExtractor}: reads the text layer of the e-policy PDF (OpenPDF) and
 * applies the patterns of the insurer, then the default patterns (BRNB.104). A scanned PDF without
 * a text layer yields nothing and is completed by the reviewer; OCR is parked (Q24). Documents of
 * other kinds (submitted policies, BRIDSP-02) get a proposal of every field of their patterns; a
 * document without text goes through the {@link OcrEngine} first.
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
  @Override
  @Transactional(readOnly = true)
  public ExtractionProposal propose(ExtractionRequest request) {
    byte[] content = request.content();
    String text = text(content);
    if (text.isBlank()) {
      text = ocr.read(content).orElse("");
    }
    if (text.isBlank()) {
      return ExtractionProposal.notReadable();
    }
    Map<String, ExtractedValue> found = new LinkedHashMap<>();
    List<String> missing = new ArrayList<>();
    List<ExtractionPattern> kindPatterns =
        patterns.findByKindAndActiveTrueOrderByPriorityAscIdAsc(request.kind()).stream()
            .filter(
                p -> p.getInsurerCode() == null || p.getInsurerCode().equals(request.insurerCode()))
            .sorted(Comparator.comparing((ExtractionPattern p) -> p.getInsurerCode() == null))
            .toList();
    for (ExtractionPattern p : kindPatterns) {
      String field = p.getField().name();
      if (!found.containsKey(field)) {
        PolicyTextParser.firstMatch(text, p.getPattern())
            .ifPresent(
                v ->
                    found.put(
                        field,
                        new ExtractedValue(
                            v,
                            p.getInsurerCode() == null
                                ? DEFAULT_CONFIDENCE
                                : INSURER_CONFIDENCE)));
      }
    }
    kindPatterns.stream()
        .map(p -> p.getField().name())
        .distinct()
        .filter(f -> !found.containsKey(f))
        .forEach(missing::add);
    return new ExtractionProposal(
        found, true, missing.isEmpty() ? null : "Not found: " + String.join(", ", missing));
  }

  List<Rule> rules(String insurerCode) {
    return patterns
        .findByKindAndActiveTrueOrderByPriorityAscIdAsc(ExtractionPattern.KIND_EPOLICY)
        .stream()
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
