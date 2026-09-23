package com.iortatechnxt.finverse.report.render;

import java.time.Instant;

/**
 * Print header context (standard report header of the GI report book).
 *
 * @param companyName company printed at the top
 * @param generatedBy user id
 * @param generatedAt run time
 */
public record ReportContext(String companyName, String generatedBy, Instant generatedAt) {}
