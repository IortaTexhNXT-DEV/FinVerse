package com.iortatechnxt.brokerverse.acsl.api.dto;

import com.iortatechnxt.brokerverse.acsl.domain.BookFigures;
import com.iortatechnxt.brokerverse.acsl.domain.GlSlControl;
import com.iortatechnxt.brokerverse.acsl.domain.GlSlRecon;
import com.iortatechnxt.brokerverse.acsl.domain.GlSlRun;
import com.iortatechnxt.brokerverse.acsl.domain.ReconBucket;
import com.iortatechnxt.brokerverse.acsl.domain.ReconResult;
import com.iortatechnxt.brokerverse.acsl.domain.ReconRun;
import com.iortatechnxt.brokerverse.acsl.domain.SlSource;
import com.iortatechnxt.brokerverse.acsl.domain.SoaLayout;
import com.iortatechnxt.brokerverse.acsl.domain.SoaLine;
import com.iortatechnxt.brokerverse.acsl.domain.SoaUpload;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Response records of the insurer SOA and GL-SL reconciliation endpoints (ACSL 2.4.0, 2.13-2.14).
 */
public interface SoaViews {

  /**
   * An SOA upload with its latest reconciliation counts.
   *
   * @param id id
   * @param uploadNo number
   * @param insurerCode insurer
   * @param periodFrom first day covered
   * @param periodTo last day covered
   * @param fileName file
   * @param layoutCode layout used
   * @param rowsRead rows read
   * @param rowsLoaded rows loaded
   * @param rowsFailed rows failed
   * @param run latest run, may be null
   * @param reportName file name of the reconciliation report
   * @param createdBy uploaded by
   * @param createdAt uploaded at
   */
  record UploadView(
      Long id,
      String uploadNo,
      String insurerCode,
      LocalDate periodFrom,
      LocalDate periodTo,
      String fileName,
      String layoutCode,
      int rowsRead,
      int rowsLoaded,
      int rowsFailed,
      RunView run,
      String reportName,
      String createdBy,
      Instant createdAt) {

    /**
     * Maps an upload.
     *
     * @param u upload
     * @param run latest run, or null
     * @return view
     */
    public static UploadView from(SoaUpload u, ReconRun run) {
      return new UploadView(
          u.getId(),
          u.getUploadNo(),
          u.getInsurerCode(),
          u.getPeriodFrom(),
          u.getPeriodTo(),
          u.getFileName(),
          u.getLayoutCode(),
          u.getRowsRead(),
          u.getRowsLoaded(),
          u.getRowsFailed(),
          run == null ? null : RunView.from(run),
          u.reportName(),
          u.getCreatedBy(),
          u.getCreatedAt());
    }
  }

  /**
   * A reconciliation run.
   *
   * @param runNo run number
   * @param runAt when
   * @param runBy who
   * @param outstanding lines outstanding
   * @param forRemittance lines for remittance
   * @param remitted lines remitted
   * @param cancelled lines cancelled
   * @param directBilled lines direct billed
   * @param notFound lines not found
   * @param withVariance lines with a variance
   */
  record RunView(
      int runNo,
      Instant runAt,
      String runBy,
      int outstanding,
      int forRemittance,
      int remitted,
      int cancelled,
      int directBilled,
      int notFound,
      int withVariance) {

    /**
     * Maps a run.
     *
     * @param r run
     * @return view
     */
    public static RunView from(ReconRun r) {
      return new RunView(
          r.getRunNo(),
          r.getRunAt(),
          r.getRunBy(),
          r.getOutstanding(),
          r.getForRemittance(),
          r.getRemitted(),
          r.getCancelled(),
          r.getDirectBilled(),
          r.getNotFound(),
          r.getWithVariance());
    }
  }

  /**
   * A row of the upload log.
   *
   * @param rowNo row
   * @param status LOADED or FAILED
   * @param error reason
   * @param invoiceNo invoice
   * @param policyNo policy
   * @param assuredName assured
   * @param grossPremium premium
   * @param balance balance
   * @param paid payments
   */
  record LogView(
      int rowNo,
      String status,
      String error,
      String invoiceNo,
      String policyNo,
      String assuredName,
      BigDecimal grossPremium,
      BigDecimal balance,
      BigDecimal paid) {

    /**
     * Maps a row.
     *
     * @param l row
     * @return view
     */
    public static LogView from(SoaLine l) {
      return new LogView(
          l.getRowNo(),
          l.getStatus(),
          l.getError(),
          l.getInvoiceNo(),
          l.getPolicyNo(),
          l.getAssuredName(),
          l.getGrossPremium(),
          l.getBalance(),
          l.getPaid());
    }
  }

  /**
   * A reconciled line.
   *
   * @param rowNo row
   * @param invoiceNo invoice
   * @param policyNo policy
   * @param assuredName assured
   * @param rootInvoiceNo root invoice
   * @param bucket bucket
   * @param book BDOI's figures
   * @param soaPremium insurer premium
   * @param soaBalance insurer balance
   * @param premiumVariance premium variance
   * @param outstandingVariance outstanding variance
   */
  record ResultView(
      int rowNo,
      String invoiceNo,
      String policyNo,
      String assuredName,
      String rootInvoiceNo,
      ReconBucket bucket,
      BookFigures book,
      BigDecimal soaPremium,
      BigDecimal soaBalance,
      BigDecimal premiumVariance,
      BigDecimal outstandingVariance) {

    /**
     * Maps a result.
     *
     * @param r result
     * @return view
     */
    public static ResultView from(ReconResult r) {
      return new ResultView(
          r.getRowNo(),
          r.getInvoiceNo(),
          r.getPolicyNo(),
          r.getAssuredName(),
          r.getRootInvoiceNo(),
          r.getBucket(),
          r.bookOrNone(),
          r.getSoaPremium(),
          r.getSoaBalance(),
          r.getPremiumVariance(),
          r.getOutstandingVariance());
    }
  }

  /**
   * An SOA layout.
   *
   * @param insurerCode insurer ({@code *} standard)
   * @param name name
   * @param headers headers of invoice, policy, assured, inception, expiry, premium, balance, paid
   * @param active active
   */
  record LayoutView(String insurerCode, String name, List<String> headers, boolean active) {

    /**
     * Maps a layout.
     *
     * @param l layout
     * @return view
     */
    public static LayoutView from(SoaLayout l) {
      return new LayoutView(
          l.getInsurerCode(),
          l.getName(),
          List.of(
              l.getInvoiceHeader(),
              l.getPolicyHeader(),
              l.getAssuredHeader(),
              l.getInceptionHeader(),
              l.getExpiryHeader(),
              l.getGrossHeader(),
              l.getBalanceHeader(),
              l.getPaidHeader()),
          l.isActive());
    }
  }

  /**
   * A GL-SL run.
   *
   * @param id id
   * @param asOf as of
   * @param runAt when
   * @param runBy who
   * @param accounts accounts compared
   * @param differences accounts with a difference
   * @param totalDifference sum of the absolute differences
   */
  record GlSlRunView(
      Long id,
      LocalDate asOf,
      Instant runAt,
      String runBy,
      int accounts,
      int differences,
      BigDecimal totalDifference) {

    /**
     * Maps a run.
     *
     * @param r run
     * @return view
     */
    public static GlSlRunView from(GlSlRun r) {
      return new GlSlRunView(
          r.getId(),
          r.getAsOf(),
          r.getRunAt(),
          r.getRunBy(),
          r.getAccounts(),
          r.getDifferences(),
          r.getTotalDifference());
    }
  }

  /**
   * One account of a GL-SL run.
   *
   * @param accountCode account
   * @param accountName name
   * @param source sub-ledger
   * @param glBalance GL
   * @param slBalance SL
   * @param difference difference
   */
  record GlSlRowView(
      String accountCode,
      String accountName,
      SlSource source,
      BigDecimal glBalance,
      BigDecimal slBalance,
      BigDecimal difference) {

    /**
     * Maps an account row.
     *
     * @param r row
     * @return view
     */
    public static GlSlRowView from(GlSlRecon r) {
      return new GlSlRowView(
          r.getAccountCode(),
          r.getAccountName(),
          r.getSource(),
          r.getGlBalance(),
          r.getSlBalance(),
          r.getDifference());
    }
  }

  /**
   * The sub-ledger configuration of a control account.
   *
   * @param accountCode account
   * @param source sub-ledger
   * @param components Operations components
   * @param documentTypes open-item document types
   * @param currency Operations invoice currency
   * @param active used or not
   */
  record ControlView(
      String accountCode,
      SlSource source,
      String components,
      String documentTypes,
      String currency,
      boolean active) {

    /**
     * Maps a configuration.
     *
     * @param c configuration
     * @return view
     */
    public static ControlView from(GlSlControl c) {
      return new ControlView(
          c.getAccountCode(),
          c.getSource(),
          c.getComponents(),
          c.getDocumentTypes(),
          c.getCurrency(),
          c.isActive());
    }
  }
}
