import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { configPromotionApi } from '@/api/configPromotion';
import type { ConfigPackage, PackageKind } from '@/api/configPromotion';
import { saveFile } from '@/api/client';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { Pager } from '@/components/ui/Pager';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import type { RowAction } from '@/components/ui/RowActionMenu';
import { Tabs } from '@/components/ui/Tabs';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { fileSize, packageKindLabel, shortHash } from './promotion';

type KindFilter = 'ALL' | PackageKind;

/**
 * Packages and History (Configuration Promotion): every package exported here, uploaded for an
 * import or kept as a snapshot before an import, with its source, contents and checksum; a package
 * can be downloaded, opened, or marked as the configuration baseline of this environment.
 */
export default function PackagesPage() {
  const { can } = useAuth();
  const [kind, setKind] = useState<KindFilter>('ALL');
  const [page, setPage] = useState(0);
  const [opened, setOpened] = useState<ConfigPackage | null>(null);
  const [marking, setMarking] = useState<ConfigPackage | null>(null);
  const packages = useQuery({
    queryKey: ['config-promotion', 'packages', kind, page],
    queryFn: () => configPromotionApi.packages(kind === 'ALL' ? undefined : kind, page),
  });
  const actions = (p: ConfigPackage): RowAction[] => {
    const list: RowAction[] = [
      { label: 'View Contents', onSelect: () => setOpened(p) },
      {
        label: 'Download',
        onSelect: () =>
          void configPromotionApi.file(p.id).then((f) => saveFile(f.blob, f.fileName)),
      },
    ];
    if (can('CONFIG_BASELINE_MANAGE') && p.kind !== 'SNAPSHOT') {
      list.push({ label: 'Mark as Baseline', onSelect: () => setMarking(p) });
    }
    return list;
  };
  return (
    <div className="stack">
      <PageHeader
        section="Configuration Promotion"
        title="Packages and History"
        description="Every configuration package of this environment with its source, contents and checksum."
      />
      <Tabs<KindFilter>
        tabs={[
          { id: 'ALL', label: 'All' },
          { id: 'EXPORT', label: 'Exported' },
          { id: 'UPLOAD', label: 'Uploaded' },
          { id: 'SNAPSHOT', label: 'Snapshots' },
        ]}
        active={kind}
        onChange={(k) => {
          setKind(k);
          setPage(0);
        }}
      />
      <Card title="Packages" flush>
        <ErrorAlert error={packages.error} />
        <DataTable<ConfigPackage>
          callout="config-packages"
          loading={packages.isLoading}
          rows={packages.data?.content ?? []}
          rowKey={(p) => p.id}
          emptyMessage="No package yet."
          columns={[
            {
              key: 'no',
              header: 'Package',
              kind: 'code',
              render: (p) => (
                <CellStack main={<strong>{p.packageNo}</strong>} sub={packageKindLabel(p.kind)} />
              ),
            },
            {
              key: 'source',
              header: 'From',
              kind: 'code',
              width: '90px',
              render: (p) => p.sourceEnvironment,
            },
            {
              key: 'mode',
              header: 'Contents',
              render: (p) => (
                <CellStack
                  main={`${p.datasetCount} datasets, ${p.rowCount.toLocaleString('en-PH')} items`}
                  sub={p.mode === 'FULL' ? 'Full' : 'Changes since a baseline'}
                />
              ),
            },
            {
              key: 'purpose',
              header: 'Purpose',
              truncate: true,
              render: (p) => p.description ?? '',
            },
            {
              key: 'sha',
              header: 'Checksum',
              kind: 'code',
              width: '140px',
              render: (p) => <CellStack main={shortHash(p.sha256)} sub={fileSize(p.fileSize)} />,
            },
            {
              key: 'created',
              header: 'Created',
              render: (p) => (
                <CellStack
                  main={<UserName login={p.createdBy} />}
                  sub={formatDateTime(p.createdAt)}
                />
              ),
            },
            {
              key: 'actions',
              header: <span className="visually-hidden">Actions</span>,
              width: '64px',
              render: (p) => <RowActionMenu label={p.packageNo} actions={actions(p)} />,
            },
          ]}
        />
        <Pager
          page={packages.data?.page ?? 0}
          totalPages={packages.data?.totalPages ?? 0}
          total={packages.data?.totalElements ?? 0}
          size={packages.data?.size}
          onPage={setPage}
        />
      </Card>
      {opened !== null && <ContentsDialog pkg={opened} onClose={() => setOpened(null)} />}
      {marking !== null && <BaselineDialog pkg={marking} onClose={() => setMarking(null)} />}
    </div>
  );
}

function ContentsDialog({ pkg, onClose }: Readonly<{ pkg: ConfigPackage; onClose: () => void }>) {
  const detail = useQuery({
    queryKey: ['config-promotion', 'package', pkg.id],
    queryFn: () => configPromotionApi.pkg(pkg.id),
  });
  return (
    <Modal title={`Package ${pkg.packageNo}`} open onClose={onClose}>
      <ErrorAlert error={detail.error} />
      <DataTable
        callout="package-contents"
        loading={detail.isLoading}
        rows={detail.data?.datasets ?? []}
        rowKey={(d) => d.code}
        columns={[
          { key: 'name', header: 'Dataset', render: (d) => d.name },
          { key: 'rows', header: 'Items', numeric: true, width: '90px', render: (d) => d.rows },
          {
            key: 'sha',
            header: 'Checksum',
            kind: 'code',
            width: '140px',
            render: (d) => shortHash(d.contentSha256),
          },
        ]}
      />
    </Modal>
  );
}

function BaselineDialog({ pkg, onClose }: Readonly<{ pkg: ConfigPackage; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [remarks, setRemarks] = useState('');
  const mark = useMutation({
    mutationFn: () => configPromotionApi.markBaseline(pkg.id, name, remarks),
    onSuccess: async (b) => {
      await queryClient.invalidateQueries({ queryKey: ['config-promotion'] });
      toast.success(`Baseline ${b.name} marked`);
      onClose();
    },
  });
  return (
    <Modal
      title="Mark as Baseline"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Go Back
          </Button>
          <Button busy={mark.isPending} disabled={name.trim() === ''} onClick={() => mark.mutate()}>
            Mark as Baseline
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={mark.error} />
        <p>
          Package {pkg.packageNo} becomes a configuration baseline of this environment; the drift
          report compares the current configuration with it.
        </p>
        <Field label="Name" required>
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={120}
              value={name}
              placeholder="UAT signed-off baseline 2027-11"
              onChange={(e) => setName(e.target.value)}
            />
          )}
        </Field>
        <Field label="Remarks">
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={500}
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
