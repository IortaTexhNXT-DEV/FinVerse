import { useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { BulkUploadWizard } from '@/components/broking/BulkUploadWizard';
import { PageHeader } from '@/components/ui/PageHeader';

/**
 * Booking by upload (BRNB.036/061, handler BOOKING_UPLOAD): one ARN per row with an optional
 * booking date and cost center; each valid row is booked on its own, exactly as an individual
 * booking.
 */
export default function BookingUploadPage() {
  const queryClient = useQueryClient();
  return (
    <div className="stack">
      <PageHeader
        section="Booking"
        backTo="/booking"
        title="Upload Bookings"
        description="Download the template, list the accounts to book (ARN, optional booking date and cost center) and upload it."
        actions={<Link to="/bulk">All upload types</Link>}
      />
      <BulkUploadWizard
        handler="BOOKING_UPLOAD"
        onCommitted={() => void queryClient.invalidateQueries({ queryKey: ['booking'] })}
      />
    </div>
  );
}
