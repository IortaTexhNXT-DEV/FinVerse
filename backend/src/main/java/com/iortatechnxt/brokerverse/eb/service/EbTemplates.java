package com.iortatechnxt.brokerverse.eb.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Merges the EB templates (subject and body, placeholders {@code {{...}}}) and composes the EB
 * documents as PDF on the company letterhead.
 */
@Component
public class EbTemplates {

  private final DocTemplateService templates;
  private final DocumentComposer composer;
  private final OrganizationService organizations;
  private final Clock clock;

  /**
   * Creates the helper.
   *
   * @param templates templates
   * @param composer PDF composer
   * @param organizations company name
   * @param clock clock
   */
  public EbTemplates(
      DocTemplateService templates,
      DocumentComposer composer,
      OrganizationService organizations,
      Clock clock) {
    this.templates = templates;
    this.composer = composer;
    this.organizations = organizations;
    this.clock = clock;
  }

  /**
   * Merges a template with its subject filled too.
   *
   * @param code template code
   * @param values placeholder values
   * @return subject and body
   */
  public MergedText merge(String code, Map<String, ?> values) {
    MergedText text = templates.merge(code, BusinessClock.today(clock), values);
    return new MergedText(
        text.code(), text.versionNo(), DocTemplateService.fill(text.title(), values), text.text());
  }

  /**
   * Composes a PDF.
   *
   * @param companyId company (letterhead)
   * @param heading document title and reference
   * @param sections content
   * @param signatures signatories
   * @return PDF bytes
   */
  public byte[] pdf(
      Long companyId,
      Heading heading,
      List<DocumentSpec.Section> sections,
      List<String> signatures) {
    return composer.pdf(
        new DocumentSpec(
            organizations.getCompany(companyId).getName(),
            heading.title(),
            heading.reference(),
            sections,
            signatures,
            heading.footer()));
  }

  /**
   * Title block of a document.
   *
   * @param title title
   * @param reference reference
   * @param footer small print, may be null
   */
  public record Heading(String title, String reference, String footer) {}
}
