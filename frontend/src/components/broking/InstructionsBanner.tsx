import { useQuery } from '@tanstack/react-query';
import { clientsApi } from '@/api/clients';
import { Notice } from '@/components/ui/Notice';
import { Tag } from '@/components/ui/Tag';
import { formatDate } from '@/utils/format';
import { bannerKey } from './bannerKey';

/** The client tags and special instructions in force (one query shared by both parts). */
function useClientBanner(clientId: number) {
  return useQuery({
    queryKey: bannerKey(clientId),
    queryFn: () => clientsApi.banner(clientId),
    enabled: clientId > 0,
    staleTime: 60_000,
  });
}

/**
 * The client's tags (BRNB.091, e.g. "BDO employee") as standard tags for the record header's flag
 * list. Renders nothing when the client has none.
 */
export function ClientTagFlags({ clientId }: Readonly<{ clientId: number }>) {
  const tags = useClientBanner(clientId).data?.tags ?? [];
  return (
    <>
      {tags.map((t) => (
        <Tag key={t.code} tone="info">
          {t.label}
        </Tag>
      ))}
    </>
  );
}

/**
 * The special instructions in force for the client (BRNB.091), shown on every screen of the
 * client's quotations and accounts as one info notice, one bullet per instruction. The client tags
 * are in the record header (`ClientTagFlags`). Renders nothing when there is no instruction.
 */
export function InstructionsBanner({ clientId }: Readonly<{ clientId: number }>) {
  const instructions = useClientBanner(clientId).data?.instructions ?? [];
  if (instructions.length === 0) {
    return null;
  }
  return (
    <Notice
      tone="info"
      title="Special instructions"
      items={instructions.map((i) => (
        <span key={i.id}>
          <strong>{i.typeLabel}:</strong> {i.text}{' '}
          <span className="muted">
            (until {i.effectiveTo === undefined ? 'further notice' : formatDate(i.effectiveTo)})
          </span>
        </span>
      ))}
    />
  );
}
