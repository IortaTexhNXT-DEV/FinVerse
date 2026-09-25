import { SectionLanding } from '@/components/broking/SectionLanding';

/**
 * Screening home (SNSRP-402, 405): open cases per stage, SLA due today and breached, potential
 * matches not yet cased and the last watchlist run, once the case and matching waves add them.
 */
export default function ScreeningHomePage() {
  return (
    <SectionLanding
      section="Client & Policy"
      title="Screening Home"
      description="Sanction and PEP screening of clients: open cases by stage, SLA due and breached, potential matches to review and the status of the last watchlist run."
      cardTitle="Screening Work"
      emptyMessage="No screening cases to show yet"
    />
  );
}
