import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Product } from '@/api/catalog';
import { productCatalogApi } from '@/api/productCatalog';
import type { VersionSummary } from '@/api/productCatalog';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';

function NewVersionModal({ code, onClose }: Readonly<{ code: string; onClose: () => void }>) {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [summary, setSummary] = useState('');
  const [touched, setTouched] = useState(false);
  const create = useMutation({
    mutationFn: () => productCatalogApi.newVersion(code, companyId, summary.trim()),
    onSuccess: async (v) => {
      await queryClient.invalidateQueries({ queryKey: ['catalog'] });
      toast.success(`Draft version ${v.summary.versionNo} created`);
      void navigate(`/catalog/products/${code}/versions/${v.summary.versionNo}`);
    },
  });
  const missing = summary.trim() === '';
  return (
    <Modal
      open
      title="New Version"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={create.isPending}
            onClick={() => {
              setTouched(true);
              if (!missing) {
                create.mutate();
              }
            }}
          >
            Create Draft
          </Button>
        </>
      }
    >
      <p className="muted">
        The draft copies the version in force and sells from tomorrow once validated; the current
        version keeps selling meanwhile.
      </p>
      <ErrorAlert error={create.error} />
      <Field
        label="What changes"
        required
        error={touched && missing ? 'Describe the change' : undefined}
      >
        {(id) => (
          <textarea
            id={id}
            className="input"
            rows={3}
            value={summary}
            onChange={(e) => setSummary(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}

/**
 * Versions of a package (BRPM.006/007): newest first with status, selling period and validator; a
 * row opens the version editor. MBS creates the next draft with "New Version".
 */
export function ProductVersionsTab({ product }: Readonly<{ product: Product }>) {
  const { can } = useAuth();
  const navigate = useNavigate();
  const [creating, setCreating] = useState(false);
  const versions = useQuery({
    queryKey: ['catalog', 'versions', product.code],
    queryFn: () => productCatalogApi.versions(product.code),
  });
  if (!product.packaged) {
    return (
      <EmptyState message="Non-package products are rated on the product rate: they have no versions." />
    );
  }
  const mayCreate =
    can('PRODUCT_MAINTAIN') && !product.openVersionNo && Boolean(product.currentVersionNo);
  return (
    <Card
      title="Versions"
      flush
      actions={
        mayCreate && (
          <Button
            variant="accent"
            size="sm"
            icon={<Plus size={14} />}
            onClick={() => setCreating(true)}
          >
            New Version
          </Button>
        )
      }
    >
      <ErrorAlert error={versions.error} />
      <DataTable<VersionSummary>
        loading={versions.isLoading}
        rows={versions.data ?? []}
        rowKey={(v) => v.versionNo}
        onRowClick={(v) =>
          void navigate(`/catalog/products/${product.code}/versions/${v.versionNo}`)
        }
        emptyMessage="No version yet."
        columns={[
          { key: 'n', header: 'Version', render: (v) => <strong>v{v.versionNo}</strong> },
          { key: 's', header: 'Status', render: (v) => <StatusBadge status={v.status} /> },
          { key: 'f', header: 'Effective From', render: (v) => formatDate(v.effectiveFrom) },
          { key: 't', header: 'Effective To', render: (v) => formatDate(v.effectiveTo) },
          { key: 'r', header: 'Rate %', numeric: true, render: (v) => v.defaultRate ?? '' },
          { key: 'e', header: 'Package End', render: (v) => formatDate(v.packageEndDate) },
          { key: 'q', header: 'Request', render: (v) => v.sourceRequestNo ?? '' },
          { key: 'v', header: 'Validated By', render: (v) => v.validatedBy ?? '' },
          { key: 'c', header: 'Change', render: (v) => v.changeSummary ?? '' },
        ]}
      />
      {creating && <NewVersionModal code={product.code} onClose={() => setCreating(false)} />}
    </Card>
  );
}
