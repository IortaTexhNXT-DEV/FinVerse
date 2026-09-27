package com.iortatechnxt.brokerverse.acsl.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Insurer SOA uploads (ACSL 2.4.0). */
public interface SoaUploadRepository extends JpaRepository<SoaUpload, Long> {

  /**
   * Uploads of a company, filtered by insurer.
   *
   * @param companyId company
   * @param insurer lower-case like pattern on the insurer or upload number ({@code %} for all)
   * @param pageable page
   * @return uploads
   */
  @Query(
      """
      select u from SoaUpload u where u.companyId = :companyId
        and (lower(u.insurerCode) like :q or lower(u.uploadNo) like :q)
      """)
  Page<SoaUpload> search(
      @Param("companyId") Long companyId, @Param("q") String insurer, Pageable pageable);

  /**
   * An upload by number.
   *
   * @param uploadNo upload number
   * @return upload
   */
  Optional<SoaUpload> findByUploadNo(String uploadNo);

  /**
   * The same file uploaded earlier for the insurer.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param sha256 checksum
   * @return earlier upload
   */
  Optional<SoaUpload> findByCompanyIdAndInsurerCodeAndSha256(
      Long companyId, String insurerCode, String sha256);
}
