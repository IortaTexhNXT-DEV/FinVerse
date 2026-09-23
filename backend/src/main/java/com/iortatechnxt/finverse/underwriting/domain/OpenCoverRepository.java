package com.iortatechnxt.finverse.underwriting.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link OpenCover}. */
public interface OpenCoverRepository extends JpaRepository<OpenCover, Long> {

  /**
   * Lists a company's open covers.
   *
   * @param companyId company
   * @return open covers, newest first
   */
  @EntityGraph(attributePaths = {"product", "customer"})
  List<OpenCover> findByCompanyIdOrderByIdDesc(Long companyId);

  /**
   * Loads an open cover with its product and client.
   *
   * @param id id
   * @return open cover
   */
  @EntityGraph(attributePaths = {"product", "customer"})
  Optional<OpenCover> findWithDetailsById(Long id);

  /**
   * Finds an open cover by number.
   *
   * @param companyId company
   * @param openCoverNo number
   * @return open cover
   */
  @EntityGraph(attributePaths = {"product", "customer"})
  Optional<OpenCover> findByCompanyIdAndOpenCoverNo(Long companyId, String openCoverNo);
}
