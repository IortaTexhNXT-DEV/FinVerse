import { ClaimsPlaceholder } from '../ClaimsPlaceholder';

/**
 * Claims Setup (BRCLM.010/012/014/017/036): status and settlement attributes, the status access
 * matrix, the claims handler register and the Claims lists. Placeholder of the foundation; built
 * by CL1-B.
 */
export default function ClaimsSetupPage() {
  return (
    <ClaimsPlaceholder
      title="Claims Setup"
      description="Status and settlement type attributes, who may set each status, the claims handlers with their units, and the Claims lists of values."
      cardTitle="Setup"
      emptyMessage="Claims setup is not available yet"
    />
  );
}
