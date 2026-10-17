package com.iortatechnxt.brokerverse.fixedasset.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link AssetMovement}. */
public interface AssetMovementRepository extends JpaRepository<AssetMovement, Long> {

  /**
   * Movement history of an asset.
   *
   * @param assetId asset
   * @return movements in date order
   */
  List<AssetMovement> findByAssetIdOrderByMovementDateAscIdAsc(Long assetId);
}
