package com.iortatechnxt.brokerverse.collections.billing.api;

import com.iortatechnxt.brokerverse.collections.billing.api.dto.BillingDtos.GenerateDueRequest;
import com.iortatechnxt.brokerverse.collections.billing.api.dto.BillingDtos.GenerateRequest;
import com.iortatechnxt.brokerverse.collections.billing.api.dto.BillingDtos.RecipientResponse;
import com.iortatechnxt.brokerverse.collections.billing.api.dto.BillingDtos.SendRequest;
import com.iortatechnxt.brokerverse.collections.billing.api.dto.BillingDtos.StatementResponse;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingDocument;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatement.StatementStatus;
import com.iortatechnxt.brokerverse.collections.billing.service.BillingStatementService;
import com.iortatechnxt.brokerverse.collections.billing.service.SoaDispatch;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Statements of account per billing cycle (BRCLXN.058/060): list, detail, the statements of a plan,
 * generation for one cycle or for the cycles due in a period ({@code CLX_BILLING}), the PDF,
 * sending by e-mail and cancellation.
 */
@RestController
@RequestMapping("/api/v1/collections/billing/statements")
public class BillingStatementController {

  private static final int MAX_PAGE = 200;

  private final BillingStatementService statements;
  private final SoaDispatch dispatch;

  /**
   * Creates the controller.
   *
   * @param statements statements
   * @param dispatch e-mail
   */
  public BillingStatementController(BillingStatementService statements, SoaDispatch dispatch) {
    this.statements = statements;
    this.dispatch = dispatch;
  }

  /**
   * Statements by due date, newest first.
   *
   * @param companyId company
   * @param status statuses
   * @param from first due date
   * @param to last due date
   * @param q SOA, account, client or assured
   * @param page page
   * @param size size
   * @return statements
   */
  @GetMapping
  @PreAuthorize("hasAuthority('CLX_VIEW')")
  public PageResponse<StatementResponse> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<StatementStatus> status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        statements.search(
            companyId,
            status,
            from,
            to,
            q,
            PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE),
                Sort.by(Sort.Direction.DESC, "dueDate", "id"))),
        StatementResponse::from);
  }

  /**
   * A statement with its lines.
   *
   * @param id statement
   * @return statement
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('CLX_VIEW')")
  public StatementResponse get(@PathVariable Long id) {
    return StatementResponse.detail(statements.get(id));
  }

  /**
   * Statements of a plan, by cycle.
   *
   * @param planId plan
   * @return statements
   */
  @GetMapping("/by-plan/{planId}")
  @PreAuthorize("hasAuthority('CLX_VIEW')")
  public List<StatementResponse> byPlan(@PathVariable Long planId) {
    return statements.forPlan(planId).stream().map(StatementResponse::from).toList();
  }

  /**
   * The statement of one billing cycle.
   *
   * @param request plan and cycle
   * @return statement
   */
  @PostMapping
  @PreAuthorize("hasAuthority('CLX_BILLING')")
  @ResponseStatus(HttpStatus.CREATED)
  public StatementResponse generate(@Valid @RequestBody GenerateRequest request) {
    return StatementResponse.detail(statements.generate(request.planId(), request.cycleSeq()));
  }

  /**
   * The statements of the cycles falling due in a period and not billed yet.
   *
   * @param request company and period
   * @return statements generated
   */
  @PostMapping("/generate-due")
  @PreAuthorize("hasAuthority('CLX_BILLING')")
  public List<StatementResponse> generateDue(@Valid @RequestBody GenerateDueRequest request) {
    return statements.generateDue(request.companyId(), request.from(), request.to()).stream()
        .map(StatementResponse::from)
        .toList();
  }

  /**
   * The PDF of a statement.
   *
   * @param id statement
   * @return file
   */
  @GetMapping("/{id}/document")
  @PreAuthorize("hasAuthority('CLX_VIEW')")
  public ResponseEntity<byte[]> document(@PathVariable Long id) {
    BillingDocument file = statements.document(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.getContentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(file.getFileName()).build().toString())
        .body(file.getContent());
  }

  /**
   * The client's e-mail address, suggested as the recipient.
   *
   * @param id statement
   * @return address
   */
  @GetMapping("/{id}/recipient")
  @PreAuthorize("hasAuthority('CLX_BILLING')")
  public RecipientResponse recipient(@PathVariable Long id) {
    return new RecipientResponse(dispatch.clientEmail(id).orElse(null));
  }

  /**
   * E-mails a statement (PDF protected with a password sent separately).
   *
   * @param id statement
   * @param request recipients, subject and body
   * @return statement
   */
  @PostMapping("/{id}/send")
  @PreAuthorize("hasAuthority('CLX_BILLING')")
  public StatementResponse send(@PathVariable Long id, @Valid @RequestBody SendRequest request) {
    dispatch.send(id, request.toMail());
    return StatementResponse.detail(statements.get(id));
  }

  /**
   * Cancels a statement so the cycle can be billed again.
   *
   * @param id statement
   * @param request reason
   * @return statement
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('CLX_BILLING')")
  public StatementResponse cancel(
      @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return StatementResponse.detail(statements.cancel(id, request.reason()));
  }
}
