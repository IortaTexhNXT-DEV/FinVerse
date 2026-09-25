import { SectionLanding } from '@/components/broking/SectionLanding';

/** BDOI report pack and service fee (FRBS 2.10, 3.2): report groups once A1-FRBS adds them. */
export default function FrbsHomePage() {
  return (
    <SectionLanding
      section="Finance"
      title="Report Pack"
      description="The BDOI report pack (end of day, GARD, subsidiaries, schedules, Mancom, government) and the service-fee runs."
      cardTitle="Report Groups"
      emptyMessage="No reports to show yet"
    />
  );
}
