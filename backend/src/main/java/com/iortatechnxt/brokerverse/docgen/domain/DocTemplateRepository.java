package com.iortatechnxt.brokerverse.docgen.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Document templates. */
public interface DocTemplateRepository extends JpaRepository<DocTemplate, Long> {

  /**
   * Versions of a template, newest first.
   *
   * @param code template
   * @return versions
   */
  List<DocTemplate> findByCodeOrderByVersionNoDesc(String code);

  /**
   * Every version of every template.
   *
   * @return versions by code and number
   */
  List<DocTemplate> findAllByOrderByCodeAscVersionNoDesc();
}
