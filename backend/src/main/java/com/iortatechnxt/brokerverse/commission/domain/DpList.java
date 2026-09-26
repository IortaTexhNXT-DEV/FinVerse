package com.iortatechnxt.brokerverse.commission.domain;

import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.ListSource;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A direct payment list submitted by Head Office or a branch's Marketing Collection, or received
 * from the Collection feed (CMRID.001): branch, submission date from the file name convention
 * {@code <Branch>_DP_<yyyyMMdd>}, file checksum and the counts after consolidation.
 */
@Entity
@Table(name = "cmr_dp_list")
public class DpList extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "list_no", nullable = false, length = 30, updatable = false)
  private String listNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private ListSource source;

  @Column(name = "branch_code", nullable = false, length = 60, updatable = false)
  private String branchCode;

  @Column(name = "submission_date", nullable = false, updatable = false)
  private LocalDate submissionDate;

  @Column(name = "file_name", length = 255, updatable = false)
  private String fileName;

  @Column(length = 64, updatable = false)
  private String sha256;

  @Column(name = "run_no", length = 30)
  private String runNo;

  @Column(name = "item_count", nullable = false)
  private int itemCount;

  @Column(name = "valid_count", nullable = false)
  private int validCount;

  @Column(name = "excluded_count", nullable = false)
  private int excludedCount;

  protected DpList() {}

  /**
   * A received list.
   *
   * @param companyId company
   * @param listNo list number
   * @param origin source, branch and submission date
   * @param file file name and checksum (null for a feed)
   */
  public DpList(Long companyId, String listNo, Origin origin, FileKey file) {
    this.companyId = companyId;
    this.listNo = listNo;
    this.source = origin.source();
    this.branchCode = origin.branchCode();
    this.submissionDate = origin.submissionDate();
    this.fileName = file.fileName();
    this.sha256 = file.sha256();
  }

  /**
   * Records the run and counts once every account is in.
   *
   * @param run flow-in run
   * @param items accounts read
   * @param valid accounts valid
   * @param excluded accounts excluded by the sanitation
   */
  public void counted(String run, int items, int valid, int excluded) {
    this.runNo = run;
    this.itemCount = items;
    this.validCount = valid;
    this.excludedCount = excluded;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getListNo() {
    return listNo;
  }

  public ListSource getSource() {
    return source;
  }

  public String getBranchCode() {
    return branchCode;
  }

  public LocalDate getSubmissionDate() {
    return submissionDate;
  }

  public String getFileName() {
    return fileName;
  }

  public String getSha256() {
    return sha256;
  }

  public String getRunNo() {
    return runNo;
  }

  public int getItemCount() {
    return itemCount;
  }

  public int getValidCount() {
    return validCount;
  }

  public int getExcludedCount() {
    return excludedCount;
  }

  /**
   * Where a list comes from.
   *
   * @param source branch, head office or feed
   * @param branchCode branch (HO for Head Office)
   * @param submissionDate submission date
   */
  public record Origin(ListSource source, String branchCode, LocalDate submissionDate) {}

  /**
   * The list's file.
   *
   * @param fileName file name, null for a feed
   * @param sha256 checksum, null for a feed
   */
  public record FileKey(String fileName, String sha256) {

    /** No file (Collection feed). */
    public static final FileKey NONE = new FileKey(null, null);
  }
}
