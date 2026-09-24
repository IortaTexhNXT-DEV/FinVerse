package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.HoldCoverStatus;
import com.iortatechnxt.brokerverse.placement.domain.HoldCover;
import com.iortatechnxt.brokerverse.placement.domain.HoldCoverRepository;
import com.iortatechnxt.brokerverse.placement.domain.PlacementSlip;
import com.iortatechnxt.brokerverse.placement.domain.PlacementSlipRepository;
import com.iortatechnxt.brokerverse.placement.domain.SlipStatus;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Placement reads for the workbench and for other modules (contract): the slips of an account
 * ({@link #slipsFor}), its hold cover ({@link #holdCover}), the workbench lists and the tile
 * counts.
 */
@Service
@Transactional(readOnly = true)
public class PlacementQueryService {

  private static final List<HoldCoverStatus> OPEN_COVERS =
      List.of(HoldCoverStatus.REQUESTED, HoldCoverStatus.CONFIRMED);
  private static final int DEFAULT_ALERT_DAYS = 5;

  private final PlacementSlipRepository slips;
  private final HoldCoverRepository holdCovers;
  private final PlacementAccounts accounts;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param slips placement slips
   * @param holdCovers hold covers
   * @param accounts account look-ups
   * @param parameters business parameters
   * @param clock clock
   */
  public PlacementQueryService(
      PlacementSlipRepository slips,
      HoldCoverRepository holdCovers,
      PlacementAccounts accounts,
      SystemParameterService parameters,
      Clock clock) {
    this.slips = slips;
    this.holdCovers = holdCovers;
    this.accounts = accounts;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * Every slip version covering an account, newest first (contract for issuance and booking).
   *
   * @param arn Account Reference Number
   * @return slips with their accounts
   */
  public List<PlacementSlip> slipsFor(String arn) {
    List<PlacementSlip> found = slips.findByArn(arn);
    found.forEach(PlacementSlip::getAccounts);
    return found;
  }

  /**
   * The current slip of an account: the latest version that is not superseded.
   *
   * @param arn Account Reference Number
   * @return slip
   */
  public Optional<PlacementSlip> currentSlip(String arn) {
    return slipsFor(arn).stream().filter(s -> s.getStatus() != SlipStatus.SUPERSEDED).findFirst();
  }

  /**
   * The latest hold cover of an account (contract for issuance and booking).
   *
   * @param arn Account Reference Number
   * @return hold cover
   */
  public Optional<HoldCover> holdCover(String arn) {
    return holdCovers.findFirstByArnOrderByIdDesc(arn);
  }

  /**
   * Every hold cover of an account, newest first.
   *
   * @param arn Account Reference Number
   * @return hold covers
   */
  public List<HoldCover> holdCovers(String arn) {
    return holdCovers.findByArnOrderByIdDesc(arn);
  }

  /**
   * One page of a workbench tab.
   *
   * @param companyId company
   * @param tab tab
   * @param text ARN, client code or name fragment
   * @param pageable page
   * @return rows with the current slip and hold cover
   */
  public Page<WorkbenchRow> workbench(
      Long companyId, WorkbenchTab tab, String text, Pageable pageable) {
    if (tab == WorkbenchTab.HOLD_COVER_EXPIRING) {
      return holdCovers
          .findByCompanyIdAndStatusInAndExpiryDateLessThanEqualOrderByExpiryDateAsc(
              companyId, OPEN_COVERS, horizon(), pageable)
          .map(h -> row(accounts.require(h.getArn())));
    }
    return accounts.page(companyId, tab.statuses(), text, pageable).map(this::row);
  }

  /**
   * Counts of the workbench tiles.
   *
   * @param companyId company
   * @return counts
   */
  public WorkbenchCounts counts(Long companyId) {
    return new WorkbenchCounts(
        accounts.count(companyId, List.of(AccountStatus.AWAITING_PAYMENT)),
        accounts.count(companyId, List.of(AccountStatus.READY_FOR_PLACEMENT)),
        accounts.count(companyId, List.of(AccountStatus.PLACED)),
        accounts.count(companyId, List.of(AccountStatus.RETURNED_BY_INSURER)),
        holdCovers.countByCompanyIdAndStatusInAndExpiryDateLessThanEqual(
            companyId, OPEN_COVERS, horizon()),
        accounts.count(companyId, List.of(AccountStatus.PLACEMENT_CANCELLED)),
        accounts.count(companyId, List.of(AccountStatus.POLICY_ISSUED)),
        accounts.count(companyId, List.of(AccountStatus.BOOKED)));
  }

  private LocalDate horizon() {
    return LocalDate.now(clock)
        .plusDays(parameters.intValue(HoldCoverService.ALERT_DAYS, DEFAULT_ALERT_DAYS));
  }

  private WorkbenchRow row(Account account) {
    return new WorkbenchRow(
        account,
        currentSlip(account.getArn()).orElse(null),
        holdCover(account.getArn()).orElse(null));
  }

  /**
   * A workbench row.
   *
   * @param account account
   * @param slip current placement slip, may be null
   * @param holdCover latest hold cover, may be null
   */
  public record WorkbenchRow(Account account, PlacementSlip slip, HoldCover holdCover) {}

  /**
   * Workbench tile counts.
   *
   * @param awaitingPayment awaiting payment or confirmation
   * @param readyForPlacement ready for placement
   * @param placed placed, awaiting the policy
   * @param returnedByInsurer returned by the insurer
   * @param holdCoverExpiring open hold covers expiring within the alert lead time
   * @param placementCancelled cancelled placements
   * @param policyIssued policy issued, waiting for booking
   * @param booked booked
   */
  public record WorkbenchCounts(
      long awaitingPayment,
      long readyForPlacement,
      long placed,
      long returnedByInsurer,
      long holdCoverExpiring,
      long placementCancelled,
      long policyIssued,
      long booked) {}
}
