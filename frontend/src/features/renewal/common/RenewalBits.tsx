import type { BatchOutcome, CandidateRow } from '@/api/renewal';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tag } from '@/components/ui/Tag';
import { bucketTone } from './renewalCodes';

const BUCKET_LABELS: Record<string, string> = {
  CLEAN: 'Clean',
  REVIEW: 'Review',
  EXCEPTION: 'Exception',
};

/** The Classification pill of a renewal. */
export function BucketPill({ bucket }: Readonly<{ bucket: string | null }>) {
  if (bucket === null) {
    return null;
  }
  return (
    <StatusBadge
      status={bucket}
      label={BUCKET_LABELS[bucket] ?? bucket}
      tone={bucketTone(bucket)}
    />
  );
}

type ChipTone = 'flag' | 'danger' | 'info' | 'neutral';

const CHIPS: { key: keyof CandidateRow['flags']; label: string; tone: ChipTone }[] = [
  { key: 'urgent', label: 'Urgent', tone: 'danger' },
  { key: 'returned', label: 'Returned', tone: 'danger' },
  { key: 'transferred', label: 'Transferred', tone: 'info' },
  { key: 'endorsed', label: 'Endorsed', tone: 'flag' },
  { key: 'claims', label: 'Claims', tone: 'flag' },
  { key: 'outstanding', label: 'Outstanding', tone: 'flag' },
  { key: 'kycDue', label: 'KYC due', tone: 'info' },
  { key: 'nrns', label: 'NRNS', tone: 'flag' },
  { key: 'stp', label: 'Straight-through', tone: 'neutral' },
  { key: 'nfrSent', label: 'NFR sent', tone: 'neutral' },
  { key: 'locked', label: 'Locked', tone: 'neutral' },
];

/** The flag chips of a renewal, next to its reference (never inside the pill). */
export function FlagChips({ row }: Readonly<{ row: CandidateRow }>) {
  const chips = CHIPS.filter((c) => row.flags[c.key] === true);
  if (chips.length === 0) {
    return null;
  }
  return (
    <span className="rnw-flags">
      {chips.map((c) => (
        <Tag key={c.key} tone={c.tone}>
          {c.label}
        </Tag>
      ))}
    </span>
  );
}

/** The done and refused renewals of an action on several renewals. */
export function OutcomeDialog({
  title,
  outcome,
  onClose,
}: Readonly<{ title: string; outcome: BatchOutcome; onClose: () => void }>) {
  const refused = Object.entries(outcome.refused);
  return (
    <Modal open title={title} onClose={onClose} footer={<Button onClick={onClose}>Close</Button>}>
      <p>
        {outcome.done.length} renewal(s) done
        {refused.length > 0 ? `, ${String(refused.length)} refused:` : '.'}
      </p>
      {refused.length > 0 && (
        <ul className="rnw-refused">
          {refused.map(([ref, reason]) => (
            <li key={ref}>
              <strong>{ref}</strong>: {reason}
            </li>
          ))}
        </ul>
      )}
    </Modal>
  );
}
