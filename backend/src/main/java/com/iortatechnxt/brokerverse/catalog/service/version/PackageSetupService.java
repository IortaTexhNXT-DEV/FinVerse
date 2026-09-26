package com.iortatechnxt.brokerverse.catalog.service.version;

/**
 * Package set-up (BRPM.015, PMADD01/02/06; PRODUCT_MAINTENANCE_DESIGN sections 5.1 and 9.1): turns
 * negotiated, signed-off package terms into a DRAFT catalog version. Called by {@code productmaint}
 * on the MBS {@code setup} action of a package request, and by the catalog "New Version" editor.
 *
 * <p>Contract for the implementation (catalog, P1-A):
 *
 * <ul>
 *   <li>Runs in the caller's transaction and needs PRODUCT_MAINTAIN (MBS); the creator is the
 *       version's maker, who can never validate it (PMADD06).
 *   <li>A DRAFT is editable and invisible to rating. It is released only through the version
 *       checkpoint ({@code submitForValidation}, then {@code validate} by a PRODUCT_VALIDATE
 *       holder), which publishes {@link ProductVersionReleased} after commit; a return publishes
 *       {@link ProductVersionReturned}.
 *   <li>Every change writes an audit row (BRPM.024).
 *   <li>Rule violations are {@code BusinessRuleException}s with these codes: {@code
 *       VERSION_IN_PROGRESS} (a DRAFT or FOR_VALIDATION version already exists), {@code
 *       VERSION_NOT_DRAFT} (update of a version that is not DRAFT), {@code PRODUCT_CODE_PATTERN}
 *       (new code does not match the line's pattern), {@code PRODUCT_NOT_ACTIVE} (retirement of a
 *       product that is not ACTIVE), plus the not-found error of an unknown product, line, cover
 *       type, coverage, clause or insurer.
 * </ul>
 *
 * <p>Until the catalog implementation exists, {@link PackageVersionStubDefaults} registers an
 * in-memory stub so that {@code productmaint} runs end to end.
 */
public interface PackageSetupService {

  /**
   * Creates the next DRAFT version of a packaged product, creating the product first when {@code
   * spec.newProduct()} is given (NEW and REACTIVATE requests start here too).
   *
   * @param spec package content, dates, rate scheme and origin
   * @return the new DRAFT version
   */
  VersionRef createDraftVersion(PackageSpec spec);

  /**
   * Replaces the content of a DRAFT version (a request returned by the validator and set up again).
   *
   * @param productCode risk code
   * @param versionNo DRAFT version number
   * @param spec new content (its product code must be {@code productCode})
   * @return the updated DRAFT version
   */
  VersionRef updateDraftVersion(String productCode, int versionNo, PackageSpec spec);

  /**
   * Retires a package (BRPM.011 "deletion" is a RETIRE request; BRPM.006): the product's lifecycle
   * becomes RETIRED, it is no longer sellable for new business, and it and its versions stay
   * readable and searchable. Nothing is deleted.
   *
   * @param productCode risk code
   * @param origin request number, ManCom sign-off reference and reason
   */
  void retireProduct(String productCode, PackageSpec.Origin origin);
}
