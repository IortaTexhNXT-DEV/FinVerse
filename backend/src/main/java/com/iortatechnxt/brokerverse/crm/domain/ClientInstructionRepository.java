package com.iortatechnxt.brokerverse.crm.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Client special instructions. */
public interface ClientInstructionRepository extends JpaRepository<ClientInstruction, Long> {

  /**
   * Instructions of a client, newest first.
   *
   * @param clientId client
   * @return instructions
   */
  List<ClientInstruction> findByClientIdOrderByIdDesc(Long clientId);
}
