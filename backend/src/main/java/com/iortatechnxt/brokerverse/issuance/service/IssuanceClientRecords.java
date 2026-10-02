package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.crm.service.ClientRecordsProvider;
import com.iortatechnxt.brokerverse.crm.service.RecordDescriptions;
import com.iortatechnxt.brokerverse.issuance.domain.InsuranceAdvice;
import com.iortatechnxt.brokerverse.issuance.domain.InsuranceAdviceRepository;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The Insurance Advices of a client for the client 360 view (BRNB.099/060). */
@Component
public class IssuanceClientRecords implements ClientRecordsProvider {

  private final InsuranceAdviceRepository advices;
  private final LovService lovs;

  /**
   * Creates the provider.
   *
   * @param advices insurance advices
   * @param lovs mortgagee bank names
   */
  public IssuanceClientRecords(InsuranceAdviceRepository advices, LovService lovs) {
    this.advices = advices;
    this.lovs = lovs;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientRecord> recordsOf(Long clientId) {
    return advices.findByClientIdOrderByIdDesc(clientId).stream().map(this::record).toList();
  }

  private ClientRecord record(InsuranceAdvice a) {
    return new ClientRecord(
        "Insurance Advice",
        a.getIaNo(),
        RecordDescriptions.insuranceAdvice(
            a.getArn(), lovs.label("MORTGAGEE_BANK", a.getMortgageeBank())),
        a.getStatus().name(),
        BusinessClock.dateOf(a.getCreatedAt()),
        "/issuance/insurance-advice?ia=" + a.getIaNo());
  }
}
