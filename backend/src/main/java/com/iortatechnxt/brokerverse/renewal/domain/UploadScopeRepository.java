package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Declared scopes of uploads. */
public interface UploadScopeRepository extends JpaRepository<UploadScope, Long> {

  /**
   * The scope of an upload job.
   *
   * @param jobNo job
   * @return scope
   */
  Optional<UploadScope> findByJobNo(String jobNo);
}
