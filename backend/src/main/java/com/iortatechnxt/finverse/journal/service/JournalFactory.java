package com.iortatechnxt.finverse.journal.service;

import com.iortatechnxt.finverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.finverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.finverse.journal.domain.JournalHeader;
import com.iortatechnxt.finverse.journal.domain.JournalLineSpec;
import com.iortatechnxt.finverse.journal.service.JournalLineResolver.HeaderContext;
import com.iortatechnxt.finverse.organization.domain.Branch;
import com.iortatechnxt.finverse.organization.domain.Company;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates journal batches: allocates the batch number ({@code TYPE-BRANCH-YEAR-NNNNNN}), resolves
 * lines and persists the new aggregate. Shared by manual and system journal services.
 */
@Component
public class JournalFactory {

  private final JournalBatchRepository batches;
  private final JournalLineResolver resolver;
  private final OrganizationService organization;
  private final DocumentNumberService numbers;

  /**
   * Creates the factory.
   *
   * @param batches batch repository
   * @param resolver line resolver
   * @param organization organization service
   * @param numbers document numbering
   */
  public JournalFactory(
      JournalBatchRepository batches,
      JournalLineResolver resolver,
      OrganizationService organization,
      DocumentNumberService numbers) {
    this.batches = batches;
    this.resolver = resolver;
    this.organization = organization;
    this.numbers = numbers;
  }

  /**
   * Creates and saves a batch from line requests.
   *
   * @param header header values
   * @param lines line requests
   * @return saved batch
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public JournalBatch create(JournalHeader header, List<JournalLineRequest> lines) {
    return createFromSpecs(header, resolve(header, lines));
  }

  /**
   * Creates and saves a batch from already resolved lines (copy, reversal).
   *
   * @param header header values
   * @param specs resolved lines
   * @return saved batch
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public JournalBatch createFromSpecs(JournalHeader header, List<JournalLineSpec> specs) {
    JournalBatch batch = new JournalBatch(header, allocateNumber(header));
    batch.replaceLines(specs);
    return batches.save(batch);
  }

  /**
   * Resolves line requests against a header (account lookup, currency conversion).
   *
   * @param header header values
   * @param lines line requests
   * @return resolved lines
   */
  public List<JournalLineSpec> resolve(JournalHeader header, List<JournalLineRequest> lines) {
    Company company = organization.getCompany(header.companyId());
    HeaderContext ctx =
        new HeaderContext(
            header.companyId(),
            header.branchId(),
            header.currency(),
            company.getBaseCurrency(),
            header.valueDate());
    return resolver.resolve(ctx, lines);
  }

  private String allocateNumber(JournalHeader header) {
    Branch branch = organization.getBranch(header.branchId());
    String prefix =
        header.journalType().prefix() + "-" + branch.getCode() + "-" + header.valueDate().getYear();
    return numbers.next(prefix);
  }
}
