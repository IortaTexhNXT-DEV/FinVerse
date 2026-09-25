import { SectionLanding } from '@/components/broking/SectionLanding';

/** Refund and cash-advance requests (MKT 1.2-2.26): requests by stage once A1-PRQ adds them. */
export default function PayRequestsHomePage() {
  return (
    <SectionLanding
      section="Finance"
      title="Refund & Cash Advance Requests"
      description="Client refund requests, employee cash advances and disbursed-check cancellations, from preparation to payment."
      emptyMessage="No requests to show yet"
    />
  );
}
