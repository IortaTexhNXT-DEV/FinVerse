package com.iortatechnxt.brokerverse.submitted.intake.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.submitted.domain.SbmDocStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHandlingFee;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHandlingFeeRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLetter;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLetterRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRenewal;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRenewalRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTorRepository;
import com.iortatechnxt.brokerverse.submitted.intake.api.dto.IntakeDtos.HomeCounts;
import com.iortatechnxt.brokerverse.submitted.intake.api.dto.IntakeDtos.IntakeRunView;
import com.iortatechnxt.brokerverse.submitted.intake.service.IntakeRunService;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.MasterlistController;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Upload and Intake runs and the work counts of the module home (FR-SP-001 to 006). The files
 * themselves go through the bulk upload of each source.
 */
@RestController
@RequestMapping("/api/v1/submitted")
@PreAuthorize(MasterlistController.VIEW)
public class IntakeController {

  private static final Set<String> OPTION_PERMISSIONS =
      Set.of("SBM_MAINTAIN", "IAAF_APPROVE", "TOR_APPROVE", "TOR_PREPARE", "IAAF_PREPARE");

  private final IntakeRunService runs;
  private final Counts counts;
  private final UserDirectory users;

  /**
   * The repositories counted on the home.
   *
   * @param iaafs IAAFs
   * @param tors TORs
   * @param renewals hand-offs
   * @param letters letters
   * @param fees handling fees
   */
  public record Counts(
      SbmIaafRepository iaafs,
      SbmTorRepository tors,
      SbmRenewalRepository renewals,
      SbmLetterRepository letters,
      SbmHandlingFeeRepository fees) {}

  /**
   * Creates the controller.
   *
   * @param runs intake runs
   * @param iaafs IAAFs
   * @param tors TORs
   * @param renewals hand-offs
   * @param letters letters
   * @param fees handling fees
   * @param users user directory (handler options)
   */
  public IntakeController(
      IntakeRunService runs,
      SbmIaafRepository iaafs,
      SbmTorRepository tors,
      SbmRenewalRepository renewals,
      SbmLetterRepository letters,
      SbmHandlingFeeRepository fees,
      UserDirectory users) {
    this.runs = runs;
    this.counts = new Counts(iaafs, tors, renewals, letters, fees);
    this.users = users;
  }

  /**
   * The users holding a permission of the module (handler, approver and Account Officer choices).
   *
   * @param permission SBM_MAINTAIN, IAAF_APPROVE, TOR_APPROVE or TOR_PREPARE
   * @return users with their names, by name
   */
  @GetMapping("/users")
  public List<UserOption> users(@RequestParam String permission) {
    if (!OPTION_PERMISSIONS.contains(permission)) {
      return List.of();
    }
    return users.usersWithPermission(permission).stream()
        .map(u -> new UserOption(u, users.displayName(u)))
        .sorted(java.util.Comparator.comparing(UserOption::displayName))
        .toList();
  }

  /**
   * A user choice.
   *
   * @param username login
   * @param displayName name
   */
  public record UserOption(String username, String displayName) {}

  /**
   * Intake runs of a company.
   *
   * @param companyId company
   * @param pageable page
   * @return runs, newest first
   */
  @GetMapping("/intake-runs")
  public PageResponse<IntakeRunView> runs(@RequestParam Long companyId, Pageable pageable) {
    return PageResponse.of(runs.list(companyId, pageable), IntakeRunView::from);
  }

  /**
   * The work counts of the home.
   *
   * @param companyId company
   * @return counts
   */
  @GetMapping("/home")
  public HomeCounts home(@RequestParam Long companyId) {
    return new HomeCounts(
        counts.iaafs().countByCompanyIdAndStatus(companyId, SbmDocStatus.FOR_APPROVAL),
        counts.iaafs().countByCompanyIdAndStatus(companyId, SbmDocStatus.APPROVED),
        counts.tors().countByCompanyIdAndStatus(companyId, SbmDocStatus.FOR_APPROVAL),
        counts.renewals().countByCompanyIdAndHandoffStatus(companyId, SbmRenewal.PENDING),
        counts.letters().countByCompanyIdAndStatus(companyId, SbmLetter.FAILED),
        counts.fees().countByCompanyIdAndStatus(companyId, SbmHandlingFee.BILLED),
        counts.fees().countByCompanyIdAndStatus(companyId, SbmHandlingFee.TAGGED));
  }
}
