import { Bot, UserRound } from 'lucide-react';
import type { HistoryEntry } from '@/api/workflow';
import { formatDateTime, humanize } from '@/utils/format';

/**
 * Status history of a record (BRNB.022/115): every stage change with who (or "system"), when,
 * the reason and the comment, newest first.
 */
export function StageTimeline({ history }: Readonly<{ history: HistoryEntry[] }>) {
  if (history.length === 0) {
    return <p className="muted">No history yet.</p>;
  }
  const entries = [...history].reverse();
  return (
    <ol className="timeline" aria-label="Status history">
      {entries.map((h, i) => (
        <li
          key={`${h.occurredAt}-${i}`}
          className={i === 0 ? 'timeline-item current' : 'timeline-item'}
        >
          <span className="timeline-dot" aria-hidden="true" />
          <div className="timeline-body">
            <div className="timeline-title">
              <strong>{h.toStageName ?? humanize(h.toStage)}</strong>
              {h.fromStage && (
                <span className="muted"> from {h.fromStageName ?? humanize(h.fromStage)}</span>
              )}
            </div>
            <div className="timeline-meta muted">
              {h.automatic ? (
                <Bot size={12} aria-label="System action" />
              ) : (
                <UserRound size={12} aria-hidden="true" />
              )}{' '}
              {h.automatic ? `System (${h.actor})` : h.actor} · {formatDateTime(h.occurredAt)} ·{' '}
              {humanize(h.action)}
            </div>
            {h.reasonCode && (
              <div className="timeline-reason">Reason: {humanize(h.reasonCode)}</div>
            )}
            {h.comment && <div className="timeline-comment">“{h.comment}”</div>}
          </div>
        </li>
      ))}
    </ol>
  );
}
