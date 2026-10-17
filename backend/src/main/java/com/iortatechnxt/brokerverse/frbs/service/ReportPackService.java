package com.iortatechnxt.brokerverse.frbs.service;

import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The BDOI report pack (FRBS 3.2.0, Appendix A; V899): the report groups in order with their
 * reports, each marked available when the report exists in the catalogue and the user may run it.
 * Every entry is exported to Excel and PDF through the Report Centre; an entry for which BDOI asks
 * for a Word document (board decks) is also exported to Word (client requirement 16).
 */
@Service
@Transactional(readOnly = true)
public class ReportPackService {

  private static final String ENTRIES =
      "select group_code, group_name, report_code, schedule_code, title, source_ref, word_requested"
          + " from frbs_report_pack_entry order by group_seq, seq";

  private final JdbcTemplate jdbc;
  private final ReportService reports;

  /**
   * Creates the service.
   *
   * @param jdbc JDBC template
   * @param reports report catalogue
   */
  public ReportPackService(JdbcTemplate jdbc, ReportService reports) {
    this.jdbc = jdbc;
    this.reports = reports;
  }

  /**
   * The entries of the pack, in group and report order.
   *
   * @return entries with their availability to the current user
   */
  public List<PackEntry> entries() {
    Set<String> runnable =
        reports.catalogue().stream().map(ReportMetadata::code).collect(Collectors.toSet());
    return jdbc.query(
        ENTRIES,
        (rs, i) ->
            new PackEntry(
                rs.getString("group_code"),
                rs.getString("group_name"),
                rs.getString("report_code"),
                rs.getString("schedule_code"),
                rs.getString("title"),
                rs.getString("source_ref"),
                rs.getBoolean("word_requested"),
                runnable.contains(rs.getString("report_code"))));
  }

  /**
   * A report of the pack.
   *
   * @param groupCode group
   * @param groupName group title
   * @param reportCode report code
   * @param scheduleCode schedule definition of {@code GL-SCHEDULE}, null for other reports
   * @param title title
   * @param sourceRef appendix row or report list number
   * @param wordRequested whether BDOI asks for a Word document (exported to Word as well)
   * @param available whether the current user may run it
   */
  public record PackEntry(
      String groupCode,
      String groupName,
      String reportCode,
      String scheduleCode,
      String title,
      String sourceRef,
      boolean wordRequested,
      boolean available) {}
}
