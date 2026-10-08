package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.tax.domain.PartyTaxProfile;
import com.iortatechnxt.brokerverse.tax.domain.TaxCode;
import com.iortatechnxt.brokerverse.tax.domain.TaxForm;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Approval inbox source: tax codes, tax forms and party tax profiles pending authorization (checker
 * permission MASTER_AUTHORIZE; makers never see their own changes).
 */
@Component
public class TaxApprovalSource implements PendingApprovalSource {

  private static final String MASTERS_LINK = "/tax/codes";

  private final MasterRecordApprovals records;

  /**
   * Creates the source.
   *
   * @param records master record helper
   */
  public TaxApprovalSource(MasterRecordApprovals records) {
    this.records = records;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    List<PendingApproval> out = new ArrayList<>();
    out.addAll(
        records.pending(
            viewer,
            TaxCode.class,
            c ->
                new RecordFacts(
                    "Tax code", c.getCode(), c.getName(), c.getCompanyId(), MASTERS_LINK)));
    out.addAll(
        records.pending(
            viewer,
            TaxForm.class,
            f ->
                new RecordFacts(
                    "Tax form", f.getCode(), f.getName(), f.getCompanyId(), MASTERS_LINK)));
    out.addAll(
        records.pending(
            viewer,
            PartyTaxProfile.class,
            p ->
                new RecordFacts(
                    "Party tax profile",
                    p.getPartyCode(),
                    p.getRegisteredName(),
                    p.getCompanyId(),
                    "/tax/profiles")));
    return out;
  }
}
