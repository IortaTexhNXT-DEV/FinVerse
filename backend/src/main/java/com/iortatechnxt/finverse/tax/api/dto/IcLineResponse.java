package com.iortatechnxt.finverse.tax.api.dto;

import com.iortatechnxt.finverse.common.domain.RecordStatus;
import com.iortatechnxt.finverse.tax.domain.IcLineItem;
import com.iortatechnxt.finverse.tax.domain.IcMeasure;
import com.iortatechnxt.finverse.tax.domain.IcSchedule;
import com.iortatechnxt.finverse.tax.domain.NormalBalance;
import java.math.BigDecimal;

/**
 * IC schedule mapping line.
 *
 * @param id id
 * @param companyId company
 * @param schedule schedule
 * @param lineCode line code
 * @param description description
 * @param lineOrder print order
 * @param accountFrom range start
 * @param accountTo range end
 * @param reportGroup report group
 * @param normalBalance natural side
 * @param signFactor +1 or -1
 * @param measure balance or movement
 * @param rbcFactor RBC factor in percent
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 */
public record IcLineResponse(
    Long id,
    Long companyId,
    IcSchedule schedule,
    String lineCode,
    String description,
    int lineOrder,
    String accountFrom,
    String accountTo,
    String reportGroup,
    NormalBalance normalBalance,
    int signFactor,
    IcMeasure measure,
    BigDecimal rbcFactor,
    RecordStatus recordStatus,
    String maker) {

  /**
   * Maps an entity.
   *
   * @param i line
   * @return response
   */
  public static IcLineResponse from(IcLineItem i) {
    return new IcLineResponse(
        i.getId(),
        i.getCompanyId(),
        i.getSchedule(),
        i.getLineCode(),
        i.getDescription(),
        i.getLineOrder(),
        i.getAccountFrom(),
        i.getAccountTo(),
        i.getReportGroup(),
        i.getNormalBalance(),
        i.getSignFactor(),
        i.getMeasure(),
        i.getRbcFactor(),
        i.getRecordStatus(),
        i.getMaker());
  }
}
