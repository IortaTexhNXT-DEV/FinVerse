import { PR2307_SCREEN } from './legacy/batchScreens';
import { LegacyBatchList } from './legacy/LegacyBatchList';

/** Legacy PR 2307 Reversal: batches settling legacy PR 2307 balances against the insurer. */
export default function LegacyPr2307Page() {
  return <LegacyBatchList screen={PR2307_SCREEN} />;
}
