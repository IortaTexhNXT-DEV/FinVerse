import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { intercompanyApi } from '@/api/consolidation';
import type { IcTransaction, ReconciliationLine, Relationship } from '@/api/consolidation';
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
import { useCompanyId, useWorkspace } from '@/context/workspaceContext';
import { formatDate, today } from '@/utils/format';
import { IcTransactionForm } from './IcTransactionForm';

const ACCOUNT_FIELDS = [
  ['aDueFromAccount', 'Due-from account (this company)'],
  ['aDueToAccount', 'Due-to account (this company)'],
  ['bDueFromAccount', 'Due-from account (counterparty)'],
  ['bDueToAccount', 'Due-to account (counterparty)'],
] as const;

/** Inter-company relationships, mirror-journal transactions and due-to / due-from reconciliation. */
export default function IntercompanyPage() {
  const companyId = useCompanyId();
  const { companies } = useWorkspace();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [asOf, setAsOf] = useState(today());
  const [adding, setAdding] = useState(false);
  const [form, setForm] = useState({
    companyBId: 0,
    aDueFromAccount: '1607',
    aDueToAccount: '2510',
    bDueFromAccount: '1607',
    bDueToAccount: '2510',
  });
  const code = (id: number) => companies.find((c) => c.id === id)?.code ?? String(id);
  const counterparties = companies.filter((c) => c.id !== companyId);
  const companyB = form.companyBId || (counterparties[0]?.id ?? 0);

  const relationships = useQuery({
    queryKey: ['ic', 'relationships', companyId],
    queryFn: () => intercompanyApi.relationships(companyId),
    enabled: companyId > 0,
  });
  const transactions = useQuery({
    queryKey: ['ic', 'transactions', companyId],
    queryFn: () => intercompanyApi.transactions(companyId),
    enabled: companyId > 0,
  });
  const reconciliation = useQuery({
    queryKey: ['ic', 'reconciliation', companyId, asOf],
    queryFn: () => intercompanyApi.reconciliation(companyId, asOf),
    enabled: companyId > 0,
  });
  const create = useMutation({
    mutationFn: () =>
      intercompanyApi.createRelationship({ companyAId: companyId, ...form, companyBId: companyB }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['ic'] });
      setAdding(false);
      toast.success('Inter-company relationship created');
    },
  });
  const toggle = useMutation({
    mutationFn: (r: Relationship) => intercompanyApi.setActive(r.id, !r.active),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['ic'] });
    },
  });
  const manage = can('CONSOLIDATION_RUN');

  return (
    <div className="stack">
      <PageHeader
        section="Planning & Closing"
        title="Inter-company"
        description="Due-to / due-from relationships between group companies. A transaction posts mirror journals in both companies with one IC reference."
        actions={
          manage && (
            <Button variant="secondary" icon={<Plus size={16} />} onClick={() => setAdding(true)}>
              New Relationship
            </Button>
          )
        }
      />
      <ErrorAlert error={relationships.error ?? toggle.error} />
      <Card title="Relationships" flush>
        <DataTable<Relationship>
          loading={relationships.isLoading}
          rows={relationships.data ?? []}
          rowKey={(r) => r.id}
          columns={[
            {
              key: 'p',
              header: 'Companies',
              render: (r) => `${code(r.companyAId)} ↔ ${code(r.companyBId)}`,
            },
            {
              key: 'a',
              header: 'A Due-from / Due-to',
              render: (r) => `${r.aDueFromAccount} / ${r.aDueToAccount}`,
            },
            {
              key: 'b',
              header: 'B Due-from / Due-to',
              render: (r) => `${r.bDueFromAccount} / ${r.bDueToAccount}`,
            },
            {
              key: 's',
              header: 'Status',
              render: (r) => <StatusBadge status={r.active ? 'ACTIVE' : 'INACTIVE'} />,
            },
            {
              key: 'x',
              header: 'Actions',
              render: (r) =>
                manage && (
                  <Button size="sm" variant="ghost" onClick={() => toggle.mutate(r)}>
                    {r.active ? 'Deactivate' : 'Activate'}
                  </Button>
                ),
            },
          ]}
        />
      </Card>
      {manage && (
        <Card title="Post inter-company transaction">
          <IcTransactionForm companies={companies} companyId={companyId} />
        </Card>
      )}
      <Card
        title="Reconciliation (transaction currency)"
        flush
        actions={
          <input
            className="input"
            type="date"
            aria-label="Reconciliation date"
            value={asOf}
            onChange={(e) => setAsOf(e.target.value)}
          />
        }
      >
        <ErrorAlert error={reconciliation.error} />
        <DataTable<ReconciliationLine>
          loading={reconciliation.isLoading}
          rows={reconciliation.data ?? []}
          rowKey={(l) => `${l.relationshipId}-${l.creditorCompanyId}-${l.currency}`}
          columns={[
            {
              key: 'c',
              header: 'Due-from',
              render: (l) => `${code(l.creditorCompanyId)} ${l.dueFromAccount}`,
            },
            {
              key: 'd',
              header: 'Due-to',
              render: (l) => `${code(l.debtorCompanyId)} ${l.dueToAccount}`,
            },
            { key: 'y', header: 'Currency', render: (l) => l.currency },
            {
              key: 'f',
              header: 'Due-from (FC)',
              numeric: true,
              render: (l) => <Amount value={l.dueFromFc} />,
            },
            {
              key: 't',
              header: 'Due-to (FC)',
              numeric: true,
              render: (l) => <Amount value={l.dueToFc} />,
            },
            {
              key: 'x',
              header: 'Difference',
              numeric: true,
              render: (l) => <Amount value={l.differenceFc} />,
            },
            {
              key: 's',
              header: 'Status',
              render: (l) => (
                <span className={`badge ${l.matched ? 'success' : 'danger'}`}>
                  {l.matched ? 'Matched' : 'Mismatch'}
                </span>
              ),
            },
          ]}
        />
      </Card>
      <Card title="Transactions" flush>
        <DataTable<IcTransaction>
          loading={transactions.isLoading}
          rows={transactions.data ?? []}
          rowKey={(t) => t.id}
          columns={[
            { key: 'r', header: 'IC Reference', render: (t) => <strong>{t.icReference}</strong> },
            { key: 'd', header: 'Value Date', render: (t) => formatDate(t.valueDate) },
            { key: 't', header: 'Type', render: (t) => t.type },
            {
              key: 'p',
              header: 'Creditor → Debtor',
              render: (t) => `${code(t.creditorCompanyId)} → ${code(t.debtorCompanyId)}`,
            },
            {
              key: 'a',
              header: 'Amount',
              numeric: true,
              render: (t) => <Amount value={t.amount} />,
            },
            { key: 'c', header: 'Ccy', render: (t) => t.currency },
            {
              key: 'j',
              header: 'Journals',
              render: (t) => `${t.creditorBatchNo} / ${t.debtorBatchNo}`,
            },
            { key: 'n', header: 'Narration', render: (t) => t.narration },
          ]}
        />
      </Card>
      <Modal
        title="New Inter-company Relationship"
        open={adding}
        onClose={() => setAdding(false)}
        footer={
          <Button
            variant="accent"
            busy={create.isPending}
            disabled={companyB === 0}
            onClick={() => create.mutate()}
          >
            Create
          </Button>
        }
      >
        <ErrorAlert error={create.error} />
        <div className="form-grid">
          <Field label="Counterparty company" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={companyB}
                onChange={(e) => setForm({ ...form, companyBId: Number(e.target.value) })}
              >
                {counterparties.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.code} – {c.name}
                  </option>
                ))}
              </select>
            )}
          </Field>
          {ACCOUNT_FIELDS.map(([key, label]) => (
            <Field key={key} label={label} required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  value={form[key]}
                  onChange={(e) => setForm({ ...form, [key]: e.target.value })}
                />
              )}
            </Field>
          ))}
        </div>
      </Modal>
    </div>
  );
}
