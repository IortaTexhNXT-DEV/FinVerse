import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileDown, FileSpreadsheet, RefreshCw, SlidersHorizontal } from 'lucide-react';
import { useState } from 'react';
import { productMaintApi } from '@/api/productmaint';
import type { ComparativeOutput, ComparativeRow, PackageRequest } from '@/api/productmaint';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { TextInput } from '@/features/assets/FormControls';
import { formatDateTime } from '@/utils/format';
import { COMPARATIVE_FIELDS } from './packageRequest';

function orDash(value: string | undefined): string {
  return value === undefined || value === '' ? '—' : value;
}

const ROW_COLUMNS: Column<ComparativeRow>[] = [
  {
    key: 'insurer',
    header: 'Insurer',
    render: (r) => (
      <>
        <strong>{r.insurerName}</strong>
        {r.lowest && <span className="tag">Lowest rate</span>}
      </>
    ),
  },
  { key: 'outcome', header: 'Outcome', render: (r) => <StatusBadge status={r.outcome} /> },
  { key: 'rate', header: 'Rate %', numeric: true, render: (r) => r.rate ?? '—' },
  { key: 'min', header: 'Minimum', numeric: true, render: (r) => r.minimumPremium ?? '—' },
  { key: 'coverages', header: 'Coverages', render: (r) => orDash(r.coverages) },
  { key: 'deductibles', header: 'Deductibles', render: (r) => orDash(r.deductibles) },
  { key: 'conditions', header: 'Conditions', render: (r) => r.conditions ?? '—' },
];

function toggle(list: string[], value: string): string[] {
  return list.includes(value) ? list.filter((v) => v !== value) : [...list, value];
}

function ClientViewDialog({
  request,
  insurers,
  onClose,
  onDone,
}: Readonly<{
  request: PackageRequest;
  insurers: { code: string; name: string }[];
  onClose: () => void;
  onDone: () => Promise<void>;
}>) {
  const [title, setTitle] = useState(`Client view – ${request.clientName ?? request.title}`);
  const [fields, setFields] = useState<string[]>(['OUTCOME', 'RATE', 'COVERAGES', 'DEDUCTIBLES']);
  const [chosen, setChosen] = useState<string[]>(insurers.map((i) => i.code));
  const run = useMutation({
    mutationFn: () => productMaintApi.clientView(request.id, title.trim(), fields, chosen),
    onSuccess: onDone,
  });
  return (
    <Modal
      open
      title="Generate Client View"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={run.isPending}
            disabled={title.trim() === '' || fields.length === 0 || chosen.length === 0}
            onClick={() => run.mutate()}
          >
            Generate
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={run.error} />
        <TextInput label="Title" required value={title} onChange={setTitle} />
        <fieldset className="field">
          <legend>Fields shown</legend>
          <div className="insurer-choices">
            {COMPARATIVE_FIELDS.map((f) => (
              <label key={f.code} className="checkbox">
                <input
                  type="checkbox"
                  checked={fields.includes(f.code)}
                  onChange={() => setFields(toggle(fields, f.code))}
                />
                {f.label}
              </label>
            ))}
          </div>
        </fieldset>
        <fieldset className="field">
          <legend>Insurers shown</legend>
          <div className="insurer-choices">
            {insurers.map((i) => (
              <label key={i.code} className="checkbox">
                <input
                  type="checkbox"
                  checked={chosen.includes(i.code)}
                  onChange={() => setChosen(toggle(chosen, i.code))}
                />
                {i.name}
              </label>
            ))}
          </div>
        </fieldset>
        <p className="muted">
          A client view is derived from the current master and never changes a value (PMADD03).
        </p>
      </div>
    </Modal>
  );
}

function OutputsCard({
  request,
  outputs,
}: Readonly<{ request: PackageRequest; outputs: ComparativeOutput[] }>) {
  const download = useFileDownload();
  const columns: Column<ComparativeOutput>[] = [
    {
      key: 'title',
      header: 'Output',
      render: (o) => (
        <>
          <strong>{o.title}</strong>
          <div className="muted">
            {o.kind === 'MASTER' ? 'Audit master' : 'Client view'} · round {o.roundNo}
          </div>
        </>
      ),
    },
    {
      key: 'state',
      header: 'Status',
      render: (o) =>
        o.kind === 'MASTER' ? <StatusBadge status={o.current ? 'CURRENT' : 'SUPERSEDED'} /> : '—',
    },
    {
      key: 'fields',
      header: 'Fields',
      render: (o) => (o.fields.length === COMPARATIVE_FIELDS.length ? 'All' : o.fields.length),
    },
    {
      key: 'by',
      header: 'Generated',
      render: (o) => `${o.generatedBy} · ${formatDateTime(o.generatedAt)}`,
    },
    {
      key: 'hash',
      header: 'SHA-256',
      render: (o) => <span className="mono">{o.sha256.slice(0, 12)}…</span>,
    },
    {
      key: 'files',
      header: 'Files',
      render: (o) => (
        <span className="row">
          <Button
            size="sm"
            variant="ghost"
            icon={<FileDown size={14} />}
            onClick={() =>
              download.mutate(() => productMaintApi.outputFile(request.id, o.id, 'pdf'))
            }
          >
            PDF
          </Button>
          <Button
            size="sm"
            variant="ghost"
            icon={<FileSpreadsheet size={14} />}
            onClick={() =>
              download.mutate(() => productMaintApi.outputFile(request.id, o.id, 'xlsx'))
            }
          >
            Excel
          </Button>
        </span>
      ),
    },
  ];
  return (
    <Card title="Stored outputs" flush>
      <ErrorAlert error={download.error} />
      <DataTable<ComparativeOutput>
        rows={outputs}
        rowKey={(o) => o.id}
        emptyMessage="No comparative output yet"
        columns={columns}
      />
    </Card>
  );
}

function useComparative(requestId: number) {
  const rounds = useQuery({
    queryKey: ['package-request-tab', requestId, 'rounds'],
    queryFn: () => productMaintApi.rounds(requestId),
  });
  const latest = rounds.data?.at(-1);
  const sent = latest?.sentAt !== undefined;
  const table = useQuery({
    queryKey: ['package-request-tab', requestId, 'comparative', latest?.roundNo],
    queryFn: () => productMaintApi.comparative(requestId, latest?.roundNo ?? 1),
    enabled: sent,
  });
  const outputs = useQuery({
    queryKey: ['package-request-tab', requestId, 'outputs'],
    queryFn: () => productMaintApi.outputs(requestId),
  });
  return {
    title: latest === undefined ? 'Comparison' : `Round ${latest.roundNo} comparison`,
    sent,
    rows: table.data?.rows ?? [],
    loading: table.isLoading,
    outputs: outputs.data ?? [],
    error: rounds.error ?? table.error ?? outputs.error,
  };
}

function ComparativeActions({
  requestId,
  sent,
  hasMaster,
  onGenerate,
  onCompiled,
}: Readonly<{
  requestId: number;
  sent: boolean;
  hasMaster: boolean;
  onGenerate: () => void;
  onCompiled: () => Promise<void>;
}>) {
  const compile = useMutation({
    mutationFn: () => productMaintApi.compileMaster(requestId),
    onSuccess: onCompiled,
  });
  return (
    <span className="row">
      <Button
        size="sm"
        variant="secondary"
        icon={<RefreshCw size={14} />}
        busy={compile.isPending}
        disabled={!sent}
        onClick={() => compile.mutate()}
      >
        Compile Master
      </Button>
      <Button
        size="sm"
        variant="accent"
        icon={<SlidersHorizontal size={14} />}
        disabled={!hasMaster}
        onClick={onGenerate}
      >
        Generate Client View
      </Button>
      <ErrorAlert error={compile.error} />
    </span>
  );
}

/**
 * Comparative table of a package request (BRPM.014, PMADD03): the live table of the latest round,
 * the stored outputs (one current audit master, superseded masters, client views) with their PDF
 * and Excel files, and the actions to compile a master and generate a client view.
 */
export function ComparativeTab({ request }: Readonly<{ request: PackageRequest }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [generating, setGenerating] = useState(false);
  const data = useComparative(request.id);
  const refresh = () =>
    queryClient.invalidateQueries({ queryKey: ['package-request-tab', request.id] });
  const hasMaster = data.outputs.some((o) => o.kind === 'MASTER' && o.current);
  return (
    <div className="stack">
      <ErrorAlert error={data.error} />
      <Card
        title={data.title}
        flush
        actions={
          can('PKG_NEGOTIATE') && (
            <ComparativeActions
              requestId={request.id}
              sent={data.sent}
              hasMaster={hasMaster}
              onGenerate={() => setGenerating(true)}
              onCompiled={async () => {
                await refresh();
                toast.success('Comparative master compiled');
              }}
            />
          )
        }
      >
        <DataTable<ComparativeRow>
          loading={data.loading}
          rows={data.rows}
          rowKey={(r) => r.insurerCode}
          emptyMessage="No insurer terms to compare yet"
          columns={ROW_COLUMNS}
        />
      </Card>
      <OutputsCard request={request} outputs={data.outputs} />
      {generating && (
        <ClientViewDialog
          request={request}
          insurers={data.rows.map((r) => ({ code: r.insurerCode, name: r.insurerName }))}
          onClose={() => setGenerating(false)}
          onDone={async () => {
            setGenerating(false);
            await refresh();
            toast.success('Client view generated');
          }}
        />
      )}
    </div>
  );
}
