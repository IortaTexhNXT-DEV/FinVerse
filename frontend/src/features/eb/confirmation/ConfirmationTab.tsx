import { useQuery } from '@tanstack/react-query';
import { Ban, CheckSquare, Rocket } from 'lucide-react';
import { useState } from 'react';
import type { CycleAccount, CycleView, ProgrammeView } from '@/api/eb';
import { ebMarketApi } from '@/api/ebMarket';
import type { Confirmation, Proposal } from '@/api/ebMarket';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { ebLabel } from '../common/ebCodes';
import { currentCycleOf, useCycleList } from '../common/useCycleList';
import { useEbMutation } from '../common/useEbMutation';
import { savedRecommendation } from '../comparative/comparativeSteps';
import { ConfirmationDialog } from './ConfirmationDialog';

type Line = Confirmation['lines'][number];

/** Stages in which the confirmation may still be voided. */
const VOIDABLE = new Set(['CONFIRMED', 'THRESHOLD_APPROVAL']);

const LINE_COLUMNS: Column<Line>[] = [
  { key: 'line', header: 'Line', kind: 'center', render: (l) => l.lineNo },
  { key: 'benefit', header: 'Benefit Line', render: (l) => l.benefitLine },
  { key: 'insurer', header: 'Insurer', kind: 'code', render: (l) => l.insurerCode },
  {
    key: 'premium',
    header: 'Annual Premium',
    kind: 'amount',
    render: (l) => <Amount value={l.annualPremium} />,
  },
  {
    key: 'si',
    header: 'Sum Insured',
    kind: 'amount',
    render: (l) => <Amount value={l.sumInsured} />,
  },
  { key: 'arn', header: 'Account', kind: 'code', render: (l) => l.accountArn ?? '' },
];

/** The recorded confirmation with its chosen proposals and the accounts placed from them. */
function ConfirmationCard({ c }: Readonly<{ c: Confirmation }>) {
  return (
    <div className="stack">
      <DefinitionGrid
        columns={2}
        items={[
          {
            label: 'Status',
            value: (
              <StatusBadge status={c.status} tone={c.status === 'ACTIVE' ? 'success' : undefined} />
            ),
          },
          { label: 'Confirmed By', value: ebLabel(c.channel) },
          { label: 'Confirmed On', value: formatDate(c.confirmedOn) },
          { label: 'Recorded', value: formatDateTime(c.recordedAt) },
          { label: 'Recorded By', value: <UserName login={c.recordedBy} /> },
          {
            label: c.voidReason ? 'Void Reason' : 'Remarks',
            value: c.voidReason ?? c.remarks ?? '',
          },
        ]}
      />
      <DataTable<Line> rows={c.lines} rowKey={(l) => l.lineNo} columns={LINE_COLUMNS} />
    </div>
  );
}

type Mode = 'record' | 'void' | 'trigger';

/** The confirmations of the cycle, the active one first. */
function Confirmations({ rows, loading }: Readonly<{ rows: Confirmation[]; loading: boolean }>) {
  if (rows.length === 0 && !loading) {
    return <p className="muted">No confirmation recorded on the current cycle</p>;
  }
  return (
    <div className="stack">
      {rows.map((c) => (
        <ConfirmationCard key={c.id} c={c} />
      ))}
    </div>
  );
}

/** The recommendation of the comparative presented on the cycle, the default choices. */
function useRecommendation(programmeId: number, cycleId: number | undefined) {
  const companyId = useCompanyId();
  const comparatives = useQuery({
    queryKey: ['eb', 'comparatives', programmeId],
    queryFn: () => ebMarketApi.comparatives(companyId, programmeId),
  });
  const presented = comparatives.data?.find(
    (c) => c.cycleId === cycleId && c.status === 'PRESENTED',
  );
  const view = useQuery({
    queryKey: ['eb', 'comparative', presented?.id],
    queryFn: () => ebMarketApi.comparative(companyId, presented?.id ?? 0),
    enabled: presented !== undefined,
  });
  return view.data ? savedRecommendation(view.data) : {};
}

/** The actions of the tab: Record, Void, Trigger Placement, as the cycle allows. */
function ConfirmationActions({
  stage,
  active,
  onMode,
}: Readonly<{ stage: string; active: boolean; onMode: (m: Mode) => void }>) {
  if (!active) {
    return (
      <Button
        variant="secondary"
        size="sm"
        icon={<CheckSquare size={14} />}
        onClick={() => onMode('record')}
      >
        Record Confirmation
      </Button>
    );
  }
  return (
    <>
      {VOIDABLE.has(stage) && (
        <Button
          variant="secondary"
          size="sm"
          icon={<Ban size={14} />}
          onClick={() => onMode('void')}
        >
          Void Confirmation
        </Button>
      )}
      {stage === 'CONFIRMED' && (
        <Button
          variant="accent"
          size="sm"
          icon={<Rocket size={14} />}
          onClick={() => onMode('trigger')}
        >
          Trigger Placement
        </Button>
      )}
    </>
  );
}

/** Void Confirmation and Trigger Placement on the current cycle. */
function CycleStepDialog({
  mode,
  cycle,
  onClose,
}: Readonly<{ mode: 'void' | 'trigger'; cycle: CycleView; onClose: () => void }>) {
  const voiding = useEbMutation(
    (c, reason: string) => ebMarketApi.voidConfirmation(c, cycle.id, reason),
    'Confirmation voided',
    onClose,
  );
  const trigger = useEbMutation(
    (c, id: number) => ebMarketApi.triggerPlacement(c, id),
    (accounts: CycleAccount[]) =>
      `${String(accounts.length)} account(s) submitted for placement: ${accounts.map((a) => a.arn).join(', ')}`,
    onClose,
  );
  if (mode === 'void') {
    return (
      <ConfirmDialog
        title="Void Confirmation"
        record={cycle.cycleNo}
        effect="The cycle goes back to With Client; record the client's corrected confirmation next."
        confirmLabel="Void"
        reason="required"
        destructive
        busy={voiding.isPending}
        error={voiding.error}
        onConfirm={(reason) => voiding.mutate(reason)}
        onClose={onClose}
      />
    );
  }
  return (
    <ConfirmDialog
      title="Trigger Placement"
      record={cycle.cycleNo}
      effect="One account per confirmed benefit line is created with the chosen insurer, the confirmation and Broker on Record are attached, and each account is submitted for placement."
      confirmLabel="Trigger Placement"
      busy={trigger.isPending}
      error={trigger.error}
      onConfirm={() => trigger.mutate(cycle.id)}
      onClose={onClose}
    />
  );
}

/**
 * Confirmation tab: the client's confirmation of the chosen proposals of the current cycle;
 * Record Confirmation, Void Confirmation and Trigger Placement, which creates one account per
 * benefit line with the chosen insurer and submits it for placement once the value threshold is
 * approved and the required documents are on file.
 */
export function ConfirmationTab({ programme }: Readonly<{ programme: ProgrammeView }>) {
  const { can } = useAuth();
  const cycle = currentCycleOf(programme);
  const [mode, setMode] = useState<Mode>();
  const close = () => setMode(undefined);
  const confirmations = useCycleList('confirmations', cycle?.id, ebMarketApi.confirmations);
  const proposals = useCycleList<Proposal>('proposals', cycle?.id, ebMarketApi.proposals);
  const recommended = useRecommendation(programme.id, cycle?.id);
  const active = confirmations.rows.some((c) => c.status === 'ACTIVE');
  return (
    <Card
      title="Client Confirmation"
      actions={
        can('EB_MARKET') &&
        cycle && <ConfirmationActions stage={cycle.stage} active={active} onMode={setMode} />
      }
    >
      <ErrorAlert error={confirmations.error} onRetry={() => void confirmations.refetch()} />
      <Confirmations rows={confirmations.rows} loading={confirmations.isLoading} />
      {mode === 'record' && cycle && (
        <ConfirmationDialog
          cycleId={cycle.id}
          lines={programme.lines.filter((l) => l.active)}
          proposals={proposals.rows.filter((p) => p.status === 'VALIDATED')}
          recommended={recommended}
          onClose={close}
        />
      )}
      {mode !== undefined && mode !== 'record' && cycle && (
        <CycleStepDialog mode={mode} cycle={cycle} onClose={close} />
      )}
    </Card>
  );
}
