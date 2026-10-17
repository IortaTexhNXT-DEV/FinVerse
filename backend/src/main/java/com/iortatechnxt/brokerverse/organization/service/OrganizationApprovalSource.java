package com.iortatechnxt.brokerverse.organization.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/** Approval inbox source: companies and branches pending authorization. */
@Component
public class OrganizationApprovalSource implements PendingApprovalSource {

  private final MasterRecordApprovals records;

  /**
   * Creates the source.
   *
   * @param records master record helper
   */
  public OrganizationApprovalSource(MasterRecordApprovals records) {
    this.records = records;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    return Stream.concat(
            records
                .pending(
                    viewer,
                    Company.class,
                    c ->
                        new RecordFacts(
                            "Company", c.getCode(), c.getName(), c.getId(), "/setup/companies"))
                .stream(),
            records
                .pending(
                    viewer,
                    Branch.class,
                    b ->
                        new RecordFacts(
                            "Branch",
                            b.getCode(),
                            b.getName(),
                            b.getCompany().getId(),
                            "/setup/branches"))
                .stream())
        .toList();
  }
}
