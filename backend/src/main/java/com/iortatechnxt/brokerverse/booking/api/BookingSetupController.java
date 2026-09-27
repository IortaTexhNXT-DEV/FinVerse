package com.iortatechnxt.brokerverse.booking.api;

import com.iortatechnxt.brokerverse.booking.api.dto.AutoBookRuleDto;
import com.iortatechnxt.brokerverse.booking.api.dto.IncentiveRuleDto;
import com.iortatechnxt.brokerverse.booking.api.dto.ServiceInvoiceTypeDto;
import com.iortatechnxt.brokerverse.booking.service.BookingRuleService;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceTypeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Booking setup: auto-book rules (BRNB.076), incentive rules (BRNB.107) and service invoice types
 * (BRNB.100).
 */
@RestController
@RequestMapping("/api/v1/booking/setup")
public class BookingSetupController {

  private final BookingRuleService rules;
  private final ServiceInvoiceTypeService types;

  /**
   * Creates the controller.
   *
   * @param rules auto-book and incentive rules
   * @param types service invoice types
   */
  public BookingSetupController(BookingRuleService rules, ServiceInvoiceTypeService types) {
    this.rules = rules;
    this.types = types;
  }

  /**
   * Auto-book rules.
   *
   * @param companyId company
   * @return rules
   */
  @GetMapping("/auto-book-rules")
  @PreAuthorize(BookingAccess.SETUP_VIEW)
  public List<AutoBookRuleDto> autoBookRules(@RequestParam Long companyId) {
    return rules.autoBookRules(companyId).stream().map(AutoBookRuleDto::from).toList();
  }

  /**
   * Adds an auto-book rule.
   *
   * @param companyId company
   * @param request rule
   * @return rule
   */
  @PostMapping("/auto-book-rules")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(BookingAccess.SETUP_MAINTAIN)
  public AutoBookRuleDto createAutoBookRule(
      @RequestParam Long companyId, @Valid @RequestBody AutoBookRuleDto request) {
    return AutoBookRuleDto.from(rules.createAutoBookRule(companyId, request.toCriteria()));
  }

  /**
   * Changes an auto-book rule.
   *
   * @param id rule
   * @param request rule
   * @return rule
   */
  @PutMapping("/auto-book-rules/{id}")
  @PreAuthorize(BookingAccess.SETUP_MAINTAIN)
  public AutoBookRuleDto updateAutoBookRule(
      @PathVariable Long id, @Valid @RequestBody AutoBookRuleDto request) {
    return AutoBookRuleDto.from(rules.updateAutoBookRule(id, request.toCriteria()));
  }

  /**
   * Incentive rules.
   *
   * @param companyId company
   * @return rules
   */
  @GetMapping("/incentive-rules")
  @PreAuthorize(BookingAccess.SETUP_VIEW)
  public List<IncentiveRuleDto> incentiveRules(@RequestParam Long companyId) {
    return rules.incentiveRules(companyId).stream().map(IncentiveRuleDto::from).toList();
  }

  /**
   * Adds an incentive rule.
   *
   * @param companyId company
   * @param request rule
   * @return rule
   */
  @PostMapping("/incentive-rules")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(BookingAccess.SETUP_MAINTAIN)
  public IncentiveRuleDto createIncentiveRule(
      @RequestParam Long companyId, @Valid @RequestBody IncentiveRuleDto request) {
    return IncentiveRuleDto.from(rules.createIncentiveRule(companyId, request.toCriteria()));
  }

  /**
   * Changes an incentive rule.
   *
   * @param id rule
   * @param request rule
   * @return rule
   */
  @PutMapping("/incentive-rules/{id}")
  @PreAuthorize(BookingAccess.SETUP_MAINTAIN)
  public IncentiveRuleDto updateIncentiveRule(
      @PathVariable Long id, @Valid @RequestBody IncentiveRuleDto request) {
    return IncentiveRuleDto.from(rules.updateIncentiveRule(id, request.toCriteria()));
  }

  /**
   * Service invoice types.
   *
   * @return types
   */
  @GetMapping("/service-invoice-types")
  @PreAuthorize(BookingAccess.SETUP_VIEW)
  public List<ServiceInvoiceTypeDto> serviceInvoiceTypes() {
    return types.list().stream().map(ServiceInvoiceTypeDto::from).toList();
  }

  /**
   * Adds a service invoice type.
   *
   * @param request type
   * @return type
   */
  @PostMapping("/service-invoice-types")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(BookingAccess.SETUP_MAINTAIN)
  public ServiceInvoiceTypeDto createServiceInvoiceType(
      @Valid @RequestBody ServiceInvoiceTypeDto request) {
    return ServiceInvoiceTypeDto.from(types.create(request.code(), request.toSettings()));
  }

  /**
   * Changes a service invoice type.
   *
   * @param id type
   * @param request type
   * @return type
   */
  @PutMapping("/service-invoice-types/{id}")
  @PreAuthorize(BookingAccess.SETUP_MAINTAIN)
  public ServiceInvoiceTypeDto updateServiceInvoiceType(
      @PathVariable Long id, @Valid @RequestBody ServiceInvoiceTypeDto request) {
    return ServiceInvoiceTypeDto.from(types.update(id, request.toSettings()));
  }
}
