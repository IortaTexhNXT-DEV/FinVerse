package com.iortatechnxt.brokerverse.disbursement.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** OR / AR and CWT tags of vouchers (DIS 2.10, 2.11). */
public interface VoucherTagRepository extends JpaRepository<VoucherTag, Long> {

  /**
   * The tags of a voucher, oldest first.
   *
   * @param voucherId voucher
   * @return tags
   */
  List<VoucherTag> findByVoucherIdOrderByIdAsc(Long voucherId);
}
