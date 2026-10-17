package com.iortatechnxt.brokerverse.docgen.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Word renditions of composed documents. */
public interface DocRenditionRepository extends JpaRepository<DocRendition, Long> {

  /**
   * The rendition of a PDF.
   *
   * @param sha256 SHA-256 of the PDF bytes
   * @return rendition
   */
  Optional<DocRendition> findBySha256(String sha256);

  /**
   * Whether a PDF has a rendition.
   *
   * @param sha256 SHA-256 of the PDF bytes
   * @return true when recorded
   */
  boolean existsBySha256(String sha256);
}
