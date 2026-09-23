package com.iortatechnxt.finverse.support;

import com.iortatechnxt.finverse.organization.domain.Branch;
import com.iortatechnxt.finverse.organization.domain.BranchRepository;
import com.iortatechnxt.finverse.organization.domain.Company;
import com.iortatechnxt.finverse.organization.domain.CompanyRepository;
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
