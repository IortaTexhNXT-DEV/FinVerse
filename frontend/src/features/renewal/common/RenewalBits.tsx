import type { BatchOutcome, CandidateRow } from '@/api/renewal';
import { InsurerName, ProductName, SalesUnitName } from '@/components/broking/LovLabel';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { Kpi } from '@/components/ui/Kpi';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tag } from '@/components/ui/Tag';
import { bucketTone } from './renewalCodes';

const BUCKET_LABELS: Record<string, string> = {
  CLEAN: 'Clean',
  REVIEW: 'Review',
  EXCEPTION: 'Exception',
};

/** The insurer of a renewal by name (the name the server gives, else the catalogue's). */
export function RenewalInsurer({ row }: Readonly<{ row: CandidateRow }>) {
  return row.names?.insurer ? (
    <>{row.names.insurer}</>
  ) : (
    <InsurerName code={row.policy.insurerCode} />
  );
}

/** The owner unit of a renewal by name (the name the server gives, else the organisation's). */
export function RenewalUnit({ row }: Readonly<{ row: CandidateRow }>) {
  return row.names?.ownerUnit ? (
    <>{row.names.ownerUnit}</>
  ) : (
    <SalesUnitName code={row.parties.ownerUnit} />
  );
}

/** The product of a renewal by name (the name kept with the renewal, else the catalogue's). */
export function RenewalProduct({ row }: Readonly<{ row: CandidateRow }>) {
  const p = row.policy;
  if (p.productName) {
    return <span title={p.productCode ?? undefined}>{p.productName}</span>;
  }
  return <ProductName code={p.productCode} />;
}

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
      <div className="stack">
        <div className="grid-2">
          <Kpi label="Done" value={outcome.done.length} />
          <Kpi label="Refused" value={refused.length} accent={refused.length > 0} />
        </div>
        {refused.length > 0 && (
          <DataTable<{ ref: string; reason: string }>
            caption="Renewals refused"
            rows={refused.map(([ref, reason]) => ({ ref, reason }))}
            rowKey={(r) => r.ref}
            columns={[
              { key: 'r', header: 'Renewal', kind: 'code', render: (r) => <code>{r.ref}</code> },
              { key: 'w', header: 'Why It Was Refused', render: (r) => r.reason },
            ]}
          />
        )}
      </div>
    </Modal>
  );
}
