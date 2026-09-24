import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Filter, Inbox, Plus, Search, Send, Upload } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { quotationsApi } from '@/api/quotations';
import type { QuotationListItem } from '@/api/quotations';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { BatchSendDialog } from './BatchSendDialog';
import { criteriaOf, QUICK_FILTERS, QUOTATION_TABS, sendable, toggle } from './quotationList';
import type { QuickFilter, QuotationTab } from './quotationList';
import { QUOTATION_COLUMNS } from './quotationColumns';
import { selectionColumn } from './selection';
import '@/styles/quotation.css';

const EXPIRY_WARNING_DAYS = 7;

function Toolbar({
  text,
  onText,
  onSearch,
  onFilters,
  selectedCount,
  onSend,
}: Readonly<{
  text: string;
  onText: (t: string) => void;
  onSearch: () => void;
  onFilters: () => void;
  selectedCount: number;
  onSend: () => void;
}>) {
  return (
    <form
      className="worklist-toolbar"
      onSubmit={(e) => {
        e.preventDefault();
        onSearch();
      }}
    >
      <Field label="Search Proposal No.">
        {(id) => (
          <input
            id={id}
            className="input"
            placeholder="Proposal No., ARN, client code or name"
            value={text}
            onChange={(e) => onText(e.target.value)}
          />
        )}
      </Field>
      <Button type="submit" variant="primary" icon={<Search size={16} />}>
        Search
      </Button>
      <Button variant="ghost" icon={<Filter size={16} />} onClick={onFilters}>
        Filters
      </Button>
      <div className="spacer" />
      <Button
        variant="secondary"
        icon={<Send size={16} />}
        disabled={selectedCount === 0}
        onClick={onSend}
      >
        Send via Email{selectedCount > 0 ? ` (${selectedCount})` : ''}
      </Button>
    </form>
  );
}

/**
 * Quotations (BRNB.020-024/042/043): the work list of package quotations with status tabs, quick
 * filters, search by Proposal No., ARN or client, and the batch send of approved quotations (one
 * e-mail per client, BRNB.042).
 */
export default function QuotationsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<QuotationTab>('drafts');
  const [quick, setQuick] = useState<QuickFilter>();
  const [text, setText] = useState('');
  const [applied, setApplied] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<ReadonlySet<number>>(new Set());
  const [sending, setSending] = useState(false);
  const criteria = criteriaOf(tab, quick, applied);
  const list = useQuery({
    queryKey: ['quotations', companyId, criteria, page],
    queryFn: () => quotationsApi.search(companyId, criteria, page),
    enabled: companyId > 0,
  });
  const rows = list.data?.content ?? [];
  const ready = sendable(rows, selected);
  const batch = useMutation({
    mutationFn: (hint?: string) =>
      quotationsApi.batchSend(
        ready.map((r) => r.id),
        hint,
      ),
    onSuccess: async (result) => {
      setSending(false);
      setSelected(new Set());
      await queryClient.invalidateQueries({ queryKey: ['quotations'] });
      toast.success(`${result.quotations} quotation(s) sent in ${result.clients} e-mail(s)`);
    },
  });
  const choose = (next: QuotationTab, q?: QuickFilter) => {
    setTab(next);
    setQuick(q);
    setPage(0);
    setSelected(new Set());
  };
  const columns = can('QUOTE_MAINTAIN')
    ? [
        selectionColumn<QuotationListItem>(
          (q) => q.id,
          selected,
          (id) => setSelected((s) => toggle(s, id)),
          (q) => q.quotationNo,
        ),
        ...QUOTATION_COLUMNS,
      ]
    : QUOTATION_COLUMNS;
  return (
    <div className="stack">
      <PageHeader
        section="Quotation / Proposal"
        title="Quotations"
        description="Package quotations from draft to acceptance. Approved quotations can be sent to clients one by one or in a batch."
        actions={
          can('QUOTE_MAINTAIN') && (
            <>
              <Link className="btn btn-secondary" to="/quotations/requests">
                <Inbox size={16} aria-hidden="true" /> Request Inbox
              </Link>
              {can('BULK_PROCESS') && (
                <Link className="btn btn-secondary" to="/bulk/QUOTATION_CREATE">
                  <Upload size={16} aria-hidden="true" /> Bulk Upload
                </Link>
              )}
              <Link className="btn btn-accent" to="/quotations/new">
                <Plus size={16} aria-hidden="true" /> New Quotation
              </Link>
            </>
          )
        }
      />
      <div className="quick-filters" role="group" aria-label="Quick filters">
        {(Object.keys(QUICK_FILTERS) as QuickFilter[]).map((q) => (
          <button
            key={q}
            type="button"
            className="quick-filter"
            aria-pressed={quick === q}
            onClick={() => choose(QUICK_FILTERS[q].tab, quick === q ? undefined : q)}
          >
            {QUICK_FILTERS[q].label}
          </button>
        ))}
      </div>
      <Card flush>
        <Tabs tabs={QUOTATION_TABS} active={tab} onChange={(t) => choose(t)} />
        <Toolbar
          text={text}
          onText={setText}
          onSearch={() => {
            setApplied(text);
            setPage(0);
          }}
          onFilters={() => setShowFilters((s) => !s)}
          selectedCount={ready.length}
          onSend={() => setSending(true)}
        />
        {showFilters && (
          <div className="worklist-filters">
            <label className="checkbox">
              <input
                type="checkbox"
                checked={quick === 'myDrafts'}
                onChange={(e) => choose('drafts', e.target.checked ? 'myDrafts' : undefined)}
              />
              Only my drafts
            </label>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={quick === 'expiring'}
                onChange={(e) => choose('sent', e.target.checked ? 'expiring' : undefined)}
              />
              Expiring within {EXPIRY_WARNING_DAYS} days
            </label>
          </div>
        )}
        <ErrorAlert error={list.error ?? batch.error} />
        <DataTable<QuotationListItem>
          loading={list.isLoading}
          rows={rows}
          rowKey={(q) => q.id}
          onRowClick={(q) => void navigate(`/quotations/${q.id}`)}
          emptyMessage="No items to display"
          columns={columns}
        />
        <PageFooter data={list.data} noun="quotations" onPage={setPage} />
      </Card>
      {sending && (
        <BatchSendDialog
          references={ready.map((r) => `${r.quotationNo} – ${r.clientName}`)}
          busy={batch.isPending}
          error={batch.error}
          onClose={() => setSending(false)}
          onSend={(hint) => batch.mutate(hint)}
        />
      )}
    </div>
  );
}
