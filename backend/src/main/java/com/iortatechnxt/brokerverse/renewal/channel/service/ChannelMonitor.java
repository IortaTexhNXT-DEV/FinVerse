package com.iortatechnxt.brokerverse.renewal.channel.service;

import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The monitoring of the delivery channels: the messages by channel, status, kind of document and
 * reference, latest first; the error report of the failed messages; and the connection settings of
 * each channel (never the access key itself, only whether it is set).
 */
@Service
@Transactional(readOnly = true)
public class ChannelMonitor {

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private static final String LIST =
      "select message_no, channel, direction, doc_kind, doc_ref, renewal_ref, file_name,"
          + " recipients_to, status, external_ref, attempts, last_error, created_at, submitted_at,"
          + " sent_at, delivered_at, created_by from rnw_channel_message where company_id = :companyId"
          + " and (cast(:channel as varchar) is null or channel = :channel)"
          + " and (cast(:status as varchar) is null or status = :status)"
          + " and (cast(:kind as varchar) is null or doc_kind = :kind)"
          + " and (cast(:search as varchar) is null or renewal_ref ilike '%' || :search || '%'"
          + "  or doc_ref ilike '%' || :search || '%' or message_no ilike '%' || :search || '%')"
          + " order by id desc limit 1000";

  private final NamedParameterJdbcTemplate jdbc;
  private final SystemParameterService parameters;
  private final Environment environment;
  private final DocumentComposer composer;

  /**
   * Creates the monitor.
   *
   * @param jdbc messages
   * @param parameters connection settings
   * @param environment whether the access keys are set
   * @param composer error report
   */
  public ChannelMonitor(
      NamedParameterJdbcTemplate jdbc,
      SystemParameterService parameters,
      Environment environment,
      DocumentComposer composer) {
    this.jdbc = jdbc;
    this.parameters = parameters;
    this.environment = environment;
    this.composer = composer;
  }

  /**
   * The messages, latest first.
   *
   * @param companyId company
   * @param filter channel, status, kind and search, each may be null
   * @return messages
   */
  public List<Row> list(Long companyId, Filter filter) {
    Map<String, Object> a = new HashMap<>();
    a.put("companyId", companyId);
    a.put("channel", blank(filter.channel()));
    a.put("status", blank(filter.status()));
    a.put("kind", blank(filter.kind()));
    a.put("search", blank(filter.search()));
    return jdbc.query(
        LIST,
        a,
        (rs, i) ->
            new Row(
                rs.getString("message_no"),
                rs.getString("channel"),
                rs.getString("direction"),
                rs.getString("doc_kind"),
                rs.getString("doc_ref"),
                rs.getString("renewal_ref"),
                rs.getString("file_name"),
                rs.getString("recipients_to"),
                rs.getString("status"),
                rs.getString("external_ref"),
                rs.getInt("attempts"),
                rs.getString("last_error"),
                instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("submitted_at")),
                instant(rs.getTimestamp("sent_at")),
                instant(rs.getTimestamp("delivered_at")),
                rs.getString("created_by")));
  }

  /**
   * The error report: the failed messages with their error, to correct and resend.
   *
   * @param companyId company
   * @param channel channel, may be null
   * @return workbook
   */
  public MessageFile errorReport(Long companyId, String channel) {
    List<List<Object>> rows = new ArrayList<>();
    for (Row r : list(companyId, new Filter(channel, "FAILED", null, null))) {
      rows.add(
          List.of(
              r.messageNo(),
              r.channel(),
              r.docKind(),
              nz(r.renewalRef()),
              nz(r.fileName()),
              nz(r.recipients()),
              r.attempts(),
              nz(r.lastError())));
    }
    return new MessageFile(
        "Channel Error Report.xlsx",
        XLSX,
        composer.xlsx(
            new SheetSpec(
                "Failed messages",
                List.of(
                    "Message",
                    "Channel",
                    "Document",
                    "Reference Number",
                    "File Name",
                    "Recipients",
                    "Attempts",
                    "Error"),
                rows)));
  }

  /**
   * The connection settings of a channel.
   *
   * @param channel CCM or MFT
   * @return settings
   */
  public Settings settings(String channel) {
    String keySetting = parameters.text("RNW_" + channel + "_KEY_SETTING", "").strip();
    return new Settings(
        channel,
        parameters.text("RNW_" + channel + "_MODE", "SIMULATOR").strip(),
        parameters.text("RNW_" + channel + "_ENDPOINT", "").strip(),
        keySetting,
        !keySetting.isEmpty() && environment.getProperty(keySetting) != null);
  }

  private static Instant instant(Timestamp t) {
    return t == null ? null : t.toInstant();
  }

  private static String blank(String v) {
    return v == null || v.isBlank() ? null : v.strip();
  }

  private static String nz(String v) {
    return v == null ? "" : v;
  }

  /**
   * Filters of the monitor.
   *
   * @param channel CCM, MFT or EMAIL
   * @param status status
   * @param kind kind of document
   * @param search reference, document or message number
   */
  public record Filter(String channel, String status, String kind, String search) {}

  /**
   * A message as listed.
   *
   * @param messageNo number
   * @param channel channel
   * @param direction OUTBOUND or INBOUND
   * @param docKind kind of document
   * @param docRef document reference
   * @param renewalRef renewal reference
   * @param fileName file name
   * @param recipients recipients
   * @param status status
   * @param externalRef transaction reference of the channel
   * @param attempts attempts
   * @param lastError last error
   * @param createdAt queued at
   * @param submittedAt submitted at
   * @param sentAt sent at
   * @param deliveredAt delivered at
   * @param createdBy queued by
   */
  public record Row(
      String messageNo,
      String channel,
      String direction,
      String docKind,
      String docRef,
      String renewalRef,
      String fileName,
      String recipients,
      String status,
      String externalRef,
      int attempts,
      String lastError,
      Instant createdAt,
      Instant submittedAt,
      Instant sentAt,
      Instant deliveredAt,
      String createdBy) {}

  /**
   * The connection settings of a channel.
   *
   * @param channel channel
   * @param mode LIVE or SIMULATOR
   * @param endpoint endpoint
   * @param keySetting name of the environment setting of the key
   * @param keySet whether the key is set on this server
   */
  public record Settings(
      String channel, String mode, String endpoint, String keySetting, boolean keySet) {}
}
