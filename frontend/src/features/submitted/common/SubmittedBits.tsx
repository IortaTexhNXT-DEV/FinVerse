import { Link } from 'react-router-dom';
import { Tag } from '@/components/ui/Tag';
import { FLAG_LABELS, policyLink } from './submittedCodes';

/** The flag chips of a masterlist record (Renewable, FFY, No Touch, Migrated, Insurer approval). */
export function FlagChips({ flags }: Readonly<{ flags: readonly string[] }>) {
  if (flags.length === 0) {
    return <span className="muted">—</span>;
  }
  return (
    <span className="tag-list">
      {flags.map((f) => (
        <Tag key={f} tone={f === 'FALLOUT' || f === 'NON_RENEWABLE' ? 'danger' : 'flag'}>
          {FLAG_LABELS[f] ?? f}
        </Tag>
      ))}
    </span>
  );
}

/** Masterlist number linked to its record. */
export function SbmLink({ id, sbmNo }: Readonly<{ id: number | null; sbmNo: string | null }>) {
  if (id === null || sbmNo === null) {
    return <span className="muted">—</span>;
  }
  return <Link to={policyLink(id)}>{sbmNo}</Link>;
}
