package com.iortatechnxt.brokerverse.report.api.dto;

import com.iortatechnxt.brokerverse.report.core.ReportBatchService.BatchRequest;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.report.render.PrintOptions;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * Report batch request (FRBS 2.4.5 / 2.4.7 / 2.4.9).
 *
 * @param codes report codes, in order
 * @param parameters shared parameters
 * @param format format of the files in the ZIP (ignored for a merged PDF)
 * @param mergedPdf one merged PDF instead of a ZIP
 * @param paper paper size of PDFs
 * @param orientation orientation of PDFs
 * @param fitToWidth fit PDF tables to the page width, null = true
 */
public record ReportBatchRequest(
    @NotEmpty @Size(max = 30) List<@NotBlank String> codes,
    Map<String, String> parameters,
    ExportFormat format,
    boolean mergedPdf,
    String paper,
    String orientation,
    Boolean fitToWidth) {

  /**
   * The service request.
   *
   * @return request
   */
  public BatchRequest toRequest() {
    return new BatchRequest(
        codes, parameters, format, mergedPdf, PrintOptions.of(paper, orientation, fitToWidth));
  }
}
