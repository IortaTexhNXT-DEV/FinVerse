import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { investmentsApi } from '@/api/investments';
import type { Holding, HoldingStatus } from '@/api/investments';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useDefaultBranchId } from '@/context/workspaceContext';
import { makerOf } from '@/utils/makerChecker';
import { formatAmount, formatDate, humanize, today } from '@/utils/format';
import { SelectInput, TextInput } from './FormControls';
import { HoldingFormModal } from './HoldingFormModal';
import type { HoldingForm } from './HoldingFormModal';
import { enumOptions } from './options';
import { useAssetLookups } from './useAssetLookups';

const STATUSES = enumOptions(['PENDING_APPROVAL', 'ACTIVE', 'MATURED', 'SOLD']);

function total(rows: Holding[], pick: (h: Holding) => number): string {
  return formatAmount(rows.reduce((acc, h) => acc + pick(h), 0));
}

/** Investment holdings: capture (maker) and approval with purchase posting (checker). */
export default function InvestmentsPage() {
  const { companyId, portfolios, baseCurrency } = useAssetLookups();
  const defaultBranch = useDefaultBranchId();
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<HoldingStatus | ''>('');
  const [portfolioId, setPortfolioId] = useState('');
  const [q, setQ] = useState('');
  const [form, setForm] = useState<HoldingForm | null>(null);
  const holdings = useQuery({
    queryKey: ['holdings', companyId, status, portfolioId, q],
    queryFn: () =>
      investmentsApi.holdings(
        companyId,
        status === '' ? undefined : status,
        portfolioId === '' ? undefined : Number(portfolioId),
        q,
      ),
    enabled: companyId > 0,
  });
  const approve = useMutation({
    mutationFn: (id: number) => investmentsApi.approve(id),
    onSuccess: async (h) => {
      await queryClient.invalidateQueries({ queryKey: ['holdings'] });
      toast.success(`${h.holdingNo} approved – journal ${h.purchaseBatchNo ?? ''}`);
    },
  });
  const rows = holdings.data ?? [];
  const active = rows.filter((h) => h.status === 'ACTIVE');

  return (
    <div className="stack">
      <PageHeader
        section="Assets & Investments"
        title="Investments"
        description="Time deposits, treasury bills, bonds and equities. New holdings post their purchase when a checker approves them."
        actions={
          can('MASTER_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() =>
                setForm({
                  branchId: defaultBranch || undefined,
                  currency: baseCurrency,
                  instrumentType: 'GOVERNMENT_BOND',
                  couponFrequency: 'SEMI_ANNUAL',
                  dayCount: 'ACT_365',
                  couponRate: 0,
                  tradeDate: today(),
                  settlementDate: today(),
                  securityDeposit: false,
                  takeOn: false,
                })
              }
            >
              New Investment
            </Button>
          )
        }
      />
      <div className="form-grid">
        <Kpi label="Holdings held" value={active.length} />
        <Kpi label="Face value" value={total(active, (h) => h.faceValue)} />
        <Kpi label="Carrying amount" accent value={total(active, (h) => h.carryingAmount)} />
        <Kpi label="Accrued interest" value={total(active, (h) => h.accruedInterest)} />
      </div>
      <Card>
        <div className="form-grid">
          <SelectInput
            label="Status"
            blank="All"
            value={status}
            options={STATUSES}
            onChange={(v) => setStatus(v as HoldingStatus | '')}
          />
          <SelectInput
            label="Portfolio"
            blank="All"
            value={portfolioId}
            options={portfolios.map((p) => ({ value: String(p.id), label: p.name }))}
            onChange={setPortfolioId}
          />
          <TextInput label="Holding / security / description" value={q} onChange={setQ} />
        </div>
      </Card>
      <ErrorAlert error={holdings.error ?? approve.error} />
      <Card flush>
        <DataTable<Holding>
          loading={holdings.isLoading}
          rows={rows}
          rowKey={(h) => h.id}
          onRowClick={
            can('MASTER_MAINTAIN')
              ? (h) => h.status === 'PENDING_APPROVAL' && setForm(h)
              : undefined
          }
          columns={[
            { key: 'n', header: 'Holding', render: (h) => <strong>{h.holdingNo}</strong> },
            { key: 'd', header: 'Description', render: (h) => h.description },
            { key: 't', header: 'Instrument', render: (h) => humanize(h.instrumentType) },
            { key: 'p', header: 'Portfolio', render: (h) => h.portfolioCode },
            { key: 'm', header: 'Maturity', render: (h) => formatDate(h.maturityDate) },
            { key: 'r', header: 'Coupon %', numeric: true, render: (h) => h.couponRate },
            {
              key: 'f',
              header: 'Face',
              numeric: true,
              render: (h) => <Amount value={h.faceValue} />,
            },
            {
              key: 'c',
              header: 'Carrying',
              numeric: true,
              render: (h) => <Amount value={h.carryingAmount} />,
            },
            {
              key: 'a',
              header: 'Accrued',
              numeric: true,
              render: (h) => <Amount value={h.accruedInterest} />,
            },
            { key: 'sd', header: 'IC Deposit', render: (h) => (h.securityDeposit ? 'Yes' : '') },
            { key: 's', header: 'Status', render: (h) => <StatusBadge status={h.status} /> },
            {
              key: 'x',
              header: 'Actions',
              render: (h) =>
                h.status === 'PENDING_APPROVAL' &&
                can('MASTER_AUTHORIZE') &&
                makerOf(h) !== user?.username && (
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={(e) => {
                      e.stopPropagation();
                      approve.mutate(h.id);
                    }}
                  >
                    Approve
                  </Button>
                ),
            },
          ]}
        />
      </Card>
      {form !== null && <HoldingFormModal initial={form} onClose={() => setForm(null)} />}
    </div>
  );
}
