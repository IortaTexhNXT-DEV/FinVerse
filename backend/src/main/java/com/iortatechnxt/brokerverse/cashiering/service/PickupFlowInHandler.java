package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInContext;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Upload of the Collection feed {@code COLLECTION_CHECK_PICKUP} (CSHID.009, OQ01/OQ13) on the
 * Operations Interfaces screen until the Collection system sends it: a comma-separated file with a
 * header line and the columns {@code company, collectionRef, reference, clientCode, payorName,
 * pickupDate, amount, currency, checkNo, checkBank, requestor}. Each line queues one request,
 * idempotent on the Collection reference.
 */
@Component
public class PickupFlowInHandler implements FlowInHandler {

  private final ObjectProvider<PickupService> pickups;
  private final CompanyRepository companies;
  private final CashieringSettings settings;

  /**
   * Creates the handler.
   *
   * @param pickups pick-up queue (resolved on use: the queue reaches the flow-in service through
   *     the Collection feed port)
   * @param companies companies
   * @param settings settings
   */
  public PickupFlowInHandler(
      ObjectProvider<PickupService> pickups,
      CompanyRepository companies,
      CashieringSettings settings) {
    this.pickups = pickups;
    this.companies = companies;
    this.settings = settings;
  }

  @Override
  public String feedCode() {
    return PickupService.FEED;
  }

  @Override
  public void handle(FlowInFile file, FlowInContext context) {
    List<String> lines =
        new String(file.content(), StandardCharsets.UTF_8)
            .lines()
            .filter(l -> !l.isBlank())
            .toList();
    if (lines.isEmpty()) {
      return;
    }
    String[] headers = lines.get(0).split(",", -1);
    for (String line : lines.subList(1, lines.size())) {
      Map<String, String> fields = new HashMap<>();
      String[] cells = line.split(",", -1);
      for (int i = 0; i < headers.length && i < cells.length; i++) {
        fields.put(headers[i].strip(), cells[i].strip());
      }
      String key =
          fields.getOrDefault("company", "") + ":" + fields.getOrDefault("collectionRef", "");
      context.accept(key, line, () -> queue(fields));
    }
  }

  private String queue(Map<String, String> fields) {
    Long companyId =
        companies
            .findByCode(fields.getOrDefault("company", ""))
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "PICKUP_COMPANY_UNKNOWN", "Unknown company " + fields.get("company")))
            .getId();
    return pickups
        .getObject()
        .create(
            companyId,
            settings.headOffice(companyId).getId(),
            PickupService.details(fields.get("collectionRef"), fields))
        .getCollectionRef();
  }
}
