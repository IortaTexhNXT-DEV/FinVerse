import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { csfCaseApi } from '@/api/csfCases';
import type { CaseRoute } from '@/api/csfCases';
import { LovLabel } from '@/components/broking/LovLabel';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { CASE_LOV } from './caseCodes';

const ANY = 'Any';

function TurnaroundTimes() {
  const queryClient = useQueryClient();
  const tats = useQuery({ queryKey: ['csf-cases', 'tat'], queryFn: csfCaseApi.turnaroundTimes });
  const [edit, setEdit] = useState<Record<string, string>>({});
  const save = useMutation({
    mutationFn: (v: { caseType: string; tatDays: number; autoEscalate: boolean }) =>
      csfCaseApi.saveTurnaroundTime(v),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['csf-cases', 'tat'] }),
  });
  return (
    <Card title="Turnaround Times">
      <ErrorAlert error={tats.error ?? save.error} />
      <DataTable
        columns={[
          {
            key: 'type',
            header: 'Case Type',
            render: (t) => <LovLabel type={CASE_LOV.type} code={t.caseType} />,
          },
          {
            key: 'tat',
            header: 'Working Days',
            render: (t) => (
              <input
                className="input"
                aria-label={`Working days of ${t.caseType}`}
                inputMode="numeric"
                value={edit[t.caseType] ?? String(t.tatDays)}
                onChange={(e) => setEdit({ ...edit, [t.caseType]: e.target.value })}
              />
            ),
          },
          {
            key: 'auto',
            header: 'Escalated on Breach',
            render: (t) => (t.autoEscalate ? 'Yes' : 'No'),
          },
          {
            key: 'save',
            header: '',
            render: (t) => (
              <Button
                variant="secondary"
                disabled={edit[t.caseType] === undefined}
                onClick={() =>
                  save.mutate({
                    caseType: t.caseType,
                    tatDays: Number(edit[t.caseType]),
                    autoEscalate: t.autoEscalate,
                  })
                }
              >
                Save
              </Button>
            ),
          },
        ]}
        rows={tats.data ?? []}
        rowKey={(t) => t.caseType}
        loading={tats.isLoading}
        caption="Turnaround times"
      />
    </Card>
  );
}

const NEW_ROUTE: Omit<CaseRoute, 'id'> = {
  caseType: '',
  marketSegment: '',
  nature: '',
  fulfilmentUnit: '',
  escalationGroup: null,
  active: true,
};

function RoutingTable() {
  const queryClient = useQueryClient();
  const routes = useQuery({ queryKey: ['csf-cases', 'routes'], queryFn: csfCaseApi.routes });
  const [draft, setDraft] = useState(NEW_ROUTE);
  const save = useMutation({
    mutationFn: (v: { route: Omit<CaseRoute, 'id'>; id?: number }) =>
      csfCaseApi.saveRoute(v.route, v.id),
    onSuccess: () => {
      setDraft(NEW_ROUTE);
      void queryClient.invalidateQueries({ queryKey: ['csf-cases', 'routes'] });
    },
  });
  const label = (type: string, code: string | null) =>
    code ? <LovLabel type={type} code={code} /> : ANY;
  return (
    <Card title="Routing Table">
      <div className="stack">
        <ErrorAlert error={routes.error ?? save.error} title="The routing row was not saved" />
        <DataTable
          columns={[
            { key: 'type', header: 'Case Type', render: (r) => label(CASE_LOV.type, r.caseType) },
            {
              key: 'segment',
              header: 'Market Segment',
              render: (r) => label(CASE_LOV.segment, r.marketSegment),
            },
            {
              key: 'nature',
              header: 'Nature of Transaction',
              render: (r) => label(CASE_LOV.nature, r.nature),
            },
            {
              key: 'unit',
              header: 'Fulfilment Unit',
              render: (r) => label(CASE_LOV.unit, r.fulfilmentUnit),
            },
            {
              key: 'group',
              header: 'Escalation Group',
              render: (r) =>
                r.escalationGroup ? label(CASE_LOV.unit, r.escalationGroup) : 'Supervisors',
            },
            {
              key: 'active',
              header: 'Active',
              render: (r) => (
                <Button
                  variant="secondary"
                  onClick={() => save.mutate({ route: { ...r, active: !r.active }, id: r.id })}
                >
                  {r.active ? 'Deactivate' : 'Activate'}
                </Button>
              ),
            },
          ]}
          rows={routes.data ?? []}
          rowKey={(r) => r.id}
          loading={routes.isLoading}
          caption="Routing table"
        />
        <div className="filter-bar">
          <Field label="Case Type">
            {(id) => (
              <LovSelect
                id={id}
                type={CASE_LOV.type}
                value={draft.caseType}
                placeholder={ANY}
                onChange={(v) => setDraft({ ...draft, caseType: v })}
              />
            )}
          </Field>
          <Field label="Market Segment">
            {(id) => (
              <LovSelect
                id={id}
                type={CASE_LOV.segment}
                value={draft.marketSegment}
                placeholder={ANY}
                onChange={(v) => setDraft({ ...draft, marketSegment: v })}
              />
            )}
          </Field>
          <Field label="Nature of Transaction">
            {(id) => (
              <LovSelect
                id={id}
                type={CASE_LOV.nature}
                value={draft.nature}
                placeholder={ANY}
                onChange={(v) => setDraft({ ...draft, nature: v })}
              />
            )}
          </Field>
          <Field label="Fulfilment Unit" required>
            {(id) => (
              <LovSelect
                id={id}
                type={CASE_LOV.unit}
                value={draft.fulfilmentUnit}
                onChange={(v) => setDraft({ ...draft, fulfilmentUnit: v })}
                required
              />
            )}
          </Field>
          <Button
            disabled={draft.fulfilmentUnit === ''}
            busy={save.isPending}
            onClick={() => save.mutate({ route: draft })}
          >
            Add Routing Row
          </Button>
        </div>
      </div>
    </Card>
  );
}

function Members() {
  const queryClient = useQueryClient();
  const members = useQuery({ queryKey: ['csf-cases', 'members'], queryFn: csfCaseApi.members });
  const [unit, setUnit] = useState('');
  const [user, setUser] = useState('');
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['csf-cases', 'members'] });
  const add = useMutation({
    mutationFn: () => csfCaseApi.addMember(unit, user),
    onSuccess: () => {
      setUser('');
      refresh();
    },
  });
  const remove = useMutation({
    mutationFn: (id: number) => csfCaseApi.removeMember(id),
    onSuccess: refresh,
  });
  return (
    <Card title="Fulfilment Unit Members">
      <div className="stack">
        <ErrorAlert error={members.error ?? add.error ?? remove.error} />
        <DataTable
          columns={[
            {
              key: 'unit',
              header: 'Fulfilment Unit',
              render: (m) => <LovLabel type={CASE_LOV.unit} code={m.unitCode} />,
            },
            { key: 'user', header: 'User ID', render: (m) => m.username },
            {
              key: 'remove',
              header: '',
              render: (m) => (
                <Button variant="secondary" onClick={() => remove.mutate(m.id)}>
                  Remove
                </Button>
              ),
            },
          ]}
          rows={members.data ?? []}
          rowKey={(m) => m.id}
          loading={members.isLoading}
          caption="Fulfilment unit members"
        />
        <div className="filter-bar">
          <Field label="Fulfilment Unit" required>
            {(id) => (
              <LovSelect id={id} type={CASE_LOV.unit} value={unit} onChange={setUnit} required />
            )}
          </Field>
          <Field label="User ID" required>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={50}
                value={user}
                onChange={(e) => setUser(e.target.value)}
              />
            )}
          </Field>
          <Button
            disabled={unit === '' || user.trim() === ''}
            busy={add.isPending}
            onClick={() => add.mutate()}
          >
            Add Member
          </Button>
        </div>
      </div>
    </Card>
  );
}

/**
 * Case Routing (FR-CSF-051, 060, 061): the turnaround time of each case type, the routing table
 * that proposes the fulfilment unit of a case from its case type, market segment and nature of
 * transaction (a blank matches any value), and the members of the fulfilment units. The case lists
 * themselves are maintained on Lists of Values.
 */
export default function CaseRoutingPage() {
  return (
    <div className="stack">
      <PageHeader
        section="Broking Setup"
        title="Case Routing"
        description="Turnaround times, routing of the cases to the fulfilment units and their members."
      />
      <TurnaroundTimes />
      <RoutingTable />
      <Members />
    </div>
  );
}
