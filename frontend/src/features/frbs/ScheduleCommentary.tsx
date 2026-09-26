import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Save } from 'lucide-react';
import { useState } from 'react';
import type { ReportResult } from '@/api/reports';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { frbsApi } from './api';
import { periodOf } from './schedules';

interface Row {
  code: string;
  name: string;
}

/**
 * Commentary of a variance analysis (Appendix A II-45, III): one comment per row and month, kept
 * by FRBS and printed in the schedule's commentary column; a blank comment removes it.
 */
export function ScheduleCommentary({
  code,
  asOf,
  result,
  onSaved,
}: Readonly<{ code: string; asOf: string; result: ReportResult; onSaved: () => void }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const period = periodOf(asOf);
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const comments = useQuery({
    queryKey: ['frbs', 'comments', code, companyId, period],
    queryFn: () => frbsApi.comments(code, companyId, period),
    enabled: companyId > 0,
  });
  const save = useMutation({
    mutationFn: (rowKey: string) =>
      frbsApi.comment(code, { companyId, period, rowKey, text: drafts[rowKey] ?? '' }),
    onSuccess: async (_saved, rowKey) => {
      toast.success(`Comment on ${rowKey} saved`);
      await queryClient.invalidateQueries({ queryKey: ['frbs', 'comments', code] });
      onSaved();
    },
  });
  const current = new Map((comments.data ?? []).map((c) => [c.rowKey, c.text]));
  const rows: Row[] = result.rows
    .filter((r) => r.kind === 'DETAIL')
    .map((r) => ({ code: String(r.cells.code ?? ''), name: String(r.cells.name ?? '') }));
  const editable = can('FRBS_REPORT_EXPORT');
  const columns: Column<Row>[] = [
    {
      key: 'row',
      header: 'Row',
      render: (r) => (
        <>
          <strong>{r.code}</strong>
          <span className="cell-sub">{r.name}</span>
        </>
      ),
    },
    {
      key: 'comment',
      header: `Commentary · ${period}`,
      render: (r) =>
        editable ? (
          <input
            className="input frbs-comment"
            aria-label={`Commentary on ${r.code}`}
            maxLength={1000}
            value={drafts[r.code] ?? current.get(r.code) ?? ''}
            onChange={(e) => setDrafts((d) => ({ ...d, [r.code]: e.target.value }))}
          />
        ) : (
          (current.get(r.code) ?? '—')
        ),
    },
    {
      key: 'save',
      header: '',
      render: (r) =>
        editable && drafts[r.code] !== undefined ? (
          <Button
            size="sm"
            icon={<Save size={14} />}
            busy={save.isPending && save.variables === r.code}
            onClick={() => save.mutate(r.code)}
          >
            Save Comment
          </Button>
        ) : null,
    },
  ];
  return (
    <Card title="Commentary" flush>
      <ErrorAlert error={comments.error ?? save.error} />
      <DataTable
        caption="Commentary per row"
        columns={columns}
        rows={rows}
        rowKey={(r) => r.code}
        loading={comments.isLoading}
        emptyMessage="No rows to comment on"
      />
    </Card>
  );
}
