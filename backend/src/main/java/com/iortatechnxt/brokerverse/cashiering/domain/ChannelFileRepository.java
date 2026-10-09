package com.iortatechnxt.brokerverse.cashiering.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Payment files received. */
public interface ChannelFileRepository extends JpaRepository<ChannelFile, Long> {

  /**
   * The earlier file of a type with the same content that was not refused.
   *
   * @param companyId company
   * @param fileType type
   * @param sha256 checksum
   * @param status refused
   * @return earlier file
   */
  Optional<ChannelFile> findFirstByCompanyIdAndFileTypeAndSha256AndStatusNot(
      Long companyId, String fileType, String sha256, String status);

  /**
   * Files of a company, newest first.
   *
   * @param companyId company
   * @param types types
   * @param pageable page
   * @return files
   */
  Page<ChannelFile> findByCompanyIdAndFileTypeInOrderByIdDesc(
      Long companyId, Collection<String> types, Pageable pageable);

  /**
   * The processed files of a name (Direct Credit: BP filename).
   *
   * @param companyId company
   * @param fileName name, any case
   * @return files, newest first
   */
  List<ChannelFile> findByCompanyIdAndFileNameIgnoreCaseAndBulkJobNoNotNullOrderByIdDesc(
      Long companyId, String fileName);
}
