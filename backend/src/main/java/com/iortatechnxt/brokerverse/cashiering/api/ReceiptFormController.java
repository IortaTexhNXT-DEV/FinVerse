package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.domain.FormText;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptForm;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptFormService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The AR and OR forms set up online (FRS.CSH.02.06.01 to 02.06.03): the versions with their
 * effective dates, a change proposed by one user and approved or rejected by another.
 */
@RestController
@RequestMapping("/api/v1/cashiering/receipt-forms")
public class ReceiptFormController {

  private final ReceiptFormService forms;

  /**
   * Creates the controller.
   *
   * @param forms forms
   */
  public ReceiptFormController(ReceiptFormService forms) {
    this.forms = forms;
  }

  /**
   * The versions of the forms of a company.
   *
   * @param companyId company
   * @return versions
   */
  @GetMapping
  @PreAuthorize(CashAccess.VIEW + " or " + CashAccess.SERIES_AUTHORIZE)
  public List<FormResponse> list(@RequestParam Long companyId) {
    return forms.list(companyId).stream().map(FormResponse::from).toList();
  }

  /**
   * Proposes a new version of a form.
   *
   * @param request form, texts and effective date
   * @return the version waiting for approval
   */
  @PostMapping
  @PreAuthorize(CashAccess.SERIES)
  public FormResponse propose(@Valid @RequestBody FormRequest request) {
    return FormResponse.from(
        forms.propose(
            request.companyId(), request.formKind(), request.text(), request.effectiveFrom()));
  }

  /**
   * Approves a version.
   *
   * @param id version
   * @return the version
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(CashAccess.SERIES_AUTHORIZE)
  public FormResponse approve(@PathVariable Long id) {
    return FormResponse.from(forms.decide(id, true, null));
  }

  /**
   * Rejects a version.
   *
   * @param id version
   * @param body reason
   * @return the version
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(CashAccess.SERIES_AUTHORIZE)
  public FormResponse reject(@PathVariable Long id, @Valid @RequestBody Remarks body) {
    return FormResponse.from(forms.decide(id, false, body.remarks()));
  }

  /**
   * A new version of a form.
   *
   * @param companyId company
   * @param formKind AR or OR
   * @param text header, note and footer lines
   * @param effectiveFrom first print date
   */
  public record FormRequest(
      @NotNull Long companyId,
      @NotNull @Pattern(regexp = "AR|OR") String formKind,
      @NotNull @Valid FormText text,
      @NotNull LocalDate effectiveFrom) {}

  /**
   * The reason of a rejection.
   *
   * @param remarks reason
   */
  public record Remarks(@Size(max = 250) String remarks) {}

  /**
   * A version of a form.
   *
   * @param id id
   * @param formKind AR or OR
   * @param versionNo version
   * @param text texts
   * @param effectiveFrom first print date
   * @param status PENDING_APPROVAL, APPROVED or REJECTED
   * @param createdBy changed by
   * @param createdAt changed at
   * @param decidedBy approved or rejected by
   * @param decidedAt decided at
   * @param decisionRemarks reason of a rejection
   */
  public record FormResponse(
      Long id,
      String formKind,
      int versionNo,
      FormText text,
      LocalDate effectiveFrom,
      String status,
      String createdBy,
      Instant createdAt,
      String decidedBy,
      Instant decidedAt,
      String decisionRemarks) {

    static FormResponse from(ReceiptForm f) {
      return new FormResponse(
          f.getId(),
          f.getFormKind(),
          f.getVersionNo(),
          f.getText(),
          f.getEffectiveFrom(),
          f.getStatus(),
          f.getCreatedBy(),
          f.getCreatedAt(),
          f.getDecidedBy(),
          f.getDecidedAt(),
          f.getDecisionRemarks());
    }
  }
}
