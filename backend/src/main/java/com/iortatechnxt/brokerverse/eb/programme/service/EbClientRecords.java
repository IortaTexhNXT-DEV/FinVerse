package com.iortatechnxt.brokerverse.eb.programme.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.crm.service.ClientRecordsProvider;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Employee Benefits programmes of a client for the client 360 view (BRID-006; design 10.1):
 * programme number, name and benefit lines, status, linked to the programme page.
 */
@Component
public class EbClientRecords implements ClientRecordsProvider {

  private final EbProgrammeRepository programmes;

  /**
   * Creates the provider.
   *
   * @param programmes programmes
   */
  public EbClientRecords(EbProgrammeRepository programmes) {
    this.programmes = programmes;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientRecord> recordsOf(Long clientId) {
    return programmes.findByClientIdOrderByIdDesc(clientId).stream()
        .map(EbClientRecords::record)
        .toList();
  }

  private static ClientRecord record(EbProgramme p) {
    String lines =
        p.getLines().stream()
            .filter(EbProgrammeLine::isActive)
            .map(EbProgrammeLine::getBenefitLine)
            .collect(Collectors.joining(", "));
    return new ClientRecord(
        "EB Programme",
        p.getProgrammeNo(),
        p.getName() + (lines.isEmpty() ? "" : " - " + lines),
        p.getStatus().name(),
        BusinessClock.dateOf(p.getCreatedAt()),
        EbCodes.PROGRAMME_LINK + p.getId());
  }
}
