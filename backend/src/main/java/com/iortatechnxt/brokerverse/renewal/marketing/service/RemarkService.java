package com.iortatechnxt.brokerverse.renewal.marketing.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalRemark;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalRemarkRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remarks of a renewal (FR-RN-044): at most 200 characters, stamped with the user, time and stage,
 * never edited. Transfers, returns and re-openings add their remarks here too.
 */
@Service
@Transactional
public class RemarkService {

  private final RenewalRecords records;
  private final RenewalRemarkRepository remarks;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param remarks remarks
   */
  public RemarkService(RenewalRecords records, RenewalRemarkRepository remarks) {
    this.records = records;
    this.remarks = remarks;
  }

  /**
   * Adds a remark to a renewal of the user's scope.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param text remark
   * @return remark
   */
  public RenewalRemark add(Long companyId, String renewalRef, String text) {
    return add(records.get(companyId, renewalRef), requireText(text, "Enter the remarks"));
  }

  /**
   * Adds a remark to a renewal already loaded; a longer text is cut at the limit.
   *
   * @param c renewal
   * @param text remark
   * @return remark
   */
  public RenewalRemark add(RenewalCandidate c, String text) {
    String value =
        text.length() > RenewalRemark.MAX_LENGTH
            ? text.substring(0, RenewalRemark.MAX_LENGTH)
            : text;
    return remarks.save(new RenewalRemark(c.getId(), c.getStage(), value));
  }

  /**
   * The remarks of a renewal, newest first.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @return remarks
   */
  @Transactional(readOnly = true)
  public List<RenewalRemark> of(Long companyId, String renewalRef) {
    return remarks.findByCandidateIdOrderByIdDesc(records.get(companyId, renewalRef).getId());
  }

  /**
   * A required remark of at most 200 characters.
   *
   * @param text text
   * @param missing message when blank
   * @return stripped text
   */
  public static String requireText(String text, String missing) {
    if (text == null || text.isBlank()) {
      throw new BusinessRuleException("RNW_REMARKS_REQUIRED", missing);
    }
    String value = text.strip();
    if (value.length() > RenewalRemark.MAX_LENGTH) {
      throw new BusinessRuleException(
          "RNW_REMARKS_TOO_LONG", "Remarks may have at most 200 characters");
    }
    return value;
  }
}
