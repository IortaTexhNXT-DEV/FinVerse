import { SectionLanding } from '@/components/broking/SectionLanding';

/** Collections home (BRCLXN.001-012, CQ21): the collector's work queues once C1-A adds them. */
export default function CollectionsHomePage() {
  return (
    <SectionLanding
      section="Finance"
      title="Collections Home"
      description="Outstanding premium receivables to follow up: accounts, promises, installments, escalations and unapplied payments."
      emptyMessage="No collection work to show yet"
    />
  );
}
