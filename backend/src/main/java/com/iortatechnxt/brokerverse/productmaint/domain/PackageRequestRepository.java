package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Package requests. */
public interface PackageRequestRepository
    extends JpaRepository<PackageRequest, Long>, JpaSpecificationExecutor<PackageRequest> {

  /**
   * A request by its number (catalog events carry it as the source request).
   *
   * @param requestNo request number
   * @return request
   */
  Optional<PackageRequest> findByRequestNo(String requestNo);

  /**
   * Requests of a product of the given types in the given stages (open renewal look-up).
   *
   * @param productCode target product
   * @param types request types
   * @param stages stages
   * @return requests, newest first
   */
  List<PackageRequest> findByTargetProductCodeAndRequestTypeInAndStatusInOrderByIdDesc(
      String productCode, Collection<RequestType> types, Collection<RequestStage> stages);
}
