import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Upload } from 'lucide-react';
import { useState } from 'react';
import { renewalApi } from '@/api/renewal';
import type { PackageMapData, PackageMapView } from '@/api/renewal';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { UploadPanel } from '../common/UploadPanel';
import { ApprovalCell } from './setupBits';
import { numberOrNull, pending, textOrNull } from './setupCodes';
import { ConfirmButton } from '@/components/ui/ConfirmButton';

const EMPTY: PackageMapData = {
  legacyPackageCode: '',
  legacyPackageVersion: null,
  riskCode: null,
  insurerCode: null,
  siFrom: null,
  siTo: null,
  productCode: null,
  productVersionNo: null,
  remarks: null,
};

type Form = Record<keyof PackageMapData, string>;

function toForm(d: PackageMapData): Form {
  const s = (v: string | number | null) => (v === null ? '' : String(v));
  return {
    legacyPackageCode: d.legacyPackageCode,
    legacyPackageVersion: s(d.legacyPackageVersion),
    riskCode: s(d.riskCode),
    insurerCode: s(d.insurerCode),
    siFrom: s(d.siFrom),
    siTo: s(d.siTo),
    productCode: s(d.productCode),
    productVersionNo: s(d.productVersionNo),
    remarks: s(d.remarks),
  };
}

function toData(f: Form): PackageMapData {
  return {
    legacyPackageCode: f.legacyPackageCode.trim(),
    legacyPackageVersion: textOrNull(f.legacyPackageVersion),
    riskCode: textOrNull(f.riskCode),
    insurerCode: textOrNull(f.insurerCode),
    siFrom: numberOrNull(f.siFrom),
    siTo: numberOrNull(f.siTo),
    productCode: textOrNull(f.productCode),
    productVersionNo: numberOrNull(f.productVersionNo),
    remarks: textOrNull(f.remarks),
  };
}

const LABELS: [keyof PackageMapData, string][] = [
  ['legacyPackageCode', 'Legacy Package Code'],
  ['legacyPackageVersion', 'Legacy Package Version'],
  ['riskCode', 'Risk Code'],
  ['insurerCode', 'Insurance Company'],
  ['siFrom', 'Sum Insured From'],
  ['siTo', 'Sum Insured To'],
  ['productCode', 'Product Code (blank to reject)'],
  ['productVersionNo', 'Product Version'],
  ['remarks', 'Remarks'],
];

function MapDialog({
  row,
  onClose,
}: Readonly<{ row: PackageMapView | null; onClose: () => void }>) {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Form>(toForm(row?.data ?? EMPTY));
  const save = useMutation({
    mutationFn: () => renewalApi.saveMapEntry(companyId, row?.id ?? null, toData(form)),
    onSuccess: async () => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'setup'] });
    },
  });
  return (
    <Modal
      open
      title={row === null ? 'New package mapping' : `Edit ${row.data.legacyPackageCode}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={save.isPending}
            disabled={form.legacyPackageCode.trim() === ''}
            onClick={() => save.mutate()}
          >
            Save for Authorization
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        {LABELS.map(([key, label]) => (
          <Field key={key} label={label} required={key === 'legacyPackageCode'}>
            {(id) => (
              <input
                id={id}
                className="input"
                value={form[key]}
                onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))}
              />
            )}
          </Field>
        ))}
      </div>
    </Modal>
  );
}

/**
 * Package code map (DMQ36): the package of a migrated policy, with its optional qualifiers, and
 * the product version it renews on, or Reject.
 */
export function PackageMapTab() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<PackageMapView | null>();
  const [upload, setUpload] = useState(false);
  const rows = useQuery({
    queryKey: ['renewal', 'setup', 'package-map', companyId],
    queryFn: () => renewalApi.packageMap(companyId),
    enabled: companyId > 0,
  });
  const act = useMutation({
    mutationFn: ({ id, action }: { id: number; action: 'AUTHORIZE' | 'DEACTIVATE' }) =>
      renewalApi.mapEntryAction(companyId, id, action),
    onSuccess: async (r) => {
      toast.success(
        `${r.data.legacyPackageCode} ${r.approval.recordStatus === 'ACTIVE' ? 'authorized' : 'deactivated'}`,
      );
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'setup'] });
    },
  });
  return (
    <div className="stack">
      {upload && (
        <UploadPanel
          label="Upload Package Map"
          handler="RNW_PACKAGE_MAP"
          onClose={() => setUpload(false)}
        />
      )}
      <Card
        title="Package code map"
        flush
        actions={
          <span className="rnw-actions">
            <Button variant="secondary" icon={<Upload size={16} />} onClick={() => setUpload(true)}>
              Upload
            </Button>
            <Button icon={<Plus size={16} />} onClick={() => setEditing(null)}>
              New Mapping
            </Button>
          </span>
        }
      >
        <ErrorAlert error={rows.error ?? act.error} />
        <DataTable<PackageMapView>
          loading={rows.isLoading}
          rows={rows.data ?? []}
          rowKey={(r) => r.id}
          emptyMessage="No package mappings"
          columns={[
            {
              key: 'legacy',
              header: 'Legacy Package',
              kind: 'code',
              render: (r) => (
                <CellStack
                  main={r.data.legacyPackageCode}
                  sub={r.data.legacyPackageVersion ?? ''}
                />
              ),
            },
            {
              key: 'qual',
              header: 'Risk / Insurer',
              render: (r) => (
                <CellStack main={r.data.riskCode ?? 'Any'} sub={r.data.insurerCode ?? 'Any'} />
              ),
            },
            {
              key: 'si',
              header: 'Sum Insured band',
              render: (r) =>
                r.data.siFrom === null && r.data.siTo === null ? (
                  'Any'
                ) : (
                  <>
                    <Amount value={r.data.siFrom} /> – <Amount value={r.data.siTo} />
                  </>
                ),
            },
            {
              key: 'target',
              header: 'Renews on',
              render: (r) =>
                r.action === 'REJECT'
                  ? 'Rejected (not renewable as is)'
                  : `${r.data.productCode ?? ''} v${String(r.data.productVersionNo ?? '')}`,
            },
            { key: 'source', header: 'Source', render: (r) => r.source },
            {
              key: 'status',
              header: 'Status',
              render: (r) => <ApprovalCell approval={r.approval} />,
            },
            {
              key: 'act',
              header: '',
              render: (r) => (
                <span className="rnw-actions">
                  {pending(r.approval) && (
                    <ConfirmButton
                      size="sm"
                      confirm={{
                        title: 'Authorize Package Mapping',
                        effect: 'The mapping change takes effect.',
                      }}
                      onConfirm={() => act.mutateAsync({ id: r.id, action: 'AUTHORIZE' })}
                    >
                      Authorize
                    </ConfirmButton>
                  )}
                  <Button size="sm" variant="ghost" onClick={() => setEditing(r)}>
                    Edit
                  </Button>
                  {r.approval.recordStatus === 'ACTIVE' && (
                    <ConfirmButton
                      size="sm"
                      variant="ghost"
                      confirm={{
                        title: 'Deactivate Package Mapping',
                        effect: 'The mapping is no longer used for new renewals.',
                        destructive: true,
                      }}
                      onConfirm={() => act.mutateAsync({ id: r.id, action: 'DEACTIVATE' })}
                    >
                      Deactivate
                    </ConfirmButton>
                  )}
                </span>
              ),
            },
          ]}
        />
      </Card>
      {editing !== undefined && <MapDialog row={editing} onClose={() => setEditing(undefined)} />}
    </div>
  );
}
