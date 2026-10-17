package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBankAccount;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBankAccount.BankAccountDetails;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBankAccountRepository;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerContact;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerContact.ContactDetails;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerContactRepository;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile.RemittanceTerms;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The full insurer page (G08) beyond the profile, branches and commissions of {@link
 * InsurerService}: payment terms and remittance details, contact persons, bank accounts and the
 * products the insurer is linked to. Every change is maker-checker: the record turns pending and is
 * authorized through {@link CatalogRecords} by another user, and the change log keeps the fields
 * changed.
 */
@Service
@Transactional
public class InsurerPageService {

  static final String PACKAGE_PRODUCTS =
      "select p.code, p.name, pi.role, v.version_no, v.status"
          + " from cat_package_insurer pi"
          + " join cat_product_version v on v.id = pi.version_id"
          + " join cat_product p on p.code = v.product_code"
          + " where pi.company_id = ? and pi.insurer_code = ?"
          + " order by p.code, v.version_no desc";

  static final String COMMISSION_PRODUCTS =
      "select distinct p.code, p.name from cat_commission_rate c"
          + " join cat_product p on p.code = c.product_code"
          + " where c.company_id = ? and c.insurer_code = ? order by p.code";

  private final InsurerService insurers;
  private final InsurerContactRepository contacts;
  private final InsurerBankAccountRepository accounts;
  private final AuditTrailService audit;
  private final JdbcTemplate jdbc;

  /**
   * Creates the service.
   *
   * @param insurers insurer profiles
   * @param contacts contacts
   * @param accounts bank accounts
   * @param audit audit trail
   * @param jdbc JDBC (linked products)
   */
  public InsurerPageService(
      InsurerService insurers,
      InsurerContactRepository contacts,
      InsurerBankAccountRepository accounts,
      AuditTrailService audit,
      JdbcTemplate jdbc) {
    this.insurers = insurers;
    this.contacts = contacts;
    this.accounts = accounts;
    this.audit = audit;
    this.jdbc = jdbc;
  }

  /**
   * A product linked to an insurer: on a package version (with the insurer's role and the latest
   * version) or through a product-specific commission rate.
   *
   * @param productCode risk code
   * @param productName product name
   * @param role LEAD, PARTICIPANT or PANEL on the latest package version, null for a commission
   * @param versionNo latest package version naming the insurer, null for a commission only
   * @param versionStatus status of that version
   * @param commission whether a product-specific commission rate exists
   */
  public record LinkedProduct(
      String productCode,
      String productName,
      String role,
      Integer versionNo,
      String versionStatus,
      boolean commission) {}

  /**
   * Changes the payment terms and remittance details; the insurer is pending authorization again.
   *
   * @param insurerId insurer profile
   * @param terms terms
   * @return insurer
   */
  public InsurerProfile updateRemittance(Long insurerId, RemittanceTerms terms) {
    InsurerProfile insurer = insurers.get(insurerId);
    insurer.updateRemittance(terms);
    audit.record(
        CatalogKind.INSURER.label(),
        insurer.getPartyCode(),
        AuditAction.UPDATE,
        "Changed payment terms and remittance details");
    return insurer;
  }

  /**
   * Contacts of an insurer.
   *
   * @param insurerId insurer profile
   * @return contacts
   */
  @Transactional(readOnly = true)
  public List<InsurerContact> contacts(Long insurerId) {
    return contacts.findByInsurerIdOrderByRoleAscNameAsc(insurerId);
  }

  /**
   * Adds a contact, pending authorization.
   *
   * @param insurerId insurer profile
   * @param details contact
   * @return contact
   */
  public InsurerContact addContact(Long insurerId, ContactDetails details) {
    InsurerProfile insurer = insurers.get(insurerId);
    InsurerContact saved = contacts.save(new InsurerContact(insurer.getId(), details));
    audit.record(
        CatalogKind.INSURER_CONTACT.label(),
        saved.catalogReference(),
        AuditAction.CREATE,
        saved.catalogDescription());
    return saved;
  }

  /**
   * Changes a contact; it is pending authorization again.
   *
   * @param contactId contact
   * @param details contact
   * @return contact
   */
  public InsurerContact updateContact(Long contactId, ContactDetails details) {
    InsurerContact contact =
        contacts
            .findById(contactId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(CatalogKind.INSURER_CONTACT.label(), contactId));
    contact.update(details);
    audit.record(
        CatalogKind.INSURER_CONTACT.label(),
        contact.catalogReference(),
        AuditAction.UPDATE,
        contact.catalogDescription());
    return contact;
  }

  /**
   * Bank accounts of an insurer.
   *
   * @param insurerId insurer profile
   * @return accounts
   */
  @Transactional(readOnly = true)
  public List<InsurerBankAccount> bankAccounts(Long insurerId) {
    return accounts.findByInsurerIdOrderByPurposeAscAccountNoAsc(insurerId);
  }

  /**
   * Adds a bank account, pending authorization.
   *
   * @param insurerId insurer profile
   * @param details account
   * @return account
   */
  public InsurerBankAccount addBankAccount(Long insurerId, BankAccountDetails details) {
    InsurerProfile insurer = insurers.get(insurerId);
    if (accounts.findByInsurerIdAndAccountNo(insurer.getId(), details.accountNo()).isPresent()) {
      throw new DuplicateResourceException(
          CatalogKind.INSURER_BANK_ACCOUNT.label(), details.accountNo());
    }
    InsurerBankAccount saved = accounts.save(new InsurerBankAccount(insurer.getId(), details));
    audit.record(
        CatalogKind.INSURER_BANK_ACCOUNT.label(),
        saved.catalogReference(),
        AuditAction.CREATE,
        saved.catalogDescription());
    return saved;
  }

  /**
   * Changes a bank account; it is pending authorization again.
   *
   * @param accountId account
   * @param details account
   * @return account
   */
  public InsurerBankAccount updateBankAccount(Long accountId, BankAccountDetails details) {
    InsurerBankAccount account =
        accounts
            .findById(accountId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        CatalogKind.INSURER_BANK_ACCOUNT.label(), accountId));
    accounts
        .findByInsurerIdAndAccountNo(account.getInsurerId(), details.accountNo())
        .filter(other -> !other.getId().equals(accountId))
        .ifPresent(
            other -> {
              throw new DuplicateResourceException(
                  CatalogKind.INSURER_BANK_ACCOUNT.label(), details.accountNo());
            });
    account.update(details);
    audit.record(
        CatalogKind.INSURER_BANK_ACCOUNT.label(),
        account.catalogReference(),
        AuditAction.UPDATE,
        account.catalogDescription());
    return account;
  }

  /**
   * The products an insurer is linked to, by risk code.
   *
   * @param insurerId insurer profile
   * @return products
   */
  @Transactional(readOnly = true)
  public List<LinkedProduct> linkedProducts(Long insurerId) {
    InsurerProfile insurer = insurers.get(insurerId);
    Map<String, LinkedProduct> byCode = new LinkedHashMap<>();
    jdbc.query(
        PACKAGE_PRODUCTS,
        rs -> {
          String code = rs.getString("code");
          byCode.putIfAbsent(
              code,
              new LinkedProduct(
                  code,
                  rs.getString("name"),
                  rs.getString("role"),
                  rs.getInt("version_no"),
                  rs.getString("status"),
                  false));
        },
        insurer.getCompanyId(),
        insurer.getPartyCode());
    jdbc.query(
        COMMISSION_PRODUCTS,
        rs -> {
          String code = rs.getString("code");
          LinkedProduct known = byCode.get(code);
          byCode.put(
              code,
              known == null
                  ? new LinkedProduct(code, rs.getString("name"), null, null, null, true)
                  : new LinkedProduct(
                      code,
                      known.productName(),
                      known.role(),
                      known.versionNo(),
                      known.versionStatus(),
                      true));
        },
        insurer.getCompanyId(),
        insurer.getPartyCode());
    List<LinkedProduct> products = new ArrayList<>(byCode.values());
    products.sort((a, b) -> a.productCode().compareTo(b.productCode()));
    return products;
  }
}
