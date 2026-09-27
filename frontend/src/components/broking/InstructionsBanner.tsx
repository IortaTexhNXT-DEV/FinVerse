import { useQuery } from '@tanstack/react-query';
import { Info, Tag } from 'lucide-react';
import { clientsApi } from '@/api/clients';
import { formatDate } from '@/utils/format';
import { bannerKey } from './bannerKey';

/**
 * Client tags and special instructions in force (BRNB.091), shown at the top of every screen of
 * the client's quotations and accounts. Renders nothing when the client has none.
 */
export function InstructionsBanner({ clientId }: Readonly<{ clientId: number }>) {
  const banner = useQuery({
    queryKey: bannerKey(clientId),
    queryFn: () => clientsApi.banner(clientId),
    enabled: clientId > 0,
    staleTime: 60_000,
  });
  const data = banner.data;
  if (data === undefined || (data.tags.length === 0 && data.instructions.length === 0)) {
    return null;
  }
  return (
    <section className="instructions-banner" aria-label="Client tags and special instructions">
      {data.tags.length > 0 && (
        <div className="instructions-banner-tags">
          <Tag size={14} aria-hidden="true" />
          {data.tags.map((t) => (
            <span key={t.code} className="client-tag">
              {t.label}
            </span>
          ))}
        </div>
      )}
      {data.instructions.map((i) => (
        <div key={i.id} className="instructions-banner-item">
          <Info size={14} aria-hidden="true" />
          <strong>{i.typeLabel}:</strong> <span>{i.text}</span>
          <span className="muted">
            {' '}
            (until {i.effectiveTo === undefined ? 'further notice' : formatDate(i.effectiveTo)})
          </span>
        </div>
      ))}
    </section>
  );
}
