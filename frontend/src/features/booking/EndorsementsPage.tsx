import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { bookingApi } from '@/api/booking';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { useCompanyId } from '@/context/workspaceContext';
import { EndorsementTable } from './InvoiceTabs';

/**
 * Endorsements and cancellations of booked accounts (BRNB.076/081/094), newest first. New
 * endorsements and cancellations are entered from the booked invoice.
 */
export default function EndorsementsPage() {
  const companyId = useCompanyId();
  const [page, setPage] = useState(0);
  const rows = useQuery({
    queryKey: ['booking', 'endorsements', companyId, page],
    queryFn: () => bookingApi.endorsements(companyId, page),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Booking"
        title="Endorsements"
        description="Positive, negative and non-financial endorsements and cancellations of booked accounts. Open a booked invoice to enter a new one."
      />
      <ErrorAlert error={rows.error} />
      <Card flush>
        <EndorsementTable rows={rows.data?.content ?? []} loading={rows.isLoading} />
        <PageFooter data={rows.data} noun="endorsements" onPage={setPage} />
      </Card>
    </div>
  );
}
