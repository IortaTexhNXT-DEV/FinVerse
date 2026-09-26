package com.iortatechnxt.brokerverse.docgen.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.docgen.domain.DocRendition;
import com.iortatechnxt.brokerverse.docgen.domain.DocRenditionRepository;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Word copies of composed documents (client requirement 16). Every PDF composed by {@link
 * DocumentComposer} is recorded with the SHA-256 of its bytes, its content and date; whoever holds
 * the PDF (a download, a stored slip, advice or invoice) gets the same document as Word by
 * presenting it. The PDF itself is the proof of access: nothing is served for a hash alone.
 *
 * <p>PDFs that are not composed (the BIR 2307 form, merged print batches) and password-protected
 * e-mail attachments have no rendition: they stay PDF only.
 */
@Service
public class DocumentRenditionService {

  private static final String ENTITY = "Document";
  private static final Pattern UNSAFE_FILE_CHARS = Pattern.compile("[^A-Za-z0-9._-]+");
  private static final int HASH_LENGTH = 64;

  private final DocRenditionRepository renditions;
  private final ObjectMapper mapper;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param renditions rendition store
   * @param mapper JSON mapper (document content)
   * @param audit audit trail
   */
  public DocumentRenditionService(
      DocRenditionRepository renditions, ObjectMapper mapper, AuditTrailService audit) {
    this.renditions = renditions;
    this.mapper = mapper;
    this.audit = audit;
  }

  /**
   * Records the content of a composed PDF, in its own transaction so documents composed in
   * read-only transactions (downloads) are recorded too. Recording the same PDF twice is a no-op.
   *
   * @param pdf composed PDF bytes
   * @param spec document content
   * @param date date printed on the document
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(byte[] pdf, DocumentSpec spec, LocalDate date) {
    String hash = Sha256.hex(pdf);
    if (renditions.existsBySha256(hash)) {
      return;
    }
    renditions.save(new DocRendition(hash, spec.title(), spec.reference(), date, json(spec)));
  }

  /**
   * Whether a PDF, identified by its SHA-256, can be downloaded as Word.
   *
   * @param sha256 SHA-256 of the PDF (hex)
   * @return the document title when it can
   */
  @Transactional(readOnly = true)
  public Optional<String> available(String sha256) {
    String hash = sha256 == null ? "" : sha256.trim().toLowerCase(Locale.ROOT);
    if (hash.length() != HASH_LENGTH) {
      return Optional.empty();
    }
    return renditions.findBySha256(hash).map(DocRendition::getTitle);
  }

  /**
   * The Word copy of a composed PDF: the same content and date, in the Word layout.
   *
   * @param pdf PDF bytes as downloaded
   * @return Word file
   * @throws ResourceNotFoundException when the PDF was not composed here (or was changed)
   */
  @Transactional
  public WordCopy word(byte[] pdf) {
    DocRendition r =
        renditions
            .findBySha256(Sha256.hex(pdf))
            .orElseThrow(() -> new ResourceNotFoundException("Word copy of document", "PDF"));
    DocumentSpec spec = spec(r.getSpec());
    byte[] content = DocumentWordWriter.write(spec, r.getDocDate());
    String key = r.getReference() == null ? r.getTitle() : r.getReference();
    audit.record(ENTITY, key, AuditAction.EXPORT, "Word copy of " + r.getTitle());
    return new WordCopy(fileName(key), content);
  }

  private String json(DocumentSpec spec) {
    try {
      return mapper.writeValueAsString(spec);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Document content cannot be recorded", ex);
    }
  }

  private DocumentSpec spec(String json) {
    try {
      return mapper.readValue(json, DocumentSpec.class);
    } catch (JsonProcessingException ex) {
      throw new BusinessRuleException(
          "DOCUMENT_WORD_UNAVAILABLE", "This document cannot be produced in Word", ex);
    }
  }

  private static String fileName(String key) {
    String base = UNSAFE_FILE_CHARS.matcher(key.strip()).replaceAll("-");
    return (base.isEmpty() ? "document" : base) + "." + DocumentFormat.DOCX.extension();
  }

  /**
   * A Word file.
   *
   * @param fileName suggested file name
   * @param content DOCX bytes
   */
  public record WordCopy(String fileName, byte[] content) {}
}
