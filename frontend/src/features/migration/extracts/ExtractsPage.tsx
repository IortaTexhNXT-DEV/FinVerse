import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileDown, Upload } from 'lucide-react';
import { useState } from 'react';
import { migrationApi } from '@/api/migration';
import type { Extract } from '@/api/migration';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { MissingInputs } from '@/components/ui/MissingInputs';
import { Notice } from '@/components/ui/Notice';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { countOf, formatDateTime } from '@/utils/format';
import { MigStatus } from '../common/MigStatus';
import { MIG_SECTION } from '../common/migrationCodes';
import { useDownload } from '../common/useDownload';
import '../migration.css';
import { UserName } from '@/components/ui/UserName';

/** Every failed intake check of a rejected extract as "Check: reason". */
function rejectReasons(e: Extract): string[] {
  const checks = e.rejectChecks ?? [];
  if (checks.length > 0) {
    return checks.map((c) => `${c.check}: ${c.reason}`);
  }
  return e.rejectMessage ? [e.rejectMessage] : [];
}

/**
 * Extracts (DATA_MIGRATION_DESIGN section 5): upload of a legacy extract named
 * LAYOUT_SOURCE_yyyyMMdd_nn with its control file; the intake checks (checksum, header, row count,
 * amount and hash totals) stage it or reject it with the reason. Files are kept in the migration
 * store and purged after the retention days.
 */
export default function ExtractsPage() {
  const companyId = useCompanyId();
  const client = useQueryClient();
  const toast = useToast();
  const download = useDownload();
  const [page, setPage] = useState(0);
  const [objectFilter, setObjectFilter] = useState('');
  const [mode, setMode] = useState('FULL');
  const [data, setData] = useState<File>();
  const [control, setControl] = useState<File>();
  const [round, setRound] = useState(0);
  const [rejected, setRejected] = useState<Extract>();
  const extracts = useQuery({
    queryKey: ['migration', 'extracts', companyId, objectFilter, page],
    queryFn: () => migrationApi.extracts(companyId, objectFilter || undefined, page),
    enabled: companyId > 0,
  });
  const upload = useMutation({
    mutationFn: (file: File) => migrationApi.uploadExtract(companyId, { mode, file, control }),
    onSuccess: async (e) => {
      if (e.status === 'REJECTED') {
        setRejected(e);
        toast.error(`${e.extractNo} rejected: ${countOf(rejectReasons(e).length, 'check')} failed`);
      } else {
        setRejected(undefined);
        toast.success(`${e.extractNo} staged with ${countOf(e.stagedRows, 'row')}`);
      }
      setData(undefined);
      setControl(undefined);
      setRound((r) => r + 1);
      await client.invalidateQueries({ queryKey: ['migration'] });
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section={MIG_SECTION}
        title="Extracts"
        description="Legacy extracts received with their control files and the result of the intake checks."
      />
      <Card title="Upload an extract">
        <ErrorAlert error={upload.error} />
        {rejected && (
          <Notice
            tone="error"
            title={`${rejected.extractNo} rejected: the file was not staged`}
            items={rejectReasons(rejected)}
          >
            Correct the extract or its control file and upload both again.
          </Notice>
        )}
        <div className="form-grid">
          <Field label="Data file" required hint="LAYOUT_SOURCE_yyyyMMdd_nn.csv or .xlsx">
            {(id) => (
              <FileDropZone
                key={`data-${String(round)}`}
                id={id}
                accept=".csv,.xlsx"
                label={data?.name ?? 'Drop the extract here'}
                onChange={(files) => setData(files[0])}
              />
            )}
          </Field>
          <Field label="Control file" required>
            {(id) => (
              <FileDropZone
                key={`control-${String(round)}`}
                id={id}
                accept=".csv,.xlsx"
                label={control?.name ?? 'Drop the control file here'}
                onChange={(files) => setControl(files[0])}
              />
            )}
          </Field>
          <Field label="Mode">
            {(id) => (
              <select
                id={id}
                className="select"
                value={mode}
                onChange={(e) => setMode(e.target.value)}
              >
                <option value="FULL">Full extract</option>
                <option value="DELTA">Delta since the previous extract</option>
              </select>
            )}
          </Field>
        </div>
        <div className="mig-actions">
          <Button
            variant="primary"
            icon={<Upload size={16} />}
            disabled={data === undefined || control === undefined}
            busy={upload.isPending}
            onClick={() => {
              if (data !== undefined) {
                upload.mutate(data);
              }
            }}
          >
            Upload and Check
          </Button>
          <MissingInputs
            missing={[
              data === undefined && 'the data file',
              control === undefined && 'the control file',
            ]}
          />
        </div>
      </Card>
      <Card flush>
        <div className="worklist-filters form-grid">
          <Field label="Object">
            {(id) => (
              <input
                id={id}
                className="input"
                placeholder="All objects, e.g. C01"
                value={objectFilter}
                onChange={(e) => {
                  setPage(0);
                  setObjectFilter(e.target.value.toUpperCase());
                }}
              />
            )}
          </Field>
        </div>
        <ErrorAlert
          error={extracts.error ?? download.error}
          onRetry={() => void extracts.refetch()}
        />
        <DataTable<Extract>
          loading={extracts.isLoading}
          rows={extracts.data?.content ?? []}
          rowKey={(e) => e.extractNo}
          emptyMessage="No extract received"
          columns={[
            {
              key: 'no',
              header: 'Extract',
              kind: 'code',
              render: (e) => <CellStack main={e.extractNo} sub={e.fileName} />,
            },
            {
              key: 'object',
              header: 'Layout',
              render: (e) => (
                <CellStack
                  main={`${e.layoutCode} v${String(e.layoutVersion)}`}
                  sub={`${e.sourceSystem} · ${e.mode === 'DELTA' ? 'Delta' : 'Full'}`}
                />
              ),
            },
            {
              key: 'asOf',
              header: 'As of',
              kind: 'datetime',
              render: (e) => formatDateTime(e.asOf),
            },
            {
              key: 'received-rows',
              header: 'Rows Received',
              numeric: true,
              render: (e) => e.parsedRows,
            },
            {
              key: 'staged-rows',
              header: 'Rows Staged',
              numeric: true,
              render: (e) => e.stagedRows,
            },
            {
              key: 'masked',
              header: 'Masked',
              kind: 'center',
              render: (e) => (e.masked ? 'Yes' : 'No'),
            },
            {
              key: 'received',
              header: 'Received On',
              render: (e) => (
                <CellStack
                  main={formatDateTime(e.receivedAt)}
                  sub={<UserName login={e.receivedBy} empty="" />}
                />
              ),
            },
            {
              key: 'reason',
              header: 'Rejection Reason',
              render: (e) =>
                rejectReasons(e).length === 0 ? null : (
                  <CellStack
                    main={rejectReasons(e)[0]}
                    sub={rejectReasons(e).slice(1).join(' · ')}
                  />
                ),
            },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (e) => <MigStatus status={e.purgedAt ? 'PURGED' : e.status} />,
            },
            {
              key: 'files',
              header: '',
              render: (e) =>
                e.dataFileId !== undefined && !e.purgedAt ? (
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label={`Download ${e.fileName}`}
                    icon={<FileDown size={14} />}
                    onClick={() =>
                      download.run(() => migrationApi.extractFile(e.extractNo, 'data'))
                    }
                  />
                ) : null,
            },
          ]}
        />
        <PageFooter data={extracts.data} noun="extracts" onPage={setPage} />
      </Card>
    </div>
  );
}
