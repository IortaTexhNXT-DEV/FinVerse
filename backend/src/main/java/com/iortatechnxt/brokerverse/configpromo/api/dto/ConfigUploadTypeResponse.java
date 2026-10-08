package com.iortatechnxt.brokerverse.configpromo.api.dto;

import com.iortatechnxt.brokerverse.configpromo.upload.ConfigUploadHandler;
import java.util.List;

/**
 * An upload of a configuration screen.
 *
 * @param code upload type
 * @param templateId tab of the master data and configuration workbook
 * @param title what is uploaded
 * @param screen configuration screen
 * @param filledBy who fills the template
 * @param rules rules of the upload
 * @param mayUpload whether the user may upload
 * @param mayApprove whether the user may approve
 */
public record ConfigUploadTypeResponse(
    String code,
    String templateId,
    String title,
    String screen,
    String filledBy,
    List<String> rules,
    boolean mayUpload,
    boolean mayApprove) {

  /**
   * Maps an upload type.
   *
   * @param h handler
   * @param mayUpload whether the user may upload
   * @param mayApprove whether the user may approve
   * @return response
   */
  public static ConfigUploadTypeResponse from(
      ConfigUploadHandler h, boolean mayUpload, boolean mayApprove) {
    return new ConfigUploadTypeResponse(
        h.code(),
        h.templateId(),
        h.title(),
        h.screen(),
        h.filledBy(),
        h.rules(),
        mayUpload,
        mayApprove);
  }
}
