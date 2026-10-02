import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import type { VersionView } from '@/api/renewal';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { RowActions } from '@/components/ui/RowActions';
import { versionActions } from '../common/presentation';

/** An editable column of a rule. */
export interface RuleField<R> {
  key: string;
  label: string;
  get: (rule: R) => string;
  set: (rule: R, value: string) => R;
  /** Choices; an input when absent. */
  options?: { code: string; label: string }[];
  numeric?: boolean;
}

interface Api<R> {
  list: (companyId: number) => Promise<VersionView<R>[]>;
  save: (
    companyId: number,
    input: { id?: number; effectiveFrom?: string; description?: string; rules: R[] },
  ) => Promise<VersionView<R>>;
  decide: (
    companyId: number,
    id: number,
    step: string,
    remarks?: string,
  ) => Promise<VersionView<R>>;
}

function RuleEditor<R>({
  title,
  base,
  fields,
  blank,
  api,
  onClose,
}: Readonly<{
  title: string;
  base: VersionView<R> | undefined;
  fields: RuleField<R>[];
  blank: (priority: number) => R;
  api: Api<R>;
  onClose: () => void;
}>) {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const [rules, setRules] = useState<R[]>(base?.rules ?? [blank(10)]);
  const [from, setFrom] = useState('');
  const [description, setDescription] = useState('');
  const save = useMutation({
    mutationFn: () =>
      api.save(companyId, {
        effectiveFrom: from || undefined,
        description: description.trim() || undefined,
        rules,
      }),
    onSuccess: async () => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'setup'] });
    },
  });
  const update = (i: number, field: RuleField<R>, value: string) =>
    setRules((rs) => rs.map((r, j) => (j === i ? field.set(r, value) : r)));
  return (
    <Modal
      open
      title={title}
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={save.isPending} disabled={rules.length === 0} onClick={() => save.mutate()}>
            Save Draft
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <Field label="Effective From">
          {(id) => <DateInput id={id} value={from} onChange={(e) => setFrom(e.target.value)} />}
        </Field>
        <Field label="Description">
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={200}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          )}
        </Field>
      </div>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              {fields.map((f) => (
                <th key={f.key}>{f.label}</th>
              ))}
              <th aria-label="Remove" />
            </tr>
          </thead>
          <tbody>
            {rules.map((r, i) => (
              <tr key={`${String(i)}-${fields[0]?.get(r) ?? ''}`}>
                {fields.map((f) => (
                  <td key={f.key}>
                    {f.options ? (
                      <select
                        className="select"
                        aria-label={f.label}
                        value={f.get(r)}
                        onChange={(e) => update(i, f, e.target.value)}
                      >
                        {f.options.map((o) => (
                          <option key={o.code} value={o.code}>
                            {o.label}
                          </option>
                        ))}
                      </select>
                    ) : (
                      <input
                        className="input"
                        aria-label={f.label}
                        inputMode={f.numeric === true ? 'numeric' : undefined}
                        value={f.get(r)}
                        onChange={(e) => update(i, f, e.target.value)}
                      />
                    )}
                  </td>
                ))}
                <td>
                  <Button
                    size="sm"
                    variant="ghost"
                    aria-label="Remove rule"
                    icon={<Trash2 size={14} />}
                    onClick={() => setRules((rs) => rs.filter((_, j) => j !== i))}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Button
        variant="secondary"
        icon={<Plus size={16} />}
        onClick={() => setRules((rs) => [...rs, blank((rs.length + 1) * 10)])}
      >
        Add Rule
      </Button>
    </Modal>
  );
}

/**
 * Versioned rules with maker and checker (BRRN.023, 031): the versions, the rules of the one
 * selected, a new draft from the active version, Submit, Activate (which retires the active one)
 * and Reject.
 */
export function RuleVersionsTab<R>({
  title,
  queryKey,
  api,
  fields,
  blank,
}: Readonly<{
  title: string;
  queryKey: string;
  api: Api<R>;
  fields: RuleField<R>[];
  blank: (priority: number) => R;
}>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<number>();
  const [editing, setEditing] = useState(false);
  const versions = useQuery({
    queryKey: ['renewal', 'setup', queryKey, companyId],
    queryFn: () => api.list(companyId),
    enabled: companyId > 0,
  });
  const step = useMutation({
    mutationFn: ({ id, name, remarks }: { id: number; name: string; remarks?: string }) =>
      api.decide(companyId, id, name, remarks),
    onSuccess: async (v) => {
      toast.success(`Version ${String(v.versionNo)} ${v.status.toLowerCase()}`);
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'setup'] });
    },
  });
  const list = versions.data ?? [];
  const active = list.find((v) => v.status === 'ACTIVE');
  const shown = list.find((v) => v.id === selected) ?? active ?? list[0];
  const maintain = can('RNW_SETUP');
  return (
    <div className="stack">
      <Card
        title={title}
        flush
        actions={
          maintain && (
            <Button icon={<Plus size={16} />} onClick={() => setEditing(true)}>
              New Version
            </Button>
          )
        }
      >
        <ErrorAlert error={versions.error ?? step.error} />
        <DataTable<VersionView<R>>
          loading={versions.isLoading}
          rows={list}
          rowKey={(v) => v.id}
          selectedKey={shown?.id}
          onRowClick={(v) => setSelected(v.id)}
          emptyMessage="No versions"
          columns={[
            { key: 'no', header: 'Version', kind: 'code', render: (v) => v.versionNo },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (v) => <StatusBadge status={v.status} />,
            },
            {
              key: 'from',
              header: 'Effective',
              kind: 'date',
              render: (v) => formatDate(v.effectiveFrom),
            },
            { key: 'desc', header: 'Description', render: (v) => v.description ?? '' },
            { key: 'maker', header: 'Maker', render: (v) => <UserName login={v.maker ?? ''} /> },
            {
              key: 'checker',
              header: 'Approved by',
              render: (v) => <UserName login={v.approvedBy ?? ''} />,
            },
            {
              key: 'act',
              header: '',
              render: (v) => (
                <RowActions
                  record={`Version ${String(v.versionNo)}`}
                  actions={versionActions(v.status, v.versionNo, maintain, {
                    submit: () => step.mutate({ id: v.id, name: 'SUBMIT' }),
                    activate: () => step.mutateAsync({ id: v.id, name: 'ACTIVATE' }),
                    reject: (reason) =>
                      step.mutateAsync({ id: v.id, name: 'REJECT', remarks: reason }),
                  })}
                />
              ),
            },
          ]}
        />
      </Card>
      {shown && (
        <Card title={`Rules of version ${String(shown.versionNo)}`} flush>
          <DataTable<R>
            rows={shown.rules}
            rowKey={(r) => fields[0]?.get(r) ?? ''}
            emptyMessage="No rules"
            columns={fields.map((f) => ({
              key: f.key,
              header: f.label,
              render: (r: R) => f.options?.find((o) => o.code === f.get(r))?.label ?? f.get(r),
            }))}
          />
        </Card>
      )}
      {editing && (
        <RuleEditor
          title={`New version of the ${title.toLowerCase()}`}
          base={active}
          fields={fields}
          blank={blank}
          api={api}
          onClose={() => setEditing(false)}
        />
      )}
    </div>
  );
}
