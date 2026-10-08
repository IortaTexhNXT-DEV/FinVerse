import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Send } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { ebApi } from '@/api/eb';
import type { ProgrammeRow, ProgrammeTab, SendRaResult } from '@/api/eb';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { countOf, formatDate, humanize } from '@/utils/format';
import { EB_SECTION } from '../EbPlaceholder';
import { BenefitLines, EbLov } from '../common/EbLabels';
import { EB_LOV, ebLabel } from '../common/ebCodes';
import { CYCLE_STAGES, PROGRAMME_TABS, sendRaSummary, tabOf } from './programmeList';

const COLUMNS: Column<ProgrammeRow>[] = [
  {
    key: 'programme',
    header: 'Programme',
    kind: 'code',
    render: (p) => <CellStack main={p.programmeNo} sub={p.name} />,
  },
  {
    key: 'client',
    header: 'Client',
    render: (p) => <CellStack main={p.clientName} sub={p.clientCode} />,
  },
  { key: 'lines', header: 'Lines', render: (p) => <BenefitLines codes={p.lines} /> },
  { key: 'team', header: 'Team', render: (p) => <EbLov type={EB_LOV.team} code={p.teamCode} /> },
  { key: 'ao', header: 'Account Officer', render: (p) => <UserName login={p.accountOfficer} /> },
  { key: 'expiry', header: 'Next Expiry', kind: 'date', render: (p) => formatDate(p.nextExpiry) },
  {
    key: 'cycle',
    header: 'Cycle',
    kind: 'code',
    render: (p) =>
      p.cycle ? <CellStack main={p.cycle.cycleNo} sub={ebLabel(p.cycle.businessType)} /> : '',
  },
  {
    key: 'stage',
    header: 'Stage',
    kind: 'status',
    render: (p) => (p.cycle ? <StatusBadge status={p.cycle.stage} /> : ''),
  },
  {
    key: 'status',
    header: 'Status',
    kind: 'status',
    render: (p) => <StatusBadge status={p.status} />,
  },
];

/** Result of Send RA per programme. */
function SendRaResults({
  results,
  onClose,
}: Readonly<{ results: SendRaResult[]; onClose: () => void }>) {
  return (
    <Modal
      open
      title="Renewal Advice Results"
      onClose={onClose}
      footer={<Button onClick={onClose}>Close</Button>}
    >
      <DataTable<SendRaResult>
        rows={results}
        rowKey={(r) => r.programmeId}
        columns={[
          { key: 'no', header: 'Programme', kind: 'code', render: (r) => r.programmeNo ?? '' },
          {
            key: 'result',
            header: 'Result',
            kind: 'status',
            render: (r) => (
              <StatusBadge
                status={r.sent ? 'SENT' : 'REJECTED'}
                label={r.sent ? 'Sent' : 'Not Sent'}
              />
            ),
          },
          { key: 'detail', header: 'Cycle / Reason', render: (r) => r.cycleNo ?? r.message ?? '' },
        ]}
      />
    </Modal>
  );
}

/**
 * Programmes (BRID-001-017; design 10.1; FR-EB-021, FR-EB-022): the EB programmes by status tab,
 * searched by programme number, client or name and filtered by cycle stage, with Send RA for the
 * selected programmes and New Programme.
 */
export default function ProgrammesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const [tab, setTab] = useState<ProgrammeTab>(tabOf(params.get('tab')));
  const [stage, setStage] = useState(params.get('stage') ?? '');
  const [team, setTeam] = useState('');
  const [text, setText] = useState('');
  const [showFilters, setShowFilters] = useState(params.get('stage') !== null);
  const [page, setPage] = useState(0);
  const selection = useRowSelection();
  const [confirming, setConfirming] = useState(false);
  const [results, setResults] = useState<SendRaResult[]>();
  const filters = { tab, stage, team, q: text };
  const list = useQuery({
    queryKey: ['eb', 'programmes', companyId, filters, page],
    queryFn: () => ebApi.programmes(companyId, filters, page),
    enabled: companyId > 0,
  });
  const rows = list.data?.content ?? [];
  const send = useMutation({
    mutationFn: () => ebApi.sendRa(companyId, selection.keys.map(Number)),
    onSuccess: async (sent) => {
      setConfirming(false);
      selection.clear();
      setResults(sent);
      toast.success(sendRaSummary(sent));
      await queryClient.invalidateQueries({ queryKey: ['eb'] });
    },
  });
  const reset = () => {
    setPage(0);
    selection.clear();
  };
  const marketing = can('EB_MARKET');
  const columns: Column<ProgrammeRow>[] = marketing
    ? [
        selectionColumn(
          rows,
          (p) => String(p.id),
          selection,
          (p) => p.programmeNo,
        ),
        ...COLUMNS,
      ]
    : COLUMNS;
  return (
    <div className="stack">
      <PageHeader
        section={EB_SECTION}
        title="Programmes"
        description="Employee Benefits programmes by renewal and placement status."
        actions={
          marketing && (
            <Link className="btn btn-accent" to="/eb/programmes/new">
              <Plus size={16} aria-hidden="true" /> New Programme
            </Link>
          )
        }
      />
      <Card flush>
        <Tabs
          tabs={PROGRAMME_TABS}
          active={tab}
          onChange={(t) => {
            setTab(t);
            reset();
          }}
        />
        <WorklistToolbar
          placeholder="Search programme, client or name"
          onSearch={(q) => {
            setText(q);
            reset();
          }}
          filters={{ open: showFilters, onToggle: () => setShowFilters((f) => !f) }}
        >
          {marketing && (
            <Button
              variant="secondary"
              icon={<Send size={16} />}
              disabled={selection.keys.length === 0}
              onClick={() => setConfirming(true)}
            >
              Send RA{selection.keys.length > 0 ? ` (${String(selection.keys.length)})` : ''}
            </Button>
          )}
        </WorklistToolbar>
        {showFilters && (
          <div className="worklist-filters form-grid">
            <Field label="Cycle Stage">
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={stage}
                  onChange={(e) => {
                    setStage(e.target.value);
                    reset();
                  }}
                >
                  <option value="">All stages</option>
                  {CYCLE_STAGES.map((s) => (
                    <option key={s} value={s}>
                      {humanize(s)}
                    </option>
                  ))}
                </select>
              )}
            </Field>
            <Field label="Team">
              {(id) => (
                <LovSelect
                  id={id}
                  type={EB_LOV.team}
                  value={team}
                  placeholder="All teams"
                  onChange={(v) => {
                    setTeam(v);
                    reset();
                  }}
                />
              )}
            </Field>
          </div>
        )}
        <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />
        <DataTable<ProgrammeRow>
          loading={list.isLoading}
          rows={rows}
          rowKey={(p) => p.id}
          onRowClick={(p) => void navigate(`/eb/programmes/${String(p.id)}`)}
          emptyMessage="No programmes to display"
          columns={columns}
        />
        <PageFooter data={list.data} noun="programmes" onPage={setPage} />
      </Card>
      {confirming && (
        <ConfirmDialog
          title="Send Renewal Advice"
          record={countOf(selection.keys.length, 'programme')}
          effect="The renewal advice is e-mailed, password-protected, to the HR contacts of each programme flagged for renewal, and its renewal cycle moves to Renewal Advice Sent."
          confirmLabel="Send RA"
          busy={send.isPending}
          error={send.error}
          onConfirm={() => send.mutate()}
          onClose={() => setConfirming(false)}
        />
      )}
      {results && <SendRaResults results={results} onClose={() => setResults(undefined)} />}
    </div>
  );
}
