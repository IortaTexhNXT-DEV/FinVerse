package com.iortatechnxt.brokerverse.collections.unapplied.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.ApplicationRequest;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.ApplicationRequestRepository;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.PaymentSnapshot;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DropContent;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DroppedFile;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDispositionRequests.Action;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The daily "For Application To Invoice" text file (BRCLXN.041/042): the application requests made
 * up to the end of a day (Philippine time) and not yet listed, one pipe-delimited line each with
 * the p.60 fields (payment date, payment file name, transaction no., paid amount, payment type,
 * payor, reference no., assured, invoice no. and the user ID of the disposition), dropped through
 * {@link FileDropPort} in the folder of the BDOI file server FS04 (in-system extract repository
 * until OQ17; layout to confirm, CQ12). Each request records the name of the file that listed it,
 * so a request is listed once.
 */
@Service
@Transactional
public class ApplicationFileService {

  /** Folder of the file (FS04, OQ17). */
  public static final String FOLDER = "FS04/CLX_APPLICATION_TO_INVOICE";

  /** Header line of the file. */
  static final String HEADER =
      "PAYMENT_DATE|PAYMENT_FILE_NAME|TRANSACTION_NO|PAID_AMOUNT|CURRENCY|PAYMENT_TYPE|PAYOR"
          + "|REFERENCE_NO|ASSURED|INVOICE_NO|USER_ID|UNAPPLIED_REF|REQUEST_REF";

  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
  private static final String SEPARATOR = "|";

  private final ApplicationRequestRepository requests;
  private final FileDropPort drop;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param drop file drop
   * @param audit audit trail
   * @param clock clock
   */
  public ApplicationFileService(
      ApplicationRequestRepository requests,
      FileDropPort drop,
      AuditTrailService audit,
      Clock clock) {
    this.requests = requests;
    this.drop = drop;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The day whose requests the job files: yesterday in the Philippines.
   *
   * @return day
   */
  public LocalDate previousDay() {
    return LocalDate.ofInstant(clock.instant(), MANILA).minusDays(1);
  }

  /**
   * Writes the file of the application requests made up to the end of a day and not yet listed.
   *
   * @param companyId company
   * @param day last day of the requests (Philippine time)
   * @return the file, empty when there was nothing to list
   */
  public Optional<DroppedFile> publish(Long companyId, LocalDate day) {
    List<ApplicationRequest> due =
        requests.findByCompanyIdAndActionAndFileRunNoIsNullAndRequestedAtBeforeOrderByIdAsc(
            companyId,
            Action.APPLY_TO_INVOICE.name(),
            day.plusDays(1).atStartOfDay(MANILA).toInstant());
    if (due.isEmpty()) {
      return Optional.empty();
    }
    String fileName =
        "FOR_APPLICATION_TO_INVOICE_"
            + day.format(DateTimeFormatter.BASIC_ISO_DATE)
            + "_"
            + clock.instant().atZone(MANILA).format(STAMP)
            + ".txt";
    String content =
        Stream.concat(Stream.of(HEADER), due.stream().map(ApplicationFileService::line))
                .collect(Collectors.joining("\r\n"))
            + "\r\n";
    DroppedFile file =
        drop.drop(
            companyId,
            new ExtractFile.Location(FOLDER, fileName),
            new DropContent("text/plain", content.getBytes(StandardCharsets.UTF_8)),
            new ExtractFile.Origin("COLLECTIONS", "CLX_APPLICATION_FILE:" + day));
    due.forEach(r -> r.filed(fileName, clock.instant()));
    audit.record(
        ApplicationRequestService.ENTITY,
        fileName,
        AuditAction.EXPORT,
        due.size() + " application request(s) of " + day);
    return Optional.of(file);
  }

  /**
   * One line of the file.
   *
   * @param r request
   * @return pipe-delimited fields
   */
  static String line(ApplicationRequest r) {
    PaymentSnapshot p = r.getPayment();
    return Stream.of(
            p.paymentDate() == null ? "" : p.paymentDate().toString(),
            p.paymentFileName(),
            p.transactionNo(),
            p.paidAmount() == null ? "" : p.paidAmount().toPlainString(),
            p.currency(),
            p.paymentType(),
            p.payorName(),
            p.referenceNo(),
            p.assuredName(),
            r.getInvoiceNo(),
            r.getRequestedBy(),
            r.getUnappliedRef(),
            r.getSourceRef())
        .map(ApplicationFileService::clean)
        .collect(Collectors.joining(SEPARATOR));
  }

  private static String clean(String value) {
    return value == null ? "" : value.replace(SEPARATOR, "/").replace('\r', ' ').replace('\n', ' ');
  }
}
