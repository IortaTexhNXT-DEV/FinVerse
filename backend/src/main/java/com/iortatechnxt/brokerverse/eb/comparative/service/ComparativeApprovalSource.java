package com.iortatechnxt.brokerverse.eb.comparative.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbComparative;
import com.iortatechnxt.brokerverse.eb.domain.EbComparativeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * My Approvals items of Employee Benefits (FR-EB-041, 042): comparatives to sign off for the
 * holders of {@code EB_COMPARATIVE_APPROVE} other than their maker, and comparatives above the
 * threshold for the holders of the rule's approver permission other than the maker and the
 * programme's account officer.
 */
@Component
@Transactional(readOnly = true)
public class ComparativeApprovalSource implements PendingApprovalSource {

  /** Inbox module. */
  public static final String MODULE = "EMPLOYEE_BENEFITS";

  private final EbComparativeRepository comparatives;
  private final EbProgrammeRepository programmes;

  /**
   * Creates the source.
   *
   * @param comparatives comparatives
   * @param programmes programmes (account officer)
   */
  public ComparativeApprovalSource(
      EbComparativeRepository comparatives, EbProgrammeRepository programmes) {
    this.comparatives = comparatives;
    this.programmes = programmes;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    List<PendingApproval> items = new ArrayList<>();
    for (EbComparative c :
        comparatives.findByStatusIn(
            Set.of(EbComparative.Status.FOR_APPROVAL, EbComparative.Status.THRESHOLD_APPROVAL))) {
      Optional<EbProgramme> programme = programmes.findById(c.getProgrammeId());
      if (programme.isPresent() && waitsFor(c, programme.get(), viewer)) {
        items.add(item(c, programme.get()));
      }
    }
    return items;
  }

  private static boolean waitsFor(EbComparative c, EbProgramme programme, ApprovalViewer viewer) {
    if (!viewer.mayApproveItemOf(c.maker())) {
      return false;
    }
    if (c.getStatus() == EbComparative.Status.FOR_APPROVAL) {
      return viewer.can(EbCodes.PERMISSION_COMPARATIVE_APPROVE);
    }
    String permission =
        c.getApproverPermission() == null
            ? EbCodes.PERMISSION_THRESHOLD_APPROVE
            : c.getApproverPermission();
    return viewer.can(permission)
        && (viewer.systemView()
            || !CurrentUser.sameUser(viewer.username(), programme.getAccountOfficer()));
  }

  private static PendingApproval item(EbComparative c, EbProgramme programme) {
    boolean threshold = c.getStatus() == EbComparative.Status.THRESHOLD_APPROVAL;
    BigDecimal total =
        c.getLines().stream()
            .map(EbComparative.Line::getLowestPremium)
            .filter(v -> v != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new PendingApproval(
        MODULE,
        threshold ? "Comparative above threshold" : "Comparative to sign off",
        c.getComparativeNo(),
        programme.getName() + (threshold ? " - " + c.getThresholdRules() : ""),
        threshold ? total : null,
        threshold ? "PHP" : null,
        c.maker(),
        c.getSubmittedAt() == null ? c.getCreatedAt() : c.getSubmittedAt(),
        c.getCompanyId(),
        EbCodes.COMPARATIVE_LINK + c.getId());
  }
}
