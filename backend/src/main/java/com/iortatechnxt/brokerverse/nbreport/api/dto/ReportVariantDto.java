package com.iortatechnxt.brokerverse.nbreport.api.dto;

import com.iortatechnxt.brokerverse.nbreport.domain.ReportVariant;
import java.util.Map;

/**
 * A saved report variant (BRNB.057).
 *
 * @param id id
 * @param reportCode report
 * @param name name
 * @param owner user who saved it
 * @param shared whether other users see it
 * @param mine whether the current user saved it (may delete it)
 * @param parameters saved parameter values
 */
public record ReportVariantDto(
    Long id,
    String reportCode,
    String name,
    String owner,
    boolean shared,
    boolean mine,
    Map<String, String> parameters) {

  /**
   * Maps a variant.
   *
   * @param v variant
   * @param mine whether the current user owns it
   * @param parameters its parameter values
   * @return DTO
   */
  public static ReportVariantDto from(
      ReportVariant v, boolean mine, Map<String, String> parameters) {
    return new ReportVariantDto(
        v.getId(), v.getReportCode(), v.getName(), v.getOwner(), v.isShared(), mine, parameters);
  }
}
