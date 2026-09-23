package com.iortatechnxt.finverse.reserves.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link TakafulSetting}. */
public interface TakafulSettingRepository extends JpaRepository<TakafulSetting, Long> {

  /**
   * Settings of a company.
   *
   * @param companyId company
   * @return settings if configured
   */
  Optional<TakafulSetting> findByCompanyId(Long companyId);
}
