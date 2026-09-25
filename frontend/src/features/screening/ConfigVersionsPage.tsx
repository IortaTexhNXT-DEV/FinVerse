import { SectionLanding } from '@/components/broking/SectionLanding';

/**
 * Screening configuration versions (SNSRP-101-109): matching criteria, risk rules, approval,
 * assignment and SLA matrices, validation rules and templates as dated versions under maker-checker.
 */
export default function ConfigVersionsPage() {
  return (
    <SectionLanding
      section="Setup & Administration"
      title="Configuration Versions"
      description="Matching thresholds, risk categories and rules, approval, assignment and SLA matrices, validation rules and templates, each as a dated version approved by a Compliance Checker."
      cardTitle="Versions by Configuration Type"
      emptyMessage="No configuration versions to show yet"
    />
  );
}
