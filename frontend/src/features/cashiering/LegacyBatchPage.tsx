import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { cashBatchApi } from '@/api/legacyBatches';
import { cashScreenOf } from './legacy/batchScreens';
import { LegacyBatchDetail } from './legacy/LegacyBatchDetail';

/** A Cashiering legacy batch (income reclassification or legacy PR 2307 reversal). */
export default function LegacyBatchPage() {
  const batchNo = useParams().batchNo ?? '';
  const head = useQuery({
    queryKey: ['legacy-batches', 'detail', batchNo],
    queryFn: () => cashBatchApi('INCOME_RECLASS').get(batchNo),
  });
  return <LegacyBatchDetail screen={cashScreenOf(head.data?.batch)} batchNo={batchNo} />;
}
