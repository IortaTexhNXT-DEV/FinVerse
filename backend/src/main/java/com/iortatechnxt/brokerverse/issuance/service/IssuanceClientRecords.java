package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.crm.service.ClientRecordsProvider;
import com.iortatechnxt.brokerverse.issuance.domain.InsuranceAdvice;
import com.iortatechnxt.brokerverse.issuance.domain.InsuranceAdviceRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The Insurance Advices of a client for the client 360 view (BRNB.099/060). */
@Component
public class IssuanceClientRecords implements ClientRecordsProvider {

  private final InsuranceAdviceRepository advices;

  /**
   * Creates the provider.
   *
   * @param advices insurance advices
   */
  public IssuanceClientRecords(InsuranceAdviceRepository advices) {
    this.advices = advices;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientRecord> recordsOf(Long clientId) {
    return advices.findByClientIdOrderByIdDesc(clientId).stream()
        .map(IssuanceClientRecords::record)
        .toList();
  }

  private static ClientRecord record(InsuranceAdvice a) {
    return new ClientRecord(
        "Insurance Advice",
        a.getIaNo(),
        a.getArn() + " - mortgagee " + a.getMortgageeBank(),
        a.getStatus().name(),
        LocalDate.ofInstant(a.getCreatedAt(), ZoneOffset.UTC),
        "/issuance/insurance-advice?ia=" + a.getIaNo());
  }
}
