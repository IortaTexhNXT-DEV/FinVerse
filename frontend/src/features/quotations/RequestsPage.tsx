import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Search } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { quotationRequestsApi } from '@/api/quotations';
import type { QuotationRequest, RequestStatus } from '@/api/quotations';
import { useAuth } from '@/auth/authContext';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActions } from '@/components/ui/RowActions';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { CaptureRequestDialog } from './CaptureRequestDialog';
import { requestActions } from './requestActions';
import '@/styles/quotation.css';
import { ClientLabel } from '@/components/broking/ClientLabel';
import { LovLabel, ProductName } from '@/components/broking/LovLabel';
import { CellStack } from '@/components/ui/CellStack';

const TABS: readonly { id: RequestStatus; label: string }[] = [
  { id: 'NEW', label: 'To Quote' },
  { id: 'QUOTED', label: 'Quoted' },
  { id: 'CLOSED', label: 'Closed' },
];

/**
 * Quotation request inbox (BRNB.041, BRNB.023 staging): requests received by e-mail (captured with
 * the e-mail attached), uploaded in bulk or delivered by a source system, waiting to be quoted. A
 * request without a client gets its prospect first (BRNB.063).
 */
export default function RequestsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<RequestStatus>('NEW');
  const [text, setText] = useState('');
  const [applied, setApplied] = useState('');
  const [page, setPage] = useState(0);
  const [capturing, setCapturing] = useState(false);
  const [closing, setClosing] = useState<QuotationRequest | null>(null);
  const list = useQuery({
    queryKey: ['quotation-requests', companyId, status, applied, page],
    queryFn: () => quotationRequestsApi.search(companyId, status, applied, page),
    enabled: companyId > 0,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['quotation-requests'] });
  const prospect = useMutation({
    mutationFn: (r: QuotationRequest) => quotationRequestsApi.prospect(r.id),
    onSuccess: async (r) => {
      await refresh();
      toast.success(`${r.requestNo}: prospect created, you can now quote it`);
    },
  });
  const close = useMutation({
    mutationFn: ({ r, reason }: { r: QuotationRequest; reason: string }) =>
      quotationRequestsApi.close(r.id, reason),
    onSuccess: async (r) => {
      setClosing(null);
      await refresh();
      toast.success(`${r.requestNo} closed`);
    },
  });
  const canMaintain = can('QUOTE_MAINTAIN');
  return (
    <div className="stack">
      <PageHeader
        backTo="/quotations"
        section="Quotation / Proposal"
        title="Quotation Requests"
        description="Requests received by e-mail, upload or source system, waiting to be quoted."
        actions={
          canMaintain && (
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => setCapturing(true)}>
              Capture Request
            </Button>
          )
        }
      />
      <ErrorAlert error={list.error ?? prospect.error} />
      <Card flush>
        <Tabs
          tabs={TABS}
          active={status}
          onChange={(s) => {
            setStatus(s);
            setPage(0);
          }}
        />
        <form
          className="worklist-toolbar"
          onSubmit={(e) => {
            e.preventDefault();
            setApplied(text);
            setPage(0);
          }}
        >
          <Field label="Search Request No.">
            {(id) => (
              <input
                id={id}
                className="input"
                placeholder="Request No., prospect or cover"
                value={text}
                onChange={(e) => setText(e.target.value)}
              />
            )}
          </Field>
          <Button type="submit" icon={<Search size={16} />}>
            Search
          </Button>
        </form>
        <DataTable<QuotationRequest>
          loading={list.isLoading}
          rows={list.data?.content ?? []}
          rowKey={(r) => r.id}
          emptyMessage="No items to display"
          columns={[
            {
              key: 'no',
              header: 'Request No.',
              render: (r) => (
                <>
                  <strong className="mono">{r.requestNo}</strong>
                  <div className="muted">{formatDateTime(r.receivedAt)}</div>
                </>
              ),
            },
            {
              key: 'channel',
              header: 'Channel',
              render: (r) => (
                <CellStack
                  main={<LovLabel type="SOURCE_CHANNEL" code={r.channel} />}
                  sub={r.externalRef}
                />
              ),
            },
            {
              key: 'client',
              header: 'Client / Prospect',
              render: (r) =>
                r.prospectName === undefined && r.clientId !== undefined ? (
                  <ClientLabel clientId={r.clientId} />
                ) : (
                  <span>{r.prospectName ?? '—'}</span>
                ),
            },
            {
              key: 'product',
              header: 'Product',
              render: (r) => <ProductName code={r.productCode} withCode />,
            },
            { key: 'cover', header: 'Requested Cover', render: (r) => r.requestedCover ?? '' },
            {
              key: 'status',
              header: 'Status',
              render: (r) => (
                <span>
                  <StatusBadge status={r.status} />
                  {r.closeReason && <div className="muted">{r.closeReason}</div>}
                </span>
              ),
            },
            {
              key: 'actions',
              header: 'Actions',
              render: (r) => (
                <RowActions
                  record={r.requestNo}
                  actions={requestActions(r, canMaintain, {
                    open: (to) => void navigate(to),
                    prospect: () => prospect.mutate(r),
                    close: () => setClosing(r),
                  })}
                />
              ),
            },
          ]}
        />
        <PageFooter data={list.data} noun="requests" onPage={setPage} />
      </Card>
      {capturing && (
        <CaptureRequestDialog
          onClose={() => setCapturing(false)}
          onSaved={(r) => {
            setCapturing(false);
            void refresh();
            toast.success(`${r.requestNo} captured`);
          }}
        />
      )}
      {closing && (
        <ActionDialog
          title={`Close ${closing.requestNo}`}
          confirmLabel="Close Request"
          commentRequired
          commentLabel="Reason for Closing"
          busy={close.isPending}
          error={close.error}
          onClose={() => setClosing(null)}
          onConfirm={(note) => {
            if (note.comment) {
              close.mutate({ r: closing, reason: note.comment });
            }
          }}
        />
      )}
    </div>
  );
}
