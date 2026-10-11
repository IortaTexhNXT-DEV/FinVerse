package com.iortatechnxt.brokerverse.renewal.letter.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.domain.LetterStatus;
import com.iortatechnxt.brokerverse.renewal.domain.RaNotice;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetter;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The letter actions of BDOI's FRS on top of the letter service: Generate and Send RA in one step,
 * and the resend of a letter already sent (FRRN.022.01, FRRN.023.02).
 */
@Service
public class LetterActions {

  private final LetterService letters;
  private final LetterWriter writer;
  private final RenewalRecords records;

  /**
   * Creates the actions.
   *
   * @param letters letter service
   * @param writer letter writer
   * @param records renewals
   */
  public LetterActions(LetterService letters, LetterWriter writer, RenewalRecords records) {
    this.letters = letters;
    this.writer = writer;
    this.records = records;
  }

  /**
   * Generates and sends Renewal Advices at once (Generate and Send RA, FRRN.022.01): the notice is
   * determined by the system when the setting asks for it.
   *
   * @param companyId company
   * @param refs renewals
   * @param notice notice chosen by the user (manual setting)
   * @param confirmLate the user confirms late Renewal Advices
   * @return the renewals sent and the refused ones
   */
  public BatchOutcome generateAndSendRa(
      Long companyId, List<String> refs, RaNotice notice, boolean confirmLate) {
    BatchOutcome generated = letters.generateRa(companyId, refs, notice, confirmLate);
    if (generated.done().isEmpty()) {
      return generated;
    }
    BatchOutcome sent = letters.sendRa(companyId, generated.done());
    java.util.Map<String, String> refused = new java.util.LinkedHashMap<>(generated.refused());
    refused.putAll(sent.refused());
    return new BatchOutcome(sent.done(), refused);
  }

  /**
   * Sends a letter again (FRRN.022.01, FRRN.023.02): the same stored letter, a new transmission,
   * recorded in the audit trail.
   *
   * @param companyId company
   * @param letterNo letter
   * @return the refusal, null when sent
   */
  @Transactional
  public String resend(Long companyId, String letterNo) {
    RenewalLetter letter = letters.letter(companyId, letterNo);
    if (letter.getStatus() == LetterStatus.GENERATED
        || letter.getStatus() == LetterStatus.CANCELLED) {
      throw new BusinessRuleException(
          "RNW_LETTER_RESEND", "Letter " + letterNo + " was not sent yet: send it first");
    }
    return writer.resend(records.byId(letter.getCandidateId()), letter).orElse(null);
  }
}
