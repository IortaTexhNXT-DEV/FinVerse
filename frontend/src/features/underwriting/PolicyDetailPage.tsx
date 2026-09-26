import { useQuery, useQueryClient } from '@tanstack/react-query';
import { FilePlus2 } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { underwritingApi } from '@/api/underwriting';
import type { Policy } from '@/api/underwriting';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, humanize } from '@/utils/format';
import { DebitNote } from './DebitNote';
import { EndorsementDialog } from './EndorsementDialog';
import { EndorsementHistory } from './EndorsementHistory';
import { businessLabel } from './policyForm';
import { WorkflowActions } from './WorkflowActions';

function Terms({ policy: p }: Readonly<{ policy: Policy }>) {
  const facts: [string, string][] = [
    ['Product', `${p.productCode} – ${p.productName}`],
    ['Client', `${p.customerCode} – ${p.customerName}`],
    [
      'Source',
      p.intermediaryName === undefined
        ? 'Direct'
        : `${humanize(p.sourceType)}: ${p.intermediaryName}`,
    ],
    ['Period', `${formatDate(p.periodFrom)} – ${formatDate(p.periodTo)}`],
    ['Issue date', formatDate(p.issueDate)],
    ['UW year', String(p.uwYear)],
    ['Currency', p.currency],
    ['Business', businessLabel(p)],
    ['Discount / loading', `${String(p.discountRate)}% / ${String(p.loadingRate)}%`],
    ['Open cover', p.openCoverNo ?? '—'],
    ['Created by', p.document.createdBy],
    ['Approved by', p.document.approvedBy ?? '—'],
  ];
  return (
    <Card title="Policy terms">
      <dl className="form-grid" style={{ margin: 0 }}>
        {facts.map(([label, value]) => (
          <div key={label}>
            <dt className="muted">{label}</dt>
            <dd style={{ margin: 0, fontWeight: 600 }}>{value}</dd>
          </div>
        ))}
      </dl>
      {p.document.rejectionReason !== undefined && (
        <div className="alert warning" style={{ marginTop: 12 }}>
          Returned by the checker: {p.document.rejectionReason}
        </div>
      )}
    </Card>
  );
}

/** Policy view: terms, risks, debit note, endorsement history and workflow actions. */
export default function PolicyDetailPage() {
  const id = Number(useParams().id);
  const navigate = useNavigate();
  const toast = useToast();
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [endorsing, setEndorsing] = useState(false);
  const policy = useQuery({ queryKey: ['policy', id], queryFn: () => underwritingApi.policy(id) });
  const endorsements = useQuery({
    queryKey: ['endorsements', id],
    queryFn: () => underwritingApi.endorsements(id),
  });
  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['policy', id] });
    await queryClient.invalidateQueries({ queryKey: ['policies'] });
    await queryClient.invalidateQueries({ queryKey: ['endorsements', id] });
  };

  if (policy.data === undefined) {
    return policy.error ? (
      <ErrorAlert error={policy.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const p = policy.data;
  const canEndorse = p.document.status === 'APPROVED' && can('POLICY_MAINTAIN');

  return (
    <div className="stack">
      <PageHeader
        section="Underwriting · Policy"
        title={p.policyNo}
        description={p.insuredName}
        actions={
          <>
            <StatusBadge status={p.document.status} />
            <WorkflowActions
              label={p.policyNo}
              facts={p.document}
              handlers={{
                edit: () => void navigate(`/underwriting/policies/${String(p.id)}/edit`),
                submit: () => underwritingApi.submitPolicy(p.id),
                discard: () => underwritingApi.discardPolicy(p.id),
                approve: (date) => underwritingApi.approvePolicy(p.id, date),
                reject: (reason) => underwritingApi.rejectPolicy(p.id, reason),
                done: refresh,
              }}
            />
            {canEndorse && (
              <Button
                variant="secondary"
                icon={<FilePlus2 size={16} />}
                onClick={() => setEndorsing(true)}
              >
                Endorse
              </Button>
            )}
          </>
        }
      />
      <Terms policy={p} />
      <Card title="Risks" flush>
        <DataTable
          rows={p.risks}
          rowKey={(r) => r.lineNo}
          columns={[
            { key: 'no', header: '#', render: (r) => r.lineNo },
            { key: 'desc', header: 'Description', render: (r) => r.description },
            {
              key: 'occ',
              header: 'Occupation',
              render: (r) => r.occupation ?? r.marine?.vesselName ?? '',
            },
            { key: 'zone', header: 'Zone', render: (r) => r.accumulationZone ?? '' },
            {
              key: 'si',
              header: 'Sum Insured',
              numeric: true,
              render: (r) => <Amount value={r.sumInsured} />,
            },
            { key: 'rate', header: 'Rate %', numeric: true, render: (r) => r.rate },
            {
              key: 'prem',
              header: 'Premium',
              numeric: true,
              render: (r) => <Amount value={r.premium} />,
            },
          ]}
        />
      </Card>
      <DebitNote policy={p} document={p.document} premium={p.premium} reference={p.policyNo} />
      <EndorsementHistory policy={p} endorsements={endorsements.data ?? []} />
      {canEndorse && (
        <EndorsementDialog
          policy={p}
          open={endorsing}
          onClose={() => setEndorsing(false)}
          onCreated={async (e) => {
            setEndorsing(false);
            toast.success(`${e.documentNo} created as draft`);
            await refresh();
          }}
        />
      )}
    </div>
  );
}
