import { EbPlaceholder } from '../EbPlaceholder';

/**
 * New Programme (BRID-006, 022.01; FR-EB-021): client, lines, team, funding, contacts. Placeholder
 * of the foundation; built by wave E1-B.
 */
export default function NewProgrammePage() {
  return (
    <EbPlaceholder
      title="New Programme"
      description="Pick the client, then enter the benefit lines, the team, the funding and the HR contacts."
      backTo="/eb/programmes"
      emptyMessage="The programme form comes with the marketing wave"
    />
  );
}
