import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FilePlus2 } from 'lucide-react';
import { useState } from 'react';
import { submittedApi } from '@/api/submitted';
import type { PolicyRow } from '@/api/submitted';
import { proposalsApi } from '@/api/submittedProposals';
import type { ProposedLine } from '@/api/submittedProposals';
import { InsurerName, LovLabel } from '@/components/broking/LovLabel';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate } from '@/utils/format';
import { SBM_LOV } from '../common/submittedCodes';
import { applyRate, generationLines, premiumAt } from './proposalLogic';
import type { RateEdit } from './proposalLogic';

function RateBar({
  lines,
  onApply,
}: Readonly<{
  lines: ProposedLine[];
  onApply: (edit: RateEdit, only: { vehicleType?: string; insurerCode?: string }) => void;
}>) {
  const [rate, setRate] = useState('');
  const [reason, setReason] = useState('');
  const [vehicleType, setVehicleType] = useState('');
  const [insurer, setInsurer] = useState('');
  const classes = [
    ...new Set(lines.map((l) => l.vehicleType).filter((v): v is string => v !== null)),
  ];
  const insurers = [
    ...new Set(lines.map((l) => l.insurerCode).filter((v): v is string => v !== null)),
  ];
  return (
    <div className="form-grid">
      <Field label="Rate (%)">
        {(id) => (
          <input
            id={id}
            className="input"
            inputMode="decimal"
            value={rate}
            onChange={(e) => setRate(e.target.value)}
          />
        )}
      </Field>
      <Field label="Reason">
        {(id) => (
          <input
            id={id}
            className="input"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
        )}
      </Field>
      <Field label="Vehicle Classification">
        {(id) => (
          <select
            id={id}
            className="select"
            value={vehicleType}
            onChange={(e) => setVehicleType(e.target.value)}
          >
            <option value="">Every classification</option>
            {classes.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Insurer">
        {(id) => (
          <select
            id={id}
            className="select"
            value={insurer}
            onChange={(e) => setInsurer(e.target.value)}
          >
            <option value="">Every insurer</option>
            {insurers.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Button
        variant="secondary"
        disabled={rate.trim() === ''}
        onClick={() =>
          onApply(
            { rate, reason },
            { vehicleType: vehicleType || undefined, insurerCode: insurer || undefined },
          )
        }
      >
        Apply Rate
      </Button>
    </div>
  );
}

function Preview({ lines, onDone }: Readonly<{ lines: ProposedLine[]; onDone: () => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [edits, setEdits] = useState<Record<number, RateEdit>>({});
  const plan = generationLines(lines, edits);
  const editOf = (policyId: number): RateEdit | undefined => edits[policyId];
  const generate = useMutation({
    mutationFn: () => proposalsApi.generate(companyId, plan.lines),
    onSuccess: async (batch) => {
      toast.success(
        `Batch ${batch.batchNo} created with ${String(batch.proposals.length)} proposal(s) for review`,
      );
      await queryClient.invalidateQueries({ queryKey: ['submitted', 'proposals'] });
      onDone();
    },
  });
  return (
    <Card title="Proposed rates and premiums" flush>
      <RateBar
        lines={lines}
        onApply={(edit, only) => setEdits((e) => applyRate(e, lines, edit, only))}
      />
      <ErrorAlert error={generate.error} />
      {plan.problem && <p className="muted">{plan.problem}</p>}
      <DataTable<ProposedLine>
        rows={lines}
        rowKey={(l) => l.policyId}
        columns={[
          { key: 'sbm', header: 'Masterlist No.', kind: 'code', render: (l) => l.sbmNo },
          { key: 'assured', header: 'Assured', render: (l) => l.assuredName },
          { key: 'class', header: 'Vehicle Classification', render: (l) => l.vehicleType ?? '—' },
          {
            key: 'insurer',
            header: 'Insurer',
            render: (l) => <InsurerName code={l.insurerCode} />,
          },
          {
            key: 'nominated',
            header: 'Nominated Rate',
            kind: 'amount',
            render: (l) =>
              l.nominatedRate === null ? (l.problem ?? '—') : `${String(l.nominatedRate)}%`,
          },
          {
            key: 'applied',
            header: 'Applied Rate',
            kind: 'amount',
            render: (l) => {
              const edit = editOf(l.policyId);
              return edit ? `${edit.rate}%` : '—';
            },
          },
          {
            key: 'premium',
            header: 'Premium',
            kind: 'amount',
            render: (l) => {
              const edit = editOf(l.policyId);
              return formatAmount(
                premiumAt(l.sumInsured, edit ? Number(edit.rate) : l.nominatedRate),
              );
            },
          },
        ]}
      />
      <div className="row-actions-bar">
        <Button
          busy={generate.isPending}
          disabled={plan.problem !== undefined}
          onClick={() => generate.mutate()}
        >
          Generate
        </Button>
      </div>
    </Card>
  );
}

/** Create Proposals: the records For Renewal, the proposed rates, the edited rates and Generate. */
export function CreateProposals() {
  const companyId = useCompanyId();
  const selection = useRowSelection();
  const [lines, setLines] = useState<ProposedLine[]>();
  const records = useQuery({
    queryKey: ['submitted', 'proposals', 'for-renewal', companyId],
    queryFn: () => submittedApi.list(companyId, { tab: 'FOR_RENEWAL' }, 0, 100),
    enabled: companyId > 0,
  });
  const rows = records.data?.content ?? [];
  const chosen = rows.filter((r) => selection.has(String(r.id)));
  const preview = useMutation({
    mutationFn: () =>
      proposalsApi.preview(
        companyId,
        chosen.map((r) => r.id),
      ),
    onSuccess: setLines,
  });
  return (
    <div className="stack">
      <Card
        title="Policies for renewal"
        flush
        actions={
          <Button
            icon={<FilePlus2 size={16} />}
            disabled={chosen.length === 0}
            busy={preview.isPending}
            onClick={() => preview.mutate()}
          >
            Create Proposals
          </Button>
        }
      >
        <ErrorAlert error={records.error ?? preview.error} />
        <DataTable<PolicyRow>
          loading={records.isLoading}
          rows={rows}
          rowKey={(r) => r.id}
          emptyMessage="No policy is for renewal"
          columns={[
            selectionColumn(
              rows,
              (r) => String(r.id),
              selection,
              (r) => r.sbmNo,
            ),
            { key: 'sbm', header: 'Masterlist No.', kind: 'code', render: (r) => r.sbmNo },
            { key: 'assured', header: 'Assured', render: (r) => r.assuredName },
            {
              key: 'segment',
              header: 'Segment',
              render: (r) => <LovLabel type={SBM_LOV.segment} code={r.segment} />,
            },
            {
              key: 'insurer',
              header: 'Expiring Insurer',
              render: (r) => <InsurerName code={r.insurerCode} />,
            },
            {
              key: 'expiry',
              header: 'Expiry',
              kind: 'date',
              render: (r) => formatDate(r.expiryDate),
            },
            {
              key: 'si',
              header: 'Sum Insured',
              kind: 'amount',
              render: (r) => formatAmount(r.sumInsured),
            },
          ]}
        />
      </Card>
      {lines && (
        <Preview
          lines={lines}
          onDone={() => {
            setLines(undefined);
            selection.clear();
          }}
        />
      )}
    </div>
  );
}
