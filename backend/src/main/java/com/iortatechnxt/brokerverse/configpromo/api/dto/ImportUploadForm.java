package com.iortatechnxt.brokerverse.configpromo.api.dto;

import java.util.List;

/**
 * The form fields sent with a package to import.
 *
 * @param datasets datasets to import, empty for all
 * @param deactivate datasets whose items only in the target are deactivated
 * @param includeUsers whether the users of the package are imported
 * @param changeReference change request number (mandatory in production)
 * @param reason reason of the import
 * @param pipeline whether a deployment pipeline sends the package
 * @param apply pipeline only: apply the import when its dry run is clean and allowed here
 */
public record ImportUploadForm(
    List<String> datasets,
    List<String> deactivate,
    Boolean includeUsers,
    String changeReference,
    String reason,
    Boolean pipeline,
    Boolean apply) {

  /**
   * The choices of the dry run.
   *
   * @return options
   */
  public OptionsRequest options() {
    return new OptionsRequest(datasets, deactivate, Boolean.TRUE.equals(includeUsers));
  }

  /**
   * Whether a deployment pipeline sends the package.
   *
   * @return true for a pipeline
   */
  public boolean fromPipeline() {
    return Boolean.TRUE.equals(pipeline);
  }

  /**
   * Whether the pipeline asks to apply at once.
   *
   * @return true to apply
   */
  public boolean applyNow() {
    return fromPipeline() && Boolean.TRUE.equals(apply);
  }
}
