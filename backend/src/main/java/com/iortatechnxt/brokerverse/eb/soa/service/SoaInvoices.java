package com.iortatechnxt.brokerverse.eb.soa.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The booked invoices of a programme, read from the invoice ledger (FR-EB-053 R2: EB keeps no copy
 * of the payment status): the invoices of the lines' current accounts and of the accounts placed by
 * its cycles.
 */
@Component
@Transactional(readOnly = true)
public class SoaInvoices {

  private final OpsInvoiceRepository invoices;
  private final EbCycleRepository cycles;

  /**
   * Creates the reader.
   *
   * @param invoices invoice ledger
   * @param cycles cycles (placed accounts)
   */
  public SoaInvoices(OpsInvoiceRepository invoices, EbCycleRepository cycles) {
    this.invoices = invoices;
    this.cycles = cycles;
  }

  /**
   * The invoices of a programme.
   *
   * @param programme programme
   * @return invoices, latest first
   */
  public List<OpsInvoice> ofProgramme(EbProgramme programme) {
    Set<String> arns = new LinkedHashSet<>();
    programme.getLines().stream()
        .map(EbProgrammeLine::getCurrentArn)
        .filter(a -> a != null && !a.isBlank())
        .forEach(arns::add);
    for (EbCycle cycle : cycles.findByProgrammeIdOrderByPolicyYearDescIdDesc(programme.getId())) {
      arns.addAll(cycle.getAccountArns());
    }
    List<OpsInvoice> result = new ArrayList<>();
    arns.forEach(arn -> result.addAll(invoices.findByArnOrderByPolicyYearAscIdAsc(arn)));
    result.sort(Comparator.comparing(OpsInvoice::getId).reversed());
    return result;
  }

  /**
   * Refuses invoices that are not the programme's.
   *
   * @param programme programme
   * @param invoiceNos invoices chosen, may be null
   * @return the distinct invoices
   */
  public List<String> requireOfProgramme(EbProgramme programme, List<String> invoiceNos) {
    if (invoiceNos == null || invoiceNos.isEmpty()) {
      return List.of();
    }
    Set<String> own =
        ofProgramme(programme).stream().map(OpsInvoice::getInvoiceNo).collect(Collectors.toSet());
    List<String> chosen = invoiceNos.stream().filter(n -> n != null && !n.isBlank()).map(String::strip).distinct().toList();
    chosen.stream()
        .filter(n -> !own.contains(n))
        .findFirst()
        .ifPresent(
            n -> {
              throw new BusinessRuleException(
                  "EB_SOA_INVOICE", "Invoice " + n + " is not a booked invoice of the programme");
            });
    return chosen;
  }

  /**
   * The payment status of invoices, as the ledger shows it.
   *
   * @param invoiceNos invoices
   * @return status by invoice (UNKNOWN when not in the ledger)
   */
  public Map<String, String> paymentStatus(List<String> invoiceNos) {
    return invoiceNos.stream()
        .collect(
            Collectors.toMap(
                n -> n,
                n -> status(invoices.findByInvoiceNo(n)),
                (a, b) -> a,
                java.util.LinkedHashMap::new));
  }

  private static String status(Optional<OpsInvoice> invoice) {
    return invoice.map(i -> i.getPaymentStatus().name()).orElse("UNKNOWN");
  }
}
