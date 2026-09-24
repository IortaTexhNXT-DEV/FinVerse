import { useMutation, useQuery } from '@tanstack/react-query';
import { FileDown, Printer } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { saveFile } from '@/api/client';
import { clientsApi } from '@/api/clients';
import type { BankFilter, ClientListItem, KycReviewFilters } from '@/api/clients';
import { reportApi } from '@/api/reports';
import type { ExportFormat } from '@/api/reports';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime, humanize } from '@/utils/format';

type Filters = Omit<KycReviewFilters, 'companyId'>;

const BANK_OPTIONS: { value: BankFilter; label: string }[] = [
  { value: 'NON_BANK', label: 'Non-bank clients' },
  { value: 'BANK', label: 'BDO bank clients' },
  { value: 'ALL', label: 'All clients' },
];

/** Parameters of report NB-KYC-DUE for the current filters. */
function reportParams(companyId: number, f: Filters): Record<string, string> {
  const params: Record<string, string> = {
    companyId: String(companyId),
    bankClients: f.bank ?? 'NON_BANK',
  };
  if (f.dueBy) {
    params.dueBy = f.dueBy;
  }
  if (f.riskRating) {
    params.riskRating = f.riskRating;
  }
  return params;
}

function FilterBar({
  filters,
  onChange,
}: Readonly<{ filters: Filters; onChange: (f: Filters) => void }>) {
  const set = (patch: Partial<Filters>) => onChange({ ...filters, ...patch, page: 0 });
  return (
    <Card>
      <div className="form-grid">
        <Field label="Due by" hint="Blank = overdue and due within the review window">
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              value={filters.dueBy ?? ''}
              onChange={(e) => set({ dueBy: e.target.value || undefined })}
            />
          )}
        </Field>
        <Field label="Clients">
          {(id) => (
            <select
              id={id}
              className="select"
              value={filters.bank ?? 'NON_BANK'}
              onChange={(e) => set({ bank: e.target.value as BankFilter })}
            >
              {BANK_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Risk rating">
          {(id) => (
            <LovSelect
              id={id}
              type="KYC_RISK_RATING"
              placeholder="All"
              value={filters.riskRating ?? ''}
              onChange={(code) => set({ riskRating: code || undefined })}
            />
          )}
        </Field>
        <Field label="Market segment">
          {(id) => (
            <LovSelect
              id={id}
              type="MARKET_SEGMENT"
              placeholder="All"
              value={filters.marketSegment ?? ''}
              onChange={(code) => set({ marketSegment: code || undefined })}
            />
          )}
        </Field>
      </div>
    </Card>
  );
}

/**
 * KYC Reviews Due (BRNB.110): clients whose periodic KYC review is overdue (expired) or due, by
 * default the non-bank clients. Download (report NB-KYC-DUE) and print the list.
 */
export default function KycReviewsPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const [filters, setFilters] = useState<Filters>({ bank: 'NON_BANK' });
  const query: KycReviewFilters = { ...filters, companyId };
  const due = useQuery({
    queryKey: ['crm', 'kyc-reviews', query],
    queryFn: () => clientsApi.kycReviews(query),
    enabled: companyId > 0,
  });
  const download = useMutation({
    mutationFn: (format: ExportFormat) =>
      reportApi.export('NB-KYC-DUE', reportParams(companyId, filters), format),
    onSuccess: (file) => {
      saveFile(file.blob, file.fileName);
      toast.success(`${file.fileName} downloaded`);
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section="Clients"
        title="KYC Reviews Due"
        description="Clients whose periodic KYC review is overdue or coming due. The monthly KYC_REVIEW_DUE job expires overdue KYC and notifies the Account Officers."
        actions={
          <div className="row no-print">
            <Button
              variant="secondary"
              icon={<FileDown size={16} />}
              busy={download.isPending && download.variables === 'XLSX'}
              onClick={() => download.mutate('XLSX')}
            >
              Excel
            </Button>
            <Button
              variant="secondary"
              icon={<FileDown size={16} />}
              busy={download.isPending && download.variables === 'PDF'}
              onClick={() => download.mutate('PDF')}
            >
              PDF
            </Button>
            <Button variant="secondary" icon={<Printer size={16} />} onClick={() => window.print()}>
              Print
            </Button>
          </div>
        }
      />
      <FilterBar filters={filters} onChange={setFilters} />
      <ErrorAlert error={due.error ?? download.error} />
      <Card flush title={`${String(due.data?.totalElements ?? 0)} client(s) to review`}>
        <DataTable<ClientListItem>
          loading={due.isLoading}
          rows={due.data?.content ?? []}
          rowKey={(c) => c.id}
          onRowClick={(c) => void navigate(`/crm/clients/${String(c.id)}`)}
          emptyMessage="No KYC review is due for these filters."
          columns={[
            { key: 'code', header: 'Code', render: (c) => <span className="mono">{c.code}</span> },
            { key: 'name', header: 'Client', render: (c) => c.displayName },
            { key: 'segment', header: 'Segment', render: (c) => c.marketSegment ?? '' },
            {
              key: 'risk',
              header: 'Risk',
              render: (c) => (c.riskRating ? humanize(c.riskRating) : ''),
            },
            {
              key: 'verified',
              header: 'Last verified',
              render: (c) => formatDateTime(c.kycVerifiedAt),
            },
            { key: 'due', header: 'Review due', render: (c) => formatDate(c.kycReviewDue) },
            { key: 'kyc', header: 'KYC', render: (c) => <StatusBadge status={c.kycStatus} /> },
          ]}
        />
        <PageFooter
          data={due.data}
          noun="clients"
          onPage={(page) => setFilters({ ...filters, page })}
        />
      </Card>
    </div>
  );
}
