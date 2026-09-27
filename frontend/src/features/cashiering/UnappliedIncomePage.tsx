import { INCOME_SCREEN } from './legacy/batchScreens';
import { LegacyBatchList } from './legacy/LegacyBatchList';

/**
 * Unapplied to Income: batches of old unapplied payments, new and legacy, reclassified to other
 * income after the approval of the Cashiering team lead and of top management.
 */
export default function UnappliedIncomePage() {
  return <LegacyBatchList screen={INCOME_SCREEN} />;
}
