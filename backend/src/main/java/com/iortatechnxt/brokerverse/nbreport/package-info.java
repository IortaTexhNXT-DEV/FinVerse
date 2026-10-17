/**
 * New Business reports and dashboard (W4): the NB operational reports on the report framework
 * (BRNB.011, 031, 037, 057, 075, 078, 115), the NB dashboard (BRNB.012), production targets and
 * saved report variants.
 *
 * <p>The module reads the broking modules' tables through constant SQL aggregates (the pattern of
 * {@code dashboard.service.DashboardLedgerQueries}); it calls no broking service, so it adds no
 * compile-time dependency on the broking modules and none of them depends on it. See
 * docs/architecture/BROKING_ARCHITECTURE.md section 16.
 */
package com.iortatechnxt.brokerverse.nbreport;
