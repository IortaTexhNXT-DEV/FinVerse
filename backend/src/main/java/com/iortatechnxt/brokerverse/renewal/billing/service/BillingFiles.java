package com.iortatechnxt.brokerverse.renewal.billing.service;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelGateways;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelService;
import com.iortatechnxt.brokerverse.renewal.domain.BillingFile;
import com.iortatechnxt.brokerverse.renewal.domain.BillingFileRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CLPC billing files (FRRN.27.01 to FRRN.27.03): the accounts For Billing Generation - CBG Home
 * accounts the set days before expiry (daily job), FFY Motor accounts (job on the set days of the
 * month), or those of a period of at most 31 days chosen by the user - split per product line into
 * the Built-in file (amortized accounts) and the Non-Built-in file, named {@code <Product
 * Line>_CLPC Billing Built in_MMDDYYYY.csv} and {@code <Product Line>_CLPC Billing NonBuilt
 * in_MMDDYYYY.csv} with the fields of the billing file template; each file is attached to its
 * accounts, sent to the nominated recipients and written to the shared LMS directory.
 */
@Service
public class BillingFiles {

  /** Run: CBG Home accounts before expiry. */
  public static final String CBG_HOME = "CBG_HOME";

  /** Run: FFY Motor accounts. */
  public static final String FFY = "FFY";

  /** Run: a period chosen by the user. */
  public static final String MANUAL = "MANUAL";

  /** Columns of the billing file. */
  public static final List<String> HEADERS =
      List.of(
          "No.",
          "PN #",
          "Account No.",
          "Assured's Name",
          "Loan Booking Date",
          "Loan Maturity Date",
          "Inception Date",
          "Expiry Date",
          "Loan Type",
          "Total Premium (1 Year)",
          "Logical Branch Description PN",
          "Insure Branch",
          "Enrolled Account Number");

  private static final DateTimeFormatter NAME_DATE =
      DateTimeFormatter.ofPattern("MMddyyyy", Locale.ROOT);
  private static final DateTimeFormatter CELL_DATE =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
  private static final int MAX_DAYS = 31;
  private static final String HOME_LINE = "PROPERTY";
  private static final String KIND = "BILLING";

  private final NamedParameterJdbcTemplate jdbc;
  private final RenewalCandidateRepository candidates;
  private final BillingFileRepository files;
  private final DocumentService storage;
  private final ChannelService channels;
  private final LmsDirectory lms;
  private final RenewalParameters renewal;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param jdbc eligible accounts and loan data
   * @param candidates renewals
   * @param files billing files
   * @param storage stored files
   * @param channels delivery
   * @param lms shared LMS directory
   * @param renewal CBG segments
   * @param parameters days and recipients
   * @param audit audit trail
   */
  public BillingFiles(
      NamedParameterJdbcTemplate jdbc,
      RenewalCandidateRepository candidates,
      BillingFileRepository files,
      DocumentService storage,
      ChannelService channels,
      LmsDirectory lms,
      RenewalParameters renewal,
      SystemParameterService parameters,
      AuditTrailService audit) {
    this.jdbc = jdbc;
    this.candidates = candidates;
    this.files = files;
    this.storage = storage;
    this.channels = channels;
    this.lms = lms;
    this.renewal = renewal;
    this.parameters = parameters;
    this.audit = audit;
  }

  /**
   * Generates the billing files of a period chosen by the user.
   *
   * @param companyId company
   * @param from first day
   * @param to last day
   * @param today business date
   * @return the files
   */
  @Transactional
  public List<BillingFile> manual(Long companyId, LocalDate from, LocalDate to, LocalDate today) {
    if (from == null || to == null || from.isAfter(to)) {
      throw new BusinessRuleException(
          "RNW_BILLING_PERIOD", "From Date must not be later than To Date");
    }
    if (ChronoUnit.DAYS.between(from, to) > MAX_DAYS) {
      throw new BusinessRuleException(
          "RNW_BILLING_PERIOD", "From Date cannot be more than 31 calendar days before To Date");
    }
    if (to.isAfter(today)) {
      throw new BusinessRuleException("RNW_BILLING_PERIOD", "To Date cannot be after today");
    }
    return generate(companyId, new BillingFile.Run(MANUAL, from, to), today);
  }

  /**
   * Generates the billing files of a scheduled run.
   *
   * @param companyId company
   * @param kind CBG_HOME or FFY
   * @param today business date
   * @return the files
   */
  @Transactional
  public List<BillingFile> scheduled(Long companyId, String kind, LocalDate today) {
    return generate(companyId, new BillingFile.Run(kind, null, null), today);
  }

  /**
   * The billing files of a company, newest first.
   *
   * @param companyId company
   * @return files
   */
  @Transactional(readOnly = true)
  public List<BillingFile> list(Long companyId) {
    return files.findTop200ByCompanyIdOrderByIdDesc(companyId);
  }

  private List<BillingFile> generate(Long companyId, BillingFile.Run run, LocalDate today) {
    Map<BillingFile.Group, List<RenewalCandidate>> groups = new LinkedHashMap<>();
    for (RenewalCandidate c : eligible(companyId, run, today)) {
      String line = c.getSnapshot().product() == null ? null : c.getSnapshot().product().lineCode();
      groups
          .computeIfAbsent(
              new BillingFile.Group(line, c.getPlacement().getTags().isAmortized()),
              k -> new ArrayList<>())
          .add(c);
    }
    List<BillingFile> made = new ArrayList<>();
    groups.forEach((group, accounts) -> made.add(file(companyId, group, run, accounts, today)));
    return made;
  }

  private BillingFile file(
      Long companyId,
      BillingFile.Group group,
      BillingFile.Run run,
      List<RenewalCandidate> accounts,
      LocalDate today) {
    String name =
        (group.lineCode() == null ? "" : group.lineCode() + "_")
            + (group.builtIn() ? "CLPC Billing Built in_" : "CLPC Billing NonBuilt in_")
            + NAME_DATE.format(today)
            + ".csv";
    byte[] csv = csv(accounts);
    BillingFile f = files.save(new BillingFile(companyId, name, group, run, accounts.size()));
    f.stored(attach(accounts, name, csv));
    accounts.forEach(c -> c.getPlacement().getTags().billed(f.getId()));
    send(f);
    f.lms(lms.upload(name, csv));
    audit.record(
        "BillingFile", name, AuditAction.CREATE, accounts.size() + " account(s), " + run.kind());
    return f;
  }

  private void send(BillingFile f) {
    List<String> to = parameters.items("RNW_CLPC_BILLING_RECIPIENTS");
    if (to.isEmpty()) {
      f.delivered("FAILED", null, "No recipient of the billing files is set");
      return;
    }
    ChannelService.Sent sent =
        channels.send(
            f.getCompanyId(),
            new ChannelService.Outbound(
                ChannelGateways.CCM,
                new ChannelMessage.Document(
                    KIND, f.getFileName(), null, null, f.getFileName(), f.getAttachmentId()),
                new ChannelMessage.Address(
                    String.join(",", to),
                    null,
                    "CLPC billing file " + f.getFileName(),
                    "Please find attached the CLPC billing file " + f.getFileName() + ".",
                    null)));
    f.delivered(
        sent.error() == null ? sent.message().getStatus().name() : "FAILED",
        sent.message().getMessageNo(),
        sent.error());
  }

  private Long attach(List<RenewalCandidate> accounts, String name, byte[] csv) {
    RenewalCandidate first = accounts.get(0);
    Long id =
        storage
            .upload(
                new AttachmentTarget(RenewalCodes.ENTITY, first.getId().toString()),
                List.of(new DocumentService.UploadedFile(name, csv)),
                new DocumentService.UploadOptions(
                    RenewalCodes.DOC_RENEWAL_LETTER, false, first.getRenewalRef(), name, null))
            .get(0)
            .getId();
    List<AttachmentTarget> others =
        accounts.stream()
            .skip(1)
            .map(c -> new AttachmentTarget(RenewalCodes.ENTITY, c.getId().toString()))
            .toList();
    if (!others.isEmpty()) {
      storage.link(id, others);
    }
    return id;
  }

  private List<RenewalCandidate> eligible(Long companyId, BillingFile.Run run, LocalDate today) {
    MapSqlParameterSource args =
        new MapSqlParameterSource()
            .addValue("companyId", companyId)
            .addValue(
                "limit", today.plusDays(parameters.intValue("RNW_CLPC_BILLING_DAYS_BEFORE", 10)))
            .addValue("from", run.from())
            .addValue("to", run.to() == null ? null : run.to().plusDays(1));
    String where =
        switch (run.kind()) {
          case CBG_HOME -> " and c.line_code = 'PROPERTY' and c.expiry_date <= :limit";
          case FFY ->
              " and exists (select 1 from acc_account a where a.arn = c.expiring_arn and a.ffy)";
          default ->
              " and c.client_status_at >= cast(:from as date)"
                  + " and c.client_status_at < cast(:to as date)";
        };
    List<Long> ids =
        jdbc.queryForList(
            "select c.id from rnw_candidate c where c.company_id = :companyId"
                + " and c.stage = 'RA_SENT' and c.client_status = 'NOT_APPLICABLE'"
                + " and c.billing_file_id is null"
                + where
                + " order by c.renewal_ref",
            args,
            Long.class);
    return candidates.findAllById(ids).stream()
        .filter(c -> !CBG_HOME.equals(run.kind()) || cbg(c))
        .toList();
  }

  private boolean cbg(RenewalCandidate c) {
    var p = c.getSnapshot().product();
    return p != null && renewal.cbgSegment(p.segment()) && HOME_LINE.equals(p.lineCode());
  }

  private byte[] csv(List<RenewalCandidate> accounts) {
    StringBuilder out = new StringBuilder(line(HEADERS));
    int no = 1;
    for (RenewalCandidate c : accounts) {
      out.append(line(row(no++, c)));
    }
    return out.toString().getBytes(StandardCharsets.UTF_8);
  }

  private List<String> row(int no, RenewalCandidate c) {
    Map<String, Object> loan = loan(c);
    LocalDate inception = c.getExpiryDate();
    return List.of(
        String.valueOf(no),
        text(c.getSnapshot().pnNos()),
        c.getRenewalRef(),
        text(c.getSnapshot().clientName()),
        date(loan.get("book_date")),
        date(loan.get("maturity_date")),
        CELL_DATE.format(inception),
        CELL_DATE.format(inception.plusYears(1)),
        text(loan.get("ibg_cbg_tag")),
        c.getSnapshot().premium() == null || c.getSnapshot().premium().grossPremium() == null
            ? ""
            : c.getSnapshot().premium().grossPremium().toPlainString(),
        text(loan.get("branch")),
        c.getSnapshot().sales() == null ? "" : text(c.getSnapshot().sales().branchCode()),
        "");
  }

  private Map<String, Object> loan(RenewalCandidate c) {
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "select book_date, maturity_date, ibg_cbg_tag, branch from rnw_lamd_loan"
                + " where candidate_id = :id and report_kind = 'CBG_LOANS' order by id desc limit 1",
            Map.of("id", c.getId()));
    return rows.isEmpty() ? Map.of() : rows.get(0);
  }

  private static String date(Object value) {
    if (value instanceof java.sql.Date d) {
      return CELL_DATE.format(d.toLocalDate());
    }
    return value instanceof LocalDate l ? CELL_DATE.format(l) : "";
  }

  private static String text(Object value) {
    return value == null ? "" : value.toString();
  }

  private static String line(List<String> cells) {
    List<String> quoted = new ArrayList<>();
    for (String cell : cells) {
      quoted.add(
          cell.contains(",") || cell.contains("\"")
              ? "\"" + cell.replace("\"", "\"\"") + "\""
              : cell);
    }
    return String.join(",", quoted) + "\r\n";
  }
}
