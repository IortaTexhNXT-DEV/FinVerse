import type { Checklist, CheckItem } from '@/api/closing';
import { DataTable } from '@/components/ui/DataTable';

/** Pass / fail badge of a checklist control (non-blocking failures show as a warning). */
export function CheckBadge({ item }: Readonly<{ item: CheckItem }>) {
  if (item.passed) {
    return <span className="badge success">Pass</span>;
  }
  if (!item.blocking) {
    return <span className="badge warning">Warning</span>;
  }
  return <span className="badge danger">Fail</span>;
}

/** Readiness message: blocked, ready with warnings to review, or ready. */
function readiness(checklist: Checklist): string {
  if (!checklist.ready) {
    return 'Closing is blocked until every failed control is resolved.';
  }
  if (checklist.items.some((i) => !i.passed)) {
    return 'Ready to close: no blocking control failed; review the warnings below.';
  }
  return 'All controls passed: ready to close.';
}

/** Checklist table with an overall readiness banner. */
export function ChecklistView({
  checklist,
  loading,
}: Readonly<{ checklist?: Checklist; loading: boolean }>) {
  return (
    <div className="stack">
      {checklist && (
        <div className={`alert ${checklist.ready ? 'success' : 'warning'}`} role="status">
          {readiness(checklist)}
        </div>
      )}
      <DataTable<CheckItem>
        loading={loading}
        rows={checklist?.items ?? []}
        rowKey={(i) => i.code}
        columns={[
          { key: 's', header: 'Result', width: '90px', render: (i) => <CheckBadge item={i} /> },
          { key: 'l', header: 'Control', render: (i) => <strong>{i.label}</strong> },
          { key: 'd', header: 'Detail', render: (i) => i.detail },
        ]}
      />
    </div>
  );
}
