import { useParams } from 'react-router-dom';
import { DPPR_SCREEN } from '@/features/cashiering/legacy/batchScreens';
import { LegacyBatchDetail } from '@/features/cashiering/legacy/LegacyBatchDetail';

/** A legacy direct payment PR reversal batch. */
export default function DpprBatchPage() {
  return <LegacyBatchDetail screen={DPPR_SCREEN} batchNo={useParams().batchNo ?? ''} />;
}
