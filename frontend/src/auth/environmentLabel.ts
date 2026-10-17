/** Labels of the environments that are not production, shown on the sign-in page. */
const ENVIRONMENT_LABELS: Readonly<Record<string, string>> = {
  local: 'Local',
  dev: 'Development',
  sit: 'SIT',
  uat: 'UAT',
  training: 'Training',
  preprod: 'Pre-production',
};

/** The environment label, or null in production (where no label is shown). */
export function environmentLabel(environment: string | undefined): string | null {
  if (environment === undefined || environment === '') {
    return null;
  }
  const key = environment.toLowerCase();
  if (key === 'production' || key === 'prod') {
    return null;
  }
  return ENVIRONMENT_LABELS[key] ?? environment.toUpperCase();
}
