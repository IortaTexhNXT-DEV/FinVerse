import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { reinsuranceApi } from '@/api/reinsurance';
import type { Soa, SoaGenerateInput } from '@/api/reinsurance';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { DateField, NumberField, SelectField } from '@/features/underwriting/FormFields';
import { formatDate, today } from '@/utils/format';
import { previousQuarter, QUARTERS } from './soa';
import { SoaDialog } from './SoaDialog';
import { useRiLookups } from './useRiLookups';

/** Quarterly statements of account per treaty participant: generate, approve, print, settle. */
export default function SoaPage() {
  const lookups = useRiLookups();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState<Soa | null>(null);
  const statements = useQuery({
    queryKey: ['ri-soas', lookups.companyId],
    queryFn: () => reinsuranceApi.statements(lookups.companyId),
    enabled: lookups.companyId > 0,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['ri-soas'] });

  return (
    <div className="stack">
      <PageHeader
        section="Reinsurance"
        title="Statements of Account"
        description="Quarterly income and outgo per treaty participant; the balance is placed on the smaller side. Approval posts levy, reserves and interest; settlement pays or receives the balance and matches the reinsurer's open items."
      />
      {can('REINSURANCE_MAINTAIN') && (
        <GenerateCard
          companyId={lookups.companyId}
          treaties={lookups.activeTreaties.map((t) => ({
            value: t.code,
            label: `${t.code} ${t.name}`,
          }))}
          onGenerated={async (count) => {
            await refresh();
            toast.success(`${count} statement(s) generated – pending approval`);
          }}
        />
      )}
      <ErrorAlert error={statements.error} />
      <Card flush>
        <DataTable<Soa>
          rows={statements.data ?? []}
          loading={statements.isLoading}
          rowKey={(s) => s.id}
          onRowClick={(s) => setOpen(s)}
          caption="Statements of account"
          columns={[
            { key: 'n', header: 'Statement', render: (s) => <strong>{s.soaNo}</strong> },
            { key: 'q', header: 'Quarter', render: (s) => `Q${s.quarter} ${s.year}` },
            { key: 't', header: 'Treaty', render: (s) => s.treatyCode },
            {
              key: 'r',
              header: 'Reinsurer',
              render: (s) => `${s.reinsurerCode} ${s.reinsurerName}`,
            },
            { key: 'd', header: 'Date', render: (s) => formatDate(s.statementDate) },
            {
              key: 'b',
              header: 'Balance (due to +)',
              numeric: true,
              render: (s) => <Amount value={s.balance} />,
            },
            { key: 's', header: 'Status', render: (s) => <StatusBadge status={s.status} /> },
          ]}
        />
      </Card>
      {open !== null && (
        <SoaDialog
          key={open.id}
          soa={open}
          companyId={lookups.companyId}
          onClose={() => setOpen(null)}
          onChanged={(s, message) => {
            setOpen(s);
            toast.success(message);
            void refresh();
          }}
        />
      )}
    </div>
  );
}

interface GenerateProps {
  companyId: number;
  treaties: { value: string; label: string }[];
  onGenerated: (count: number) => Promise<void>;
}

/** Generates (or regenerates while pending) the statements of a treaty for a quarter. */
function GenerateCard({ companyId, treaties, onGenerated }: Readonly<GenerateProps>) {
  const last = previousQuarter(today());
  const [request, setRequest] = useState<SoaGenerateInput>({
    companyId: 0,
    treatyCode: '',
    year: last.year,
    quarter: last.quarter,
  });
  const generate = useMutation({
    mutationFn: () => reinsuranceApi.generateStatements({ ...request, companyId }),
    onSuccess: (list) => onGenerated(list.length),
  });
  return (
    <Card title="Generate statements">
      <ErrorAlert error={generate.error} />
      <div className="form-grid">
        <SelectField
          label="Treaty"
          required
          value={request.treatyCode}
          emptyLabel="Select treaty"
          options={treaties}
          onChange={(v) => setRequest({ ...request, treatyCode: v })}
        />
        <NumberField
          label="Year"
          required
          value={request.year}
          onChange={(v) => setRequest({ ...request, year: v ?? request.year })}
        />
        <SelectField
          label="Quarter"
          required
          value={String(request.quarter)}
          options={QUARTERS.map((q) => ({ value: String(q), label: `Q${q}` }))}
          onChange={(v) => setRequest({ ...request, quarter: Number(v) })}
        />
        <DateField
          label="Statement date (blank = day after quarter end)"
          value={request.statementDate}
          onChange={(v) => setRequest({ ...request, statementDate: v === '' ? undefined : v })}
        />
      </div>
      <Button
        variant="accent"
        busy={generate.isPending}
        disabled={request.treatyCode === ''}
        onClick={() => generate.mutate()}
      >
        Generate for all participants
      </Button>
    </Card>
  );
}
