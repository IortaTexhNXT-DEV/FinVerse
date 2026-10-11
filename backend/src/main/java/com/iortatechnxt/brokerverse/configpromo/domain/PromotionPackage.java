package com.iortatechnxt.brokerverse.configpromo.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** A configuration package kept by this environment; its file is in the file store. */
@Entity
@Table(name = "cfp_package")
public class PromotionPackage extends BaseEntity {

  @Column(name = "package_no", nullable = false, length = 30)
  private String packageNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false, length = 20)
  private PackageKind kind;

  @Column(name = "package_uuid", nullable = false, length = 36)
  private String packageUuid;

  @Column(name = "mode", nullable = false, length = 20)
  private String mode;

  @Column(name = "source_environment", nullable = false, length = 40)
  private String sourceEnvironment;

  @Column(name = "platform_version", length = 60)
  private String platformVersion;

  @Column(name = "schema_version", length = 40)
  private String schemaVersion;

  @Column(name = "include_users", nullable = false)
  private boolean includeUsers;

  @Column(name = "dataset_count", nullable = false)
  private int datasetCount;

  @Column(name = "row_count", nullable = false)
  private int rowCount;

  @Column(name = "sha256", nullable = false, length = 64)
  private String sha256;

  @Column(name = "key_id", nullable = false, length = 20)
  private String keyId;

  @Column(name = "stored_file_id", nullable = false)
  private Long storedFileId;

  @Column(name = "file_name", nullable = false, length = 200)
  private String fileName;

  @Column(name = "file_size", nullable = false)
  private long fileSize;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "manifest", nullable = false, columnDefinition = "text")
  private String manifest;

  protected PromotionPackage() {}

  /**
   * Creates a package record.
   *
   * @param packageNo number
   * @param kind origin
   * @param facts manifest facts
   * @param file stored file facts
   */
  public PromotionPackage(
      String packageNo, PackageKind kind, PackageFacts facts, PackageFile file) {
    this.packageNo = packageNo;
    this.kind = kind;
    this.packageUuid = facts.packageUuid();
    this.mode = facts.mode();
    this.sourceEnvironment = facts.sourceEnvironment();
    this.platformVersion = facts.platformVersion();
    this.schemaVersion = facts.schemaVersion();
    this.includeUsers = facts.includeUsers();
    this.datasetCount = facts.datasetCount();
    this.rowCount = facts.rowCount();
    this.description = facts.description();
    this.manifest = facts.manifest();
    this.keyId = facts.keyId();
    this.sha256 = file.sha256();
    this.storedFileId = file.storedFileId();
    this.fileName = file.fileName();
    this.fileSize = file.size();
  }

  /**
   * Facts of a package read from its manifest.
   *
   * @param packageUuid package id
   * @param mode FULL or INCREMENTAL
   * @param sourceEnvironment source environment
   * @param platformVersion platform version
   * @param schemaVersion schema version
   * @param includeUsers whether users are included
   * @param datasetCount datasets
   * @param rowCount rows
   * @param description purpose
   * @param manifest manifest JSON
   * @param keyId signing key id
   */
  public record PackageFacts(
      String packageUuid,
      String mode,
      String sourceEnvironment,
      String platformVersion,
      String schemaVersion,
      boolean includeUsers,
      int datasetCount,
      int rowCount,
      String description,
      String manifest,
      String keyId) {}

  /**
   * The stored file of a package.
   *
   * @param storedFileId stored file id
   * @param fileName file name
   * @param size bytes
   * @param sha256 checksum
   */
  public record PackageFile(Long storedFileId, String fileName, long size, String sha256) {}

  public String getPackageNo() {
    return packageNo;
  }

  public PackageKind getKind() {
    return kind;
  }

  public String getPackageUuid() {
    return packageUuid;
  }

  public String getMode() {
    return mode;
  }

  public String getSourceEnvironment() {
    return sourceEnvironment;
  }

  public String getPlatformVersion() {
    return platformVersion;
  }

  public String getSchemaVersion() {
    return schemaVersion;
  }

  public boolean isIncludeUsers() {
    return includeUsers;
  }

  public int getDatasetCount() {
    return datasetCount;
  }

  public int getRowCount() {
    return rowCount;
  }

  public String getSha256() {
    return sha256;
  }

  public String getKeyId() {
    return keyId;
  }

  public Long getStoredFileId() {
    return storedFileId;
  }

  public String getFileName() {
    return fileName;
  }

  public long getFileSize() {
    return fileSize;
  }

  public String getDescription() {
    return description;
  }

  public String getManifest() {
    return manifest;
  }
}
