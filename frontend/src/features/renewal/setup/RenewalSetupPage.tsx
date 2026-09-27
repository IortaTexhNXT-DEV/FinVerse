import { useQuery } from '@tanstack/react-query';
import { renewalApi } from '@/api/renewal';
import { useAuth } from '@/auth/authContext';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useTabParam } from '@/components/ui/useTabParam';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import { ChecksTab } from './ChecksTab';
import { GoLiveTab } from './GoLiveTab';
import { PackageChoicesTab } from './PackageChoicesTab';
import { PackageMapTab } from './PackageMapTab';
import { RiskCodesTab } from './RiskCodesTab';
import { RuleVersionsTab } from './RuleVersionsTab';
import { MATRIX_FIELDS, blankBucketRule, blankDecisionRule, bucketFields } from './ruleFields';
import '../renewal.css';

const TABS = [
  { id: 'risk', label: 'Non-renewable Risk Codes', permission: 'RNW_SETUP' },
  { id: 'checks', label: 'Checks', permission: 'RNW_SETUP' },
  { id: 'buckets', label: 'Classification Rules', permission: 'RNW_SETUP' },
  { id: 'matrix', label: 'Decision Matrix', permission: 'RNW_SETUP' },
  { id: 'packages', label: 'Package Map', permission: 'RNW_PACKAGE_REMAP' },
  { id: 'choices', label: 'Package Choices', permission: 'RNW_PACKAGE_REMAP' },
  { id: 'golive', label: 'Go-live', permission: 'RNW_SETUP' },
] as const;

type TabId = (typeof TABS)[number]['id'];

function BucketRules() {
  const checks = useQuery({
    queryKey: ['renewal', 'setup', 'checks'],
    queryFn: renewalApi.checkSettings,
  });
  return (
    <RuleVersionsTab
      title="Classification rules"
      queryKey="bucket-rules"
      api={{
        list: renewalApi.bucketRules,
        save: renewalApi.saveBucketRules,
        decide: renewalApi.decideBucketRules,
      }}
      fields={bucketFields(checks.data ?? [])}
      blank={blankBucketRule}
    />
  );
}

function Body({ tab }: Readonly<{ tab: TabId }>) {
  switch (tab) {
    case 'checks':
      return <ChecksTab />;
    case 'buckets':
      return <BucketRules />;
    case 'matrix':
      return (
        <RuleVersionsTab
          title="Decision matrix"
          queryKey="matrix"
          api={{
            list: renewalApi.matrices,
            save: renewalApi.saveMatrix,
            decide: renewalApi.decideMatrix,
          }}
          fields={MATRIX_FIELDS}
          blank={blankDecisionRule}
        />
      );
    case 'packages':
      return <PackageMapTab />;
    case 'choices':
      return <PackageChoicesTab />;
    case 'golive':
      return <GoLiveTab />;
    default:
      return <RiskCodesTab />;
  }
}

/**
 * Renewal Setup (FR-RN-016, 024, 028, 112): non-renewable risk codes, the sanitation checks, the
 * classification rules and decision matrix versions (maker and checker), the package code map of
 * migrated policies with the package choices to approve, and the go-live take-over.
 */
export default function RenewalSetupPage() {
  const { can } = useAuth();
  const tabs = TABS.filter((t) => can(t.permission));
  const ids = tabs.map((t) => t.id);
  const [tab, setTab] = useTabParam<TabId>(ids, ids[0] ?? 'risk');
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Renewal Setup"
        description="Rules of the renewal checks, classification and dispositions."
      />
      <Tabs tabs={tabs} active={tab} onChange={setTab} />
      <Body tab={tab} />
    </div>
  );
}
