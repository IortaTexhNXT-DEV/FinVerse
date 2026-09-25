import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { humanize } from '@/utils/format';
import { collectionsApi } from './api';
import type { DispositionValue } from './api';
import './collections.css';

/** Attributes each LOV type carries (V1000). */
const ATTRIBUTES: Record<string, string[]> = {
  CLX_PR_DISPOSITION: ['category', 'tagging_owner', 'ops_action', 'allowed_roles'],
  CLX_UPP_DISPOSITION: ['requires_invoice', 'cashiering_action'],
};

const HINTS: Record<string, string> = {
  category: 'A, B or C',
  tagging_owner: 'MARKETING or OPERATIONS',
  ops_action: 'NONE, CWT2307_REVERSAL, DP_REVERSAL, CHECK_PICKUP or CANCEL_REQUEST',
  allowed_roles: 'Role codes separated by commas; blank = every collector',
  requires_invoice: 'true or false',
  cashiering_action: 'APPLY_TO_INVOICE, REFUND, RECLASS, TRANSFER or NONE',
};

interface Editing {
  title: string;
  label: string;
  value: string;
  hint?: string;
  save: (value: string) => Promise<unknown>;
}

function EditDialog({ editing, onClose }: Readonly<{ editing: Editing; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [value, setValue] = useState(editing.value);
  const save = useMutation({
    mutationFn: () => editing.save(value.trim()),
    onSuccess: async () => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: ['collections', 'setup'] });
      toast.success(`${editing.label} saved`);
    },
  });
  return (
    <Modal
      title={editing.title}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={save.isPending} onClick={() => save.mutate()}>
            Save
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <Field label={editing.label} hint={editing.hint}>
          {(id) => (
            <input
              id={id}
              className="input"
              value={value}
              onChange={(e) => setValue(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

function attributeOf(v: DispositionValue, name: string): string {
  return v.attributes.find((a) => a.attribute === name)?.value ?? '';
}

/**
 * Collections Setup (BRCLXN.005-007, 011, 016/017, 037): the Collections parameters (threshold,
 * aging basis and brackets, export cap, edit lock), the attributes of the disposition values, the
 * Unit Head of each sales unit, and a refresh of the worklist on demand.
 */
export default function SetupPage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const [editing, setEditing] = useState<Editing>();
  const setup = useQuery({
    queryKey: ['collections', 'setup', companyId],
    queryFn: () => collectionsApi.setup(companyId),
    enabled: companyId > 0,
  });
  const refresh = useMutation({
    mutationFn: () => collectionsApi.refresh(companyId),
    onSuccess: (r) => toast.success(r.message),
  });
  const s = setup.data;
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections"
        backTo="/collections"
        title="Collections Setup"
        description="Listing threshold, aging, exports, disposition rules and Unit Heads. Disposition values are maintained on the LOV screen."
        actions={
          <Button
            icon={<RefreshCw size={16} />}
            busy={refresh.isPending}
            onClick={() => refresh.mutate()}
          >
            Refresh Worklist Now
          </Button>
        }
      />
      <ErrorAlert error={setup.error ?? refresh.error} />
      <Card title="Parameters" flush>
        <DataTable
          caption="Collections parameters"
          columns={[
            { key: 'k', header: 'Parameter', render: (p) => <strong>{p.key}</strong> },
            { key: 'v', header: 'Value', render: (p) => p.value },
            { key: 'd', header: 'Description', render: (p) => p.description },
            { key: 'u', header: 'Changed By', render: (p) => p.updatedBy },
          ]}
          rows={s?.parameters ?? []}
          rowKey={(p) => p.key}
          loading={setup.isLoading}
          onRowClick={(p) =>
            setEditing({
              title: 'Change Parameter',
              label: p.key,
              value: p.value,
              hint:
                p.key === 'CLX_AGING_BRACKETS'
                  ? 'Ranges such as 0-30,31-45,121- (open last range)'
                  : p.description,
              save: (value) => collectionsApi.updateParameter(companyId, p.key, value),
            })
          }
        />
      </Card>
      <Card
        title="Disposition Rules"
        actions={<Link to="/broking-setup/lists">Maintain LOV Values</Link>}
        flush
      >
        <DataTable
          caption="Disposition attributes"
          columns={[
            { key: 't', header: 'List', render: (v) => humanize(v.typeCode.replace('CLX_', '')) },
            {
              key: 'c',
              header: 'Disposition',
              render: (v) => (
                <>
                  <strong>{v.label}</strong>
                  <div className="clx-muted">{v.code}</div>
                </>
              ),
            },
            ...[
              'category',
              'tagging_owner',
              'ops_action',
              'allowed_roles',
              'requires_invoice',
              'cashiering_action',
            ].map((name) => ({
              key: name,
              header: humanize(name),
              render: (v: DispositionValue) =>
                ATTRIBUTES[v.typeCode]?.includes(name) === true ? (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() =>
                      setEditing({
                        title: `${v.label}: ${humanize(name)}`,
                        label: humanize(name),
                        value: attributeOf(v, name),
                        hint: HINTS[name],
                        save: (value) =>
                          collectionsApi.setAttribute(companyId, {
                            typeCode: v.typeCode,
                            code: v.code,
                            attribute: name,
                            value,
                          }),
                      })
                    }
                  >
                    {attributeOf(v, name) || 'Set'}
                  </Button>
                ) : (
                  ''
                ),
            })),
          ]}
          rows={s?.dispositions ?? []}
          rowKey={(v) => `${v.typeCode}-${v.code}`}
          loading={setup.isLoading}
        />
      </Card>
      <Card title="Unit Heads" flush>
        <DataTable
          caption="Sales units"
          columns={[
            { key: 'c', header: 'Unit', render: (u) => <strong>{u.code}</strong> },
            { key: 'n', header: 'Name', render: (u) => u.name },
            { key: 'l', header: 'Level', render: (u) => humanize(u.level) },
            { key: 'p', header: 'Parent', render: (u) => u.parentCode ?? '' },
            {
              key: 'h',
              header: 'Unit Head',
              render: (u) => u.headUsername ?? 'From the parent unit',
            },
          ]}
          rows={s?.units ?? []}
          rowKey={(u) => u.code}
          loading={setup.isLoading}
          onRowClick={(u) =>
            setEditing({
              title: `Unit Head of ${u.name}`,
              label: 'Unit Head (user name)',
              value: u.headUsername ?? '',
              hint: 'Blank to take the head of the parent unit',
              save: (value) => collectionsApi.setUnitHead(companyId, u.code, value),
            })
          }
        />
      </Card>
      {editing !== undefined && (
        <EditDialog editing={editing} onClose={() => setEditing(undefined)} />
      )}
    </div>
  );
}
