package com.iortatechnxt.brokerverse.eb.soa.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.eb.document.api.EbUploads;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbSoa;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.eb.soa.api.dto.SoaDtos.InvoicesRequest;
import com.iortatechnxt.brokerverse.eb.soa.api.dto.SoaDtos.ProgrammeInvoice;
import com.iortatechnxt.brokerverse.eb.soa.api.dto.SoaDtos.RejectRequest;
import com.iortatechnxt.brokerverse.eb.soa.api.dto.SoaDtos.SoaResponse;
import com.iortatechnxt.brokerverse.eb.soa.service.EbSoaService;
import com.iortatechnxt.brokerverse.eb.soa.service.SoaInvoices;
import com.iortatechnxt.brokerverse.eb.soa.service.SoaQuery;
import com.iortatechnxt.brokerverse.eb.soa.service.SoaRelease;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The SOA register and the Billing & SOA tab (FR-EB-053). */
@RestController
@RequestMapping("/api/v1/eb")
@Transactional
public class EbSoaController {

  private static final String VIEW = "hasAuthority('EB_VIEW')";

  /** The SOA register: EB processing and the collection team (EB_COLLECT) read it. */
  private static final String SOA_VIEW = "hasAnyAuthority('EB_VIEW', 'EB_PROCESS', 'EB_COLLECT')";

  private static final String INTAKE = "hasAnyAuthority('EB_MARKET', 'EB_PROCESS')";
  private static final String PROCESS = "hasAuthority('EB_PROCESS')";
  private static final int MAX_PAGE = 200;

  private final EbSoaService soas;
  private final SoaRelease release;
  private final SoaQuery query;
  private final SoaInvoices invoices;
  private final EbRecords records;
  private final EbProgrammeRepository programmes;
  private final EbParties parties;

  /**
   * Creates the controller.
   *
   * @param soas SOAs
   * @param release release to the client
   * @param query register
   * @param invoices invoices and payment status
   * @param records programme look-up
   * @param programmes programmes (register columns)
   * @param parties insurer names
   */
  @SuppressWarnings("java:S107") // constructor injection
  public EbSoaController(
      EbSoaService soas,
      SoaRelease release,
      SoaQuery query,
      SoaInvoices invoices,
      EbRecords records,
      EbProgrammeRepository programmes,
      EbParties parties) {
    this.soas = soas;
    this.release = release;
    this.query = query;
    this.invoices = invoices;
    this.records = records;
    this.programmes = programmes;
    this.parties = parties;
  }

  /**
   * The SOA register.
   *
   * @param companyId company
   * @param status status
   * @param insurer insurer
   * @param programmeId programme
   * @param q text
   * @param page page
   * @param size size
   * @return SOAs
   */
  @GetMapping("/soa")
  @PreAuthorize(SOA_VIEW)
  @SuppressWarnings("java:S107") // one parameter per filter
  public PageResponse<SoaResponse> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String insurer,
      @RequestParam(required = false) Long programmeId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return PageResponse.of(
        query.search(
            companyId,
            new SoaQuery.Filter(status, insurer, programmeId, q),
            PageRequest.of(page, Math.min(size, MAX_PAGE))),
        this::map);
  }

  /**
   * An SOA.
   *
   * @param id SOA
   * @param companyId company
   * @return SOA
   */
  @GetMapping("/soa/{id}")
  @PreAuthorize(SOA_VIEW)
  public SoaResponse get(@PathVariable Long id, @RequestParam Long companyId) {
    return map(soas.require(companyId, id));
  }

  /**
   * The booked invoices of a programme with their payment status.
   *
   * @param id programme
   * @param companyId company
   * @return invoices
   */
  @GetMapping("/programmes/{id}/invoices")
  @PreAuthorize(VIEW)
  public List<ProgrammeInvoice> programmeInvoices(
      @PathVariable Long id, @RequestParam Long companyId) {
    return invoices.ofProgramme(records.programme(companyId, id)).stream()
        .map(ProgrammeInvoice::from)
        .toList();
  }

  /**
   * Registers a received SOA.
   *
   * @param id programme
   * @param companyId company
   * @param intake SOA details
   * @param invoiceNos invoices billed
   * @param file the SOA
   * @return SOA
   */
  @PostMapping(value = "/programmes/{id}/soa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(INTAKE)
  public SoaResponse receive(
      @PathVariable Long id,
      @RequestParam Long companyId,
      SoaForm intake,
      @RequestParam(required = false) List<String> invoiceNos,
      @RequestParam(required = false) MultipartFile file) {
    return map(
        soas.receive(
            companyId,
            id,
            new EbSoa.Intake(
                intake.insurerCode(),
                intake.insurerSoaNo(),
                intake.periodFrom(),
                intake.periodTo(),
                intake.amount(),
                intake.currency(),
                intake.receivedOn(),
                intake.remarks()),
            invoiceNos,
            EbUploads.file(file)));
  }

  /**
   * Validates an SOA.
   *
   * @param id SOA
   * @param companyId company
   * @param request invoices billed
   * @return SOA
   */
  @PostMapping("/soa/{id}/validate")
  @PreAuthorize(PROCESS)
  public SoaResponse validate(
      @PathVariable Long id,
      @RequestParam Long companyId,
      @RequestBody(required = false) InvoicesRequest request) {
    return map(soas.validate(companyId, id, request == null ? null : request.invoiceNos()));
  }

  /**
   * Rejects an SOA.
   *
   * @param id SOA
   * @param companyId company
   * @param request reason and remarks
   * @return SOA
   */
  @PostMapping("/soa/{id}/reject")
  @PreAuthorize(PROCESS)
  public SoaResponse reject(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody RejectRequest request) {
    return map(soas.reject(companyId, id, request.reasonCode(), request.remarks()));
  }

  /**
   * Releases an SOA to the client and Collection.
   *
   * @param id SOA
   * @param companyId company
   * @return SOA
   */
  @PostMapping("/soa/{id}/release")
  @PreAuthorize(PROCESS)
  public SoaResponse release(@PathVariable Long id, @RequestParam Long companyId) {
    return map(release.release(companyId, id));
  }

  private SoaResponse map(EbSoa s) {
    return SoaResponse.from(
        s,
        programmes.findById(s.getProgrammeId()).orElse(null),
        parties.insurerName(s.getCompanyId(), s.getInsurerCode()),
        invoices.paymentStatus(s.getInvoiceNos()));
  }

  /**
   * The SOA fields of the intake form.
   *
   * @param insurerCode insurer
   * @param insurerSoaNo insurer's SOA number
   * @param periodFrom period from
   * @param periodTo period to
   * @param amount amount
   * @param currency currency
   * @param receivedOn received on
   * @param remarks remarks
   */
  public record SoaForm(
      String insurerCode,
      String insurerSoaNo,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
      BigDecimal amount,
      String currency,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receivedOn,
      String remarks) {}
}
