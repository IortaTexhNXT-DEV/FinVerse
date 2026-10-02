import type { BucketCount } from './api';

/** A bucket with its share of the largest one (bar length). */
export interface BucketBar extends BucketCount {
  share: number;
}

/** Bar lengths of the ageing chart: each bucket relative to the fullest one. */
export function bucketShares(buckets: BucketCount[]): BucketBar[] {
  const max = Math.max(0, ...buckets.map((b) => b.claims));
  return buckets.map((b) => ({ ...b, share: max === 0 ? 0 : b.claims / max }));
}
