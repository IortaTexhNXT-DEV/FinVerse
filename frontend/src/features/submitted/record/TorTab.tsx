import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { submittedApi } from '@/api/submitted';
import type { PolicyDetail } from '@/api/submitted';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { UserName } from '@/components/ui/UserName';
import { formatDateTime } from '@/utils/format';
import { ApprovalPanel } from '../common/ApprovalPanel';
import { UserSelect } from '../common/UserSelect';
import { TorActions } from '../reviews/IaafActions';

function NewTor({ detail, onDone }: Readonly<{ detail: PolicyDetail; onDone: () => void }>) {
  const toast = useToast();
  const policyId = detail.row.id;
  const [terms, setTerms] = useState('');
  const [ao, setAo] = useState(detail.tracking.aoUsername ?? '');
  const [touched, setTouched] = useState(false);
  const breaches = useQuery({
    queryKey: ['submitted', 'breaches', policyId],
    queryFn: () => submittedApi.breaches(policyId),
  });
  const create = useMutation({
    mutationFn: () => submittedApi.generateTor(policyId, terms.trim(), ao),
    onSuccess: (t) => {
      toast.success(`TOR ${t.torNo} generated`);
      setTerms('');
      onDone();
    },
  });
  const text = breaches.data?.breaches ?? '';
  return (
    <Card title="Generate a TOR">
      <ErrorAlert error={create.error ?? breaches.error} />
      <p>
        {text === '' ? 'The last run found no limit exceeded.' : 'Limits exceeded in the last run:'}
      </p>
      {text !== '' && <pre className="pre-wrap">{text}</pre>}
      <div className="form-grid">
        <Field
          label="Proposed Terms"
          required
          error={touched && terms.trim() === '' ? 'Enter the proposed terms' : undefined}
        >
          {(id) => (
            <textarea
              id={id}
              className="input"
              rows={3}
              value={terms}
              onChange={(e) => setTerms(e.target.value)}
              onBlur={() => setTouched(true)}
            />
          )}
        </Field>
        <Field
          label="Account Officer"
          required
          error={touched && ao === '' ? 'Select the Account Officer' : undefined}
        >
          {(id) => <UserSelect id={id} permission="TOR_PREPARE" value={ao} onChange={setAo} />}
        </Field>
      </div>
      <div className="form-actions">
        <Button
          disabled={text === '' || create.isPending}
          onClick={() => {
            setTouched(true);
            if (terms.trim() !== '' && ao !== '') {
              create.mutate();
            }
          }}
        >
          Generate TOR
        </Button>
      </div>
    </Card>
  );
}

/** The Terms of Reference of a record above the insurer limits (FR-SP-051 to 053). */
export function TorTab({ detail }: Readonly<{ detail: PolicyDetail }>) {
  const policyId = detail.row.id;
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const tors = useQuery({
    queryKey: ['submitted', 'tors-of', policyId],
    queryFn: () => submittedApi.torsOf(policyId),
  });
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['submitted'] });
  const open = (tors.data ?? []).some(
    (t) => !['CANCELLED', 'RELEASED'].includes(t.approval.status),
  );
  return (
    <div className="stack">
      <ErrorAlert error={tors.error} />
      {(tors.data ?? []).map((t) => (
        <div className="stack" key={t.id}>
          <Card title={`TOR ${t.torNo}`} actions={<TorActions tor={t} onDone={refresh} />}>
            <DefinitionGrid
              columns={2}
              items={[
                { label: 'Limits Exceeded', value: t.breaches, wide: true },
                { label: 'Proposed Terms', value: t.proposedTerms, wide: true },
                { label: 'Account Officer', value: <UserName login={t.aoUsername} /> },
                { label: 'Renewal Account', value: t.arn },
                { label: 'Approved', value: formatDateTime(t.approvedAt) },
                { label: 'Released', value: formatDateTime(t.releasedAt) },
              ]}
            />
          </Card>
          <ApprovalPanel approval={t.approval} />
        </div>
      ))}
      {!tors.isLoading &&
        (tors.data ?? []).length === 0 &&
        !detail.row.flags.includes('INSURER_APPROVAL') && (
          <EmptyState message="The policy is within the insurer limits: no TOR is needed" />
        )}
      {can('TOR_PREPARE') && !open && detail.row.flags.includes('INSURER_APPROVAL') && (
        <NewTor detail={detail} onDone={refresh} />
      )}
    </div>
  );
}
