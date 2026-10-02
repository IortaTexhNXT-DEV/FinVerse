package com.iortatechnxt.brokerverse.submitted.service.port;

import java.time.LocalDate;
import java.util.List;

/**
 * Port: hands printed letters to the mail house (COG) for snail mail (BRIDSP-22; SP SQ09;
 * SUBMITTED_POLICIES_DESIGN section 2.2). Used by the letters of this module and by the Renewal
 * module for the printed renewal letters of submitted policies. The default adapter builds a print
 * batch - one merged PDF and a control list - in the file store, for the mail house to collect.
 */
public interface MailHouseGateway {

  /**
   * Hands a batch of letters over.
   *
   * @param request letters of one type and day
   * @return the print batch
   */
  PrintHandOver handOver(PrintRequest request);

  /**
   * A batch of letters to print.
   *
   * @param companyId company
   * @param source module that produced the letters (SUBMITTED, RENEWAL)
   * @param letterType letter type
   * @param batchDate business date of the batch
   * @param letters the letters
   */
  record PrintRequest(
      Long companyId,
      String source,
      String letterType,
      LocalDate batchDate,
      List<PrintedLetter> letters) {

    /** Defensive copy. */
    public PrintRequest {
      letters = List.copyOf(letters);
    }
  }

  /**
   * One letter of a batch.
   *
   * @param reference letter reference
   * @param recipientName addressee
   * @param address mailing address
   * @param storedFileId the letter PDF in the file store
   */
  record PrintedLetter(String reference, String recipientName, String address, Long storedFileId) {}

  /**
   * The batch handed over.
   *
   * @param batchNo print batch number
   * @param mergedFileId merged PDF in the file store
   * @param controlListFileId control list in the file store
   * @param count letters in the batch
   */
  record PrintHandOver(String batchNo, Long mergedFileId, Long controlListFileId, int count) {}
}
