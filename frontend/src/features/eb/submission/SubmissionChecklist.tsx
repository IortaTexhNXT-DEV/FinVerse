import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { ebMarketApi } from '@/api/ebMarket';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';

export interface ChecklistScope {
  programmeId: number;
  cycleId?: number;
  memberChangeId?: number;
  processType: string;
}

/**
 * The required documents of a process with whether each is on file; the documents on file are
 * selected for the submission by default.
 */
export function SubmissionChecklist({
  scope,
  selected,
  onSelect,
}: Readonly<{ scope: ChecklistScope; selected: number[]; onSelect: (ids: number[]) => void }>) {
  const companyId = useCompanyId();
  const checklist = useQuery({
    queryKey: ['eb', 'checklist', scope],
    queryFn: () => ebMarketApi.checklist(companyId, scope),
  });
  const data = checklist.data;
  useEffect(() => {
    if (data) {
      onSelect(data.flatMap((i) => i.attachmentIds));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- select once per loaded checklist
  }, [data]);
  const toggle = (ids: number[], on: boolean) =>
    onSelect(on ? [...new Set([...selected, ...ids])] : selected.filter((s) => !ids.includes(s)));
  return (
    <div className="stack">
      <ErrorAlert error={checklist.error} onRetry={() => void checklist.refetch()} />
      {data?.length === 0 && <p className="muted">No document on file for this process</p>}
      <div className="eb-checklist">
        {(data ?? []).map((i) => (
          <label key={i.documentType} className="checkbox">
            <input
              type="checkbox"
              disabled={!i.present}
              checked={i.present && i.attachmentIds.every((a) => selected.includes(a))}
              onChange={(e) => toggle(i.attachmentIds, e.target.checked)}
            />
            {i.label}
            {i.mandatory && <span className="muted"> (required)</span>}{' '}
            <StatusBadge
              status={i.present ? 'ON_FILE' : 'MISSING'}
              tone={i.present ? 'success' : undefined}
            />
          </label>
        ))}
      </div>
    </div>
  );
}
