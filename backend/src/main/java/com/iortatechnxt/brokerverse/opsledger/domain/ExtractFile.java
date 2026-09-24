package com.iortatechnxt.brokerverse.opsledger.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A file in the in-system extract repository, the default {@code FileDropPort} in place of the
 * shared-drive folders (RMTID.001 one folder per remittance type, CMRID.001 DP lists; OQ17): kept
 * with folder, name, checksum and the module that produced it.
 */
@Entity
@Table(name = "ops_extract_file")
public class ExtractFile extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, length = 120, updatable = false)
  private String folder;

  @Column(name = "file_name", nullable = false, length = 255, updatable = false)
  private String fileName;

  @Column(name = "content_type", nullable = false, length = 100, updatable = false)
  private String contentType;

  @Column(name = "size_bytes", nullable = false, updatable = false)
  private long sizeBytes;

  @Column(nullable = false, length = 64, updatable = false)
  private String sha256;

  @Column(nullable = false, updatable = false)
  private byte[] content;

  @Column(name = "source_module", nullable = false, length = 30, updatable = false)
  private String sourceModule;

  @Column(name = "source_ref", length = 80, updatable = false)
  private String sourceRef;

  protected ExtractFile() {}

  /**
   * Stores a file.
   *
   * @param companyId company
   * @param location folder and file name
   * @param file type, checksum and content
   * @param source module and reference that produced it
   */
  public ExtractFile(Long companyId, Location location, Content file, Origin source) {
    this.companyId = companyId;
    this.folder = location.folder();
    this.fileName = location.fileName();
    this.contentType = file.contentType();
    this.sha256 = file.sha256();
    this.content = file.bytes();
    this.sizeBytes = this.content.length;
    this.sourceModule = source.module();
    this.sourceRef = source.reference();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getFolder() {
    return folder;
  }

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getSha256() {
    return sha256;
  }

  /**
   * The file.
   *
   * @return bytes
   */
  public byte[] getContent() {
    return content.clone();
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  /**
   * Where a file is kept.
   *
   * @param folder folder (e.g. {@code REMITTANCE/WITH_INCENTIVES})
   * @param fileName file name
   */
  public record Location(String folder, String fileName) {}

  /**
   * What a file holds.
   *
   * @param contentType MIME type
   * @param sha256 checksum
   * @param bytes content
   */
  public record Content(String contentType, String sha256, byte[] bytes) {

    /** Defensive copy. */
    public Content {
      bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
      return bytes.clone();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Content c && sha256.equals(c.sha256);
    }

    @Override
    public int hashCode() {
      return sha256.hashCode();
    }

    @Override
    public String toString() {
      return "Content[" + contentType + ", " + bytes.length + " bytes]";
    }
  }

  /**
   * Who produced a file.
   *
   * @param module module
   * @param reference business reference, may be null
   */
  public record Origin(String module, String reference) {}
}
