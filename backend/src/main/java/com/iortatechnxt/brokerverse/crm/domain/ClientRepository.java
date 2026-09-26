package com.iortatechnxt.brokerverse.crm.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Clients. */
public interface ClientRepository
    extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {

  /**
   * A client by client code or prospect code.
   *
   * @param companyId company
   * @param code client or prospect code
   * @return client
   */
  @Query(
      "select c from Client c where c.companyId = :companyId"
          + " and (c.clientCode = :code or c.prospectCode = :code)")
  Optional<Client> findByCode(@Param("companyId") Long companyId, @Param("code") String code);

  /**
   * Clients with a TIN (duplicate check).
   *
   * @param companyId company
   * @param tin TIN
   * @return clients
   */
  List<Client> findByCompanyIdAndTin(Long companyId, String tin);

  /**
   * Clients with an ID key (duplicate check).
   *
   * @param companyId company
   * @param idKey ID type and number key
   * @return clients
   */
  List<Client> findByCompanyIdAndKeysIdKey(Long companyId, String idKey);

  /**
   * Clients with an e-mail, ignoring case (duplicate check).
   *
   * @param companyId company
   * @param email e-mail
   * @return clients
   */
  List<Client> findByCompanyIdAndEmailIgnoreCase(Long companyId, String email);

  /**
   * Clients with a mobile key (duplicate check).
   *
   * @param companyId company
   * @param mobileKey mobile key
   * @return clients
   */
  List<Client> findByCompanyIdAndKeysMobileKey(Long companyId, String mobileKey);

  /**
   * Individuals with a name key and birth date (duplicate check).
   *
   * @param companyId company
   * @param nameKey last and first name key
   * @param birthDate birth date
   * @return clients
   */
  List<Client> findByCompanyIdAndKeysNameKeyAndBirthDate(
      Long companyId, String nameKey, LocalDate birthDate);

  /**
   * Corporate clients with a normalised name (duplicate check).
   *
   * @param companyId company
   * @param corporateKey corporate key
   * @return clients
   */
  List<Client> findByCompanyIdAndKeysCorporateKey(Long companyId, String corporateKey);

  /**
   * Verified clients whose KYC review date has passed (KYC review job, BRNB.110).
   *
   * @param status KYC status (VERIFIED)
   * @param date business date
   * @return clients
   */
  List<Client> findByKycStatusAndKycReviewDueBefore(KycStatus status, LocalDate date);
}
