package com.iortatechnxt.brokerverse.eb.document.service;

import com.iortatechnxt.brokerverse.eb.domain.EbBor;
import com.iortatechnxt.brokerverse.eb.domain.EbBorRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbDocument;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentStatus;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The documents a cycle holds for the required-document checks and the e-mails to insurers: the
 * active entries of the register and, for the Broker on Record, the validated version in force of
 * the programme (an uploaded or rejected BOR does not count).
 */
@Component
@Transactional(readOnly = true)
public class CycleDocuments {

  private final EbDocumentRepository register;
  private final EbBorRepository bors;

  /**
   * Creates the reader.
   *
   * @param register EB document register
   * @param bors BOR versions
   */
  public CycleDocuments(EbDocumentRepository register, EbBorRepository bors) {
    this.register = register;
    this.bors = bors;
  }

  /**
   * The document types present on a cycle.
   *
   * @param cycle cycle
   * @param date business date (BOR in force)
   * @return types
   */
  public Set<String> presentTypes(EbCycle cycle, LocalDate date) {
    Set<String> types = new HashSet<>();
    register.findByCycleIdOrderByIdAsc(cycle.getId()).stream()
        .filter(d -> d.getStatus() == EbDocumentStatus.ACTIVE)
        .map(EbDocument::getDocumentType)
        .filter(t -> !EbDocumentTypes.BOR.equals(t))
        .forEach(types::add);
    if (activeBor(cycle, date).isPresent()) {
      types.add(EbDocumentTypes.BOR);
    }
    return types;
  }

  /**
   * The stored files of the active documents of some types (the BOR in force for {@code EB_BOR}).
   *
   * @param cycle cycle
   * @param types document types
   * @param date business date
   * @return attachment ids
   */
  public List<Long> files(EbCycle cycle, Collection<String> types, LocalDate date) {
    List<Long> ids = new ArrayList<>();
    if (types.contains(EbDocumentTypes.BOR)) {
      activeBor(cycle, date).map(EbBor::getAttachmentId).ifPresent(ids::add);
    }
    register.findByCycleIdOrderByIdAsc(cycle.getId()).stream()
        .filter(d -> d.getStatus() == EbDocumentStatus.ACTIVE)
        .filter(d -> types.contains(d.getDocumentType()))
        .filter(d -> !EbDocumentTypes.BOR.equals(d.getDocumentType()))
        .map(EbDocument::getAttachmentId)
        .filter(id -> !ids.contains(id))
        .forEach(ids::add);
    return ids;
  }

  /**
   * The validated BOR of the programme in force on a date.
   *
   * @param cycle cycle
   * @param date date
   * @return BOR
   */
  public Optional<EbBor> activeBor(EbCycle cycle, LocalDate date) {
    return bors.findByProgrammeIdOrderByIdDesc(cycle.getProgrammeId()).stream()
        .filter(b -> b.activeOn(date))
        .findFirst();
  }
}
