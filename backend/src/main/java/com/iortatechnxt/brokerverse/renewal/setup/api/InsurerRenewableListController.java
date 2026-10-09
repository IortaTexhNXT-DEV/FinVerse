package com.iortatechnxt.brokerverse.renewal.setup.api;

import com.iortatechnxt.brokerverse.renewal.domain.InsurerRenewableRisk;
import com.iortatechnxt.brokerverse.renewal.setup.api.RenewalSetupController.RecordAction;
import com.iortatechnxt.brokerverse.renewal.setup.api.dto.SetupDtos.Approval;
import com.iortatechnxt.brokerverse.renewal.setup.service.InsurerRenewableListService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The insurer renewable lists of Renewal Setup (Annex BRRN.020 SC-10; FR-RN-020, 112): the risk
 * codes each insurer renews, maker-checker.
 */
@RestController
@RequestMapping("/api/v1/renewal/setup/insurer-renewable")
public class InsurerRenewableListController {

  private static final String VIEW = "hasAnyAuthority('RNW_SETUP','RNW_VIEW')";
  private static final String SETUP = "hasAuthority('RNW_SETUP')";

  private final InsurerRenewableListService lists;

  /**
   * Creates the controller.
   *
   * @param lists insurer renewable lists
   */
  public InsurerRenewableListController(InsurerRenewableListService lists) {
    this.lists = lists;
  }

  /**
   * The rows of a company.
   *
   * @param companyId company
   * @return rows
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public List<RenewableRiskView> list(@RequestParam Long companyId) {
    return lists.list(companyId).stream().map(RenewableRiskView::of).toList();
  }

  /**
   * Adds a risk code to an insurer's list.
   *
   * @param companyId company
   * @param data row
   * @return row
   */
  @PostMapping
  @PreAuthorize(SETUP)
  public RenewableRiskView create(
      @RequestParam Long companyId, @RequestBody InsurerRenewableRisk.Data data) {
    return RenewableRiskView.of(lists.create(companyId, data));
  }

  /**
   * Changes the remarks or dates of a row.
   *
   * @param companyId company
   * @param id row
   * @param data new values
   * @return row
   */
  @PutMapping("/{id}")
  @PreAuthorize(SETUP)
  public RenewableRiskView update(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @RequestBody InsurerRenewableRisk.Data data) {
    return RenewableRiskView.of(lists.update(companyId, id, data));
  }

  /**
   * Authorizes or deactivates a row.
   *
   * @param companyId company
   * @param id row
   * @param action AUTHORIZE or DEACTIVATE
   * @return row
   */
  @PostMapping("/{id}/{action}")
  @PreAuthorize(SETUP)
  public RenewableRiskView action(
      @RequestParam Long companyId, @PathVariable Long id, @PathVariable RecordAction action) {
    return RenewableRiskView.of(
        action == RecordAction.AUTHORIZE
            ? lists.authorize(companyId, id)
            : lists.deactivate(companyId, id));
  }

  /**
   * A row of an insurer renewable list.
   *
   * @param id id
   * @param insurerCode insurer
   * @param riskCode risk code renewed
   * @param remarks remarks
   * @param effectiveFrom first day
   * @param effectiveTo last day
   * @param approval maker-checker state
   */
  public record RenewableRiskView(
      Long id,
      String insurerCode,
      String riskCode,
      String remarks,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      Approval approval) {

    /**
     * Maps a row.
     *
     * @param r row
     * @return view
     */
    public static RenewableRiskView of(InsurerRenewableRisk r) {
      return new RenewableRiskView(
          r.getId(),
          r.getInsurerCode(),
          r.getRiskCode(),
          r.getRemarks(),
          r.getEffectiveFrom(),
          r.getEffectiveTo(),
          new Approval(
              r.getRecordStatus().name(), r.getMaker(), r.getAuthorizedBy(), r.getAuthorizedAt()));
    }
  }
}
