/**
 * Placement: payment gate per segment (BRD 2.3.1, BRNB.068/114), CLPC billing file and payment
 * report matching (BRNB.067/068), placement slips per insurer (BRNB.069), sending to the insurer
 * (BRNB.071), hold cover (BRNB.072/103), insurer returns (BRNB.033/034/058/059) and cancel /
 * reactivate placement (BRNB.062, BRD 2.1.16).
 *
 * <p>Account state changes go through {@code AccountLifecycleService} only. Contracts for other
 * modules: {@code PlacementQueryService} ({@code slipsFor}, {@code holdCover}) and the port {@code
 * PaymentConfirmationSource} through which Operations Cashiering will supply confirmed receipt
 * applications. See docs/architecture/BROKING_ARCHITECTURE.md (placement).
 */
package com.iortatechnxt.brokerverse.placement;
