package com.iortatechnxt.brokerverse.opsledger.service.adapter;

import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.Transport;
import com.iortatechnxt.brokerverse.opsledger.service.port.InsurerFileInbox;
import java.util.List;

/**
 * Default {@link InsurerFileInbox} (insurer channels parked, OQ22/OQ29/OQ38): there is no inbox;
 * users upload insurer files on the module screens.
 */
public class ManualInsurerFileInbox implements InsurerFileInbox {

  @Override
  public String transport() {
    return Transport.MANUAL_UPLOAD.name();
  }

  @Override
  public List<InboxFile> pending(Long companyId, String insurerCode, String fileType) {
    return List.of();
  }
}
