package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.approval.service.ApprovalViewer;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.finverse.approval.service.PendingApproval;
import com.iortatechnxt.finverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.finverse.tax.domain.IcLineItem;
import com.iortatechnxt.finverse.tax.domain.PartyTaxProfile;
import com.iortatechnxt.finverse.tax.domain.TaxCode;
import com.iortatechnxt.finverse.tax.domain.TaxForm;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Approval inbox source: tax codes, tax forms, party tax profiles and IC schedule mappings pending
 * authorization (checker permission MASTER_AUTHORIZE; makers never see their own changes).
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
    out.addAll(
        records.pending(
            viewer,
            IcLineItem.class,
            i ->
                new RecordFacts(
                    "IC schedule line",
                    i.getSchedule() + "/" + i.getLineCode(),
                    i.getDescription(),
                    i.getCompanyId(),
                    "/tax/ic-mapping")));
    return out;
  }
}
