import { useQuery, useQueryClient } from '@tanstack/react-query';
import { FileBadge, FileText, Search, Send } from 'lucide-react';
import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { issuanceApi } from '@/api/issuance';
import type { Advice, Outcome } from '@/api/issuance';
import { useAuth } from '@/auth/authContext';
import { ItemResultsDialog } from '@/components/broking/ItemResultsDialog';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime, humanize } from '@/utils/format';
import { GenerateAdviceDialog, SendAdviceDialog } from './AdviceDialogs';

/**
 * Insurance Advice register (BRNB.060/070/095/035): search by IA number, ARN or client; view and
 * download the PDF; generate for mortgaged accounts; send one or several, password protected.
 */
export default function InsuranceAdvicePage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const initial = params.get('ia') ?? '';
  const selection = useRowSelection();
  const download = useFileDownload();
  const [text, setText] = useState(initial);
  const [search, setSearch] = useState(initial);
  const [page, setPage] = useState(0);
  const [sending, setSending] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [outcomes, setOutcomes] = useState<Outcome[] | null>(null);
  const advices = useQuery({
    queryKey: ['issuance', 'advices', companyId, search, page],
    queryFn: () => issuanceApi.advices(companyId, search, page),
    enabled: companyId > 0,
  });
  const rows = advices.data?.content ?? [];
  const chosen = rows.filter((a) => selection.has(a.iaNo));
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['issuance'] });
  const canSend = ['EPOLICY_MANAGE', 'EPOLICY_SEND', 'ACCOUNT_MAINTAIN'].some((p) => can(p));
  return (
    <div className="stack">
      <PageHeader
        section="Policy Issuance"
        title="Insurance Advice"
        description="Insurance Advices of mortgaged accounts, generated on policy issue (or placement) from the Insurance Advice template, sent to the mortgagee bank password protected."
        actions={
          can('EPOLICY_MANAGE') && (
            <Button
              variant="secondary"
              icon={<FileBadge size={16} />}
              onClick={() => setGenerating(true)}
            >
              Generate Insurance Advice
            </Button>
          )
        }
      />
      <ErrorAlert error={advices.error ?? download.error} />
      <Card flush>
        <div className="work-toolbar">
          <form
            className="row"
            onSubmit={(e) => {
              e.preventDefault();
              setSearch(text.trim());
              setPage(0);
            }}
          >
            <label className="visually-hidden" htmlFor="ia-search">
              Search IA number, ARN or client
            </label>
            <input
              id="ia-search"
              className="input"
              placeholder="Search IA No., ARN or client"
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
            <Button type="submit" variant="secondary" icon={<Search size={16} />}>
              Search
            </Button>
          </form>
          <span className="spacer" />
          {canSend && (
            <Button
              variant="primary"
              icon={<Send size={16} />}
              disabled={chosen.length === 0}
              onClick={() => setSending(true)}
            >
              Send Selected
            </Button>
          )}
        </div>
        <DataTable<Advice>
          caption="Insurance Advices"
          loading={advices.isLoading}
          rows={rows}
          rowKey={(a) => a.iaNo}
          emptyMessage="No items to display"
          columns={[
            selectionColumn(
              rows,
              (a) => a.iaNo,
              selection,
              (a) => a.iaNo,
            ),
            { key: 'no', header: 'IA No.', render: (a) => <code>{a.iaNo}</code> },
            {
              key: 'client',
              header: 'Insured / Proposal No.',
              render: (a) => (
                <span>
                  <Link to={`/placement/accounts/${a.arn}`}>{a.clientName}</Link>
                  <span className="cell-sub">{a.arn}</span>
                </span>
              ),
            },
            { key: 'mortgagee', header: 'Mortgagee', render: (a) => humanize(a.mortgageeBank) },
            { key: 'policy', header: 'Policy No.', render: (a) => a.policyNumbers ?? 'to follow' },
            {
              key: 'trigger',
              header: 'Generated',
              render: (a) => `${humanize(a.triggerEvent)}, ${formatDateTime(a.createdAt)}`,
            },
            { key: 'status', header: 'Status', render: (a) => <StatusBadge status={a.status} /> },
            {
              key: 'sent',
              header: 'Last Sent',
              render: (a) =>
                a.lastSentAt ? `${formatDateTime(a.lastSentAt)} to ${a.lastSentTo ?? ''}` : '—',
            },
            {
              key: 'pdf',
              header: '',
              render: (a) => (
                <Button
                  size="sm"
                  variant="ghost"
                  icon={<FileText size={14} />}
                  onClick={() => download.mutate(() => issuanceApi.adviceFile(a.id))}
                >
                  PDF
                </Button>
              ),
            },
          ]}
        />
        <PageFooter data={advices.data} noun="advices" onPage={setPage} />
      </Card>
      {sending && (
        <SendAdviceDialog
          advices={chosen}
          onClose={() => setSending(false)}
          onSent={(sent) => {
            setSending(false);
            selection.clear();
            toast.success(`${sent.length} Insurance Advice(s) sent`);
            refresh();
          }}
        />
      )}
      {generating && (
        <GenerateAdviceDialog
          onClose={() => setGenerating(false)}
          onDone={(done) => {
            setGenerating(false);
            setOutcomes(done);
            refresh();
          }}
        />
      )}
      {outcomes && (
        <ItemResultsDialog
          title="Generate Insurance Advice"
          results={outcomes.map((o) => ({ reference: o.reference, ok: o.ok, message: o.message }))}
          onClose={() => setOutcomes(null)}
        />
      )}
    </div>
  );
}
