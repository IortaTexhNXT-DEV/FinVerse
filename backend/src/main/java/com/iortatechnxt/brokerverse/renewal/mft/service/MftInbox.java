package com.iortatechnxt.brokerverse.renewal.mft.service;

import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.bulk.service.BulkUpload;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessageRepository;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelStatus;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelGateways;
import com.iortatechnxt.brokerverse.renewal.domain.EpolicyReceipt;
import com.iortatechnxt.brokerverse.renewal.epolicy.service.EpolicyReceipts;
import com.iortatechnxt.brokerverse.renewal.holdcover.service.HoldCoverResponseHandler;
import com.iortatechnxt.brokerverse.renewal.placement.service.PlacementResponseHandler;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Files received from the insurers through MFT (FRRN.030.01, FRRN.033.01, FRRN.037.01): the
 * insurers' placement responses, hold cover responses and e-policy files placed in the inbound
 * folders under the MFT root are taken at each run and processed like uploaded files; each file is
 * logged as an inbound MFT message (Received, then Processed or Failed) and moved to the sub-folder
 * {@code processed} or {@code refused}. An e-policy summary file waits for its ZIP file of the same
 * name. In SIT and UAT the simulator places a file in a folder; at BDOI the MFT service fills the
 * folders.
 */
@Service
public class MftInbox {

  /** Inbound folder of the placement responses. */
  public static final String PLACEMENT = "placement-response";

  /** Inbound folder of the hold cover responses. */
  public static final String HOLD_COVER = "hold-cover-response";

  /** Inbound folder of the e-policy files. */
  public static final String EPOLICY = "epolicy";

  private static final Map<String, String> HANDLERS =
      Map.of(PLACEMENT, PlacementResponseHandler.CODE, HOLD_COVER, HoldCoverResponseHandler.CODE);
  private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9._ -]{1,150}");

  /** User recorded as the uploader of the files received via MFT. */
  public static final String MFT_USER = "MFT";

  private static final String PROCESSED = "processed";
  private static final String REFUSED = "refused";
  private static final String ZIP = ".zip";
  private static final String TXT = ".txt";

  private final BulkService bulk;
  private final EpolicyReceipts epolicies;
  private final ChannelMessageRepository messages;
  private final DocumentNumberService numbers;
  private final OrganizationService organization;
  private final SystemParameterService parameters;
  private final Clock clock;
  private final Path root;
  private final TransactionTemplate tx;

  /**
   * Creates the inbox.
   *
   * @param bulk uploads of the response files
   * @param epolicies e-policy receipts
   * @param messages inbound message log
   * @param numbers message numbers
   * @param organization companies
   * @param parameters company of the MFT files
   * @param clock clock
   * @param root MFT root
   * @param txManager transactions of the inbound messages
   */
  public MftInbox(
      BulkService bulk,
      EpolicyReceipts epolicies,
      ChannelMessageRepository messages,
      DocumentNumberService numbers,
      OrganizationService organization,
      SystemParameterService parameters,
      Clock clock,
      @Value("${brokerverse.renewal.mft-root:${java.io.tmpdir}/bibs-rnw-mft}") String root,
      PlatformTransactionManager txManager) {
    this.bulk = bulk;
    this.epolicies = epolicies;
    this.messages = messages;
    this.numbers = numbers;
    this.organization = organization;
    this.parameters = parameters;
    this.clock = clock;
    this.root = Path.of(root).toAbsolutePath().normalize();
    this.tx = new TransactionTemplate(txManager);
  }

  /**
   * Takes the files waiting in the inbound folders.
   *
   * @return files processed
   */
  public int poll() {
    Authentication previous = SecurityContextHolder.getContext().getAuthentication();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                MFT_USER,
                null,
                List.of(
                    new SimpleGrantedAuthority("RNW_PROCESS"),
                    new SimpleGrantedAuthority("RNW_PROCESS_ASSIGN"))));
    try {
      return take(company());
    } finally {
      SecurityContextHolder.getContext().setAuthentication(previous);
    }
  }

  private int take(Long companyId) {
    int done = 0;
    for (Map.Entry<String, String> e : HANDLERS.entrySet()) {
      for (Path file : waiting(folder(e.getKey()), null)) {
        done += takeBulk(companyId, file, e.getValue());
      }
    }
    for (Path summary : waiting(folder(EPOLICY), TXT)) {
      done += takeEpolicy(companyId, summary);
    }
    return done;
  }

  /**
   * Places a file in an inbound folder, as the MFT service does (simulator of SIT and UAT).
   *
   * @param kind folder: placement-response, hold-cover-response or epolicy
   * @param fileName file name
   * @param content content
   * @return the file placed
   */
  public Path place(String kind, String fileName, byte[] content) {
    if (!HANDLERS.containsKey(kind) && !EPOLICY.equals(kind)) {
      throw new BusinessRuleException("RNW_MFT_FOLDER", "Unknown MFT folder " + kind);
    }
    if (fileName == null || !SAFE_NAME.matcher(fileName).matches()) {
      throw new BusinessRuleException("RNW_MFT_FILE", "Invalid file name");
    }
    try {
      Path folder = folder(kind);
      Files.createDirectories(folder);
      return Files.write(folder.resolve(fileName), content);
    } catch (IOException ex) {
      throw new UncheckedIOException("MFT folder " + kind + " cannot be written", ex);
    }
  }

  /**
   * The inbound folder of a kind of file.
   *
   * @param kind kind
   * @return folder
   */
  public Path folder(String kind) {
    return root.resolve("inbound").resolve(kind).normalize();
  }

  private int takeBulk(Long companyId, Path file, String handler) {
    String name = name(file);
    ChannelMessage m = Objects.requireNonNull(tx.execute(s -> received(companyId, handler, name)));
    try {
      BulkJob job =
          bulk.upload(new BulkUpload(companyId, handler, name, Files.readAllBytes(file), Map.of()));
      job = bulk.commit(job.getId());
      m.moveTo(
          ChannelStatus.PROCESSED,
          job.getCommittedRows()
              + " of "
              + job.getTotalRows()
              + " records processed ("
              + job.getJobNo()
              + ")",
          clock.instant());
      move(file, PROCESSED);
    } catch (IOException | RuntimeException ex) {
      m.moveTo(ChannelStatus.FAILED, ex.getMessage(), clock.instant());
      move(file, REFUSED);
    }
    tx.executeWithoutResult(s -> messages.save(m));
    return 1;
  }

  private int takeEpolicy(Long companyId, Path summary) {
    String base = name(summary).substring(0, name(summary).length() - TXT.length());
    Path zip = summary.resolveSibling(base + ZIP);
    if (!Files.exists(zip)) {
      return 0;
    }
    ChannelMessage m =
        Objects.requireNonNull(tx.execute(s -> received(companyId, "EPOLICY", base)));
    try {
      EpolicyReceipt r =
          epolicies.receive(
              companyId,
              "MFT",
              new EpolicyReceipts.Incoming(name(summary), Files.readAllBytes(summary)),
              new EpolicyReceipts.Incoming(name(zip), Files.readAllBytes(zip)));
      boolean ok = EpolicyReceipt.SUCCESSFUL.equals(r.getStatus());
      m.moveTo(
          ok ? ChannelStatus.PROCESSED : ChannelStatus.FAILED,
          r.getReceiptNo() + (r.getRemarks() == null ? "" : ": " + r.getRemarks()),
          clock.instant());
      move(summary, ok ? PROCESSED : REFUSED);
      move(zip, ok ? PROCESSED : REFUSED);
    } catch (IOException ex) {
      m.moveTo(ChannelStatus.FAILED, ex.getMessage(), clock.instant());
    }
    tx.executeWithoutResult(s -> messages.save(m));
    return 1;
  }

  private ChannelMessage received(Long companyId, String kind, String fileName) {
    return messages.save(
        new ChannelMessage(
            companyId,
            numbers.next(ChannelGateways.MFT + "-IN-" + BusinessClock.today(clock).getYear()),
            new ChannelMessage.Route(ChannelGateways.MFT, "INBOUND"),
            new ChannelMessage.Document(kind, fileName, null, null, fileName, null),
            new ChannelMessage.Address("MFT", null, fileName, null, null)));
  }

  private static List<Path> waiting(Path folder, String suffix) {
    if (!Files.isDirectory(folder)) {
      return List.of();
    }
    try (Stream<Path> files = Files.list(folder)) {
      return new ArrayList<>(
          files
              .filter(Files::isRegularFile)
              .filter(f -> suffix == null || name(f).endsWith(suffix))
              .sorted()
              .toList());
    } catch (IOException ex) {
      throw new UncheckedIOException("MFT folder " + folder + " cannot be read", ex);
    }
  }

  private static void move(Path file, String to) {
    try {
      Path target = file.resolveSibling(to);
      Files.createDirectories(target);
      Files.move(file, target.resolve(name(file)), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ex) {
      throw new UncheckedIOException("MFT file " + file + " cannot be moved", ex);
    }
  }

  private static String name(Path file) {
    Path n = file.getFileName();
    return n == null ? file.toString() : n.toString();
  }

  private Long company() {
    String code = parameters.text("RNW_MFT_COMPANY_CODE", "").strip();
    List<Company> companies = organization.listCompanies();
    return companies.stream()
        .filter(c -> code.isEmpty() || code.equals(c.getCode()))
        .findFirst()
        .orElseThrow(
            () -> new BusinessRuleException("MFT_NO_COMPANY", "No company receives the MFT files"))
        .getId();
  }
}
