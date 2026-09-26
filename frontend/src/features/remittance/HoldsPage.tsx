import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { remittanceApi } from './api';
import type { Hold, HoldInput } from './api';
import { NewHoldDialog } from './HoldDialogs';
import { TemplateButton, UploadForm } from './RemittanceParts';
import { HOLD_TABS, HOLD_TEMPLATE, stagesOf } from './remittanceLabels';
import type { HoldTab } from './remittanceLabels';
import './remittance.css';

const COLUMNS: Column<Hold>[] = [
  {
    key: 'no',
    header: 'Hold No.',
    render: (h) => (
      <>
        <strong>{h.requestNo}</strong>
        <div className="remit-muted">
          {h.source === 'COLLECTION_FEED' ? 'Collection file' : h.requestedBy}
        </div>
      </>
    ),
  },
  { key: 'inv', header: 'Invoice No.', render: (h) => h.invoiceNo },
  { key: 'assured', header: 'Name of Assured', render: (h) => h.assuredName },
  { key: 'ins', header: 'Insurer', render: (h) => h.insurerCode },
  { key: 'reason', header: 'Reason', render: (h) => h.reasonCode },
  { key: 'until', header: 'Hold Until', render: (h) => formatDate(h.holdUntil) },
  { key: 'proc', header: 'Processor', render: (h) => h.assignedProcessor ?? '' },
  { key: 'stage', header: 'Status', render: (h) => <StatusBadge status={h.stage} /> },
];

/**
 * Remittance holds (MKTID.002-007, RMTID.021/031): Marketing requests, the approver decides,
 * active holds keep the invoice out of extraction until released or expired; extensions and
 * cancellations need approval. Hold files from Collection are uploaded here too.
 */
export default function HoldsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<HoldTab>('APPROVAL');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const [uploading, setUploading] = useState(false);
  const rows = useQuery({
    queryKey: ['remittance', 'holds', companyId, tab, query, page],
    queryFn: () => remittanceApi.holds(companyId, stagesOf(HOLD_TABS, tab), query, page),
    enabled: companyId > 0,
  });
  const create = useMutation({
    mutationFn: (input: HoldInput) => remittanceApi.createHold(input),
    onSuccess: async (h) => {
      setCreating(false);
      await queryClient.invalidateQueries({ queryKey: ['remittance'] });
      toast.success(`${h.requestNo} created`);
      void navigate(`/remittance/holds/${h.id}`);
    },
  });
  const upload = useMutation({
    mutationFn: (file: File) => remittanceApi.uploadHolds(file),
    onSuccess: async (run) => {
      setUploading(false);
      await queryClient.invalidateQueries({ queryKey: ['remittance'] });
      toast.success(`${run.runNo}: ${run.message ?? run.status}`);
    },
  });
  const requester = can('HOLD_REQUEST');
  return (
    <div className="stack">
      <PageHeader
        section="Remittance"
        title="Remittance Holds"
        description="Hold requests from Marketing: approval, assignment, extension, cancellation and release."
        actions={
          requester ? (
            <>
              <Button variant="secondary" onClick={() => setUploading(!uploading)}>
                Upload Hold File
              </Button>
              <Button icon={<Plus size={16} />} onClick={() => setCreating(true)}>
                New Hold Request
              </Button>
            </>
          ) : undefined
        }
      />
      <ErrorAlert error={rows.error ?? upload.error} />
      {uploading && (
        <Card
          title="Upload Collection Hold File"
          actions={<TemplateButton name="hold-template.csv" content={HOLD_TEMPLATE} />}
        >
          <UploadForm
            label="Hold file (invoiceNo, reasonCode, holdUntil, remarks)"
            busy={upload.isPending}
            onUpload={(file) => upload.mutate(file)}
          />
        </Card>
      )}
      <Card flush>
        <div>
          <Tabs
            tabs={HOLD_TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
            }}
          />
          <WorklistToolbar
            placeholder="Search Hold or Invoice No."
            onSearch={(text) => {
              setQuery(text);
              setPage(0);
            }}
          />
          <DataTable
            caption="Hold requests"
            columns={COLUMNS}
            rows={rows.data?.content ?? []}
            rowKey={(h) => h.id}
            loading={rows.isLoading}
            onRowClick={(h) => void navigate(`/remittance/holds/${h.id}`)}
          />
          <PageFooter data={rows.data} noun="holds" onPage={setPage} />
        </div>
      </Card>
      {creating && (
        <NewHoldDialog
          companyId={companyId}
          busy={create.isPending}
          error={create.error}
          onClose={() => setCreating(false)}
          onSave={(input) => create.mutate(input)}
        />
      )}
    </div>
  );
}
