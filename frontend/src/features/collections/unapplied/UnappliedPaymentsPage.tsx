import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ListChecks } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import type { DispositionDraft, UnappliedRow } from './api';
import { unappliedApi } from './api';
import { DispositionDialog } from './DispositionDialog';
import { UNAPPLIED_COLUMNS } from './columns';
import type { PanelValues, UnappliedTab } from './labels';
import { EMPTY_PANEL, UNAPPLIED_TABS, listFilters } from './labels';
import { UnappliedFilterPanel } from './UnappliedParts';

const APPLY = 'FOR_APPLICATION_TO_INVOICE';

function initialTab(value: string | null): UnappliedTab {
  return UNAPPLIED_TABS.some((t) => t.id === value) ? (value as UnappliedTab) : 'UNAPPLIED';
}

/**
 * Unapplied payments, collector view (BRCLXN.030-036): Cashiering's open unapplied items as of
 * today by Cashiering tab, with the collector filters (segment, disposition, age), and the
 * disposition of the selected item: a note, or a request to Cashiering such as "For application
 * to invoice" with a valid invoice number.
 */
export default function UnappliedPaymentsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const [tab, setTab] = useState<UnappliedTab>(initialTab(params.get('tab')));
  const [query, setQuery] = useState(params.get('q') ?? '');
  const [panel, setPanel] = useState<PanelValues>(EMPTY_PANEL);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [page, setPage] = useState(0);
  const [disposing, setDisposing] = useState<{ row: UnappliedRow; code?: string }>();
  const selection = useRowSelection();
  const filters = listFilters(tab, query, panel);
  const rows = useQuery({
    queryKey: ['collections', 'unapplied', 'list', companyId, filters, page],
    queryFn: () => unappliedApi.list(companyId, filters, page),
    enabled: companyId > 0,
  });
  const dispose = useMutation({
    mutationFn: (v: { ref: string; draft: DispositionDraft }) =>
      unappliedApi.dispose(companyId, v.ref, v.draft),
    onSuccess: async (d) => {
      setDisposing(undefined);
      selection.clear();
      await queryClient.invalidateQueries({ queryKey: ['collections', 'unapplied'] });
      toast.success(
        d.requestId === undefined
          ? `Disposition recorded on ${d.unappliedRef}`
          : `Disposition recorded; request sent to Cashiering for ${d.unappliedRef}`,
      );
    },
  });
  const list = rows.data?.content ?? [];
  const canWork = can('CLX_UNAPPLIED_WORK');
  const selected = list.find((r) => selection.keys.length === 1 && selection.has(r.unappliedRef));
  const columns = canWork
    ? [
        selectionColumn(
          list,
          (r) => r.unappliedRef,
          selection,
          (r) => r.unappliedRef,
        ),
        ...UNAPPLIED_COLUMNS,
      ]
    : UNAPPLIED_COLUMNS;
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections"
        title="Unapplied Payments"
        description="Payments Cashiering could not apply, as of today. Document their disposition or ask Cashiering to apply, refund, reclass or transfer them."
        actions={
          <Button
            variant="secondary"
            icon={<ListChecks size={16} />}
            onClick={() => void navigate('/collections/unapplied/requests')}
          >
            Requests to Cashiering
          </Button>
        }
      />
      <ErrorAlert error={rows.error} />
      <Card flush>
        <div>
          <Tabs
            tabs={UNAPPLIED_TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
              selection.clear();
            }}
          />
          <WorklistToolbar
            placeholder="Search Unapplied Reference"
            initial={query}
            onSearch={(text) => {
              setQuery(text);
              setPage(0);
            }}
            filters={{ open: filtersOpen, onToggle: () => setFiltersOpen((o) => !o) }}
          >
            {canWork && (
              <>
                <Button
                  variant="secondary"
                  disabled={selected === undefined}
                  onClick={() => selected && setDisposing({ row: selected })}
                >
                  Record Disposition
                </Button>
                <Button
                  variant="accent"
                  disabled={selected === undefined}
                  onClick={() => selected && setDisposing({ row: selected, code: APPLY })}
                >
                  Request Application
                </Button>
              </>
            )}
          </WorklistToolbar>
          {filtersOpen && (
            <UnappliedFilterPanel
              initial={panel}
              onApply={(v) => {
                setPanel(v);
                setPage(0);
              }}
            />
          )}
          <DataTable
            caption="Unapplied payments"
            columns={columns}
            rows={list}
            rowKey={(r) => r.unappliedRef}
            loading={rows.isLoading}
            onRowClick={(r) =>
              void navigate(`/collections/unapplied/${encodeURIComponent(r.unappliedRef)}`)
            }
            emptyMessage="No unapplied payments to display"
          />
          <PageFooter data={rows.data} noun="payments" onPage={setPage} />
        </div>
      </Card>
      {disposing !== undefined && (
        <DispositionDialog
          item={disposing.row}
          initialCode={disposing.code}
          busy={dispose.isPending}
          error={dispose.error}
          onClose={() => setDisposing(undefined)}
          onSave={(draft) => dispose.mutate({ ref: disposing.row.unappliedRef, draft })}
        />
      )}
    </div>
  );
}
