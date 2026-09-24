package com.iortatechnxt.brokerverse.support;

import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import org.springframework.stereotype.Component;

/** Lookup helpers for the demo data set loaded in tests. */
@Component
public class TestData {

  private final CompanyRepository companies;
  private final BranchRepository branches;

  TestData(CompanyRepository companies, BranchRepository branches) {
    this.companies = companies;
    this.branches = branches;
  }

  public Company company() {
    return companies.findByCode("FVI").orElseThrow();
  }

  public Branch branch(String code) {
    return branches.findByCompanyIdAndCode(company().getId(), code).orElseThrow();
  }
}
