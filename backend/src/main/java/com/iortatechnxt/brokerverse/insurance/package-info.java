/**
 * Insurance shared kernel: the contracts through which the claims, reinsurance and actuarial
 * reserve modules exchange data without depending on each other.
 *
 * <p>This package holds only interfaces and immutable value records and depends on no other module.
 * The direction of every dependency is from a business module to this package:
 *
 * <ul>
 *   <li>claims implements {@link com.iortatechnxt.brokerverse.insurance.ClaimsExperienceView} and
 *       notifies every {@link com.iortatechnxt.brokerverse.insurance.ClaimMovementListener} bean;
 *   <li>reinsurance implements {@link com.iortatechnxt.brokerverse.insurance.ClaimMovementListener}
 *       (to book recoveries and reserve shares) and {@link
 *       com.iortatechnxt.brokerverse.insurance.ClaimReinsuranceView};
 *   <li>actuarial reserves consume both views to compute OSLR, IBNR and the reinsurers' share.
 * </ul>
 *
 * <p>Every consumer injects these interfaces optionally ({@code ObjectProvider} or {@code
 * List<...>}) so that each module still starts and is testable when the other is not deployed.
 */
package com.iortatechnxt.brokerverse.insurance;
