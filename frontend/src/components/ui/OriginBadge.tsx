import type { RecordOriginFields } from '@/api/types';
import { Tag } from './Tag';

function detail(r: RecordOriginFields): string {
  const parts = [`Migrated from ${r.sourceSystem ?? 'a legacy system'}`];
  if (r.legacyRef) {
    parts.push(`legacy reference ${r.legacyRef}`);
  }
  if (r.migrationBatch) {
    parts.push(`batch ${r.migrationBatch}`);
  }
  return parts.join(', ');
}

/**
 * The LEGACY badge of a record migrated from a legacy system (BRD-13), with its source system,
 * legacy reference and migration batch in the tooltip. Renders nothing for a BIBS record.
 */
export function OriginBadge({ record }: Readonly<{ record: RecordOriginFields }>) {
  if (record.origin !== 'MIGRATED') {
    return null;
  }
  return (
    <Tag tone="info" title={detail(record)}>
      LEGACY
    </Tag>
  );
}
