import type { CaseRow } from '@/api/csfCases';
import { Tag } from '@/components/ui/Tag';
import { slaTone } from './caseCodes';

/** The SLA indicator and the escalation of a case. */
export function SlaTags({ row }: Readonly<{ row: CaseRow }>) {
  return (
    <>
      <Tag tone={slaTone(row.sla)}>{row.sla.label}</Tag>
      {row.escalated && (
        <Tag tone="danger" title={row.escalated.reason.label}>
          Escalated
        </Tag>
      )}
    </>
  );
}
