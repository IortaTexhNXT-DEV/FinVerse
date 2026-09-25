import { SectionLanding } from '@/components/broking/SectionLanding';

/** ACSL cases board (ACSL 2.5-2.9): cases and corrections by stage once A1-PRQ adds them. */
export default function AcslHomePage() {
  return (
    <SectionLanding
      section="Finance"
      title="ACSL Cases"
      description="Analysis requests, investigations and correction entries by stage, with the insurer SOA and GL-SL reconciliations."
      cardTitle="Cases by Stage"
      emptyMessage="No ACSL cases to show yet"
    />
  );
}
