package com.iortatechnxt.brokerverse.reserves.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Takaful surplus (Mudharabah) settings of one company (maker-checker). When enabled and
 * authorized, valuation runs compute the surplus payable to participants of expiring takaful
 * policies (PGIBR074).
 */
@Entity
@Table(name = "rsv_takaful_setting")
public class TakafulSetting extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "product_codes", length = 500)
  private String productCodes;

  @Column(name = "participant_share_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal participantSharePct;

  @Column(name = "tax_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal taxPct;

  @Column(name = "cost_center", length = 20)
  private String costCenter;

  protected TakafulSetting() {}

  /**
   * Creates the settings of a company, pending authorization.
   *
   * @param companyId company
   * @param terms values
   */
  public TakafulSetting(Long companyId, TakafulTerms terms) {
    this.companyId = companyId;
    apply(terms);
  }

  /**
   * Changes the settings; they must be re-authorized before the next run uses them.
   *
   * @param terms values
   */
  public void update(TakafulTerms terms) {
    apply(terms);
    markModified();
  }

  private void apply(TakafulTerms t) {
    this.enabled = t.enabled();
    this.productCodes = t.productCodes();
    this.participantSharePct = t.participantSharePct();
    this.taxPct = t.taxPct();
    this.costCenter = t.costCenter();
  }

  /**
   * Product codes of takaful products.
   *
   * @return codes (empty = every product)
   */
  public List<String> products() {
    if (productCodes == null || productCodes.isBlank()) {
      return List.of();
    }
    return Arrays.stream(productCodes.split(","))
        .map(s -> s.trim().toUpperCase(Locale.ROOT))
        .filter(s -> !s.isEmpty())
        .toList();
  }

  /**
   * Whether a product is a takaful product.
   *
   * @param productCode product code
   * @return true when listed (or when no product is listed)
   */
  public boolean covers(String productCode) {
    List<String> list = products();
    return list.isEmpty() || list.contains(productCode.toUpperCase(Locale.ROOT));
  }

  /**
   * Current values.
   *
   * @return values
   */
  public TakafulTerms terms() {
    return new TakafulTerms(enabled, productCodes, participantSharePct, taxPct, costCenter);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public boolean isEnabled() {
    return enabled;
  }
}
