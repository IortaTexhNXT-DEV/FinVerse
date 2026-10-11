package com.iortatechnxt.brokerverse.eb.cycle.api;

import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.eb.bor.service.BorService;
import com.iortatechnxt.brokerverse.eb.cycle.api.dto.CycleDtos.AccountResponse;
import com.iortatechnxt.brokerverse.eb.cycle.api.dto.CycleDtos.BorChecklistRequest;
import com.iortatechnxt.brokerverse.eb.cycle.api.dto.CycleDtos.BorResponse;
import com.iortatechnxt.brokerverse.eb.cycle.api.dto.CycleDtos.FeedbackResponse;
import com.iortatechnxt.brokerverse.eb.cycle.api.dto.CycleDtos.OpenCycleRequest;
import com.iortatechnxt.brokerverse.eb.cycle.api.dto.CycleDtos.RejectRequest;
import com.iortatechnxt.brokerverse.eb.cycle.service.CycleService;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentQuery;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentQuery.DocumentView;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentSource;
import com.iortatechnxt.brokerverse.eb.domain.EbFeedbackChannel;
import com.iortatechnxt.brokerverse.eb.placement.service.EbPlacementService;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeView;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeViewService;
import com.iortatechnxt.brokerverse.eb.renewal.service.FeedbackService;
import com.iortatechnxt.brokerverse.eb.renewal.service.FeedbackService.FeedbackInput;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Cycles of a programme and their records (FR-EB-021, 023, 030, 031, 046; design 10.1): open a
 * cycle, the requirement steps, client feedback, the document register, the Broker on Record and
 * the accounts created at placement.
 */
@RestController
@RequestMapping("/api/v1/eb")
public class CycleController {

  private static final String VIEW = "hasAuthority('EB_VIEW')";
  private static final String MARKET = "hasAuthority('EB_MARKET')";
  private static final String VALIDATE = "hasAnyAuthority('EB_MARKET', 'EB_PROCESS')";

  private final CycleService cycles;
  private final FeedbackService feedback;
  private final EbDocumentService documents;
  private final EbDocumentQuery documentQuery;
  private final BorService bor;
  private final EbPlacementService placement;
  private final ProgrammeViewService views;

  /**
   * Creates the controller.
   *
   * @param cycles cycles
   * @param feedback client feedback
   * @param documents document register
   * @param documentQuery Documents tab
   * @param bor Broker on Record
   * @param placement accounts of the cycles
   * @param views programme page
   */
  @SuppressWarnings("java:S107") // constructor injection
  public CycleController(
      CycleService cycles,
      FeedbackService feedback,
      EbDocumentService documents,
      EbDocumentQuery documentQuery,
      BorService bor,
      EbPlacementService placement,
      ProgrammeViewService views) {
    this.cycles = cycles;
    this.feedback = feedback;
    this.documents = documents;
    this.documentQuery = documentQuery;
    this.bor = bor;
    this.placement = placement;
    this.views = views;
  }

  /**
   * Opens a cycle.
   *
   * @param id programme
   * @param companyId company
   * @param request business type, policy year, target inception
   * @return the programme page
   */
  @PostMapping("/programmes/{id}/cycles")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MARKET)
  public ProgrammeView open(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody OpenCycleRequest request) {
    cycles.open(
        companyId,
        id,
        new CycleService.OpenCycle(
            request.businessType(), request.policyYear(), request.targetInception()));
    return views.view(companyId, id);
  }

  /**
   * A requirement step of a cycle: {@code start} (new business), {@code stay-with-incumbent} or
   * {@code remarket}.
   *
   * @param cycleId cycle
   * @param step the step
   * @param companyId company
   * @return the programme page
   */
  @PostMapping("/cycles/{cycleId}/{step:start|stay-with-incumbent|remarket}")
  @PreAuthorize(MARKET)
  public ProgrammeView step(
      @PathVariable Long cycleId, @PathVariable String step, @RequestParam Long companyId) {
    var cycle =
        switch (step) {
          case "start" -> cycles.start(companyId, cycleId);
          case "stay-with-incumbent" -> cycles.stayWithIncumbent(companyId, cycleId);
          default -> cycles.remarket(companyId, cycleId);
        };
    return views.view(companyId, cycle.getProgrammeId());
  }

  /**
   * Records client feedback on a cycle.
   *
   * @param cycleId cycle
   * @param companyId company
   * @param channel channel
   * @param receivedOn date received
   * @param text feedback text
   * @param files files
   * @return the feedback
   * @throws IOException when a file cannot be read
   */
  @PostMapping(value = "/cycles/{cycleId}/feedback", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MARKET)
  public FeedbackResponse recordFeedback(
      @PathVariable Long cycleId,
      @RequestParam Long companyId,
      @RequestParam(required = false) EbFeedbackChannel channel,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate receivedOn,
      @RequestParam(required = false) String text,
      @RequestParam(required = false) List<MultipartFile> files)
      throws IOException {
    return FeedbackResponse.from(
        feedback.record(
            companyId, cycleId, new FeedbackInput(channel, receivedOn, text, uploaded(files))));
  }

  /**
   * The client feedback of a programme.
   *
   * @param id programme
   * @param companyId company
   * @return feedback, latest first
   */
  @GetMapping("/programmes/{id}/feedback")
  @PreAuthorize(VIEW)
  public List<FeedbackResponse> feedback(@PathVariable Long id, @RequestParam Long companyId) {
    return feedback.ofProgramme(companyId, id).stream().map(FeedbackResponse::from).toList();
  }

  /**
   * Uploads documents of one type on a cycle (Documents tab).
   *
   * @param cycleId cycle
   * @param companyId company
   * @param documentType document type
   * @param processType process tag
   * @param source source (AO, PROCESSING, CLIENT, INSURER)
   * @param description description
   * @param files files
   * @return the programme's documents
   * @throws IOException when a file cannot be read
   */
  @PostMapping(
      value = "/cycles/{cycleId}/documents",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(VALIDATE)
  @SuppressWarnings("java:S107") // one parameter per form field
  public List<DocumentView> upload(
      @PathVariable Long cycleId,
      @RequestParam Long companyId,
      @RequestParam(required = false) String documentType,
      @RequestParam(required = false) String processType,
      @RequestParam(required = false) EbDocumentSource source,
      @RequestParam(required = false) String description,
      @RequestParam(required = false) List<MultipartFile> files)
      throws IOException {
    var saved =
        documents.upload(
            companyId,
            cycleId,
            new EbDocumentService.Upload(
                documentType, processType, source, description, uploaded(files)));
    return documentQuery.list(companyId, saved.get(0).getProgrammeId());
  }

  /**
   * The documents of a programme the user may see.
   *
   * @param id programme
   * @param companyId company
   * @return documents, latest first
   */
  @GetMapping("/programmes/{id}/documents")
  @PreAuthorize(VIEW)
  public List<DocumentView> documents(@PathVariable Long id, @RequestParam Long companyId) {
    return documentQuery.list(companyId, id);
  }

  /**
   * Uploads the signed BOR of a cycle as a new version.
   *
   * @param cycleId cycle
   * @param companyId company
   * @param file the BOR (PDF or Word)
   * @return the version
   * @throws IOException when the file cannot be read
   */
  @PostMapping(value = "/cycles/{cycleId}/bor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MARKET)
  public BorResponse uploadBor(
      @PathVariable Long cycleId, @RequestParam Long companyId, @RequestParam MultipartFile file)
      throws IOException {
    return BorResponse.from(
        bor.upload(
            companyId, cycleId, new UploadedFile(file.getOriginalFilename(), file.getBytes())));
  }

  /**
   * Validates a BOR version.
   *
   * @param borId version
   * @param companyId company
   * @param request checklist and validity
   * @return the version
   */
  @PostMapping("/bor/{borId}/validate")
  @PreAuthorize(VALIDATE)
  public BorResponse validateBor(
      @PathVariable Long borId,
      @RequestParam Long companyId,
      @RequestBody BorChecklistRequest request) {
    return BorResponse.from(bor.validate(companyId, borId, request.toChecklist()));
  }

  /**
   * Rejects a BOR version.
   *
   * @param borId version
   * @param companyId company
   * @param request reason
   * @return the version
   */
  @PostMapping("/bor/{borId}/reject")
  @PreAuthorize(VALIDATE)
  public BorResponse rejectBor(
      @PathVariable Long borId, @RequestParam Long companyId, @RequestBody RejectRequest request) {
    return BorResponse.from(bor.reject(companyId, borId, request.reason()));
  }

  /**
   * The BOR versions of a programme.
   *
   * @param id programme
   * @param companyId company
   * @return versions, latest first
   */
  @GetMapping("/programmes/{id}/bor")
  @PreAuthorize(VIEW)
  public List<BorResponse> bors(@PathVariable Long id, @RequestParam Long companyId) {
    return bor.ofProgramme(companyId, id).stream().map(BorResponse::from).toList();
  }

  /**
   * The accounts created for the cycles of a programme.
   *
   * @param id programme
   * @param companyId company
   * @return accounts, latest cycle first
   */
  @GetMapping("/programmes/{id}/accounts")
  @PreAuthorize(VIEW)
  public List<AccountResponse> accounts(@PathVariable Long id, @RequestParam Long companyId) {
    return placement.accountsOfProgramme(companyId, id).stream()
        .map(a -> AccountResponse.from(a.account(), a.cycleId(), a.cycleNo()))
        .toList();
  }

  private static List<UploadedFile> uploaded(List<MultipartFile> files) throws IOException {
    List<UploadedFile> result = new ArrayList<>();
    if (files != null) {
      for (MultipartFile f : files) {
        if (!f.isEmpty() || f.getOriginalFilename() != null) {
          result.add(new UploadedFile(f.getOriginalFilename(), f.getBytes()));
        }
      }
    }
    return result;
  }
}
