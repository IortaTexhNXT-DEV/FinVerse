package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Check settings. */
public interface CheckSettingRepository extends JpaRepository<CheckSetting, Long> {

  /**
   * The setting of a check.
   *
   * @param checkCode check
   * @return setting
   */
  Optional<CheckSetting> findByCheckCode(String checkCode);

  /**
   * Settings waiting for a checker.
   *
   * @param status status
   * @return settings
   */
  List<CheckSetting> findByRecordStatus(RecordStatus status);

  /**
   * All settings.
   *
   * @return settings by check
   */
  List<CheckSetting> findAllByOrderByCheckCodeAsc();
}
