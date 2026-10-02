package com.iortatechnxt.brokerverse.screening.watchlist.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A watchlist source (SNSRP-201): the AML advisory, NLDS-PEP or the internal list. A FILE source
 * receives its list as a file, uploaded on screen or staged for the scheduled run; a full-file
 * source delists the entries missing from a new file. API transports are parked (SQ01).
 */
@Entity
@Table(name = "scr_watchlist_source")
public class WatchlistSource extends BaseEntity {

  @Column(name = "code", nullable = false, length = 30, updatable = false)
  private String code;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "list_type", nullable = false, length = 30)
  private String listType;

  @Enumerated(EnumType.STRING)
  @Column(name = "transport", nullable = false, length = 10)
  private SourceTransport transport;

  @Column(name = "schedule", length = 100)
  private String schedule;

  @Column(name = "file_layout", length = 10)
  private String fileLayout;

  @Column(name = "full_file", nullable = false)
  private boolean fullFile;

  @Column(name = "drop_file", length = 100)
  private String dropFile;

  @Column(name = "ref_prefix", nullable = false, length = 10, updatable = false)
  private String refPrefix;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  /** For JPA. */
  protected WatchlistSource() {}

  /**
   * Creates a source.
   *
   * @param code unique code
   * @param name name
   * @param listType list type code
   * @param transport transport
   * @param refPrefix prefix of the references of manual entries (e.g. INT)
   */
  public WatchlistSource(
      String code, String name, String listType, SourceTransport transport, String refPrefix) {
    this.code = code;
    this.name = name;
    this.listType = listType;
    this.transport = transport;
    this.refPrefix = refPrefix;
  }

  /**
   * Changes the maintainable settings (FR-SS-020 fields).
   *
   * @param newName name
   * @param newSchedule schedule text (e.g. "Daily 01:00")
   * @param newLayout CSV or XLSX, {@code null} for a manual source
   * @param newFullFile whether a file replaces the whole list
   * @param newActive whether the source is read
   */
  public void update(
      String newName,
      String newSchedule,
      String newLayout,
      boolean newFullFile,
      boolean newActive) {
    this.name = newName;
    this.schedule = newSchedule;
    this.fileLayout = newLayout;
    this.fullFile = newFullFile;
    this.active = newActive;
  }

  /**
   * Whether a file name has an extension this source accepts (the CSV / XLSX template).
   *
   * @param fileName file name
   * @return true for .csv or .xlsx
   */
  public boolean accepts(String fileName) {
    if (fileName == null) {
      return false;
    }
    String name = fileName.toLowerCase(java.util.Locale.ROOT);
    return name.endsWith(".csv") || name.endsWith(".xlsx");
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getListType() {
    return listType;
  }

  public SourceTransport getTransport() {
    return transport;
  }

  public String getSchedule() {
    return schedule;
  }

  public String getFileLayout() {
    return fileLayout;
  }

  public boolean isFullFile() {
    return fullFile;
  }

  public String getDropFile() {
    return dropFile;
  }

  public String getRefPrefix() {
    return refPrefix;
  }

  public boolean isActive() {
    return active;
  }
}
