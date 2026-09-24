import type { ClientDetail } from '@/api/clients';
import { Card } from '@/components/ui/Card';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { describeBank } from './clientLabels';

type Fact = [string, string | undefined];

function Facts({ title, facts }: Readonly<{ title: string; facts: Fact[] }>) {
  return (
    <Card title={title}>
      <dl className="detail-list">
        {facts.map(([label, value]) => (
          <div key={label} style={{ display: 'contents' }}>
            <dt>{label}</dt>
            <dd>{value === undefined || value === '' ? '—' : value}</dd>
          </div>
        ))}
      </dl>
    </Card>
  );
}

const joined = (...parts: (string | undefined)[]) =>
  parts.filter((p) => p !== undefined && p !== '').join(', ');

const code = (value: string | undefined) => (value === undefined ? undefined : humanize(value));

function identityFacts(c: ClientDetail): Fact[] {
  if (c.clientType === 'CORPORATE') {
    return [
      ['Registered name', c.corporateName],
      ['Nature of business', c.profile.occupation],
    ];
  }
  return [
    ['Name', c.displayName],
    ['Birth date', formatDate(c.birthDate)],
    ['Nationality', code(c.profile.nationality)],
    ['Civil status', code(c.profile.civilStatus)],
    ['Occupation', c.profile.occupation],
  ];
}

const byAndWhen = (user: string | undefined, when: string | undefined) =>
  user === undefined ? undefined : `${user} · ${formatDateTime(when)}`;

function onboardingFacts(c: ClientDetail): Fact[] {
  const l = c.lifecycle;
  return [
    ['Created', byAndWhen(l.createdBy, l.createdAt)],
    ['KYC submitted', byAndWhen(c.kyc.submittedBy, c.kyc.submittedAt)],
    ['KYC verified', byAndWhen(c.kyc.verifiedBy, c.kyc.verifiedAt)],
    ['Next KYC review', formatDate(c.kyc.reviewDue)],
    ['Confirmed', byAndWhen(l.confirmedBy, l.confirmedAt)],
    [
      'Deactivated',
      l.deactivatedBy === undefined
        ? undefined
        : joined(
            byAndWhen(l.deactivatedBy, l.deactivatedAt),
            code(l.deactivationReason),
            l.deactivationNote,
          ),
    ],
  ];
}

/** Complete client details (BRNB.046): identity, contact, segment and onboarding facts. */
export function ClientDetailsTab({ client: c }: Readonly<{ client: ClientDetail }>) {
  return (
    <div className="grid-2">
      <Facts
        title="Identity"
        facts={[...identityFacts(c), ['TIN', c.tin], ['ID', joined(code(c.idType), c.idNumber)]]}
      />
      <Facts
        title="Contact & Address"
        facts={[
          ['E-mail', c.email],
          ['Mobile', c.mobile],
          ['Landline', c.phone],
          ['Address', joined(c.addressLine, c.city, c.province, c.postalCode)],
        ]}
      />
      <Facts
        title="Segment & Bank relationship"
        facts={[
          ['Market segment', c.marketSegment],
          ['Bank relationship', describeBank(c)],
          ['Source of funds', code(c.profile.sourceOfFunds)],
          ['KYC risk rating', code(c.profile.riskRating)],
          ['Sub-ledger party', c.partyCode],
        ]}
      />
      <Facts title="Onboarding" facts={onboardingFacts(c)} />
    </div>
  );
}
