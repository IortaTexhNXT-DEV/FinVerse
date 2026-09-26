import { AlarmClock } from 'lucide-react';
import type { RequestListItem } from '@/api/productmaint';
import { daysInStage, slaState } from './packageRequest';

/** Days a request has been in its stage, flagged red when past its SLA (BRPM.021). */
export function StageAge({ item }: Readonly<{ item: RequestListItem }>) {
  const now = new Date();
  const days = daysInStage(item.stageEnteredAt, now);
  const sla = slaState(item.dueAt, now);
  if (days === undefined) {
    return <>—</>;
  }
  return (
    <span className={sla === 'overdue' ? 'text-danger' : undefined}>
      {days} {days === 1 ? 'day' : 'days'}
      {sla !== 'ok' && (
        <span className="muted">
          {' '}
          <AlarmClock size={12} aria-hidden="true" /> {sla === 'overdue' ? 'overdue' : 'due soon'}
        </span>
      )}
    </span>
  );
}
