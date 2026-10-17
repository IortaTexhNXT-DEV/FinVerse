import { useMutation, useQuery } from '@tanstack/react-query';
import { RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { humanize } from '@/utils/format';
import { collectionsApi } from './api';
import type { DispositionValue } from './api';
import { EditDialog } from './SetupDialog';
import type { Editing } from './SetupDialog';
import {
  attributeChoices,
  attributeLabel,
  attributeText,
  listLabel,
  parameterChoices,
  parameterDescription,
  parameterLabel,
  parameterValueText,
} from './setupLabels';
import './collections.css';
import { UserName } from '@/components/ui/UserName';

/** Attributes each list of disposition values carries (V1000). */
const ATTRIBUTES: Record<string, string[]> = {
  CLX_PR_DISPOSITION: ['category', 'tagging_owner', 'ops_action', 'allowed_roles'],
  CLX_UPP_DISPOSITION: ['requires_invoice', 'cashiering_action'],
};

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
  const attributeEdit = (v: DispositionValue, name: string): Editing => ({
    title: `${v.label}: ${attributeLabel(name)}`,
    label: attributeLabel(name),
    value: attributeOf(v, name),
    choices: attributeChoices(name),
    multiple: name === 'allowed_roles',
    hint: name === 'allowed_roles' ? 'None ticked: every collector may record it' : undefined,
    save: (value) =>
      collectionsApi.setAttribute(companyId, {
        typeCode: v.typeCode,
        code: v.code,
        attribute: name,
        value,
      }),
  });
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections"
        backTo="/collections"
        title="Collections Setup"
        description="Listing threshold, aging, exports, disposition rules and Unit Heads."
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
            {
              key: 'k',
              header: 'Parameter',
              render: (p) => <strong>{parameterLabel(p.key)}</strong>,
            },
            { key: 'v', header: 'Value', render: (p) => parameterValueText(p.key, p.value) },
            {
              key: 'd',
              header: 'Description',
              render: (p) => parameterDescription(p.key, p.description),
            },
            { key: 'u', header: 'Changed By', render: (p) => <UserName login={p.updatedBy} /> },
          ]}
          rows={s?.parameters ?? []}
          rowKey={(p) => p.key}
          loading={setup.isLoading}
          onRowClick={(p) =>
            setEditing({
              title: 'Change Parameter',
              label: parameterLabel(p.key),
              value: p.value,
              choices: parameterChoices(p.key),
              hint:
                p.key === 'CLX_AGING_BRACKETS'
                  ? 'Ranges such as 0-30,31-45,121- (open last range)'
                  : parameterDescription(p.key, p.description),
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
        {Object.entries(ATTRIBUTES).map(([list, attributes]) => (
          <DataTable
            key={list}
            caption={listLabel(list)}
            columns={[
              {
                key: 'c',
                header: listLabel(list),
                render: (v: DispositionValue) => (
                  <>
                    <strong>{v.label}</strong>
                    <div className="clx-muted">{v.code}</div>
                  </>
                ),
              },
              ...attributes.map((name) => ({
                key: name,
                header: attributeLabel(name),
                render: (v: DispositionValue) => (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setEditing(attributeEdit(v, name))}
                  >
                    {attributeText(name, attributeOf(v, name))}
                  </Button>
                ),
              })),
            ]}
            rows={(s?.dispositions ?? []).filter((v) => v.typeCode === list)}
            rowKey={(v) => `${v.typeCode}-${v.code}`}
            loading={setup.isLoading}
          />
        ))}
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
              render: (u) => <UserName login={u.headUsername} empty="From the parent unit" />,
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
