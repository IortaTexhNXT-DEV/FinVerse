import { EbPlaceholder } from '../EbPlaceholder';

/**
 * Programme record (design 10.1): summary card, the EB_CYCLE workflow panel and the tabs. Placeholder
 * of the foundation; built by wave E1-B (Members, Member Changes, Billing & SOA and Pending Items
 * tabs by E1-C).
 */
export default function ProgrammePage() {
  return (
    <EbPlaceholder
      title="Programme"
      description="The programme with its current cycle and the tabs Cycle, Documents, BOR, Franchise, Insurer Requests, Proposals, Comparative, Members, Member Changes, Billing & SOA, Pending Items and History."
      backTo="/eb/programmes"
      emptyMessage="The programme record comes with the marketing wave"
    />
  );
}
