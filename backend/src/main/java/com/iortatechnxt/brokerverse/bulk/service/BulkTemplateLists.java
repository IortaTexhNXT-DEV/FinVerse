package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import java.util.List;

/** The codes and labels of a platform list of values, for the drop-downs of a bulk template. */
public interface BulkTemplateLists {

  /**
   * The usable values of a list today.
   *
   * @param typeCode list type
   * @return codes and labels; empty when the list is unknown
   */
  List<Choice> choices(String typeCode);
}
