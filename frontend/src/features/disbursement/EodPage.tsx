import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, FileDown, Play } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime, humanize, today } from '@/utils/format';
import { disbursementApi } from './api';
import type { EodRun } from './api';
import { DialogFooter } from './VoucherDialogs';
import './disbursement.css';

const COLUMNS: Column<EodRun>[] = [
  { key: 'no', header: 'Run', render: (r) => r.runNo },
  { key: 'date', header: 'Business Date', render: (r) => formatDate(r.businessDate) },
  { key: 'vouchers', header: 'Vouchers', numeric: true, render: (r) => r.vouchers },
  { key: 'checks', header: 'Checks', numeric: true, render: (r) => r.checks },
  { key: 'credits', header: 'Credits', numeric: true, render: (r) => r.credits },
  { key: 'forms', header: 'Forms', numeric: true, render: (r) => r.forms },
  { key: 'emails', header: 'Confirmations', numeric: true, render: (r) => r.emails },
  { key: 'by', header: 'Run By', render: (r) => `${r.runBy} · ${formatDateTime(r.runAt)}` },
  { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
];

function RunDetails({
  run,
  onConfirmed,
}: Readonly<{ run: EodRun; onConfirmed: (r: EodRun) => void }>) {
  const { can } = useAuth();
  const toast = useToast();
  const download = useFileDownload();
  const confirm = useMutation({
    mutationFn: () => disbursementApi.confirmEod(run.id),
    onSuccess: (r) => {
      toast.success(`${r.runNo}: payment confirmations sent`);
      onConfirmed(r);
    },
  });
  return (
    <Card
      title={`${run.runNo} · ${formatDate(run.businessDate)}`}
      actions={
        run.status === 'COMPLETED' && can('DISB_EOD') ? (
          <Button
            icon={<CheckCircle2 size={16} />}
            busy={confirm.isPending}
            onClick={() => confirm.mutate()}
          >
            Send Confirmations
          </Button>
        ) : undefined
      }
    >
      <div className="stack">
        <ErrorAlert error={confirm.error ?? download.error} />
        {run.message && <p>{run.message}</p>}
        <div className="dsb-outputs">
          {run.outputs.map((o) => (
            <Button
              key={o.id}
              variant="secondary"
              size="sm"
              icon={<FileDown size={16} />}
              busy={download.isPending}
              onClick={() => download.mutate(() => disbursementApi.eodOutput(o.id))}
            >
              {`${humanize(o.kind)} · ${o.fileName} (${o.itemCount})`}
            </Button>
          ))}
        </div>
      </div>
    </Card>
  );
}

function RunDialog({
  onClose,
  onDone,
}: Readonly<{ onClose: () => void; onDone: (r: EodRun) => void }>) {
  const companyId = useCompanyId();
  const [date, setDate] = useState(today());
  const run = useMutation({
    mutationFn: () => disbursementApi.runEod(companyId, date),
    onSuccess: onDone,
  });
  return (
    <Modal
      open
      title="Run End of Day"
      onClose={onClose}
      footer={
        <DialogFooter
          label="Run End of Day"
          busy={run.isPending}
          disabled={date === ''}
          onClose={onClose}
          onConfirm={() => run.mutate()}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={run.error} />
        <p>
          The approved vouchers of the date are frozen; the checks, the credit-to-account (DCTF)
          file, the ATD, MC / DD, credit ticket and TT forms and the end-of-day reports are
          produced.
        </p>
        <Field label="Business Date" required>
          {(id) => (
            <input
              id={id}
              type="date"
              className="input"
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/**
 * Disbursement end of day (DIS 2.9.0-2.9.6, 3.27.x): freezes the approved vouchers of a business
 * date and produces the checks, the DCTF credit file, the bank forms and the end-of-day reports;
 * the payment confirmations are then sent to the payees. The scheduler runs the same steps.
 */
export default function EodPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [opened, setOpened] = useState<EodRun>();
  const [running, setRunning] = useState(false);
  const runs = useQuery({
    queryKey: ['disbursement', 'eod', companyId, page],
    queryFn: () => disbursementApi.eodRuns(companyId, page),
    enabled: companyId > 0,
  });
  const changed = async (r: EodRun) => {
    setOpened(r);
    await queryClient.invalidateQueries({ queryKey: ['disbursement', 'eod'] });
  };
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Disbursement"
        title="End of Day"
        description="Checks, credit files, bank forms and reports of the approved vouchers of a day, and the payment confirmations."
        actions={
          can('DISB_EOD') ? (
            <Button variant="accent" icon={<Play size={16} />} onClick={() => setRunning(true)}>
              Run End of Day
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={runs.error} />
      <Card title="Runs" flush>
        <DataTable
          caption="End-of-day runs"
          columns={COLUMNS}
          rows={runs.data?.content ?? []}
          rowKey={(r) => r.id}
          loading={runs.isLoading}
          onRowClick={setOpened}
          emptyMessage="No end of day run yet"
        />
        <PageFooter data={runs.data} noun="runs" onPage={setPage} />
      </Card>
      {opened !== undefined && (
        <RunDetails
          key={`${opened.id}-${opened.status}`}
          run={opened}
          onConfirmed={(r) => void changed(r)}
        />
      )}
      {running && (
        <RunDialog
          onClose={() => setRunning(false)}
          onDone={(r) => {
            setRunning(false);
            toast.success(`${r.runNo} completed`);
            void changed(r);
          }}
        />
      )}
    </div>
  );
}
