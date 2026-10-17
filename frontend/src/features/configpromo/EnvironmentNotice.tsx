import { useQuery } from '@tanstack/react-query';
import { configPromotionApi } from '@/api/configPromotion';
import { Notice } from '@/components/ui/Notice';
import { windowText } from './promotion';

/**
 * Where the user works: the environment, whether it is production (change window, change
 * reference), and a warning when packages cannot be signed here.
 */
export function EnvironmentNotice() {
  const env = useQuery({
    queryKey: ['config-promotion', 'environment'],
    queryFn: configPromotionApi.environment,
  });
  if (env.data === undefined) {
    return null;
  }
  const e = env.data;
  if (!e.signingConfigured) {
    return (
      <Notice tone="warning" title="Packages cannot be signed in this environment">
        The signing key of configuration packages is not set for {e.environment}; exports and
        imports are refused until operations set it.
      </Notice>
    );
  }
  if (e.production) {
    return (
      <Notice tone="info" title={`Production (${e.environment})`}>
        {windowText(e)}
      </Notice>
    );
  }
  return null;
}
