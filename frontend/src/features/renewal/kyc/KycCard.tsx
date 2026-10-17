import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { KYC_STATUS, renewalProposalApi } from '@/api/renewalProposal';
import type { KycMonitoringView } from '@/api/renewalProposal';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';

function KycForm({ renewalRef }: Readonly<{ renewalRef: string }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState('PENDING_FOLLOW_UP');
  const [activity, setActivity] = useState('');
  const [remarks, setRemarks] = useState('');
  const save = useMutation({
    mutationFn: () =>
      renewalProposalApi.recordKyc(companyId, renewalRef, { status, activity, remarks }),
    onSuccess: async () => {
      toast.success('KYC activity recorded');
      setActivity('');
      setRemarks('');
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'kyc'] });
    },
  });
  return (
    <>
      <ErrorAlert error={save.error} />
      <div className="rnw-actions">
        <Field label="Status">
          {(id) => (
            <select
              id={id}
              className="select"
              value={status}
              onChange={(e) => setStatus(e.target.value)}
            >
              {Object.entries(KYC_STATUS).map(([code, text]) => (
                <option key={code} value={code}>
                  {text}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Activity">
          {(id) => (
            <input
              id={id}
              className="input"
              value={activity}
              onChange={(e) => setActivity(e.target.value)}
            />
          )}
        </Field>
        <Field label="Remarks or follow-up note">
          {(id) => (
            <input
              id={id}
              className="input"
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          )}
        </Field>
        <Button
          busy={save.isPending}
          disabled={activity.trim() === '' && remarks.trim() === ''}
          onClick={() => save.mutate()}
        >
          Record
        </Button>
      </div>
    </>
  );
}

function KycHistory({ history }: Readonly<{ history: KycMonitoringView['history'] }>) {
  return (
    <ul className="rnw-history">
      {history.map((h) => (
        <li key={`${h.created_at}-${h.status}`}>
          {formatDateTime(h.created_at)} · {KYC_STATUS[h.status] ?? h.status} ·{' '}
          {[h.activity, h.remarks].filter(Boolean).join(' - ')} · {h.created_by}
        </li>
      ))}
    </ul>
  );
}

/**
 * The KYC monitoring of a renewal account (FRRN.039.01, FRRN.039.02): its KYC status and review
 * date, the activities, remarks and follow-up notes recorded, and the completion of the review.
 */
export function KycCard({ renewalRef }: Readonly<{ renewalRef: string }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const kyc = useQuery({
    queryKey: ['renewal', 'kyc', companyId, renewalRef],
    queryFn: () => renewalProposalApi.kyc(companyId, renewalRef),
  });
  const current = kyc.data?.status ?? 'NONE';
  const text = KYC_STATUS[current] ?? 'Not identified';
  return (
    <Card title="KYC Monitoring">
      <ErrorAlert error={kyc.error} />
      <p>
        <StatusBadge status={current} label={text} full /> KYC review date:{' '}
        {formatDate(kyc.data?.kycReviewDue ?? null)}
      </p>
      {(can('RNW_DISPOSE') || can('RNW_PROCESS')) && <KycForm renewalRef={renewalRef} />}
      <KycHistory history={kyc.data?.history ?? []} />
    </Card>
  );
}
