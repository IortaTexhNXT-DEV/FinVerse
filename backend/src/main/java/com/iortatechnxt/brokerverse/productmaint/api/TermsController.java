package com.iortatechnxt.brokerverse.productmaint.api;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.productmaint.domain.ClientResponse;
import com.iortatechnxt.brokerverse.productmaint.domain.FinalTermChange;
import com.iortatechnxt.brokerverse.productmaint.domain.ProposalFile;
import com.iortatechnxt.brokerverse.productmaint.domain.TermsRecord;
import com.iortatechnxt.brokerverse.productmaint.service.ClientResponseService;
import com.iortatechnxt.brokerverse.productmaint.service.TermsAnswers;
import com.iortatechnxt.brokerverse.productmaint.service.TermsField;
import com.iortatechnxt.brokerverse.productmaint.service.TermsProposalService;
import com.iortatechnxt.brokerverse.productmaint.service.TermsTableService;
import com.iortatechnxt.brokerverse.productmaint.service.TermsTableService.OptionInput;
import com.iortatechnxt.brokerverse.productmaint.service.TermsTableService.TermsTable;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The comparative table of a quotation request ({@code quotation}) or a package request ({@code
 * package}) with the Final Terms for Proposal, the insurer selection, the proposal slips per
 * insurer and the client response (BDOI FRS FRPM.006.02, FRPM.008.01, FRPM.009.01, FRPM.009.02,
 * FRPM.010.01, FRPM.012.02, FRPM.013.01).
 */
@RestController
@RequestMapping("/api/v1/product-maintenance/terms/{type}/{id}")
public class TermsController {

  private static final String VIEW =
      "hasAnyAuthority('PROPOSAL_REQUEST', 'PROPOSAL_APPROVE', 'TSU_PROCESS', 'TSU_APPROVE',"
          + " 'PRODUCT_VIEW', 'PKG_REPORT_VIEW')";
  private static final String KEY_IN = "hasAnyAuthority('TSU_PROCESS', 'PKG_NEGOTIATE')";
  private static final String SELECT =
      "hasAnyAuthority('PROPOSAL_REQUEST', 'TSU_PROCESS', 'PKG_REQUEST', 'PKG_NEGOTIATE')";

  private final TermsTableService tables;
  private final TermsProposalService proposals;
  private final ClientResponseService clientResponses;
  private final UserDirectory users;
  private final CurrentUser currentUser;

  /**
   * Creates the controller.
   *
   * @param tables comparative tables
   * @param proposals selection and proposal slips
   * @param clientResponses client responses
   * @param users user names
   * @param currentUser current user (permission of the record type)
   */
  public TermsController(
      TermsTableService tables,
      TermsProposalService proposals,
      ClientResponseService clientResponses,
      UserDirectory users,
      CurrentUser currentUser) {
    this.tables = tables;
    this.proposals = proposals;
    this.clientResponses = clientResponses;
    this.users = users;
    this.currentUser = currentUser;
  }

  /**
   * The comparative table.
   *
   * @param type quotation or package
   * @param id record
   * @return table
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public TermsView table(@PathVariable String type, @PathVariable Long id) {
    return view(tables.table(record(type, id)));
  }

  /**
   * Chooses the fields shown and sent to the client.
   *
   * @param type quotation or package
   * @param id record
   * @param body fields
   * @return table
   */
  @PutMapping("/fields")
  @PreAuthorize(KEY_IN)
  public TermsView fields(
      @PathVariable String type, @PathVariable Long id, @RequestBody FieldsBody body) {
    TermsRecord record = keyIn(type, id);
    return view(tables.fields(record, body.shown(), body.client()));
  }

  /**
   * Saves a quotation option of an insurer with its response.
   *
   * @param type quotation or package
   * @param id record
   * @param insurer insurer code
   * @param body option
   * @return table
   */
  @PutMapping("/options/{insurer}")
  @PreAuthorize(KEY_IN)
  public TermsView saveOption(
      @PathVariable String type,
      @PathVariable Long id,
      @PathVariable String insurer,
      @Valid @RequestBody OptionBody body) {
    TermsRecord record = keyIn(type, id);
    return view(
        tables.saveOption(
            record,
            insurer,
            body.optionNo(),
            new OptionInput(body.answer(), body.otherAnswer(), body.values())));
  }

  /**
   * Removes a quotation option.
   *
   * @param type quotation or package
   * @param id record
   * @param insurer insurer code
   * @param optionNo option
   * @return table
   */
  @DeleteMapping("/options/{insurer}/{optionNo}")
  @PreAuthorize(KEY_IN)
  public TermsView removeOption(
      @PathVariable String type,
      @PathVariable Long id,
      @PathVariable String insurer,
      @PathVariable int optionNo) {
    return view(tables.removeOption(keyIn(type, id), insurer, optionNo));
  }

  /**
   * Changes the Final Terms for Proposal.
   *
   * @param type quotation or package
   * @param id record
   * @param values new value by field
   * @return table
   */
  @PutMapping("/final-terms")
  @PreAuthorize(SELECT)
  public TermsView saveFinalTerms(
      @PathVariable String type, @PathVariable Long id, @RequestBody Map<String, String> values) {
    return view(tables.saveFinalTerms(select(type, id), values));
  }

  /**
   * The history of the Final Terms for Proposal.
   *
   * @param type quotation or package
   * @param id record
   * @return changes, newest first
   */
  @GetMapping("/final-terms/history")
  @PreAuthorize(VIEW)
  public List<ChangeView> history(@PathVariable String type, @PathVariable Long id) {
    return tables.history(record(type, id)).stream().map(this::change).toList();
  }

  /**
   * Selects the insurers and proceeds to the proposal.
   *
   * @param type quotation or package
   * @param id record
   * @param body insurers and comment
   * @return table
   */
  @PostMapping("/proceed")
  @PreAuthorize(SELECT)
  public TermsView proceed(
      @PathVariable String type, @PathVariable Long id, @RequestBody ProceedBody body) {
    return view(proposals.proceed(select(type, id), body.insurers(), body.comment()));
  }

  /**
   * Generates the proposal slips of the selected insurers.
   *
   * @param type quotation or package
   * @param id record
   * @param body comment
   * @return files generated
   */
  @PostMapping("/proposals")
  @PreAuthorize(KEY_IN)
  public List<FileView> generate(
      @PathVariable String type, @PathVariable Long id, @RequestBody CommentOnly body) {
    return proposals.generate(keyIn(type, id), body.comment()).stream().map(this::file).toList();
  }

  /**
   * The proposal slips generated.
   *
   * @param type quotation or package
   * @param id record
   * @return files, newest first
   */
  @GetMapping("/proposals")
  @PreAuthorize(VIEW)
  public List<FileView> files(@PathVariable String type, @PathVariable Long id) {
    return proposals.files(record(type, id)).stream().map(this::file).toList();
  }

  /**
   * The comparative table with the fields chosen for the client.
   *
   * @param type quotation or package
   * @param id record
   * @param format xlsx or pdf
   * @return file
   */
  @GetMapping("/export")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> export(
      @PathVariable String type,
      @PathVariable Long id,
      @RequestParam(defaultValue = "xlsx") String format) {
    return PackageRequestController.file(
        proposals.comparative(record(type, id), !"pdf".equals(format)));
  }

  /**
   * The client responses of a quotation request.
   *
   * @param type quotation
   * @param id record
   * @return responses, newest first
   */
  @GetMapping("/client-responses")
  @PreAuthorize(VIEW)
  public List<ResponseView> clientResponses(@PathVariable String type, @PathVariable Long id) {
    return clientResponses.responses(quotation(type, id).id()).stream()
        .map(this::response)
        .toList();
  }

  /**
   * Records the client response to the proposal of a quotation request.
   *
   * @param type quotation
   * @param id record
   * @param body response
   * @return the response
   */
  @PostMapping("/client-responses")
  @PreAuthorize("hasAuthority('PROPOSAL_REQUEST')")
  public ResponseView recordResponse(
      @PathVariable String type, @PathVariable Long id, @Valid @RequestBody ResponseBody body) {
    return response(
        clientResponses.record(
            quotation(type, id).id(),
            body.response(),
            new ClientResponse.Details(body.responseDate(), body.remarks(), body.insurers())));
  }

  private static TermsRecord record(String type, Long id) {
    return new TermsRecord(
        "package".equals(type) ? TermsRecord.PACKAGE : TermsRecord.QUOTATION, id);
  }

  private static TermsRecord quotation(String type, Long id) {
    TermsRecord record = record(type, id);
    if (!record.quotation()) {
      throw new AccessDeniedException("The client response belongs to quotation requests");
    }
    return record;
  }

  private TermsRecord keyIn(String type, Long id) {
    TermsRecord record = record(type, id);
    require(record.quotation() ? "TSU_PROCESS" : "PKG_NEGOTIATE");
    return record;
  }

  private TermsRecord select(String type, Long id) {
    TermsRecord record = record(type, id);
    if (record.quotation()) {
      require(currentUser.hasAuthority("TSU_PROCESS") ? "TSU_PROCESS" : "PROPOSAL_REQUEST");
    } else {
      require(currentUser.hasAuthority("PKG_NEGOTIATE") ? "PKG_NEGOTIATE" : "PKG_REQUEST");
    }
    return record;
  }

  private void require(String permission) {
    if (!currentUser.hasAuthority(permission)) {
      throw new AccessDeniedException("Not allowed for this request");
    }
  }

  private TermsView view(TermsTable table) {
    Map<String, String> labels = new LinkedHashMap<>();
    Arrays.stream(TermsField.values()).forEach(f -> labels.put(f.name(), f.label()));
    Map<String, String> answers = new LinkedHashMap<>();
    TermsTableService.ANSWERS.stream()
        .sorted()
        .forEach(a -> answers.put(a, TermsAnswers.label(a, null)));
    return new TermsView(table, labels, answers);
  }

  private ChangeView change(FinalTermChange c) {
    return new ChangeView(
        c.getId(),
        TermsField.known(c.getFieldKey())
            ? TermsField.valueOf(c.getFieldKey()).label()
            : c.getFieldKey(),
        c.getOldValue(),
        c.getNewValue(),
        users.displayName(c.getChangedBy()),
        c.getChangedAt());
  }

  private FileView file(ProposalFile f) {
    return new FileView(
        f.getId(),
        f.getInsurerCode(),
        f.getInsurerName(),
        f.getVersionNo(),
        f.getFileName(),
        f.getAttachmentId(),
        users.displayName(f.getGeneratedBy()),
        f.getGeneratedAt());
  }

  private ResponseView response(ClientResponse r) {
    return new ResponseView(
        r.getId(),
        r.getResponse(),
        ClientResponseService.name(r.getResponse()),
        r.getResponseDate(),
        r.getRemarks(),
        r.getInsurers(),
        users.displayName(r.getRecordedBy()),
        r.getRecordedAt());
  }

  /**
   * The table with the field and response names.
   *
   * @param table table
   * @param labels field names
   * @param answers response names
   */
  public record TermsView(
      TermsTable table, Map<String, String> labels, Map<String, String> answers) {}

  /**
   * Fields chosen.
   *
   * @param shown fields shown
   * @param client fields sent to the client
   */
  public record FieldsBody(List<String> shown, List<String> client) {}

  /**
   * A quotation option.
   *
   * @param optionNo option, null for a new one
   * @param answer APPROVED, NOT_COVERED or OTHERS
   * @param otherAnswer the insurer's own wording (Others)
   * @param values value by field
   */
  public record OptionBody(
      Integer optionNo,
      @NotBlank String answer,
      @Size(max = 500) String otherAnswer,
      Map<String, String> values) {}

  /**
   * Insurer selection.
   *
   * @param insurers insurer codes
   * @param comment comment
   */
  public record ProceedBody(List<String> insurers, @Size(max = 1000) String comment) {}

  /**
   * A comment.
   *
   * @param comment comment
   */
  public record CommentOnly(@Size(max = 1000) String comment) {}

  /**
   * A client response.
   *
   * @param response ACCEPTED, REJECTED or RETURNED
   * @param responseDate response date
   * @param remarks client remarks
   * @param insurers insurers accepted
   */
  public record ResponseBody(
      @NotBlank String response,
      LocalDate responseDate,
      @Size(max = 1000) String remarks,
      @Size(max = 500) String insurers) {}

  /**
   * A change of the Final Terms.
   *
   * @param id change
   * @param field field name
   * @param oldValue old value
   * @param newValue new value
   * @param changedBy name of the user
   * @param changedAt time
   */
  public record ChangeView(
      Long id,
      String field,
      String oldValue,
      String newValue,
      String changedBy,
      Instant changedAt) {}

  /**
   * A proposal slip.
   *
   * @param id file
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param versionNo version
   * @param fileName file name
   * @param attachmentId stored document
   * @param generatedBy name of the user
   * @param generatedAt time
   */
  public record FileView(
      Long id,
      String insurerCode,
      String insurerName,
      int versionNo,
      String fileName,
      Long attachmentId,
      String generatedBy,
      Instant generatedAt) {}

  /**
   * A client response.
   *
   * @param id response
   * @param response code
   * @param responseName name
   * @param responseDate response date
   * @param remarks client remarks
   * @param insurers insurers accepted
   * @param recordedBy name of the user
   * @param recordedAt time
   */
  public record ResponseView(
      Long id,
      String response,
      String responseName,
      LocalDate responseDate,
      String remarks,
      String insurers,
      String recordedBy,
      Instant recordedAt) {}
}
