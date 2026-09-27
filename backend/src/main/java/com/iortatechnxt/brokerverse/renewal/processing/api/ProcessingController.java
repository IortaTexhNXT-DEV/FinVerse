package com.iortatechnxt.brokerverse.renewal.processing.api;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.renewal.domain.UploadScope;
import com.iortatechnxt.brokerverse.renewal.processing.service.CompleteFileService;
import com.iortatechnxt.brokerverse.renewal.processing.service.ProcessingService;
import com.iortatechnxt.brokerverse.renewal.processing.service.ProcessingService.Computations;
import com.iortatechnxt.brokerverse.renewal.processing.service.ProcessingService.Officer;
import com.iortatechnxt.brokerverse.renewal.processing.service.RenewalAccountService;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
 * Processing (FR-RN-060, 061, 063-065): Processing Officer assignment, return to Marketing, the
 * renewal account, the Computations tab and the complete-file scope of a dispositioned upload.
 */
@RestController
@RequestMapping("/api/v1/renewal")
public class ProcessingController {

  private final ProcessingService processing;
  private final RenewalAccountService accounts;
  private final CompleteFileService completeFile;

  /**
   * Creates the controller.
   *
   * @param processing processing
   * @param accounts renewal accounts
   * @param completeFile complete-file scope
   */
  public ProcessingController(
      ProcessingService processing,
      RenewalAccountService accounts,
      CompleteFileService completeFile) {
    this.processing = processing;
    this.accounts = accounts;
    this.completeFile = completeFile;
  }

  /**
   * The Processing Officers.
   *
   * @return officers
   */
  @GetMapping("/processing/officers")
  @PreAuthorize("hasAuthority('RNW_PROCESS_ASSIGN')")
  public List<Officer> officers() {
    return processing.officers();
  }

  /**
   * Assigns renewals to a Processing Officer (none: to the current user).
   *
   * @param request selection and officer
   * @return outcome
   */
  @PostMapping("/processing/assign")
  @PreAuthorize("hasAuthority('RNW_PROCESS_ASSIGN')")
  public BatchOutcome assign(@Valid @RequestBody AssignRequest request) {
    return processing.assign(request.companyId(), request.renewalRefs(), request.po());
  }

  /**
   * Returns renewals to Marketing.
   *
   * @param request selection, target, reason and remarks
   * @return outcome
   */
  @PostMapping("/candidates/return-to-marketing")
  @PreAuthorize("hasAuthority('RNW_PROCESS')")
  public BatchOutcome returnToMarketing(@Valid @RequestBody ReturnRequest request) {
    return processing.returnToMarketing(
        request.companyId(),
        request.renewalRefs(),
        request.toLeader(),
        request.reasonCode(),
        request.remarks());
  }

  /**
   * The Computations tab.
   *
   * @param companyId company
   * @param ref renewal
   * @return comparison
   */
  @GetMapping("/candidates/{ref}/computations")
  @PreAuthorize("hasAuthority('RNW_VIEW')")
  public Computations computations(@RequestParam Long companyId, @PathVariable String ref) {
    return processing.computations(companyId, ref);
  }

  /**
   * Creates the renewal account when it was not created on reaching processing.
   *
   * @param companyId company
   * @param ref renewal
   * @return the account reference
   */
  @PostMapping("/candidates/{ref}/renewal-account")
  @PreAuthorize("hasAuthority('RNW_PROCESS')")
  public AccountView createAccount(@RequestParam Long companyId, @PathVariable String ref) {
    Account a = accounts.create(companyId, ref);
    return new AccountView(a.getArn(), a.getId());
  }

  /**
   * Applies the complete-file scope of a committed dispositioned upload.
   *
   * @param jobId upload job
   * @param request company, range and unit
   * @return the scope with the number of renewals tagged
   */
  @PostMapping("/uploads/{jobId}/complete-file")
  @PreAuthorize("hasAuthority('RNW_UPLOAD')")
  public ScopeView completeFile(
      @PathVariable Long jobId, @Valid @RequestBody ScopeRequest request) {
    UploadScope scope =
        completeFile.apply(
            request.companyId(),
            jobId,
            new UploadScope.Range(request.expiryFrom(), request.expiryTo(), request.unit()));
    return new ScopeView(scope.getJobNo(), scope.getTaggedCount());
  }

  /**
   * A Processing Officer assignment.
   *
   * @param companyId company
   * @param renewalRefs renewals
   * @param po officer, empty for the current user
   */
  public record AssignRequest(
      @NotNull Long companyId, @NotEmpty List<String> renewalRefs, String po) {}

  /**
   * A return to Marketing.
   *
   * @param companyId company
   * @param renewalRefs renewals
   * @param toLeader to the Team Leader instead of the AO
   * @param reasonCode reason
   * @param remarks remarks
   */
  public record ReturnRequest(
      @NotNull Long companyId,
      @NotEmpty List<String> renewalRefs,
      boolean toLeader,
      String reasonCode,
      String remarks) {}

  /**
   * The complete-file scope.
   *
   * @param companyId company
   * @param expiryFrom first expiry
   * @param expiryTo last expiry
   * @param unit Marketing unit
   */
  public record ScopeRequest(
      @NotNull Long companyId, LocalDate expiryFrom, LocalDate expiryTo, String unit) {}

  /**
   * The renewal account.
   *
   * @param arn ARN
   * @param id account id
   */
  public record AccountView(String arn, Long id) {}

  /**
   * An applied scope.
   *
   * @param jobNo upload job
   * @param tagged renewals tagged Not for Renewal
   */
  public record ScopeView(String jobNo, int tagged) {}
}
