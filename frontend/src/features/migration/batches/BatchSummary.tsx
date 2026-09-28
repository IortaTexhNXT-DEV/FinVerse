import type { Batch } from '@/api/migration';
import { Card } from '@/components/ui/Card';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { Kpi } from '@/components/ui/Kpi';
import { formatDate, formatDateTime } from '@/utils/format';
import { BATCH_STEPS, percent, stepDone } from '../common/migrationCodes';
import type { ReactNode } from 'react';
import { UserName } from '@/components/ui/UserName';

function byAt(user: string | undefined, at: string | undefined): ReactNode {
  return user === undefined ? undefined : (
    <>
      <UserName login={user} />, {formatDateTime(at)}
    </>
  );
}

function purge(b: Batch): string {
  return b.purgedAt === undefined ? formatDate(b.purgeDueOn) : formatDateTime(b.purgedAt);
}

/** The steps timeline and the count tiles of a batch. */
export function BatchProgress({ batch: b }: Readonly<{ batch: Batch }>) {
  const c = b.counts;
  return (
    <>
      <ol className="mig-steps" aria-label="Steps">
        {BATCH_STEPS.map((s) => (
          <li key={s.id} className={stepDone(b.status, s.id) ? 'done' : undefined}>
            {s.label}
          </li>
        ))}
      </ol>
      <div className="grid-4">
        <Kpi label="Staged" value={c.staged} />
        <Kpi
          label="Valid"
          value={c.valid + c.warning}
          hint={`${String(c.warning)} with warnings`}
        />
        <Kpi
          label="Invalid"
          value={c.invalid}
          hint={`${String(c.excluded)} excluded, ${String(c.waived)} waived`}
        />
        <Kpi
          label="Loaded"
          value={c.loaded}
          hint={`${String(c.skipped)} unchanged, ${String(c.rejected)} rejected`}
        />
      </div>
    </>
  );
}

/** The facts of a batch: extracts, code map versions, operators and times. */
export function BatchSummary({ batch: b }: Readonly<{ batch: Batch }>) {
  return (
    <Card title="Batch">
      <DefinitionGrid
        columns={2}
        items={[
          { label: 'Extracts', value: b.extractNos.join(', ') },
          { label: 'Rerun of', value: b.parentBatchNo },
          {
            label: 'Code map versions',
            value: Object.entries(b.mapVersions)
              .map(([set, v]) => set + ' v' + String(v))
              .join(', '),
            wide: true,
          },
          { label: 'Error rate', value: percent(b.errorRate) },
          { label: 'Unmapped codes', value: String(b.unmappedCount) },
          { label: 'Client pairs to review', value: String(b.reviewCount) },
          { label: 'Validated by', value: byAt(b.validatedBy, b.validatedAt) },
          { label: 'Load approved by', value: byAt(b.loadApprovedBy, b.loadApprovedAt) },
          {
            label: 'Loaded by',
            value: b.loadedBy === undefined ? undefined : <UserName login={b.loadedBy} />,
          },
          { label: 'Load started', value: formatDateTime(b.startedAt) },
          { label: 'Load ended', value: formatDateTime(b.endedAt) },
          { label: 'Accepted', value: formatDateTime(b.signedOffAt) },
          { label: 'Rollback reason', value: b.rollbackReason, wide: true },
          { label: 'Staging purged on', value: purge(b) },
        ]}
      />
    </Card>
  );
}
