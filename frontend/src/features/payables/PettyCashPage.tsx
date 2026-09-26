import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { payablesApi } from '@/api/payables';
import type { Fund, FundRequest } from '@/api/payables';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useDefaultBranchId, useWorkspace } from '@/context/workspaceContext';
import { formatDate, today } from '@/utils/format';
import { fundLevel } from './payablesMath';
import { PettyCashFundPanel } from './PettyCashFundPanel';
import { usePayablesLookups } from './usePayablesLookups';
import { awaitsOtherChecker } from '@/utils/makerChecker';

type FundForm = Partial<FundRequest>;

/** Imprest petty cash: funds per branch, their vouchers and reimbursement claims. */
export default function PettyCashPage() {
  const { branches } = useWorkspace();
  const defaultBranch = useDefaultBranchId();
  const { companyId, activeBanks } = usePayablesLookups();
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<number | null>(null);
  const [form, setForm] = useState<FundForm | null>(null);

  const funds = useQuery({
    queryKey: ['petty-cash-funds', companyId],
    queryFn: () => payablesApi.funds(companyId),
    enabled: companyId > 0,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['petty-cash-funds'] });
  const create = useMutation({
    mutationFn: (f: FundForm) => payablesApi.createFund({ ...f, companyId } as FundRequest),
    onSuccess: async (f) => {
      await refresh();
      setForm(null);
      toast.success(`Fund ${f.code} created – pending authorization`);
    },
  });
  const authorize = useMutation({
    mutationFn: payablesApi.authorizeFund,
    onSuccess: async (f) => {
      await refresh();
      toast.success(`Fund ${f.code} authorized`);
    },
  });
  const establish = useMutation({
    mutationFn: (id: number) => payablesApi.establishFund(id, today()),
    onSuccess: async (f) => {
      await refresh();
      toast.success(`Fund ${f.code} established with ${f.imprestAmount.toFixed(2)}`);
    },
  });
  const rows = funds.data ?? [];
  const fund = rows.find((f) => f.id === selected);

  const fundAction = (f: Fund) => {
    if (awaitsOtherChecker(f, user?.username) && can('MASTER_AUTHORIZE')) {
      return { label: 'Authorize', run: () => authorize.mutate(f.id) };
    }
    if (
      f.recordStatus === 'ACTIVE' &&
      f.establishedOn === undefined &&
      can('RECEIPT_PAYMENT_AUTHORIZE')
    ) {
      return { label: 'Establish', run: () => establish.mutate(f.id) };
    }
    return null;
  };

  return (
    <div className="stack">
      <PageHeader
        section="Payables & Cash"
        title="Petty Cash"
        description="Imprest funds: cash in the box plus vouchers pending reimbursement always equals the imprest."
        actions={
          can('MASTER_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() =>
                setForm({
                  glAccountCode: '1102',
                  branchId: defaultBranch || undefined,
                  replenishBankAccountId: activeBanks[0]?.id,
                })
              }
            >
              New Fund
            </Button>
          )
        }
      />
      <ErrorAlert error={funds.error ?? authorize.error ?? establish.error} />
      <Card flush>
        <DataTable<Fund>
          loading={funds.isLoading}
          rows={rows}
          rowKey={(f) => f.id}
          caption="Petty cash funds"
          onRowClick={(f) => setSelected(f.id)}
          columns={[
            { key: 'c', header: 'Fund', render: (f) => <strong>{f.code}</strong> },
            { key: 'n', header: 'Name', render: (f) => f.name },
            {
              key: 'b',
              header: 'Branch',
              render: (f) => branches.find((b) => b.id === f.branchId)?.code ?? '',
            },
            { key: 'u', header: 'Custodian', render: (f) => f.custodian },
            {
              key: 'i',
              header: 'Imprest',
              numeric: true,
              render: (f) => <Amount value={f.imprestAmount} />,
            },
            {
              key: 'h',
              header: 'Cash in Box',
              numeric: true,
              render: (f) => <Amount value={f.cashBalance} />,
            },
            {
              key: 'p',
              header: 'Pending Reimb.',
              numeric: true,
              render: (f) => <Amount value={f.pendingReimbursement} />,
            },
            {
              key: 'l',
              header: 'Level',
              numeric: true,
              render: (f) => `${fundLevel(f.cashBalance, f.imprestAmount)} %`,
            },
            { key: 'e', header: 'Established', render: (f) => formatDate(f.establishedOn) },
            { key: 's', header: 'Status', render: (f) => <StatusBadge status={f.recordStatus} /> },
            {
              key: 'x',
              header: 'Actions',
              render: (f) => {
                const a = fundAction(f);
                return (
                  a !== null && (
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={(e) => {
                        e.stopPropagation();
                        a.run();
                      }}
                    >
                      {a.label}
                    </Button>
                  )
                );
              },
            },
          ]}
        />
      </Card>
      {fund !== undefined && <PettyCashFundPanel fund={fund} />}
      <Modal
        title="New Petty Cash Fund"
        open={form !== null}
        onClose={() => setForm(null)}
        footer={
          <Button
            variant="accent"
            busy={create.isPending}
            onClick={() => form && create.mutate(form)}
          >
            Save for Authorization
          </Button>
        }
      >
        <ErrorAlert error={create.error} />
        {form !== null && (
          <div className="form-grid">
            {(['code', 'name', 'custodian', 'glAccountCode'] as const).map((k) => (
              <Field
                key={k}
                label={
                  k === 'glAccountCode' ? 'GL account' : k.charAt(0).toUpperCase() + k.slice(1)
                }
                required
              >
                {(id) => (
                  <input
                    id={id}
                    className="input"
                    value={form[k] ?? ''}
                    onChange={(e) => setForm({ ...form, [k]: e.target.value })}
                  />
                )}
              </Field>
            ))}
            <Field label="Branch" required>
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={form.branchId ?? ''}
                  onChange={(e) => setForm({ ...form, branchId: Number(e.target.value) })}
                >
                  <option value="">Select…</option>
                  {branches.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.code} – {b.name}
                    </option>
                  ))}
                </select>
              )}
            </Field>
            <Field label="Replenishment bank" required>
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={form.replenishBankAccountId ?? ''}
                  onChange={(e) =>
                    setForm({ ...form, replenishBankAccountId: Number(e.target.value) })
                  }
                >
                  {activeBanks.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.code}
                    </option>
                  ))}
                </select>
              )}
            </Field>
            <Field label="Imprest amount" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  type="number"
                  step="0.01"
                  value={form.imprestAmount ?? ''}
                  onChange={(e) => setForm({ ...form, imprestAmount: Number(e.target.value) })}
                />
              )}
            </Field>
          </div>
        )}
      </Modal>
    </div>
  );
}
