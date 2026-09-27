import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { submittedApi } from '@/api/submitted';
import type { PolicyDetail, ReviewView } from '@/api/submitted';
import { useAuth } from '@/auth/authContext';
import { LovLabels } from '@/components/broking/LovLabel';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { UserName } from '@/components/ui/UserName';
import { formatDate, today } from '@/utils/format';
import { ApprovalPanel } from '../common/ApprovalPanel';
import { SBM_LOV } from '../common/submittedCodes';
import { IaafActions } from '../reviews/IaafActions';

function ReviewForm({ policyId, onDone }: Readonly<{ policyId: number; onDone: () => void }>) {
  const toast = useToast();
  const [reviewDate, setReviewDate] = useState(today());
  const [adequacy, setAdequacy] = useState('ADEQUATE');
  const [finding, setFinding] = useState('');
  const [remarks, setRemarks] = useState('');
  const [touched, setTouched] = useState(false);
  const missingFinding = adequacy === 'WITH_FINDINGS' && finding === '';
  const save = useMutation({
    mutationFn: () =>
      submittedApi.review(policyId, {
        reviewDate,
        adequacy,
        findings: finding === '' ? [] : [finding],
        remarks,
      }),
    onSuccess: () => {
      toast.success('Review recorded');
      setRemarks('');
      setFinding('');
      onDone();
    },
  });
  return (
    <Card title="Record a Review">
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <Field label="Review Date" required>
          {(id) => (
            <DateInput id={id} value={reviewDate} onChange={(e) => setReviewDate(e.target.value)} />
          )}
        </Field>
        <Field label="Result" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={adequacy}
              onChange={(e) => setAdequacy(e.target.value)}
            >
              <option value="ADEQUATE">Adequate</option>
              <option value="WITH_FINDINGS">With findings</option>
            </select>
          )}
        </Field>
        <Field
          label="Finding"
          required={adequacy === 'WITH_FINDINGS'}
          error={touched && missingFinding ? 'Select the finding' : undefined}
        >
          {(id) => (
            <LovSelect
              id={id}
              type={SBM_LOV.finding}
              value={finding}
              placeholder="No finding"
              onChange={(v) => {
                setFinding(v);
                setTouched(true);
              }}
            />
          )}
        </Field>
        <Field label="Remarks">
          {(id) => (
            <textarea
              id={id}
              className="input"
              rows={2}
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          )}
        </Field>
      </div>
      <div className="form-actions">
        <Button
          disabled={save.isPending}
          onClick={() => {
            setTouched(true);
            if (!missingFinding) {
              save.mutate();
            }
          }}
        >
          Record Review
        </Button>
      </div>
    </Card>
  );
}

function IaafSection({
  policyId,
  lastAdequate,
}: Readonly<{ policyId: number; lastAdequate: boolean }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['submitted'] });
  const iaaf = useQuery({
    queryKey: ['submitted', 'iaaf-of', policyId],
    queryFn: () => submittedApi.iaafOf(policyId),
  });
  const generate = useMutation({
    mutationFn: () => submittedApi.generateIaaf(policyId),
    onSuccess: (i) => {
      toast.success(`IAAF ${i.iaafNo} generated`);
      refresh();
    },
  });
  const current = iaaf.data?.[0];
  if (current) {
    return (
      <>
        <Card
          title={`IAAF ${current.iaafNo}`}
          actions={<IaafActions iaaf={current} onDone={refresh} />}
        >
          <p>
            {current.sentTo
              ? `Sent to ${current.sentTo} on ${formatDate(current.sentAt)}.`
              : 'Not sent yet.'}
          </p>
        </Card>
        <ApprovalPanel approval={current.approval} />
      </>
    );
  }
  if (!can('IAAF_PREPARE')) {
    return null;
  }
  return (
    <Card title="IAAF">
      <ErrorAlert error={iaaf.error ?? generate.error} />
      <p>
        {lastAdequate
          ? 'The last review is adequate: the IAAF can be generated.'
          : 'Record an adequate review to generate the IAAF.'}
      </p>
      <Button disabled={!lastAdequate || generate.isPending} onClick={() => generate.mutate()}>
        Generate IAAF
      </Button>
    </Card>
  );
}

/** Policy reviews of a record and its IAAF with the approval (FR-SP-040, 041). */
export function ReviewTab({ detail }: Readonly<{ detail: PolicyDetail }>) {
  const policyId = detail.row.id;
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const reviews = useQuery({
    queryKey: ['submitted', 'reviews', policyId],
    queryFn: () => submittedApi.reviews(policyId),
  });
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['submitted'] });
  const last = reviews.data?.[reviews.data.length - 1];
  return (
    <div className="stack">
      <ErrorAlert error={reviews.error} />
      <Card title="Reviews" flush>
        <DataTable<ReviewView>
          loading={reviews.isLoading}
          rows={reviews.data ?? []}
          rowKey={(r) => r.id}
          emptyMessage="No review recorded"
          columns={[
            { key: 'no', header: 'Review', kind: 'amount', render: (r) => r.reviewNo },
            { key: 'date', header: 'Date', kind: 'date', render: (r) => formatDate(r.reviewDate) },
            { key: 'by', header: 'Reviewer', render: (r) => <UserName login={r.reviewer} /> },
            {
              key: 'result',
              header: 'Result',
              kind: 'status',
              render: (r) => <StatusBadge status={r.adequacy} />,
            },
            {
              key: 'findings',
              header: 'Findings',
              render: (r) => <LovLabels type={SBM_LOV.finding} codes={r.findings} />,
            },
            { key: 'remarks', header: 'Remarks', render: (r) => r.remarks ?? '—' },
            { key: 'sent', header: 'Findings Sent To', render: (r) => r.sentTo ?? '—' },
          ]}
        />
      </Card>
      {can('IAAF_PREPARE') && <ReviewForm policyId={policyId} onDone={refresh} />}
      <IaafSection policyId={policyId} lastAdequate={last?.adequacy === 'ADEQUATE'} />
    </div>
  );
}
