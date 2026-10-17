package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.service.OtherIncomeItems;
import com.iortatechnxt.brokerverse.cashiering.service.OtherIncomeItems.Item;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The SOAs and fee invoices an OR of other income settles (FRS.CSH.02.02.08), offered on the OR
 * creation record.
 */
@RestController
@RequestMapping("/api/v1/cashiering/other-income-items")
public class OtherIncomeController {

  private final OtherIncomeItems items;

  /**
   * Creates the controller.
   *
   * @param items open SOAs and fee invoices
   */
  public OtherIncomeController(OtherIncomeItems items) {
    this.items = items;
  }

  /**
   * The open items of a party for an OR type.
   *
   * @param companyId company
   * @param orType INCENTIVE or SERVICE_FEE
   * @param party insurer or client code
   * @return items with their outstanding amounts
   */
  @GetMapping
  @PreAuthorize(CashAccess.VIEW)
  public List<Item> open(
      @RequestParam Long companyId, @RequestParam String orType, @RequestParam String party) {
    return items.open(companyId, orType, party);
  }
}
