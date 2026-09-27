import type { ReactNode } from 'react';
import type { ClientDetail } from '@/api/clients';
import { Card } from '@/components/ui/Card';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import type { Definition } from '@/components/ui/DefinitionGrid';
import { UserName } from '@/components/ui/UserName';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { describeBank } from './clientLabels';
import { LovLabel } from '@/components/broking/LovLabel';

function Section({ title, facts }: Readonly<{ title: string; facts: Definition[] }>) {
  return (
    <Card title={title}>
      <DefinitionGrid label={title} items={facts} collapseEmpty />
    </Card>
  );
}

const joined = (...parts: (string | undefined)[]) =>
  parts.filter((p) => p !== undefined && p !== '').join(', ');

const code = (value: string | undefined) => (value === undefined ? undefined : humanize(value));

/** The label of a list-of-values code, nothing when there is no code. */
const lov = (type: string, value: string | undefined) =>
  value ? <LovLabel type={type} code={value} /> : undefined;

function identityFacts(c: ClientDetail): Definition[] {
  if (c.clientType === 'CORPORATE') {
    return [
      { label: 'Registered Name', value: c.corporateName },
      { label: 'Nature of Business', value: c.profile.occupation },
    ];
  }
  return [
    { label: 'Name', value: c.displayName },
    { label: 'Birth Date', value: formatDate(c.birthDate) },
    { label: 'Nationality', value: lov('NATIONALITY', c.profile.nationality) },
    { label: 'Civil Status', value: code(c.profile.civilStatus) },
    { label: 'Occupation', value: c.profile.occupation },
  ];
}

/** "Aileen Account Officer · 25-Sep-2026 19:30": the user's display name and the time. */
function byAndWhen(user: string | undefined, when: string | undefined): ReactNode {
  if (user === undefined) {
    return undefined;
  }
  return (
    <span className="cell-stack">
      <UserName login={user} />
      <span className="muted">{formatDateTime(when)}</span>
    </span>
  );
}

function onboardingFacts(c: ClientDetail): Definition[] {
  const l = c.lifecycle;
  return [
    { label: 'Created', value: byAndWhen(l.createdBy, l.createdAt) },
    { label: 'KYC Submitted', value: byAndWhen(c.kyc.submittedBy, c.kyc.submittedAt) },
    { label: 'KYC Verified', value: byAndWhen(c.kyc.verifiedBy, c.kyc.verifiedAt) },
    { label: 'Next KYC Review', value: formatDate(c.kyc.reviewDue) },
    { label: 'Confirmed', value: byAndWhen(l.confirmedBy, l.confirmedAt) },
    {
      label: 'Deactivated',
      value:
        l.deactivatedBy === undefined ? undefined : (
          <span className="cell-stack">
            {byAndWhen(l.deactivatedBy, l.deactivatedAt)}
            <span className="muted">{joined(code(l.deactivationReason), l.deactivationNote)}</span>
          </span>
        ),
    },
  ];
}

/**
 * Complete client details (BRNB.046): identity, contact, segment and onboarding sections in one
 * card grid with aligned label / value rows; empty fields show a dash and sections that are
 * mostly empty collapse to their filled fields.
 */
export function ClientDetailsTab({ client: c }: Readonly<{ client: ClientDetail }>) {
  return (
    <div className="detail-grid">
      <Section
        title="Identity"
        facts={[
          ...identityFacts(c),
          { label: 'TIN', value: c.tin },
          { label: 'ID', value: joined(code(c.idType), c.idNumber) },
        ]}
      />
      <Section
        title="Contact & Address"
        facts={[
          { label: 'E-mail', value: c.email },
          { label: 'Mobile', value: c.mobile },
          { label: 'Landline', value: c.phone },
          { label: 'Address', value: joined(c.addressLine, c.city, c.province, c.postalCode) },
        ]}
      />
      <Section
        title="Segment & Bank Relationship"
        facts={[
          { label: 'Market Segment', value: lov('MARKET_SEGMENT', c.marketSegment) },
          { label: 'Bank Relationship', value: describeBank(c) },
          { label: 'Source of Funds', value: lov('SOURCE_OF_FUNDS', c.profile.sourceOfFunds) },
          { label: 'KYC Risk Rating', value: lov('KYC_RISK_RATING', c.profile.riskRating) },
          { label: 'Sub-ledger Party', value: c.partyCode },
        ]}
      />
      <Section title="Onboarding" facts={onboardingFacts(c)} />
    </div>
  );
}
