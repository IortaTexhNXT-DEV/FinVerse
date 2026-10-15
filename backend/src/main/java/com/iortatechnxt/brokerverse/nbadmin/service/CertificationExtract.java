package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.Role;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The certification extract of User Access: every account with its status, each group profile it
 * holds and each permission the group profile grants, one row per account, group profile and
 * permission (an account without a group profile has one row). The reviewers certify the access
 * from it. Each extract is logged (who, when, format, rows and the SHA-256 of the file) and
 * Information Security is told.
 */
@Service
@Transactional
public class CertificationExtract {

  /** Notification event of an extract taken. */
  public static final String CODE = "UAM_CERTIFICATION_EXTRACT";

  /** Frontend route of the extract. */
  public static final String SCREEN = "/access/reports";

  private static final String ENTITY = "CertificationExtract";

  private static final List<String> HEADER =
      List.of(
          "User ID",
          "Windows ID",
          "Name",
          "Business Unit",
          "Status",
          "Last Login",
          "Group Profile",
          "Group Profile Name",
          "Group Profile Active",
          "Privilege Level",
          "Permission");

  private final AppUserRepository users;
  private final JdbcTemplate jdbc;
  private final NoticeDelivery notices;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param users users
   * @param jdbc database (the extract log)
   * @param notices notices to Information Security
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public CertificationExtract(
      AppUserRepository users,
      JdbcTemplate jdbc,
      NoticeDelivery notices,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.users = users;
    this.jdbc = jdbc;
    this.notices = notices;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Takes an extract and logs it.
   *
   * @param format CSV or XLSX
   * @return the file
   */
  public ExtractFile take(String format) {
    boolean xlsx = "XLSX".equals(format);
    if (!xlsx && !"CSV".equals(format)) {
      throw new BusinessRuleException("CERTIFICATION_FORMAT", "Choose CSV or XLSX");
    }
    List<List<String>> rows = rows();
    byte[] content = xlsx ? xlsx(rows) : csv(rows);
    Instant now = clock.instant();
    String fileName = "uam_certification_" + BusinessClock.dateOf(now) + (xlsx ? ".xlsx" : ".csv");
    String sha256 = sha256(content);
    String by = currentUser.username();
    jdbc.update(
        "insert into nba_certification_extract (file_name, file_format, row_count, sha256,"
            + " taken_by, taken_at) values (?, ?, ?, ?, ?, ?)",
        fileName,
        format,
        rows.size(),
        sha256,
        by,
        Timestamp.from(now));
    audit.record(
        ENTITY,
        fileName,
        AuditAction.RUN,
        "Certification extract " + format + ", " + rows.size() + " row(s), SHA-256 " + sha256);
    notices.toPermission(
        SodRuleService.AUTHORIZE,
        new Notice(
            "Certification extract taken",
            by
                + " took a certification extract of the user access ("
                + format
                + ", "
                + rows.size()
                + " rows, SHA-256 "
                + sha256
                + ").",
            SCREEN,
            ENTITY,
            fileName),
        CODE,
        true);
    return new ExtractFile(fileName, xlsx, content);
  }

  /**
   * The extracts taken, newest first.
   *
   * @return log rows (at most 200)
   */
  @Transactional(readOnly = true)
  public List<LogEntry> log() {
    return jdbc.query(
        "select file_name, file_format, row_count, sha256, taken_by, taken_at"
            + " from nba_certification_extract order by taken_at desc, id desc limit 200",
        (rs, i) ->
            new LogEntry(
                rs.getString("file_name"),
                rs.getString("file_format"),
                rs.getInt("row_count"),
                rs.getString("sha256"),
                rs.getString("taken_by"),
                rs.getTimestamp("taken_at").toInstant()));
  }

  private List<List<String>> rows() {
    List<List<String>> rows = new ArrayList<>();
    List<AppUser> all = new ArrayList<>(users.findAll());
    all.sort(Comparator.comparing(AppUser::getUsername));
    for (AppUser u : all) {
      List<Role> roles = new ArrayList<>(u.getRoles());
      roles.sort(Comparator.comparing(Role::getCode));
      if (roles.isEmpty()) {
        rows.add(row(u, null, null));
      }
      for (Role r : roles) {
        List<Permission> permissions = new ArrayList<>(r.getPermissions());
        permissions.sort(Comparator.comparing(Permission::name));
        if (permissions.isEmpty()) {
          rows.add(row(u, r, null));
        }
        permissions.forEach(p -> rows.add(row(u, r, p)));
      }
    }
    return rows;
  }

  private static List<String> row(AppUser u, Role r, Permission p) {
    return List.of(
        u.getUsername(),
        text(u.getWindowsId()),
        text(u.getFullName()),
        text(u.getBusinessUnitCode()),
        status(u),
        u.getLastLoginAt() == null ? "" : u.getLastLoginAt().toString(),
        r == null ? "" : r.getCode(),
        r == null ? "" : r.getName(),
        r == null ? "" : (r.isActive() ? "Yes" : "No"),
        r == null || r.getPrivilegeLevel() == null ? "" : r.getPrivilegeLevel().name(),
        p == null ? "" : p.name());
  }

  private static String status(AppUser u) {
    if (!u.isEnabled()) {
      return "Inactive";
    }
    return u.isLocked() ? "Locked" : "Active";
  }

  private static String text(String value) {
    return Objects.toString(value, "");
  }

  private static byte[] csv(List<List<String>> rows) {
    StringBuilder out = new StringBuilder();
    appendCsv(out, HEADER);
    rows.forEach(r -> appendCsv(out, r));
    return out.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static void appendCsv(StringBuilder out, List<String> cells) {
    for (int i = 0; i < cells.size(); i++) {
      if (i > 0) {
        out.append(',');
      }
      String cell = cells.get(i);
      // A leading formula character is quoted so a spreadsheet does not run it.
      boolean formula = !cell.isEmpty() && "=+-@".indexOf(cell.charAt(0)) >= 0;
      String safe = formula ? "'" + cell : cell;
      if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
        out.append('"').append(safe.replace("\"", "\"\"")).append('"');
      } else {
        out.append(safe);
      }
    }
    out.append("\r\n");
  }

  private static byte[] xlsx(List<List<String>> rows) {
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("Certification");
      write(sheet.createRow(0), HEADER);
      for (int i = 0; i < rows.size(); i++) {
        write(sheet.createRow(i + 1), rows.get(i));
      }
      sheet.createFreezePane(0, 1);
      wb.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void write(Row row, List<String> cells) {
    for (int i = 0; i < cells.size(); i++) {
      row.createCell(i).setCellValue(cells.get(i));
    }
  }

  private static String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * An extract file.
   *
   * @param fileName file name
   * @param xlsx true for XLSX, false for CSV
   * @param content bytes
   */
  public record ExtractFile(String fileName, boolean xlsx, byte[] content) {

    /** Keeps its own copy of the bytes. */
    public ExtractFile {
      content = content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof ExtractFile f
          && fileName.equals(f.fileName)
          && xlsx == f.xlsx
          && java.util.Arrays.equals(content, f.content);
    }

    @Override
    public int hashCode() {
      return 31 * Objects.hash(fileName, xlsx) + java.util.Arrays.hashCode(content);
    }

    @Override
    public String toString() {
      return "ExtractFile[" + fileName + ", " + content.length + " bytes]";
    }
  }

  /**
   * An extract taken.
   *
   * @param fileName file name
   * @param format CSV or XLSX
   * @param rows rows of the file
   * @param sha256 SHA-256 of the file
   * @param takenBy who took it
   * @param takenAt when
   */
  public record LogEntry(
      String fileName, String format, int rows, String sha256, String takenBy, Instant takenAt) {}
}
