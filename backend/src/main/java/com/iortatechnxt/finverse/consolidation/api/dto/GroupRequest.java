package com.iortatechnxt.finverse.consolidation.api.dto;

import com.iortatechnxt.finverse.consolidation.domain.MemberValues;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Creates or updates a consolidation group.
 *
 * @param code unique code
 * @param name name
 * @param parentCompanyId parent company
 * @param currency consolidation currency
 * @param ctaAccount currency translation reserve account (equity)
 * @param nciAccount non-controlling interest account (equity)
 * @param goodwillAccount goodwill account (asset)
 * @param active whether runs are allowed
 * @param members subsidiaries
 */
public record GroupRequest(
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9_-]+") String code,
    @NotBlank @Size(max = 150) String name,
    @NotNull Long parentCompanyId,
    @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotBlank @Size(max = 30) String ctaAccount,
    @NotBlank @Size(max = 30) String nciAccount,
    @NotBlank @Size(max = 30) String goodwillAccount,
    boolean active,
    @NotNull @Size(max = 50) List<@Valid Member> members) {

  /**
   * Subsidiary settings.
   *
   * @param companyId subsidiary
   * @param ownershipPct ownership percent
   * @param investmentAccount parent's investment account (optional)
   * @param equityAccounts subsidiary share capital accounts (optional)
   */
  public record Member(
      @NotNull Long companyId,
      @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100")
          BigDecimal ownershipPct,
      @Size(max = 30) String investmentAccount,
      @Size(max = 10) List<@NotBlank @Size(max = 30) String> equityAccounts) {

    /**
     * Domain values.
     *
     * @return values
     */
    public MemberValues values() {
      String investment =
          investmentAccount == null || investmentAccount.isBlank()
              ? null
              : investmentAccount.trim();
      return new MemberValues(companyId, ownershipPct, investment, equityAccounts);
    }
  }
}
