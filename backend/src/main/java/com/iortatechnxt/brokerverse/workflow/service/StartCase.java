package com.iortatechnxt.brokerverse.workflow.service;

import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;

/**
 * Opens a work case for a new business record.
 *
 * @param companyId company
 * @param workflowCode workflow, e.g. {@code NB_ACCOUNT}
 * @param record business record facts
 * @param stageCode first stage; null for the workflow's initial stage
 */
public record StartCase(Long companyId, String workflowCode, CaseRecord record, String stageCode) {}
