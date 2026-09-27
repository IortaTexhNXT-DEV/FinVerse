package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.catalog.domain.CoverType;
import com.iortatechnxt.brokerverse.catalog.domain.CoverTypeRepository;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfileRepository;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLine;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLineRepository;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The names of catalog records as people read them in generated documents and messages: a product
 * with its name ("CAR00 - Contractor's All Risks"), a line by its name ("Engineering") and an
 * insurer by its name, never the internal code alone. An unknown code is returned as it is.
 */
@Service
@Transactional(readOnly = true)
public class CatalogNames {

  private final RiskProductRepository products;
  private final ProductLineRepository lines;
  private final InsurerProfileRepository insurers;
  private final CoverTypeRepository coverTypes;

  /**
   * Creates the service.
   *
   * @param products products
   * @param lines product lines
   * @param insurers insurers
   * @param coverTypes cover types
   */
  public CatalogNames(
      RiskProductRepository products,
      ProductLineRepository lines,
      InsurerProfileRepository insurers,
      CoverTypeRepository coverTypes) {
    this.products = products;
    this.lines = lines;
    this.insurers = insurers;
    this.coverTypes = coverTypes;
  }

  /**
   * The name of a cover type of a line.
   *
   * @param lineCode line
   * @param code cover type code, may be null
   * @return name, empty when null
   */
  public String coverType(String lineCode, String code) {
    if (code == null) {
      return "";
    }
    return coverTypes.findByLineCodeAndCode(lineCode, code).map(CoverType::getName).orElse(code);
  }

  /**
   * A product as "code - name".
   *
   * @param code risk code, may be null
   * @return text, empty when null
   */
  public String product(String code) {
    if (code == null) {
      return "";
    }
    return products.findByCode(code).map(p -> code + " - " + p.getName()).orElse(code);
  }

  /**
   * The name of a product line.
   *
   * @param code line code, may be null
   * @return name, empty when null
   */
  public String line(String code) {
    if (code == null) {
      return "";
    }
    return lines.findByCode(code).map(ProductLine::getName).orElse(code);
  }

  /**
   * The name of a product.
   *
   * @param code risk code, may be null
   * @return name, empty when null
   */
  public String productName(String code) {
    if (code == null) {
      return "";
    }
    return products.findByCode(code).map(RiskProduct::getName).orElse(code);
  }

  /**
   * The name of an insurer of a company.
   *
   * @param companyId company
   * @param partyCode insurer party code, may be null
   * @return name, empty when null
   */
  public String insurer(Long companyId, String partyCode) {
    if (partyCode == null) {
      return "";
    }
    return insurers
        .findByCompanyIdAndPartyCode(companyId, partyCode)
        .map(InsurerProfile::getName)
        .orElse(partyCode);
  }
}
