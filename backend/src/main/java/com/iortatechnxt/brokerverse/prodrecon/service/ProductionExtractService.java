package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.LedgerSearch;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DropContent;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DroppedFile;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycle;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.ExtractTrigger;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtract;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtractLine;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtractLineRepository;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtractRepository;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItemRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production register extraction (PRCID.001-006/011/020/034): the invoices booked with an insurer
 * in a booking period are read from the Operations ledger, snapshot as register lines, added to the
 * open cycle of the production month as BDOI-side items, written as the locked-column workbook
 * named per {@code PRODRECON_FILE_PATTERN} and dropped in the extract repository folder {@code
 * PRODRECON/<insurer>} (shared drive parked, OQ17).
 */
@Service
@Transactional
public class ProductionExtractService {

  /** Module name on files, movements and hand-offs. */
  public static final String MODULE = "PRODRECON";

  private static final String ENTITY = "ReconExtract";
  private static final int PAGE = 200;

  private final ReconCycleService cycles;
  private final ReconExtractRepository extracts;
  private final ReconExtractLineRepository lines;
  private final ReconItemRepository items;
  private final InvoiceLedgerQueryService ledger;
  private final ProductionRegisterWorkbook workbook;
  private final FileDropPort drop;
  private final ReconSettings settings;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param cycles cycles
   * @param extracts extracts
   * @param lines register lines
   * @param items reconciliation items
   * @param ledger Operations ledger
   * @param workbook register workbook writer
   * @param drop shared-drive drop (extract repository)
   * @param settings parameters
   * @param numbers extract numbers
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ProductionExtractService(
      ReconCycleService cycles,
      ReconExtractRepository extracts,
      ReconExtractLineRepository lines,
      ReconItemRepository items,
      InvoiceLedgerQueryService ledger,
      ProductionRegisterWorkbook workbook,
      FileDropPort drop,
      ReconSettings settings,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.cycles = cycles;
    this.extracts = extracts;
    this.lines = lines;
    this.items = items;
    this.ledger = ledger;
    this.workbook = workbook;
    this.drop = drop;
    this.settings = settings;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Extracts the production register of an insurer for a booking period. The production month (and
   * cycle) is the month of the first booking date.
   *
   * @param request company, insurer and booking period
   * @param trigger scheduled or manual
   * @return the extract
   */
  public ReconExtract extract(ExtractRequest request, ExtractTrigger trigger) {
    if (request.to().isBefore(request.from())) {
      throw new BusinessRuleException(
          "RECON_EXTRACT_PERIOD", "The booking period ends before it starts");
    }
    List<OpsInvoice> booked = booked(request);
    if (booked.isEmpty()) {
      throw new BusinessRuleException(
          "RECON_NOTHING_TO_EXTRACT",
          "No invoice was booked with "
              + request.insurerCode()
              + " from "
              + request.from()
              + " to "
              + request.to());
    }
    ReconCycle cycle = cycles.openOrGet(request.companyId(), request.insurerCode(), request.from());
    String no = numbers.next("PRX-" + request.from().getYear());
    ReconExtract extract =
        extracts.save(new ReconExtract(cycle.getId(), no, trigger, request.from(), request.to()));
    List<ReconExtractLine> written = new ArrayList<>();
    int added = 0;
    for (OpsInvoice invoice : booked) {
      ReconExtractLine line =
          lines.save(
              ReconExtractLine.of(
                  extract.getId(),
                  written.size() + 1,
                  invoice,
                  ReconFacts.lastPayment(ledger.movements(invoice.getInvoiceNo()))));
      written.add(line);
      if (items.findByCycleIdAndInvoiceNo(cycle.getId(), invoice.getInvoiceNo()).isEmpty()) {
        items.save(ReconItem.booked(cycle.getId(), ReconFacts.of(invoice, line.getId())));
        added++;
      }
    }
    store(request.companyId(), cycle, extract, written, added);
    audit.record(
        ENTITY,
        no,
        AuditAction.CREATE,
        trigger
            + " extract of "
            + written.size()
            + " invoice(s) booked with "
            + request.insurerCode()
            + " into "
            + cycle.getCycleNo()
            + " ("
            + added
            + " new)");
    return extract;
  }

  private void store(
      Long companyId,
      ReconCycle cycle,
      ReconExtract extract,
      List<ReconExtractLine> written,
      int added) {
    String no = extract.getExtractNo();
    String fileName =
        settings.fileName(
            cycle.getInsurerCode(),
            cycle.getProductionMonth(),
            no.substring(no.lastIndexOf('-') + 1),
            LocalDate.now(clock));
    DroppedFile file =
        drop.drop(
            companyId,
            new ExtractFile.Location(MODULE + "/" + cycle.getInsurerCode(), fileName),
            new DropContent(
                ProductionRegisterWorkbook.XLSX,
                workbook.write(cycle.getProductionMonth(), written)),
            new ExtractFile.Origin(MODULE, no));
    extract.stored(fileName, file.id(), file.sha256(), written.size(), added);
  }

  private List<OpsInvoice> booked(ExtractRequest r) {
    LedgerSearch search =
        new LedgerSearch(
            r.companyId(),
            null,
            r.insurerCode(),
            null,
            null,
            null,
            null,
            null,
            null,
            r.from(),
            r.to());
    List<OpsInvoice> out = new ArrayList<>();
    Pageable pageable = PageRequest.of(0, PAGE, Sort.by("id"));
    Page<OpsInvoice> page;
    do {
      page = ledger.searchLoaded(search, pageable);
      out.addAll(page.getContent());
      pageable = page.nextPageable();
    } while (page.hasNext());
    return out;
  }

  /**
   * An extract.
   *
   * @param id extract
   * @return extract
   */
  @Transactional(readOnly = true)
  public ReconExtract require(Long id) {
    return extracts.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Extracts of a cycle, newest first.
   *
   * @param cycleId cycle
   * @return extracts
   */
  @Transactional(readOnly = true)
  public List<ReconExtract> ofCycle(Long cycleId) {
    return extracts.findByCycleIdOrderByIdDesc(cycleId);
  }

  /**
   * The extract register (PRCID.005/034).
   *
   * @param companyId company
   * @param insurer insurer, null for all
   * @param pageable page
   * @return extracts
   */
  @Transactional(readOnly = true)
  public Page<ReconExtract> search(Long companyId, String insurer, Pageable pageable) {
    return extracts.search(companyId, insurer, pageable);
  }

  /**
   * Lines of an extract (PRCID.020).
   *
   * @param extractId extract
   * @param pageable page
   * @return lines
   */
  @Transactional(readOnly = true)
  public Page<ReconExtractLine> lines(Long extractId, Pageable pageable) {
    require(extractId);
    return lines.findByExtractIdOrderByLineNoAsc(extractId, pageable);
  }

  /**
   * An extraction request.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param from first booking date
   * @param to last booking date
   */
  public record ExtractRequest(Long companyId, String insurerCode, LocalDate from, LocalDate to) {}
}
