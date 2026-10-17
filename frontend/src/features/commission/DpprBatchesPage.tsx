import { DPPR_SCREEN } from '@/features/cashiering/legacy/batchScreens';
import { LegacyBatchList } from '@/features/cashiering/legacy/LegacyBatchList';

/** DP PR Legacy Reversal: batches reversing the premium of legacy invoices paid to the insurer. */
export default function DpprBatchesPage() {
  return <LegacyBatchList screen={DPPR_SCREEN} />;
}
