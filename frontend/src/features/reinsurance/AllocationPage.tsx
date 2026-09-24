import { useMutation } from '@tanstack/react-query';
import { Eye, Play } from 'lucide-react';
import { useState } from 'react';
import { reinsuranceApi } from '@/api/reinsurance';
import type { AllocationPreviewRow, AllocationRunResult } from '@/api/reinsurance';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { DateField } from '@/features/underwriting/FormFields';
import { formatAmount, formatDate, humanize, today } from '@/utils/format';
import { PolicyCessionsCard } from './PolicyCessionsCard';
import { previewTotals } from './allocation';

/** RI allocation run (preview, then post) and the per-policy cession view. */
export default function AllocationPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const [from, setFrom] = useState(`${today().slice(0, 7)}-01`);
  const [to, setTo] = useState(today());
  const [rows, setRows] = useState<AllocationPreviewRow[] | null>(null);
  const [result, setResult] = useState<AllocationRunResult | null>(null);
  const preview = useMutation({
    mutationFn: () => reinsuranceApi.preview(companyId, from, to),
    onSuccess: (r) => {
      setRows(r);
      setResult(null);
    },
  });
  const post = useMutation({
    mutationFn: () => reinsuranceApi.run(companyId, from, to),
    onSuccess: (r) => {
      setResult(r);
      setRows(null);
      toast.success(`${r.ceded} transaction(s) ceded`);
    },
  });
  const totals = previewTotals(rows ?? []);

  return (
    <div className="stack">
      <PageHeader
        section="Reinsurance"
        title="RI Allocation"
        description="Cedes approved policies and endorsements not yet ceded: retention first, then quota share, surplus lines and the facultative remainder. Endorsements and refunds follow the proportions in force."
      />
      <Card title="Allocation run">
        <ErrorAlert error={preview.error ?? post.error} />
        <div className="form-grid">
          <DateField label="Approved from" required value={from} onChange={setFrom} />
          <DateField label="Approved to" required value={to} onChange={setTo} />
        </div>
        <div className="row">
          <Button
            variant="secondary"
            icon={<Eye size={16} />}
            busy={preview.isPending}
            onClick={() => preview.mutate()}
          >
            Preview
          </Button>
          {can('REINSURANCE_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Play size={16} />}
              busy={post.isPending}
              onClick={() => post.mutate()}
            >
              Post Allocation
            </Button>
          )}
        </div>
        {result !== null && (
          <div className="grid-4">
            <Kpi label="Ceded" value={result.ceded} accent />
            <Kpi label="Failed" value={result.failed} />
            <Kpi label="Premium ceded" value={formatAmount(result.premiumCeded)} />
            <Kpi label="Job run" value={`#${result.jobRunId}`} hint={humanize(result.status)} />
          </div>
        )}
        {result !== null && result.messages.length > 0 && (
          <div className="alert warning" role="status">
            {result.messages.map((m) => (
              <div key={m}>{m}</div>
            ))}
          </div>
        )}
      </Card>
      {rows !== null && (
        <Card
          flush
          title={`${rows.length} transaction(s) to cede – treaty ${formatAmount(totals.treaty)}, FAC ${formatAmount(totals.fac)}`}
        >
          <DataTable<AllocationPreviewRow>
            rows={rows}
            rowKey={(r) => `${r.policyId}-${r.endorsementNo}`}
            caption="Allocation preview"
            columns={[
              { key: 'd', header: 'Document', render: (r) => <strong>{r.documentNo}</strong> },
              { key: 'k', header: 'Kind', render: (r) => humanize(r.kind) },
              { key: 'l', header: 'Class', render: (r) => r.businessLine },
              { key: 'a', header: 'Approved', render: (r) => formatDate(r.approvalDate) },
              { key: 'c', header: 'Ccy', render: (r) => r.currency },
              {
                key: 'p',
                header: 'Our Premium',
                numeric: true,
                render: (r) => <Amount value={r.ourPremium} />,
              },
              {
                key: 'rt',
                header: 'Retention',
                numeric: true,
                render: (r) => <Amount value={r.retention} />,
              },
              {
                key: 'q',
                header: 'QS',
                numeric: true,
                render: (r) => <Amount value={r.quotaShare} />,
              },
              {
                key: 's',
                header: 'Surplus',
                numeric: true,
                render: (r) => <Amount value={r.surplus} />,
              },
              { key: 'f', header: 'FAC', numeric: true, render: (r) => <Amount value={r.fac} /> },
              { key: 'm', header: 'Note', render: (r) => r.message ?? humanize(r.basis ?? '') },
            ]}
          />
        </Card>
      )}
      <PolicyCessionsCard companyId={companyId} />
    </div>
  );
}
