import {
  BadgeCheck,
  CalendarClock,
  CalendarRange,
  Landmark,
  ShieldCheck,
  UserRound,
  Users,
  Wallet,
} from 'lucide-react';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatAmount, formatDate, humanize } from '@/utils/format';
import type { Claim } from './api';
import { PREMIUM_LABELS, flagLabels } from './recordLogic';

/**
 * Summary card of a claim (design 11): claim number and ARN chips, the status pill, the flags
 * (unpaid premium, awaiting premium remittance, newer cover version, multi-location, multi-insurer,
 * CAT, claimant overridden) and the key facts.
 */
export function ClaimSummary({ claim }: Readonly<{ claim: Claim }>) {
  const c = claim.cover;
  const flags = flagLabels(claim.flags);
  return (
    <RecordSummary
      title={c.assuredName ?? claim.claimNo}
      chips={
        <>
          <ReferenceChip label="Claim" value={claim.claimNo} />
          <ReferenceChip label="ARN" value={c.arn} />
          <StatusBadge status={claim.progress.statusLabel ?? claim.progress.phase} />
        </>
      }
      flags={
        flags.length > 0 ? (
          <>
            {flags.map((f) => (
              <span key={f} className="tag">
                {f}
              </span>
            ))}
          </>
        ) : undefined
      }
      facts={[
        { icon: Landmark, label: 'Policy No.', value: c.policyNo ?? 'Policy number pending' },
        { icon: ShieldCheck, label: 'Cover', value: `${c.versionLabel} · year ${c.policyYear}` },
        {
          icon: CalendarRange,
          label: 'Loss / Reported',
          value: `${formatDate(claim.loss.lossDate)} / ${formatDate(claim.loss.reportedDate)}`,
        },
        {
          icon: BadgeCheck,
          label: 'Premium',
          value: [PREMIUM_LABELS[claim.premium.live.status], claim.premium.authorizationCode]
            .filter(Boolean)
            .join(' · '),
        },
        {
          icon: Wallet,
          label: `Insurer Reserve (${c.currency})`,
          value: formatAmount(claim.totalReserve),
        },
        {
          icon: Users,
          label: 'Marketing Team / AO',
          value: `${c.salesTeam ?? '—'} · ${c.accountOfficer ?? '—'}`,
        },
        {
          icon: UserRound,
          label: 'Handler',
          value: [claim.handler, claim.unitLabel].filter(Boolean).join(' · '),
        },
        {
          icon: CalendarClock,
          label: 'Next Follow-up',
          value: claim.progress.nextFollowUpDate
            ? formatDate(claim.progress.nextFollowUpDate)
            : humanize(claim.progress.phase),
        },
      ]}
    />
  );
}
