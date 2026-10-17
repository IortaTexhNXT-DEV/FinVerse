package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Default co-insurance participations of the insurers. */
public interface InsurerParticipationRepository extends JpaRepository<InsurerParticipation, Long> {

  /**
   * Participations of an insurer's arrangement.
   *
   * @param insurerId insurer profile
   * @return participations by start date and participant
   */
  List<InsurerParticipation> findByInsurerIdOrderByEffectiveFromDescParticipantCodeAsc(
      Long insurerId);
}
