package com.iortatechnxt.brokerverse.brokerclaims.status.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** The standard claim document checklist. */
public interface DocumentRequirementRepository extends JpaRepository<DocumentRequirement, Long> {

  /**
   * The checklist, by claim type and document type.
   *
   * @return requirements
   */
  List<DocumentRequirement> findAllByOrderByClaimTypeAscDocumentTypeAsc();

  /**
   * The active requirements of a claim type.
   *
   * @param claimType claim type
   * @return requirements
   */
  List<DocumentRequirement> findByClaimTypeAndActiveTrueOrderByDocumentTypeAsc(String claimType);

  /**
   * One requirement.
   *
   * @param claimType claim type
   * @param documentType document type
   * @return requirement
   */
  Optional<DocumentRequirement> findByClaimTypeAndDocumentType(
      String claimType, String documentType);
}
