import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Pencil, Plus, Send, XCircle } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { budgetApi } from '@/api/budget';
import type { Budget, BudgetVersionType } from '@/api/budget';
import { periodApi } from '@/api/periods';
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
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';

type Action = 'submit' | 'approve' | 'reject';

const EDITABLE = new Set(['DRAFT', 'REJECTED']);

function workflow(b: Budget): string {
  if (b.approvedBy) {
    return `Approved by ${b.approvedBy} · ${formatDateTime(b.approvedAt)}`;
  }
  if (b.rejectionReason) {
    return `Rejected: ${b.rejectionReason}`;
  }
  return b.submittedBy ? `Submitted by ${b.submittedBy}` : '';
}

/** Budget versions of the company: create, open the grid, submit, approve or reject. */
export default function BudgetsPage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [creating, setCreating] = useState(false);
  const [form, setForm] = useState({
    fiscalYear: 0,
    versionType: 'ORIGINAL' as BudgetVersionType,
    name: '',
  });

  const years = useQuery({
    queryKey: ['years', companyId],
    queryFn: () => periodApi.years(companyId),
    enabled: companyId > 0,
  });
  const budgets = useQuery({
    queryKey: ['budgets', companyId],
    queryFn: () => budgetApi.list(companyId),
    enabled: companyId > 0,
  });
  const fiscalYear = form.fiscalYear || (years.data?.[0]?.yearCode ?? 0);

  const create = useMutation({
    mutationFn: () => budgetApi.create({ companyId, ...form, fiscalYear }),
    onSuccess: async (b) => {
      await queryClient.invalidateQueries({ queryKey: ['budgets'] });
      setCreating(false);
      toast.success(`Budget version ${b.versionNo} created`);
      void navigate(`/planning/budgets/${b.id}`);
    },
  });
  const act = useMutation({
    mutationFn: ({ budget, action }: { budget: Budget; action: Action }) => {
      if (action === 'reject') {
        const reason = globalThis.prompt(`Reason for rejecting version ${budget.versionNo}`) ?? '';
        return budgetApi.reject(budget.id, reason);
      }
      return budgetApi[action](budget.id);
    },
    onSuccess: async (b) => {
      await queryClient.invalidateQueries({ queryKey: ['budgets'] });
      toast.success(`Budget version ${b.versionNo} is now ${b.status}`);
    },
  });

  const actions = (b: Budget) => (
    <div className="row">
      <Button
        size="sm"
        variant="ghost"
        icon={<Pencil size={14} />}
        onClick={() => void navigate(`/planning/budgets/${b.id}`)}
      >
        {EDITABLE.has(b.status) ? 'Edit' : 'View'}
      </Button>
      {EDITABLE.has(b.status) && (
        <Button
          size="sm"
          variant="secondary"
          icon={<Send size={14} />}
          onClick={() => act.mutate({ budget: b, action: 'submit' })}
        >
          Submit
        </Button>
      )}
      {b.status === 'SUBMITTED' && (
        <>
          <Button
            size="sm"
            variant="secondary"
            icon={<CheckCircle2 size={14} />}
            onClick={() => act.mutate({ budget: b, action: 'approve' })}
          >
            Approve
          </Button>
          <Button
            size="sm"
            variant="ghost"
            icon={<XCircle size={14} />}
            onClick={() => act.mutate({ budget: b, action: 'reject' })}
          >
            Reject
          </Button>
        </>
      )}
    </div>
  );

  return (
    <div className="stack">
      <PageHeader
        section="Planning & Closing"
        title="Budgets"
        description="Budget versions by fiscal year. A version is prepared, submitted and approved by a different user; the latest approved version drives Budget vs Actual."
        actions={
          <Button variant="accent" icon={<Plus size={16} />} onClick={() => setCreating(true)}>
            New version
          </Button>
        }
      />
      <ErrorAlert error={budgets.error ?? act.error} />
      <Card flush>
        <DataTable<Budget>
          loading={budgets.isLoading}
          rows={budgets.data ?? []}
          rowKey={(b) => b.id}
          columns={[
            { key: 'y', header: 'FY', render: (b) => <strong>{b.fiscalYear}</strong> },
            { key: 'v', header: 'Version', render: (b) => `v${b.versionNo} ${b.versionType}` },
            { key: 'n', header: 'Name', render: (b) => b.name },
            { key: 's', header: 'Status', render: (b) => <StatusBadge status={b.status} /> },
            { key: 'l', header: 'Lines', numeric: true, render: (b) => b.lineCount },
            {
              key: 't',
              header: 'Annual total',
              numeric: true,
              render: (b) => <Amount value={b.total} />,
            },
            {
              key: 'w',
              header: 'Workflow',
              render: workflow,
            },
            { key: 'a', header: 'Actions', render: actions },
          ]}
        />
      </Card>
      <Modal
        title="New budget version"
        open={creating}
        onClose={() => setCreating(false)}
        footer={
          <Button
            variant="accent"
            busy={create.isPending}
            disabled={form.name.trim() === '' || fiscalYear === 0}
            onClick={() => create.mutate()}
          >
            Create
          </Button>
        }
      >
        <ErrorAlert error={create.error} />
        <div className="form-grid">
          <Field label="Fiscal year" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={fiscalYear}
                onChange={(e) => setForm({ ...form, fiscalYear: Number(e.target.value) })}
              >
                {(years.data ?? []).map((y) => (
                  <option key={y.id} value={y.yearCode}>
                    {y.yearCode}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Version type" required hint="A revision starts from the approved version">
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.versionType}
                onChange={(e) =>
                  setForm({ ...form, versionType: e.target.value as BudgetVersionType })
                }
              >
                <option value="ORIGINAL">Original</option>
                <option value="REVISED">Revised</option>
              </select>
            )}
          </Field>
          <Field label="Name" required>
            {(id) => (
              <input
                id={id}
                className="input"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            )}
          </Field>
        </div>
      </Modal>
    </div>
  );
}
