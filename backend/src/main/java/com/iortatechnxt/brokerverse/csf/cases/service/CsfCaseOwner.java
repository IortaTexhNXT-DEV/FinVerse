package com.iortatechnxt.brokerverse.csf.cases.service;

import com.iortatechnxt.brokerverse.common.security.RecordOwnerLookup;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseStore;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** The company of a contact centre case, for the record scope check of the case endpoints. */
@Service
public class CsfCaseOwner implements RecordOwnerLookup {

  /** Record type of a case. */
  public static final String TYPE = "CsfCase";

  private final CsfCaseStore store;

  /**
   * Creates the lookup.
   *
   * @param store case store
   */
  public CsfCaseOwner(CsfCaseStore store) {
    this.store = store;
  }

  @Override
  public String recordType() {
    return TYPE;
  }

  @Override
  public Optional<Long> companyOf(Long id) {
    return store.find(id).map(CsfCase::companyId);
  }
}
