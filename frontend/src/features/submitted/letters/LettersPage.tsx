import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Send } from 'lucide-react';
import { submittedApi } from '@/api/submitted';
import type { LetterView, PrintBatchView } from '@/api/submitted';
import { LovLabel } from '@/components/broking/LovLabel';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmButton } from '@/components/ui/ConfirmButton';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useTabParam } from '@/components/ui/useTabParam';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { SBM_LOV, SUBMITTED_SECTION } from '../common/submittedCodes';
import { letterColumns } from '../common/letterColumns';

const TABS = [
  { id: 'FAILED', label: 'Refused', statuses: ['FAILED'] },
  { id: 'SENT', label: 'Sent', statuses: ['SENT', 'GENERATED', 'QUEUED'] },
  { id: 'PRINTED', label: 'Printed', statuses: ['PRINTED'] },
  { id: 'BATCHES', label: 'Print Batches', statuses: [] as string[] },
] as const;

type TabId = (typeof TABS)[number]['id'];

function Batches() {
  const companyId = useCompanyId();
  const download = useFileDownload();
  const batches = useQuery({
    queryKey: ['submitted', 'print-batches', companyId],
    queryFn: () => submittedApi.printBatches(companyId),
    enabled: companyId > 0,
  });
  return (
    <Card flush>
      <ErrorAlert error={batches.error ?? download.error} />
      <DataTable<PrintBatchView>
        loading={batches.isLoading}
        rows={batches.data?.content ?? []}
        rowKey={(b) => b.id}
        emptyMessage="No print batch yet"
        columns={[
          { key: 'no', header: 'Batch', kind: 'code', render: (b) => b.batchNo },
          {
            key: 'type',
            header: 'Letter',
            render: (b) => <LovLabel type={SBM_LOV.letterType} code={b.letterType} />,
          },
          {
            key: 'date',
            header: 'Batch Date',
            kind: 'date',
            render: (b) => formatDate(b.batchDate),
          },
          { key: 'count', header: 'Letters', kind: 'amount', render: (b) => b.letterCount },
          { key: 'to', header: 'Handed To', render: (b) => b.handedTo ?? '—' },
          {
            key: 'at',
            header: 'Handed Over',
            kind: 'datetime',
            render: (b) => formatDateTime(b.handedAt),
          },
          {
            key: 'files',
            header: 'Files',
            render: (b) => (
              <span className="form-actions">
                <Button
                  variant="ghost"
                  onClick={() => download.mutate(() => submittedApi.printBatchFile(b.id, 'merged'))}
                >
                  Letters PDF
                </Button>
                <Button
                  variant="ghost"
                  onClick={() =>
                    download.mutate(() => submittedApi.printBatchFile(b.id, 'control'))
                  }
                >
                  Control List
                </Button>
              </span>
            ),
          },
        ]}
      />
    </Card>
  );
}

/**
 * Letters & Print Batches (FR-SP-065, 066): the letters of the letter rules by status, sending a
 * refused letter again, the dispatch started by hand and the print batches of the mail house.
 */
export default function LettersPage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const download = useFileDownload();
  const [tab, setTab] = useTabParam<TabId>(
    TABS.map((t) => t.id),
    'FAILED',
  );
  const statuses = TABS.find((t) => t.id === tab)?.statuses ?? [];
  const letters = useQuery({
    queryKey: ['submitted', 'letters', companyId, tab],
    queryFn: () => submittedApi.letters(companyId, statuses),
    enabled: companyId > 0 && tab !== 'BATCHES',
  });
  const dispatch = useMutation({
    mutationFn: () => submittedApi.dispatch(companyId),
    onSuccess: (d) => {
      toast.success(`${String(d.letters)} letters sent, ${String(d.printBatches)} print batches`);
      void queryClient.invalidateQueries({ queryKey: ['submitted'] });
    },
  });
  const resend = useMutation({
    mutationFn: (id: number) => submittedApi.resend(id),
    onSuccess: (l) => {
      toast.success(`${l.letterNo} sent again`);
      void queryClient.invalidateQueries({ queryKey: ['submitted', 'letters'] });
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section={SUBMITTED_SECTION}
        title="Letters & Print Batches"
        description="Reminders, renewal notices and proposals of the letter rules, and the mail house batches."
        actions={
          <ConfirmButton
            icon={<Send size={16} />}
            confirm={{
              title: 'Send Due Letters',
              effect:
                'The letters due by the letter rules are sent now and the printed ones are handed to the mail house.',
            }}
            onConfirm={() => dispatch.mutateAsync()}
          >
            Send Due Letters
          </ConfirmButton>
        }
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <ErrorAlert error={letters.error ?? dispatch.error ?? resend.error ?? download.error} />
      {tab === 'BATCHES' ? (
        <Batches />
      ) : (
        <Card flush>
          <DataTable<LetterView>
            loading={letters.isLoading}
            rows={letters.data?.content ?? []}
            rowKey={(l) => l.id}
            emptyMessage="No letter in this tab"
            columns={[
              { key: 'sbm', header: 'Masterlist No.', kind: 'code', render: (l) => l.sbmNo ?? '—' },
              ...letterColumns((id) => download.mutate(() => submittedApi.letterPdf(id))),
              {
                key: 'resend',
                header: 'Actions',
                render: (l) =>
                  l.status === 'FAILED' ? (
                    <ConfirmButton
                      variant="ghost"
                      confirm={{
                        title: `Send ${l.letterNo} Again`,
                        record: l.sbmNo ?? undefined,
                        effect: 'The letter is written again with the current address and sent.',
                      }}
                      onConfirm={() => resend.mutateAsync(l.id)}
                    >
                      Send Again
                    </ConfirmButton>
                  ) : null,
              },
            ]}
          />
        </Card>
      )}
    </div>
  );
}
