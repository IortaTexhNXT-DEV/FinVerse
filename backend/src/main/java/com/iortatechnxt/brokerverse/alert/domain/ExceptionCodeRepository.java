package com.iortatechnxt.brokerverse.alert.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link ExceptionCode}. */
public interface ExceptionCodeRepository extends JpaRepository<ExceptionCode, Long> {

  /**
   * Finds a code.
   *
   * @param code exception code
   * @return code if present
   */
  Optional<ExceptionCode> findByCode(String code);

  /**
   * Lists all codes.
   *
   * @return codes ordered by module and code
   */
  List<ExceptionCode> findAllByOrderByModuleAscCodeAsc();
}
