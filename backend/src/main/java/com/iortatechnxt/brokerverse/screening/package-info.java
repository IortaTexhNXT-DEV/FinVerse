/**
 * Sanction Screening and Risk Profiling (BDOI BRD-10, SNSRP-101-903;
 * docs/architecture/SANCTION_SCREENING_DESIGN.md): versioned screening configuration, watchlist
 * sources, entries and ingestion, the matching engine, client risk profiling, screening cases and
 * STRs. It posts no journal and never writes the client master directly: risk ratings and tags go
 * through {@code crm.service.ClientRiskService}.
 *
 * <p>Sub-packages are owned by the build waves: {@code config, watchlist, common} (S1-A), {@code
 * matching, risk} (S1-B) and {@code cases, str, report} (S1-C). The other waves read the
 * configuration only through {@code config.service.ActiveConfig}.
 */
package com.iortatechnxt.brokerverse.screening;
