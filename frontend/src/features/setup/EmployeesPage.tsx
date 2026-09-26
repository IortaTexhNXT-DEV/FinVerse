import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { mastersApi } from '@/api/masters';
import type { DimensionValue } from '@/api/masters';
import type { Branch } from '@/api/types';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId, useDefaultBranchId, useWorkspace } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { frbsSetupApi } from './frbsSetupApi';
import type { Employee, EmployeeInput } from './frbsSetupApi';
import { employeeInput, employeeProblems, newEmployee } from './setupForms';
import type { FieldErrors } from './setupForms';

/**
 * Employee master with cost centres (DIS 3.30.1): used for employee payments and cash advances
 * and for the headcount per cost centre report ORG-HEADCOUNT-CC (DIS 3.30.2).
 */
export default function EmployeesPage() {
  const companyId = useCompanyId();
  const defaultBranch = useDefaultBranchId();
  const { branches } = useWorkspace();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<Employee | null>(null);
  const [form, setForm] = useState<EmployeeInput | null>(null);
  const [checked, setChecked] = useState(false);

  const query = useQuery({
    queryKey: ['employees', companyId, search, page],
    queryFn: () => frbsSetupApi.employees(companyId, search, page),
    enabled: companyId > 0,
  });
  const costCentres = useQuery({
    queryKey: ['dimensions', companyId, 'COST_CENTER'],
    queryFn: () => mastersApi.dimensions(companyId, 'COST_CENTER'),
    enabled: companyId > 0,
  });
  const save = useMutation({
    mutationFn: (body: EmployeeInput) =>
      editing === null
        ? frbsSetupApi.createEmployee(body)
        : frbsSetupApi.updateEmployee(editing.id, body),
    onSuccess: async (e) => {
      setForm(null);
      await queryClient.invalidateQueries({ queryKey: ['employees'] });
      toast.success(`Employee ${e.employeeNo} saved`);
    },
  });
  const problems = form === null ? {} : employeeProblems(form);
  const open = (e: Employee | null) => {
    setEditing(e);
    setChecked(false);
    save.reset();
    setForm(e === null ? newEmployee(companyId, defaultBranch) : employeeInput(companyId, e));
  };
  const submit = () => {
    setChecked(true);
    if (form !== null && Object.keys(problems).length === 0) {
      save.mutate(form);
    }
  };
  const branchCode = (id: number) => branches.find((b) => b.id === id)?.code ?? String(id);
  const set = (patch: Partial<EmployeeInput>) =>
    setForm((f) => (f === null ? f : { ...f, ...patch }));

  return (
    <div className="stack">
      <PageHeader
        section="Setup"
        title="Employees"
        description="Employees with their branch and cost centre, used by Disbursement and for the headcount per cost centre."
        actions={
          can('EMPLOYEE_MAINTAIN') && (
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => open(null)}>
              Add Employee
            </Button>
          )
        }
      />
      <ErrorAlert error={query.error} />
      <Card
        flush
        title={`${query.data?.totalElements ?? 0} employees`}
        actions={
          <input
            className="input"
            aria-label="Search employees"
            placeholder="Search number, name or cost centre"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
        }
      >
        <DataTable<Employee>
          loading={query.isLoading}
          rows={query.data?.content ?? []}
          rowKey={(e) => e.id}
          caption="Employees"
          emptyMessage="No employee found"
          onRowClick={can('EMPLOYEE_MAINTAIN') ? open : undefined}
          columns={[
            { key: 'no', header: 'Employee No.', render: (e) => <strong>{e.employeeNo}</strong> },
            { key: 'name', header: 'Name', render: (e) => e.fullName },
            { key: 'br', header: 'Branch', render: (e) => branchCode(e.branchId) },
            { key: 'cc', header: 'Cost Centre', render: (e) => e.costCenter },
            { key: 'pos', header: 'Position', render: (e) => e.position ?? '' },
            { key: 'hired', header: 'Hired', render: (e) => formatDate(e.hiredOn) },
            {
              key: 'st',
              header: 'Status',
              render: (e) => <StatusBadge status={e.active ? 'ACTIVE' : 'INACTIVE'} />,
            },
          ]}
        />
        <PageFooter data={query.data} onPage={setPage} />
      </Card>
      <Modal
        title={editing === null ? 'Add employee' : `Employee ${editing.employeeNo}`}
        open={form !== null}
        onClose={() => setForm(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setForm(null)}>
              Cancel
            </Button>
            <Button variant="accent" busy={save.isPending} onClick={submit}>
              Save Employee
            </Button>
          </>
        }
      >
        {form !== null && (
          <div className="stack">
            <ErrorAlert error={save.error} />
            <EmployeeFields
              form={form}
              editing={editing !== null}
              branches={branches}
              costCentres={costCentres.data ?? []}
              errors={checked ? problems : {}}
              onChange={set}
            />
          </div>
        )}
      </Modal>
    </div>
  );
}

interface FieldsProps {
  form: EmployeeInput;
  editing: boolean;
  branches: Branch[];
  costCentres: DimensionValue[];
  errors: FieldErrors<EmployeeInput>;
  onChange: (patch: Partial<EmployeeInput>) => void;
}

function EmployeeFields({
  form,
  editing,
  branches,
  costCentres,
  errors,
  onChange,
}: Readonly<FieldsProps>) {
  return (
    <div className="form-grid">
      <Field label="Employee no." required error={errors.employeeNo}>
        {(id) => (
          <input
            id={id}
            className="input"
            disabled={editing}
            value={form.employeeNo}
            onChange={(e) => onChange({ employeeNo: e.target.value.toUpperCase() })}
          />
        )}
      </Field>
      <Field label="Full name" required error={errors.fullName}>
        {(id) => (
          <input
            id={id}
            className="input"
            value={form.fullName}
            onChange={(e) => onChange({ fullName: e.target.value })}
          />
        )}
      </Field>
      <Field label="Branch" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.branchId}
            onChange={(e) => onChange({ branchId: Number(e.target.value) })}
          >
            {branches.map((b) => (
              <option key={b.id} value={b.id}>
                {b.code} – {b.name}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Cost centre" required error={errors.costCenter}>
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.costCenter}
            onChange={(e) => onChange({ costCenter: e.target.value })}
          >
            <option value="">Select…</option>
            {costCentres.map((d) => (
              <option key={d.code} value={d.code}>
                {d.code} – {d.name}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Position">
        {(id) => (
          <input
            id={id}
            className="input"
            value={form.position ?? ''}
            onChange={(e) => onChange({ position: e.target.value })}
          />
        )}
      </Field>
      <Field label="E-mail" error={errors.email}>
        {(id) => (
          <input
            id={id}
            className="input"
            type="email"
            value={form.email ?? ''}
            onChange={(e) => onChange({ email: e.target.value })}
          />
        )}
      </Field>
      <Field label="Payee party code" hint="Party of type Employee, for payments">
        {(id) => (
          <input
            id={id}
            className="input"
            value={form.partyCode ?? ''}
            onChange={(e) => onChange({ partyCode: e.target.value.toUpperCase() })}
          />
        )}
      </Field>
      <Field label="Hired on">
        {(id) => (
          <input
            id={id}
            className="input"
            type="date"
            value={form.hiredOn ?? ''}
            onChange={(e) => onChange({ hiredOn: e.target.value || undefined })}
          />
        )}
      </Field>
      <Field label="Separated on" error={errors.separatedOn} hint="Makes the employee inactive">
        {(id) => (
          <input
            id={id}
            className="input"
            type="date"
            value={form.separatedOn ?? ''}
            onChange={(e) => onChange({ separatedOn: e.target.value || undefined })}
          />
        )}
      </Field>
    </div>
  );
}
