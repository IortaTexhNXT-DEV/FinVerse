package com.iortatechnxt.finverse.common.sequence;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Allocates gapless, human readable document numbers such as {@code JV-HO-2026-000123}.
 *
 * <p>Allocation joins the caller's transaction: if the business transaction rolls back, so does the
 * counter, keeping the series gapless. Concurrent callers serialize on the sequence row lock.
 */
@Service
public class DocumentNumberService {

  private static final String FORMAT = "%s-%06d";

  private final DocumentSequenceRepository repository;

  /**
   * Creates the service.
   *
   * @param repository sequence repository
   */
  public DocumentNumberService(DocumentSequenceRepository repository) {
    this.repository = repository;
  }

  /**
   * Allocates the next number for a prefix.
   *
   * @param prefix series prefix, e.g. "JV-HO-2026"
   * @return formatted number, e.g. "JV-HO-2026-000001"
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public String next(String prefix) {
    repository.createIfMissing(prefix);
    DocumentSequence sequence =
        repository
            .lockByKey(prefix)
            .orElseThrow(() -> new IllegalStateException("Sequence not created: " + prefix));
    return String.format(FORMAT, prefix, sequence.allocate());
  }
}
