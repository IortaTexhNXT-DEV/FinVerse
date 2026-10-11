package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationContent;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationVersion;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationVersionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Storage of the quotation versions (BRNB.020): the content of the current version is written while
 * it is open; submission freezes it and the next change opens version n+1.
 */
@Component
public class QuotationVersions {

  private static final String VERSION = "Quotation version";

  private final QuotationVersionRepository versions;
  private final QuotationContentCodec codec;

  /**
   * Creates the store.
   *
   * @param versions versions
   * @param codec JSON codec
   */
  public QuotationVersions(QuotationVersionRepository versions, QuotationContentCodec codec) {
    this.versions = versions;
    this.codec = codec;
  }

  /**
   * Writes the content of the quotation's current version (created when missing).
   *
   * @param quotation quotation (saved)
   * @param content priced content
   */
  public void write(Quotation quotation, QuotationContent content) {
    String json = codec.write(content);
    BigDecimal sumInsured = QuotationPricing.totalSumInsured(content);
    BigDecimal gross = content.premium().grossPremium();
    versions
        .findByQuotationIdAndVersionNo(quotation.getId(), quotation.getCurrentVersion())
        .ifPresentOrElse(
            v -> v.write(json, sumInsured, gross),
            () ->
                versions.save(
                    new QuotationVersion(
                        quotation.getId(),
                        quotation.getCurrentVersion(),
                        json,
                        sumInsured,
                        gross)));
  }

  /**
   * Freezes the current version.
   *
   * @param quotation quotation
   * @param user submitter
   * @param when time
   */
  public void freeze(Quotation quotation, String user, Instant when) {
    QuotationVersion version = require(quotation.getId(), quotation.getCurrentVersion());
    if (!version.isFrozen()) {
      version.freeze(user, when);
    }
  }

  /**
   * Content of the current version.
   *
   * @param quotation quotation
   * @return content
   */
  public QuotationContent current(Quotation quotation) {
    return content(quotation.getId(), quotation.getCurrentVersion());
  }

  /**
   * Content of one version.
   *
   * @param quotationId quotation
   * @param versionNo version number
   * @return content
   */
  public QuotationContent content(Long quotationId, int versionNo) {
    return codec.read(require(quotationId, versionNo).getContent());
  }

  /**
   * Every version, oldest first.
   *
   * @param quotationId quotation
   * @return versions
   */
  public List<QuotationVersion> all(Long quotationId) {
    return versions.findByQuotationIdOrderByVersionNo(quotationId);
  }

  private QuotationVersion require(Long quotationId, int versionNo) {
    return versions
        .findByQuotationIdAndVersionNo(quotationId, versionNo)
        .orElseThrow(() -> new ResourceNotFoundException(VERSION, quotationId + " v" + versionNo));
  }
}
