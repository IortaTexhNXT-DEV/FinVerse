package com.iortatechnxt.brokerverse.renewal.dashboard.api;

import com.iortatechnxt.brokerverse.renewal.dashboard.service.DashboardFilter;
import com.iortatechnxt.brokerverse.renewal.dashboard.service.DashboardItem;
import com.iortatechnxt.brokerverse.renewal.dashboard.service.ProcessingDashboardService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Processing dashboard of New Business and Renewal (BDOI Renewal FRS FRRN.003): the KPI cards
 * of the Combined, New Business or Renewal view, the drill-down of a card and the assignment of the
 * Placement Processor.
 */
@RestController
@RequestMapping("/api/v1/renewal/processing-dashboard")
@PreAuthorize(
    "hasAnyAuthority('RNW_PROCESS','RNW_PROCESS_ASSIGN','PLACEMENT_MANAGE','BOOKING_PROCESS','EPOLICY_MANAGE')")
@Validated
public class ProcessingDashboardController {

  private final ProcessingDashboardService processing;

  /**
   * Creates the controller.
   *
   * @param processing processing dashboard
   */
  public ProcessingDashboardController(ProcessingDashboardService processing) {
    this.processing = processing;
  }

  /**
   * The KPI cards.
   *
   * @param companyId company
   * @param businessType NEW_BUSINESS, RENEWAL or ALL
   * @param from period from
   * @param to period to
   * @param segment market segment
   * @param officer Account Officer
   * @return cards
   */
  @GetMapping
  public ProcessingDashboardService.Dashboard dashboard(
      @RequestParam Long companyId,
      @RequestParam(required = false) String businessType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String segment,
      @RequestParam(required = false) String officer) {
    return processing.dashboard(
        new DashboardFilter(companyId, from, to, segment, officer, businessType));
  }

  /**
   * The accounts of a card.
   *
   * @param companyId company
   * @param card card key
   * @param tab ASSIGNED or UNASSIGNED (For Placement)
   * @param filter business type, period, segment and officer
   * @return accounts
   */
  @GetMapping("/drill")
  public List<ProcessingRow> drill(
      @RequestParam Long companyId,
      @RequestParam String card,
      @RequestParam(required = false) String tab,
      ProcessingFilter filter) {
    return processing.drill(filter.of(companyId), card, tab).stream()
        .map(ProcessingRow::of)
        .toList();
  }

  /**
   * Assigns the Placement Processor of accounts.
   *
   * @param companyId company
   * @param body accounts and processor
   * @return number assigned
   */
  @PostMapping("/assign")
  @PreAuthorize("hasAnyAuthority('RNW_PROCESS_ASSIGN','PLACEMENT_MANAGE')")
  public int assign(
      @RequestParam Long companyId, @jakarta.validation.Valid @RequestBody AssignBody body) {
    return processing.assign(companyId, body.arns(), body.processor());
  }

  /**
   * Filters of a drill-down.
   *
   * @param businessType business type
   * @param from period from
   * @param to period to
   * @param segment market segment
   * @param officer Account Officer
   */
  public record ProcessingFilter(
      String businessType,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      String segment,
      String officer) {

    DashboardFilter of(Long companyId) {
      return new DashboardFilter(companyId, from, to, segment, officer, businessType);
    }
  }

  /**
   * Accounts to assign.
   *
   * @param arns account references
   * @param processor user name of the Placement Processor
   */
  public record AssignBody(@NotEmpty List<String> arns, @NotBlank String processor) {}

  /**
   * An account of a processing drill-down (FRRN.003.02.01 to .06).
   *
   * @param arn account reference number
   * @param renewalRef renewal reference (renewal accounts)
   * @param businessType New Business or Renewal
   * @param branch BDOI branch
   * @param department department
   * @param unitHead unit head
   * @param accountOfficer account officer
   * @param assured assured's name
   * @param insurer insurer
   * @param riskCode risk code
   * @param insuranceLine insurance line
   * @param inceptionDate inception date
   * @param expiryDate expiry date
   * @param datePosted submitted for placement
   * @param placementDate placement sent to the insurer
   * @param bookingDate booking date
   * @param processor Placement Processor
   * @param sumInsured sum insured
   * @param premium basic premium
   * @param commission commission
   * @param policyNumber policy number
   * @param segment market segment
   * @param policyStatus policy status
   * @param ageing business days from submitted for placement
   * @param placementAgeing business days from the placement sent
   * @param tatStatus Within TAT or Beyond TAT
   * @param placementIssue placement issue
   * @param resolutionDate resolution date of the issue
   * @param policyReceivedDate policy received
   * @param transmittalStatus Sent, Pending or Unsuccessful
   * @param transmittalDate policy transmittal
   * @param deliveryAgeing days from received to transmitted
   * @param reasonForReject reason of an unsuccessful sending
   * @param returnedDate returned to Marketing
   * @param returnReason reason for return
   */
  public record ProcessingRow(
      String arn,
      String renewalRef,
      String businessType,
      String branch,
      String department,
      String unitHead,
      String accountOfficer,
      String assured,
      String insurer,
      String riskCode,
      String insuranceLine,
      LocalDate inceptionDate,
      LocalDate expiryDate,
      LocalDate datePosted,
      LocalDate placementDate,
      LocalDate bookingDate,
      String processor,
      BigDecimal sumInsured,
      BigDecimal premium,
      BigDecimal commission,
      String policyNumber,
      String segment,
      String policyStatus,
      Object ageing,
      Object placementAgeing,
      String tatStatus,
      String placementIssue,
      LocalDate resolutionDate,
      LocalDate policyReceivedDate,
      String transmittalStatus,
      LocalDate transmittalDate,
      Object deliveryAgeing,
      String reasonForReject,
      LocalDate returnedDate,
      String returnReason) {

    public static ProcessingRow of(DashboardItem i) {
      LocalDate transmitted = i.date("transmitted_at");
      return new ProcessingRow(
          i.text("arn"),
          i.text("renewal_ref"),
          i.renewal() ? "Renewal" : "New Business",
          i.text("branch_code"),
          i.text("department"),
          i.text("unit_head"),
          i.text("account_officer"),
          i.text("assured"),
          name(i, "insurer_name", "insurer_code"),
          i.text("product_code"),
          name(i, "line_name", "line_code"),
          i.date("inception_date"),
          i.date("expiry_date"),
          i.date("submitted_at"),
          i.date("placed_at"),
          i.date("booked_at"),
          i.text("processor"),
          i.amount("sum_insured"),
          i.amount("premium"),
          i.amount("commission"),
          i.text("policy_numbers"),
          name(i, "segment_name", "segment"),
          i.text("policy_status_name"),
          i.get("ageing"),
          i.get("placement_ageing"),
          i.text("tat_status"),
          i.text("placement_issue"),
          i.date("resolution_date"),
          i.date("policy_received_at"),
          transmittal(i.text("transmittal")),
          transmitted == null ? i.date("dispatched_at") : transmitted,
          i.get("delivery_ageing"),
          i.text("transmittal_reason"),
          i.date("returned_at"),
          i.text("return_reason_name") == null
              ? i.text("return_reason")
              : i.text("return_reason_name"));
    }

    private static String name(DashboardItem i, String name, String code) {
      return i.text(name) == null ? i.text(code) : i.text(name);
    }

    private static String transmittal(String code) {
      if (code == null) {
        return null;
      }
      return switch (code) {
        case "SENT" -> "Sent";
        case "UNSUCCESSFUL" -> "Unsuccessful";
        default -> "Pending";
      };
    }
  }
}
