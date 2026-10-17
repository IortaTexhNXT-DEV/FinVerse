package com.iortatechnxt.brokerverse.opsledger.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Files of the in-system extract repository. */
public interface ExtractFileRepository extends JpaRepository<ExtractFile, Long> {

  /** Listing without the content. */
  String INFO =
      "select new com.iortatechnxt.brokerverse.opsledger.domain.ExtractFileInfo(f.id, f.folder,"
          + " f.fileName, f.contentType, f.sizeBytes, f.sha256, f.sourceModule, f.sourceRef,"
          + " f.createdAt, f.createdBy) from ExtractFile f where f.companyId = :companyId";

  /**
   * A file by folder and name.
   *
   * @param companyId company
   * @param folder folder
   * @param fileName file name
   * @return file
   */
  Optional<ExtractFile> findByCompanyIdAndFolderAndFileName(
      Long companyId, String folder, String fileName);

  /**
   * Files of a company, newest first, without their content.
   *
   * @param companyId company
   * @return files
   */
  @Query(INFO + " order by f.id desc")
  List<ExtractFileInfo> list(@Param("companyId") Long companyId);

  /**
   * Files of one folder, newest first, without their content.
   *
   * @param companyId company
   * @param folder folder
   * @return files
   */
  @Query(INFO + " and f.folder = :folder order by f.id desc")
  List<ExtractFileInfo> list(@Param("companyId") Long companyId, @Param("folder") String folder);
}
