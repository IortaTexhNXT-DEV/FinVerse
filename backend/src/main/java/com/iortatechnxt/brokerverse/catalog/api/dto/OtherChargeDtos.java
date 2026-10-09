package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.ChargeBasis;
import com.iortatechnxt.brokerverse.catalog.domain.ChargeVatTreatment;
import com.iortatechnxt.brokerverse.catalog.domain.OtherCharge;
import com.iortatechnxt.brokerverse.catalog.domain.OtherCharge.Scope;
import com.iortatechnxt.brokerverse.catalog.domain.OtherCharge.Terms;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Requests and responses of the other charges (template PM-04 Charges). */
public interface OtherChargeDtos {

  /**
   * A charge row to add or change (code, line and product are kept on a change).
   *
   * @param companyId company whose chart of accounts holds the GL account
   * @param chargeCode code of the charge
   * @param name name on the breakdown and the invoice
   * @param lineCode product line, blank for every line
   * @param productCode product, blank for every product of the line
   * @param basis fixed amount or rate of the net premium
   * @param value amount, or rate in percent
   * @param vatTreatment VAT treatment, VATABLE when blank
   * @param glAccountCode GL account credited with the charge
   * @param effectiveFrom first day
   * @param effectiveTo last day, blank when open
   */
  record OtherChargeRequest(
      @NotNull Long companyId,
      @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Z0-9_]+") String chargeCode,
      @NotBlank @Size(max = 100) String name,
      @Size(max = 30) String lineCode,
      @Size(max = 30) String productCode,
      @NotNull ChargeBasis basis,
      @NotNull @DecimalMin("0") BigDecimal value,
      ChargeVatTreatment vatTreatment,
      @NotBlank @Size(max = 30) String glAccountCode,
      @NotNull LocalDate effectiveFrom,
      LocalDate effectiveTo) {

    /**
     * The scope of the row.
     *
     * @return line and product, null when blank
     */
    public Scope scope() {
      return new Scope(blankToNull(lineCode), blankToNull(productCode));
    }

    /**
     * The terms of the row.
     *
     * @return terms
     */
    public Terms terms() {
      return new Terms(
          name.strip(),
          basis,
          value,
          vatTreatment,
          glAccountCode.strip(),
          effectiveFrom,
          effectiveTo);
    }

    private static String blankToNull(String value) {
      return value == null || value.isBlank() ? null : value.strip();
    }
  }

  /**
   * A charge row.
   *
   * @param id id
   * @param chargeCode code
   * @param name name
   * @param lineCode line, null for every line
   * @param productCode product, null for every product of the line
   * @param basis amount or rate
   * @param value amount, or rate in percent
   * @param vatTreatment VAT treatment
   * @param glAccountCode GL account
   * @param effectiveFrom first day
   * @param effectiveTo last day
   * @param recordStatus maker-checker status
   * @param maker last maintainer
   * @param authorizedBy checker
   */
  record OtherChargeResponse(
      Long id,
      String chargeCode,
      String name,
      String lineCode,
      String productCode,
      ChargeBasis basis,
      BigDecimal value,
      ChargeVatTreatment vatTreatment,
      String glAccountCode,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      RecordStatus recordStatus,
      String maker,
      String authorizedBy) {

    /**
     * Maps a row.
     *
     * @param e row
     * @return response
     */
    public static OtherChargeResponse from(OtherCharge e) {
      return new OtherChargeResponse(
          e.getId(),
          e.getChargeCode(),
          e.getName(),
          e.getLineCode(),
          e.getProductCode(),
          e.getBasis(),
          e.getValue(),
          e.getVatTreatment(),
          e.getGlAccountCode(),
          e.getEffectiveFrom(),
          e.getEffectiveTo(),
          e.getRecordStatus(),
          e.getMaker(),
          e.getAuthorizedBy());
    }
  }

  /**
   * The charges and whether they are billed.
   *
   * @param enabled the parameter OTHER_CHARGES_ENABLED is on
   * @param charges charge rows
   */
  record OtherChargesView(boolean enabled, java.util.List<OtherChargeResponse> charges) {}
}
