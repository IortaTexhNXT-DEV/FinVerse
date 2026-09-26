import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Plus } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { diaryApi, mayComplete } from './api';
import type { DiaryEntry, DiaryInput } from './api';
import { DiaryDialog } from './DiaryDialog';

/**
 * Diary tab of a claim (BRCLM.022, FR-CM-052): calls, e-mails, meetings, notes and follow-ups with
 * due date and assignee; the assignee or author marks an entry done. Closed claims accept entries;
 * nothing is deleted. Mounted by the claim record page (wave CL1-A).
 */
export function DiaryTab({ claimId, companyId }: Readonly<{ claimId: number; companyId: number }>) {
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [adding, setAdding] = useState(false);
  const entries = useQuery({
    queryKey: ['broker-claims', 'diary', claimId],
    queryFn: () => diaryApi.entries(claimId, companyId),
  });
  const refresh = async (message: string) => {
    await queryClient.invalidateQueries({ queryKey: ['broker-claims'] });
    toast.success(message);
  };
  const add = useMutation({
    mutationFn: (input: DiaryInput) => diaryApi.add(claimId, companyId, input),
    onSuccess: async () => {
      setAdding(false);
      await refresh('Diary entry added');
    },
  });
  const done = useMutation({
    mutationFn: (id: number) => diaryApi.done(id, companyId),
    onSuccess: () => refresh('Diary entry done'),
  });
  return (
    <Card
      title="Diary"
      flush
      actions={
        can('BCL_RECORD') ? (
          <Button size="sm" icon={<Plus size={14} />} onClick={() => setAdding(true)}>
            Add Diary Entry
          </Button>
        ) : undefined
      }
    >
      <ErrorAlert error={entries.error ?? done.error} />
      <DataTable<DiaryEntry>
        caption="Diary entries"
        loading={entries.isLoading}
        rows={entries.data ?? []}
        rowKey={(e) => e.id}
        emptyMessage="No diary entries yet"
        columns={[
          { key: 'date', header: 'Date', render: (e) => formatDate(e.entryDate) },
          { key: 'type', header: 'Type', render: (e) => e.typeLabel },
          { key: 'text', header: 'Text', render: (e) => e.text },
          { key: 'due', header: 'Due', render: (e) => formatDate(e.dueDate) },
          { key: 'who', header: 'Assignee', render: (e) => e.assignee ?? e.createdBy },
          {
            key: 'state',
            header: 'Status',
            render: (e) =>
              e.doneAt === undefined ? (
                <StatusBadge status="OPEN" />
              ) : (
                <span title={e.doneRemark}>
                  Done {formatDateTime(e.doneAt)} by {e.doneBy}
                </span>
              ),
          },
          {
            key: 'act',
            header: '',
            render: (e) =>
              mayComplete(e, user?.username) && (
                <Button
                  size="sm"
                  variant="ghost"
                  icon={<CheckCircle2 size={14} />}
                  busy={done.isPending && done.variables === e.id}
                  onClick={() => done.mutate(e.id)}
                >
                  Mark Done
                </Button>
              ),
          },
        ]}
      />
      {adding && (
        <DiaryDialog
          busy={add.isPending}
          error={add.error}
          onClose={() => setAdding(false)}
          onSave={(input) => add.mutate(input)}
        />
      )}
    </Card>
  );
}
