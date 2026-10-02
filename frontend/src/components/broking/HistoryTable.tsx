import { ArrowDown, ArrowUp } from 'lucide-react';
import { useState } from 'react';
import type { HistoryEntry } from '@/api/workflow';
import { EmptyState } from '@/components/ui/EmptyState';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useDisplayName, useRoleName } from '@/components/ui/useDisplayName';
import { formatDateTime, humanize, titleCase } from '@/utils/format';
import { historyRows } from './historyRows';

interface HistoryTableProps {
  /** Status history, oldest first (as the workflow API returns it). */
  history: HistoryEntry[];
  /** Whether the record's current stage is final (no running duration for the last row). */
  terminal?: boolean;
  /** Accessible name of the table. */
  label?: string;
  /** The name of a reason code of the record's list of reasons; the code in words by default. */
  reasonLabel?: (code: string) => string;
}

function stageLabel(code: string | undefined, name: string | undefined): string {
  if (name) {
    return titleCase(name);
  }
  return code ? humanize(code) : '';
}

function remarks(entry: HistoryEntry, reasonLabel: (code: string) => string): string {
  const parts = [entry.reasonCode ? reasonLabel(entry.reasonCode) : '', entry.comment ?? ''];
  return parts.filter((p) => p !== '').join(' · ');
}

/**
 * Workflow and status history as a table (BDO): Stage (status pill), From Stage, Action, By (user
 * and role), Date and Time, Remarks and Duration in Stage. Newest first, with a sort toggle on the
 * date column. Replaces the free-text timeline on every record page.
 */
export function HistoryTable({
  history,
  terminal = false,
  label = 'Status history',
  reasonLabel = humanize,
}: Readonly<HistoryTableProps>) {
  const [newestFirst, setNewestFirst] = useState(true);
  const name = useDisplayName();
  const role = useRoleName();
  if (history.length === 0) {
    return <EmptyState message="No history recorded" />;
  }
  const rows = historyRows(history, terminal);
  if (newestFirst) {
    rows.reverse();
  }
  const SortIcon = newestFirst ? ArrowDown : ArrowUp;
  return (
    <div className="table-wrap history-table">
      <table className="table" aria-label={label}>
        <thead>
          <tr>
            <th className="col-status">Stage</th>
            <th>From Stage</th>
            <th>Action</th>
            <th>By</th>
            <th className="num col-datetime" aria-sort={newestFirst ? 'descending' : 'ascending'}>
              <button
                type="button"
                className="th-sort"
                onClick={() => setNewestFirst((v) => !v)}
                aria-label={
                  newestFirst ? 'Date and time, newest first' : 'Date and time, oldest first'
                }
              >
                Date and Time <SortIcon size={14} aria-hidden="true" />
              </button>
            </th>
            <th>Remarks</th>
            <th className="num">Duration in Stage</th>
          </tr>
        </thead>
        <tbody>
          {rows.map(({ entry, duration, order }) => {
            const actorRole = entry.automatic ? 'Automatic step' : role(entry.actor);
            return (
              <tr key={`${entry.occurredAt}-${String(order)}`}>
                <td className="col-status">
                  <StatusBadge
                    full
                    status={entry.toStage}
                    label={stageLabel(entry.toStage, entry.toStageName)}
                  />
                </td>
                <td>
                  {entry.fromStage ? (
                    stageLabel(entry.fromStage, entry.fromStageName)
                  ) : (
                    <span className="muted">—</span>
                  )}
                </td>
                <td>{humanize(entry.action)}</td>
                <td>
                  <span className="cell-stack" title={entry.actor}>
                    <span>{entry.automatic ? 'System' : name(entry.actor)}</span>
                    {actorRole && <span className="muted">{actorRole}</span>}
                  </span>
                </td>
                <td className="num nowrap">{formatDateTime(entry.occurredAt)}</td>
                <td className="cell-remarks">
                  {remarks(entry, reasonLabel) || <span className="muted">—</span>}
                </td>
                <td className="num nowrap">{duration || <span className="muted">—</span>}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
