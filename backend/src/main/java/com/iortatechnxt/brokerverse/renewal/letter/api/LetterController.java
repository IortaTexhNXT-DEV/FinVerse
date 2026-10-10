package com.iortatechnxt.brokerverse.renewal.letter.api;

import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.renewal.domain.RaNotice;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetter;
import com.iortatechnxt.brokerverse.renewal.letter.service.LetterActions;
import com.iortatechnxt.brokerverse.renewal.letter.service.LetterService;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Renewal letters (FR-RN-080-082, 024): Renewal Advices, closing letters and their files. */
@RestController
@RequestMapping("/api/v1/renewal")
public class LetterController {

  private final LetterService letters;
  private final LetterActions actions;
  private final DocumentService documents;

  /**
   * Creates the controller.
   *
   * @param letters letters
   * @param documents documents (letter files)
   * @param actions Generate and Send RA, resend
   */
  public LetterController(LetterService letters, DocumentService documents, LetterActions actions) {
    this.actions = actions;
    this.letters = letters;
    this.documents = documents;
  }

  /**
   * Generates Renewal Advices.
   *
   * @param request selection, notice and late confirmation
   * @return outcome
   */
  @PostMapping("/letters/ra")
  @PreAuthorize("hasAuthority('RNW_RA_GENERATE')")
  public BatchOutcome generateRa(@Valid @RequestBody RaRequest request) {
    return letters.generateRa(
        request.companyId(), request.renewalRefs(), request.notice(), request.confirmLate());
  }

  /**
   * Generates and sends Renewal Advices at once.
   *
   * @param request selection, notice and late confirmation
   * @return outcome
   */
  @PostMapping("/letters/ra-send")
  @PreAuthorize("hasAuthority('RNW_RA_GENERATE') and hasAuthority('RNW_RA_SEND')")
  public BatchOutcome generateAndSendRa(@Valid @RequestBody RaRequest request) {
    return actions.generateAndSendRa(
        request.companyId(), request.renewalRefs(), request.notice(), request.confirmLate());
  }

  /**
   * Sends a letter again.
   *
   * @param companyId company
   * @param letterNo letter
   * @return the refusal, null when sent
   */
  @PostMapping("/letters/{letterNo}/resend")
  @PreAuthorize("hasAnyAuthority('RNW_RA_SEND','RNW_PROCESS')")
  public Resent resend(@RequestParam Long companyId, @PathVariable String letterNo) {
    return new Resent(letterNo, actions.resend(companyId, letterNo));
  }

  /**
   * The outcome of a resend.
   *
   * @param letterNo letter
   * @param refusal refusal, null when sent
   */
  public record Resent(String letterNo, String refusal) {}

  /**
   * Sends the generated Renewal Advices.
   *
   * @param request selection
   * @return outcome
   */
  @PostMapping("/letters/send")
  @PreAuthorize("hasAuthority('RNW_RA_SEND')")
  public BatchOutcome send(@Valid @RequestBody Selection request) {
    return letters.sendRa(request.companyId(), request.renewalRefs());
  }

  /**
   * Generates and sends the closing letter (No Advice Letter or Not for Renewal letter).
   *
   * @param request selection
   * @return outcome
   */
  @PostMapping("/letters/closing")
  @PreAuthorize("hasAuthority('RNW_RA_GENERATE') and hasAuthority('RNW_RA_SEND')")
  public BatchOutcome closing(@Valid @RequestBody Selection request) {
    return letters.closingLetters(request.companyId(), request.renewalRefs());
  }

  /**
   * Generates and sends the closing letter of renewals unrenewed at their effective expiry: the No
   * Advice Letter (Operations) or the Non-Renewal Letter (Marketing AO), never both.
   *
   * @param request selection
   * @return outcome
   */
  @PostMapping("/letters/closing-at-expiry")
  @PreAuthorize("hasAnyAuthority('RNW_RA_SEND', 'RNW_DISPOSE')")
  public BatchOutcome closingAtExpiry(@Valid @RequestBody Selection request) {
    return letters.closingLettersAtExpiry(request.companyId(), request.renewalRefs());
  }

  /**
   * The letters of a renewal.
   *
   * @param companyId company
   * @param ref renewal
   * @return letters, newest first
   */
  @GetMapping("/candidates/{ref}/letters")
  @PreAuthorize("hasAuthority('RNW_VIEW')")
  public List<LetterView> of(@RequestParam Long companyId, @PathVariable String ref) {
    return letters.of(companyId, ref).stream().map(LetterView::of).toList();
  }

  /**
   * A letter as PDF.
   *
   * @param companyId company
   * @param letterNo letter number
   * @return PDF
   */
  @GetMapping("/letters/{letterNo}/file.pdf")
  @PreAuthorize("hasAuthority('RNW_VIEW')")
  public ResponseEntity<byte[]> file(@RequestParam Long companyId, @PathVariable String letterNo) {
    RenewalLetter letter = letters.letter(companyId, letterNo);
    byte[] pdf = documents.download(letter.getAttachmentId()).content();
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(letterNo + ".pdf"))
        .body(pdf);
  }

  /**
   * A selection of renewals.
   *
   * @param companyId company
   * @param renewalRefs renewals
   */
  public record Selection(@NotNull Long companyId, @NotEmpty List<String> renewalRefs) {}

  /**
   * A Renewal Advice generation.
   *
   * @param companyId company
   * @param renewalRefs renewals
   * @param notice FIRST or SECOND
   * @param confirmLate confirm RAs generated late
   */
  public record RaRequest(
      @NotNull Long companyId,
      @NotEmpty List<String> renewalRefs,
      RaNotice notice,
      boolean confirmLate) {}

  /**
   * A letter.
   *
   * @param letterNo number
   * @param type type
   * @param notice RA notice
   * @param status status
   * @param source source
   * @param templateCode template
   * @param recipients recipients
   * @param failure failure or refusal
   * @param lateConfirmedBy user who confirmed a late RA
   * @param generatedAt generation
   * @param sentAt delivery
   * @param attachmentId stored file
   */
  public record LetterView(
      String letterNo,
      String type,
      String notice,
      String status,
      String source,
      String templateCode,
      String recipients,
      String failure,
      String lateConfirmedBy,
      Instant generatedAt,
      Instant sentAt,
      Long attachmentId) {

    static LetterView of(RenewalLetter l) {
      return new LetterView(
          l.getLetterNo(),
          l.getType().name(),
          l.getNotice() == null ? null : l.getNotice().name(),
          l.getStatus().name(),
          l.getSource().name(),
          l.getTemplateCode(),
          l.getRecipients(),
          l.getFailure(),
          l.getMinNoticeConfirmedBy(),
          l.getGeneratedAt(),
          l.getSentAt(),
          l.getAttachmentId());
    }
  }
}
