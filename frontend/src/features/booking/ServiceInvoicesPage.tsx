import { useQuery } from '@tanstack/react-query';
import { Search } from 'lucide-react';
import { useState } from 'react';
import { bookingApi } from '@/api/booking';
import type { SiKind } from '@/api/booking';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { ServiceInvoiceTable } from './InvoiceTabs';
import './booking.css';

type KindTab = 'ALL' | SiKind;

const KIND_TABS: readonly { id: KindTab; label: string }[] = [
  { id: 'ALL', label: 'All' },
  { id: 'INVOICE', label: 'Service Invoices' },
  { id: 'CREDIT', label: 'Credits' },
];

/**
 * Service invoice register (BRNB.100/100b): every service invoice and credit with its e-mail
 * outcome; open one to download the PDF, send it again or credit it.
 */
export default function ServiceInvoicesPage() {
  const companyId = useCompanyId();
  const [kind, setKind] = useState<KindTab>('ALL');
  const [text, setText] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const rows = useQuery({
    queryKey: ['booking', 'service-invoices', companyId, kind, query, page],
    queryFn: () =>
      bookingApi.serviceInvoices(companyId, query, kind === 'ALL' ? undefined : kind, page),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Booking"
        title="Service Invoices"
        description="Commission invoices to insurers and internal service invoices, with their credits and e-mail dispatch."
      />
      <ErrorAlert error={rows.error} />
      <Card>
        <div className="stack">
          <Tabs
            tabs={KIND_TABS}
            active={kind}
            onChange={(k) => {
              setKind(k);
              setPage(0);
            }}
          />
          <form
            className="booking-toolbar"
            onSubmit={(e) => {
              e.preventDefault();
              setQuery(text.trim());
              setPage(0);
            }}
          >
            <label className="visually-hidden" htmlFor="si-search">
              Search Service Invoice No.
            </label>
            <input
              id="si-search"
              className="input"
              placeholder="Search Service Invoice No., invoice, ARN or recipient"
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
            <Button type="submit" variant="secondary" icon={<Search size={14} />}>
              Search
            </Button>
          </form>
          <ServiceInvoiceTable rows={rows.data?.content ?? []} loading={rows.isLoading} />
          <PageFooter data={rows.data} noun="service invoices" onPage={setPage} />
        </div>
      </Card>
    </div>
  );
}
