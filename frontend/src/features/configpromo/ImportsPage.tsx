import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Upload } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { configPromotionApi } from '@/api/configPromotion';
import type { ConfigImport } from '@/api/configPromotion';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';
import { PageHeader } from '@/components/ui/PageHeader';
import { Pager } from '@/components/ui/Pager';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { EnvironmentNotice } from './EnvironmentNotice';
import { ImportDetail } from './ImportDetail';
import { changeCount, importStatus } from './promotion';

const IMPORT_PARAM = 'import';

/**
 * Import Configuration (Configuration Promotion): upload a package from another environment; it is
 * verified, checked against this environment and compared in a dry run; the preparer submits it,
 * another user approves and applies it; the list keeps every import with its status.
 */
export default function ImportsPage() {
  const [params] = useSearchParams();
  const selected = params.get(IMPORT_PARAM);
  if (selected !== null && selected !== '') {
    return <ImportDetail id={Number(selected)} />;
  }
  return <ImportList />;
}

function ImportList() {
  const { can } = useAuth();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const imports = useQuery({
    queryKey: ['config-promotion', 'imports', page],
    queryFn: () => configPromotionApi.imports(page),
  });
  const open = (imp: ConfigImport) => void navigate(`?${IMPORT_PARAM}=${imp.id}`);
  return (
    <div className="stack">
      <PageHeader
        section="Configuration Promotion"
        title="Import Configuration"
        description="Brings the set-up of another environment into this one after a dry run and the approval of a second user. Transactions are never moved."
      />
      <EnvironmentNotice />
      {can('CONFIG_IMPORT_PREPARE') && <UploadCard onCreated={open} />}
      <Card title="Imports" flush>
        <ErrorAlert error={imports.error} />
        <DataTable<ConfigImport>
          callout="config-imports"
          loading={imports.isLoading}
          rows={imports.data?.content ?? []}
          rowKey={(i) => i.id}
          onRowClick={open}
          emptyMessage="No package has been imported into this environment yet."
          columns={[
            {
              key: 'no',
              header: 'Import',
              kind: 'code',
              render: (i) => <CellStack main={<strong>{i.importNo}</strong>} sub={i.packageNo} />,
            },
            {
              key: 'from',
              header: 'From',
              kind: 'code',
              width: '100px',
              render: (i) => i.sourceEnvironment,
            },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              width: '190px',
              render: (i) => {
                const s = importStatus(i.status);
                return <StatusBadge status={i.status} label={s.label} tone={s.tone} />;
              },
            },
            {
              key: 'changes',
              header: 'Changes',
              numeric: true,
              width: '100px',
              render: (i) => changeCount(i),
            },
            {
              key: 'reason',
              header: 'Change Request',
              render: (i) => (
                <CellStack main={i.changeReference ?? ''} sub={i.reason ?? undefined} />
              ),
            },
            {
              key: 'prepared',
              header: 'Prepared By',
              render: (i) => (
                <CellStack
                  main={<UserName login={i.preparedBy} />}
                  sub={formatDateTime(i.preparedAt)}
                />
              ),
            },
            {
              key: 'decided',
              header: 'Approved By',
              render: (i) =>
                i.decidedBy === null || i.decidedBy === undefined ? (
                  '—'
                ) : (
                  <CellStack
                    main={<UserName login={i.decidedBy} />}
                    sub={formatDateTime(i.decidedAt)}
                  />
                ),
            },
            {
              key: 'actions',
              header: <span className="visually-hidden">Actions</span>,
              width: '64px',
              render: (i) => (
                <RowActionMenu
                  label={i.importNo}
                  actions={[{ label: 'Open', onSelect: () => open(i) }]}
                />
              ),
            },
          ]}
        />
        <Pager
          page={imports.data?.page ?? 0}
          totalPages={imports.data?.totalPages ?? 0}
          total={imports.data?.totalElements ?? 0}
          size={imports.data?.size}
          onPage={setPage}
        />
      </Card>
    </div>
  );
}

function UploadCard({ onCreated }: Readonly<{ onCreated: (imp: ConfigImport) => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [file, setFile] = useState<File | null>(null);
  const [includeUsers, setIncludeUsers] = useState(false);
  const [changeReference, setChangeReference] = useState('');
  const [reason, setReason] = useState('');
  const upload = useMutation({
    mutationFn: () =>
      file === null
        ? Promise.reject(new Error('Choose the package to upload'))
        : configPromotionApi.upload({
            file,
            datasets: [],
            deactivate: [],
            includeUsers,
            changeReference,
            reason,
          }),
    onSuccess: async (imp) => {
      await queryClient.invalidateQueries({ queryKey: ['config-promotion'] });
      toast.success(`Import ${imp.importNo} checked`);
      onCreated(imp);
    },
  });
  return (
    <Card title="Upload a Package">
      <div className="stack">
        <ErrorAlert error={upload.error} />
        <div className="grid-3">
          <Field label="Change request number" hint="Mandatory in production.">
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={60}
                value={changeReference}
                onChange={(e) => setChangeReference(e.target.value)}
              />
            )}
          </Field>
          <Field label="Reason">
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={1000}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
            )}
          </Field>
          <Field label="Import users" hint="Only when the package holds users.">
            {(id) => (
              <select
                id={id}
                className="select"
                value={includeUsers ? 'Y' : 'N'}
                onChange={(e) => setIncludeUsers(e.target.value === 'Y')}
              >
                <option value="N">No</option>
                <option value="Y">Yes</option>
              </select>
            )}
          </Field>
        </div>
        <Field
          label="Package"
          required
          hint="The configuration package (.zip) exported from the source environment."
        >
          {(id) => (
            <FileDropZone
              id={id}
              accept=".zip"
              busy={upload.isPending}
              onChange={(files) => setFile(files[0] ?? null)}
            />
          )}
        </Field>
        <p className="muted">
          The package is refused when it was changed after its export or comes from another schema.
          Nothing changes in this environment until a second user approves the import.
        </p>
        <div className="form-actions">
          <Button
            variant="accent"
            icon={<Upload size={16} />}
            busy={upload.isPending}
            disabled={file === null}
            onClick={() => upload.mutate()}
          >
            Upload and Check
          </Button>
        </div>
      </div>
    </Card>
  );
}
