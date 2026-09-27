import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { renewalApi } from '@/api/renewal';
import type { CorrectionView } from '@/api/renewal';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { TextDialog } from '../common/ActionDialogs';
import { UploadPanel } from '../common/UploadPanel';

function TakeOver() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [date, setDate] = useState('');
  const source = useQuery({ queryKey: ['renewal', 'source'], queryFn: renewalApi.source });
  const run = useMutation({
    mutationFn: () => renewalApi.goLive(companyId, date),
    onSuccess: async (r) => {
      toast.success(`${String(r.counts.created)} renewal(s) taken over`);
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  return (
    <Card title="Go-live take-over">
      <p className="muted">
        Takes over the renewals of the migrated policies expiring from the go-live date within the
        renewal horizon. Migrated policies source:{' '}
        <strong>{source.data?.connected === true ? 'connected' : 'loaded by upload'}</strong>.
      </p>
      <ErrorAlert error={run.error ?? source.error} />
      <div className="form-grid">
        <Field label="Go-live Date" required>
          {(id) => <DateInput id={id} value={date} onChange={(e) => setDate(e.target.value)} />}
        </Field>
      </div>
      <Button busy={run.isPending} disabled={date === ''} onClick={() => run.mutate()}>
        Run Take-over
      </Button>
    </Card>
  );
}

function Corrections() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [deciding, setDeciding] = useState<{ row: CorrectionView; approve: boolean }>();
  const rows = useQuery({
    queryKey: ['renewal', 'corrections', companyId],
    queryFn: () => renewalApi.corrections(companyId),
    enabled: companyId > 0,
  });
  const decide = useMutation({
    mutationFn: ({ id, approve, remarks }: { id: number; approve: boolean; remarks: string }) =>
      renewalApi.decideCorrection(id, approve, remarks || undefined),
    onSuccess: async (c) => {
      setDeciding(undefined);
      toast.success(`Correction ${c.status.toLowerCase()}`);
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  return (
    <Card title="Renewal Advices already sent: corrections to approve" flush>
      <ErrorAlert error={rows.error} />
      <DataTable<CorrectionView>
        loading={rows.isLoading}
        rows={rows.data ?? []}
        rowKey={(r) => r.id}
        emptyMessage="No corrections waiting"
        columns={[
          { key: 'ref', header: 'Legacy Reference', kind: 'code', render: (r) => r.legacyRef },
          { key: 'date', header: 'RA Date', kind: 'date', render: (r) => formatDate(r.raDate) },
          { key: 'corr', header: 'Correction', render: (r) => r.correction },
          {
            key: 'job',
            header: 'Upload',
            render: (r) => (r.jobNo ? `${r.jobNo} row ${String(r.rowNo ?? '')}` : ''),
          },
          { key: 'by', header: 'Prepared by', render: (r) => <UserName login={r.preparedBy} /> },
          {
            key: 'act',
            header: '',
            render: (r) => (
              <span className="rnw-actions">
                <Button size="sm" onClick={() => setDeciding({ row: r, approve: true })}>
                  Approve
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => setDeciding({ row: r, approve: false })}
                >
                  Reject
                </Button>
              </span>
            ),
          },
        ]}
      />
      {deciding !== undefined && (
        <TextDialog
          title={`${deciding.approve ? 'Approve' : 'Reject'} correction of ${deciding.row.legacyRef}`}
          label="Remarks"
          confirmLabel={deciding.approve ? 'Approve' : 'Reject'}
          required={!deciding.approve}
          busy={decide.isPending}
          error={decide.error}
          onClose={() => setDeciding(undefined)}
          onConfirm={(remarks) =>
            decide.mutate({ id: deciding.row.id, approve: deciding.approve, remarks })
          }
        />
      )}
    </Card>
  );
}

/**
 * Go-live (DMQ37, DMQ38): the take-over of the renewals of the migrated policies, the upload of
 * the migrated policies when the migration source is not connected, and the Renewal Advices sent
 * by hand before go-live, recorded so they are never sent again.
 */
export function GoLiveTab() {
  const { can } = useAuth();
  const [upload, setUpload] = useState<'' | 'legacy' | 'ra'>('');
  return (
    <div className="stack">
      {can('RNW_SETUP') && <TakeOver />}
      <div className="rnw-actions">
        {can('RNW_EXTRACT') && (
          <Button variant="secondary" onClick={() => setUpload('legacy')}>
            Upload Migrated Policies
          </Button>
        )}
        {can('RNW_RA_SEND') && (
          <Button variant="secondary" onClick={() => setUpload('ra')}>
            Upload RAs Already Sent
          </Button>
        )}
      </div>
      {upload === 'legacy' && (
        <UploadPanel
          label="Upload Migrated Policies"
          handler="RNW_LEGACY_POLICIES"
          onClose={() => setUpload('')}
        />
      )}
      {upload === 'ra' && (
        <UploadPanel
          label="Upload RAs Already Sent"
          handler="RNW_RA_ALREADY_SENT"
          onClose={() => setUpload('')}
        />
      )}
      {can('RNW_RA_SEND') && <Corrections />}
    </div>
  );
}
