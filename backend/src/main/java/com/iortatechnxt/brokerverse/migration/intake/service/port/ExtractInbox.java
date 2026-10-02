package com.iortatechnxt.brokerverse.migration.intake.service.port;

import java.util.List;

/**
 * Port of the place where extract files arrive (DATA_MIGRATION_DESIGN sections 3.2 and 23). The
 * default is the console upload, which has nothing to collect; the SFTP drop is parked until BDOI
 * IT names it, when an adapter lists the files of the drop for {@code MIG_INTAKE_SCAN}.
 */
public interface ExtractInbox {

  /**
   * Whether an automatic inbox is connected.
   *
   * @return false for the console upload
   */
  boolean connected();

  /**
   * Data files waiting in the inbox with their control files.
   *
   * @return files
   */
  List<InboxFile> waiting();

  /**
   * Marks a file as taken.
   *
   * @param file file
   */
  void taken(InboxFile file);

  /**
   * A waiting file with its control file.
   *
   * @param companyId company
   * @param fileName data file name
   * @param content data file
   * @param controlName control file name
   * @param control control file
   */
  record InboxFile(
      Long companyId, String fileName, byte[] content, String controlName, byte[] control) {}
}
