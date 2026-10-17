package com.iortatechnxt.brokerverse.remittance.service.port;

/**
 * Port to BDOI's shared drive (ICD IF-RMT-04): the extract files of a remittance batch are copied
 * to the folder of its remittance type. The live connector is part of the integration scope; the
 * simulator of SIT and UAT ({@code SharedFolderSimulator}) answers until it is connected.
 */
public interface SharedFolderGateway {

  /**
   * Whether this is the live connector.
   *
   * @return false for the simulator
   */
  boolean live();

  /**
   * Copies a file to a folder.
   *
   * @param folder folder of the remittance type
   * @param fileName file name
   * @param content file
   * @return outcome
   */
  Copy copy(String folder, String fileName, byte[] content);

  /**
   * Outcome of a copy.
   *
   * @param delivered whether the file was written
   * @param reference location of the file, or the reason when not delivered
   */
  record Copy(boolean delivered, String reference) {}
}
