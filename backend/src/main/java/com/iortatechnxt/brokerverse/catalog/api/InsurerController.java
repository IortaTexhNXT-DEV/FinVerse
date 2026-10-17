package com.iortatechnxt.brokerverse.catalog.api;

import com.iortatechnxt.brokerverse.catalog.api.dto.BranchRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.BranchResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.CommissionRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.CommissionResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.InsurerDetailResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.InsurerPageDtos.BankAccountRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.InsurerPageDtos.BankAccountResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.InsurerPageDtos.ContactRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.InsurerPageDtos.ContactResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.InsurerPageDtos.RemittanceRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.InsurerRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.InsurerResponse;
import com.iortatechnxt.brokerverse.catalog.domain.CommissionRate;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBankAccount;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBranch;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerContact;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.service.InsurerLedgerService;
import com.iortatechnxt.brokerverse.catalog.service.InsurerLedgerService.InsurerAccounts;
import com.iortatechnxt.brokerverse.catalog.service.InsurerPageService;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.RateTableService;
import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import com.iortatechnxt.brokerverse.common.security.OwnedRecord;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Insurer panel: profiles, branches with LGT and commission rates (Appendix A, Q06). */
@OwnedRecord(InsurerProfile.class)
@OwnedRecord(value = InsurerContact.class, variable = "contactId")
@OwnedRecord(value = InsurerBankAccount.class, variable = "accountId")
@OwnedRecord(value = InsurerBranch.class, variable = "branchId")
@OwnedRecord(value = CommissionRate.class, variable = "rateId")
@RestController
@RequestMapping("/api/v1/catalog/insurers")
public class InsurerController {

  private final InsurerService insurers;
  private final RateTableService rates;
  private final InsurerPageService page;
  private final InsurerLedgerService ledger;

  /**
   * Creates the controller.
   *
   * @param insurers insurers
   * @param rates commission rates
   * @param page contacts, bank accounts, remittance details and linked products
   * @param ledger accounts of the insurer
   */
  public InsurerController(
      InsurerService insurers,
      RateTableService rates,
      InsurerPageService page,
      InsurerLedgerService ledger) {
    this.insurers = insurers;
    this.rates = rates;
    this.page = page;
    this.ledger = ledger;
  }

  /**
   * The read-only Accounts view of the insurer page: the general ledger accounts posted for the
   * insurer and its sub-ledger open items (G08). Refused for an insurer of a company outside the
   * reader's data scope.
   *
   * @param id insurer profile
   * @return accounts
   */
  @GetMapping("/{id}/accounts")
  @PreAuthorize(CatalogAccess.READ)
  public InsurerAccounts accounts(@PathVariable Long id) {
    return ledger.accounts(ledger.insurer(id));
  }

  /**
   * Insurers of a company.
   *
   * @param companyId company
   * @param origin BIBS or MIGRATED (Data Migration origin filter), all when absent
   * @return insurers
   */
  @GetMapping
  @PreAuthorize(CatalogAccess.INSURER_LIST)
  public List<InsurerResponse> list(
      @RequestParam Long companyId, @RequestParam(required = false) RecordOrigin.Origin origin) {
    return insurers.insurers(companyId).stream()
        .filter(i -> origin == null || i.getRecordOrigin().origin() == origin)
        .map(InsurerResponse::from)
        .toList();
  }

  /**
   * An insurer with its branches, commission rates, contacts, bank accounts and linked products.
   *
   * @param id profile
   * @return detail
   */
  @GetMapping("/{id}")
  @PreAuthorize(CatalogAccess.READ)
  public InsurerDetailResponse get(@PathVariable Long id) {
    InsurerProfile insurer = insurers.get(id);
    return new InsurerDetailResponse(
        InsurerResponse.from(insurer),
        insurers.branches(id).stream().map(BranchResponse::from).toList(),
        rates.commissions(insurer.getCompanyId(), insurer.getPartyCode()).stream()
            .map(CommissionResponse::from)
            .toList(),
        page.contacts(id).stream().map(ContactResponse::from).toList(),
        page.bankAccounts(id).stream().map(BankAccountResponse::from).toList(),
        page.linkedProducts(id));
  }

  /**
   * Changes the payment terms and remittance details of an insurer (maker; pending authorization).
   *
   * @param id profile
   * @param request terms
   * @return insurer
   */
  @PutMapping("/{id}/remittance")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public InsurerResponse updateRemittance(
      @PathVariable Long id, @Valid @RequestBody RemittanceRequest request) {
    return InsurerResponse.from(page.updateRemittance(id, request.terms()));
  }

  /**
   * Adds a contact person (maker; pending authorization).
   *
   * @param id profile
   * @param request contact
   * @return contact
   */
  @PostMapping("/{id}/contacts")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public ContactResponse addContact(
      @PathVariable Long id, @Valid @RequestBody ContactRequest request) {
    return ContactResponse.from(page.addContact(id, request.details()));
  }

  /**
   * Changes a contact person (maker; pending authorization).
   *
   * @param contactId contact
   * @param request contact
   * @return contact
   */
  @PutMapping("/contacts/{contactId}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public ContactResponse updateContact(
      @PathVariable Long contactId, @Valid @RequestBody ContactRequest request) {
    return ContactResponse.from(page.updateContact(contactId, request.details()));
  }

  /**
   * Adds a bank account (maker; pending authorization).
   *
   * @param id profile
   * @param request account
   * @return account
   */
  @PostMapping("/{id}/bank-accounts")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public BankAccountResponse addBankAccount(
      @PathVariable Long id, @Valid @RequestBody BankAccountRequest request) {
    return BankAccountResponse.from(page.addBankAccount(id, request.details()));
  }

  /**
   * Changes a bank account (maker; pending authorization).
   *
   * @param accountId account
   * @param request account
   * @return account
   */
  @PutMapping("/bank-accounts/{accountId}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public BankAccountResponse updateBankAccount(
      @PathVariable Long accountId, @Valid @RequestBody BankAccountRequest request) {
    return BankAccountResponse.from(page.updateBankAccount(accountId, request.details()));
  }

  /**
   * Adds an insurer (and its party).
   *
   * @param request insurer
   * @return insurer
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public InsurerResponse create(@Valid @RequestBody InsurerRequest request) {
    return InsurerResponse.from(
        insurers.create(
            request.companyId(), request.partyCode(), request.contact(), request.details()));
  }

  /**
   * Changes an insurer profile.
   *
   * @param id profile
   * @param request profile
   * @return insurer
   */
  @PutMapping("/{id}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public InsurerResponse update(@PathVariable Long id, @Valid @RequestBody InsurerRequest request) {
    return InsurerResponse.from(insurers.update(id, request.details()));
  }

  /**
   * Adds a branch.
   *
   * @param id profile
   * @param request branch
   * @return branch
   */
  @PostMapping("/{id}/branches")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public BranchResponse createBranch(
      @PathVariable Long id, @Valid @RequestBody BranchRequest request) {
    return BranchResponse.from(insurers.createBranch(id, request.code(), request.details()));
  }

  /**
   * Changes a branch.
   *
   * @param branchId branch
   * @param request branch
   * @return branch
   */
  @PutMapping("/branches/{branchId}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public BranchResponse updateBranch(
      @PathVariable Long branchId, @Valid @RequestBody BranchRequest request) {
    return BranchResponse.from(insurers.updateBranch(branchId, request.details()));
  }

  /**
   * Adds a commission rate.
   *
   * @param id profile
   * @param request rate
   * @return rate
   */
  @PostMapping("/{id}/commissions")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public CommissionResponse createCommission(
      @PathVariable Long id, @Valid @RequestBody CommissionRequest request) {
    InsurerProfile insurer = insurers.get(id);
    String product =
        request.productCode() == null || request.productCode().isBlank()
            ? null
            : request.productCode();
    return CommissionResponse.from(
        rates.createCommission(
            insurer.getCompanyId(), insurer.getPartyCode(), product, request.validity()));
  }

  /**
   * Changes a commission rate.
   *
   * @param rateId rate
   * @param request rate
   * @return rate
   */
  @PutMapping("/commissions/{rateId}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public CommissionResponse updateCommission(
      @PathVariable Long rateId, @Valid @RequestBody CommissionRequest request) {
    return CommissionResponse.from(rates.updateCommission(rateId, request.validity()));
  }
}
