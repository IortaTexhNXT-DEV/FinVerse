package com.iortatechnxt.brokerverse.configpromo.api.dto;

import com.iortatechnxt.brokerverse.configpromo.domain.PromotionPackage;
import java.time.Instant;

/**
 * A configuration package.
 *
 * @param id id
 * @param packageNo number
 * @param kind EXPORT, UPLOAD or SNAPSHOT
 * @param packageUuid unique id of the package (the same in every environment)
 * @param mode FULL or INCREMENTAL
 * @param sourceEnvironment environment that exported it
 * @param platformVersion platform version of the source
 * @param schemaVersion schema version of the source
 * @param includeUsers whether users are included
 * @param datasetCount datasets
 * @param rowCount items
 * @param sha256 checksum of the file
 * @param keyId identifier of the signing key
 * @param fileName file name
 * @param fileSize bytes
 * @param description purpose
 * @param createdBy user
 * @param createdAt time
 */
public record PackageResponse(
    Long id,
    String packageNo,
    String kind,
    String packageUuid,
    String mode,
    String sourceEnvironment,
    String platformVersion,
    String schemaVersion,
    boolean includeUsers,
    int datasetCount,
    int rowCount,
    String sha256,
    String keyId,
    String fileName,
    long fileSize,
    String description,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps a package.
   *
   * @param p package
   * @return response
   */
  public static PackageResponse from(PromotionPackage p) {
    return new PackageResponse(
        p.getId(),
        p.getPackageNo(),
        p.getKind().name(),
        p.getPackageUuid(),
        p.getMode(),
        p.getSourceEnvironment(),
        p.getPlatformVersion(),
        p.getSchemaVersion(),
        p.isIncludeUsers(),
        p.getDatasetCount(),
        p.getRowCount(),
        p.getSha256(),
        p.getKeyId(),
        p.getFileName(),
        p.getFileSize(),
        p.getDescription(),
        p.getCreatedBy(),
        p.getCreatedAt());
  }
}
