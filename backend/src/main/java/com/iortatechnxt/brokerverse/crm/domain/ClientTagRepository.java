package com.iortatechnxt.brokerverse.crm.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Client tags. */
public interface ClientTagRepository extends JpaRepository<ClientTag, Long> {

  /**
   * Active tags of a client.
   *
   * @param clientId client
   * @return tags
   */
  List<ClientTag> findByClientIdAndActiveTrueOrderByTagCode(Long clientId);

  /**
   * The active tag of a code.
   *
   * @param clientId client
   * @param tagCode tag
   * @return tag
   */
  Optional<ClientTag> findByClientIdAndTagCodeAndActiveTrue(Long clientId, String tagCode);
}
