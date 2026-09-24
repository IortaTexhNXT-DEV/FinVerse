import { useQuery } from '@tanstack/react-query';
import { Navigate, useParams } from 'react-router-dom';
import { bookingApi } from '@/api/booking';
import { ErrorAlert } from '@/components/ui/ErrorAlert';

/** Opens a booked invoice by its number (links from batch results and other modules). */
export default function InvoiceByNoPage() {
  const invoiceNo = decodeURIComponent(useParams().invoiceNo ?? '');
  const invoice = useQuery({
    queryKey: ['booking', 'invoice-no', invoiceNo],
    queryFn: () => bookingApi.invoiceByNo(invoiceNo),
  });
  if (invoice.data !== undefined) {
    return <Navigate to={`/booking/invoices/${String(invoice.data.id)}`} replace />;
  }
  return invoice.error ? (
    <ErrorAlert error={invoice.error} />
  ) : (
    <span className="spinner" aria-label="Loading" />
  );
}
