import { useQuery } from '@tanstack/react-query';
import { clientsApi } from '@/api/clients';
import { CellStack } from '@/components/ui/CellStack';

/**
 * A client shown by name with its client (or prospect) number on the muted line, for lists that
 * only hold the client's internal id; never "Client #123".
 */
export function ClientLabel({ clientId }: Readonly<{ clientId: number }>) {
  const client = useQuery({
    queryKey: ['crm', 'client', clientId],
    queryFn: () => clientsApi.get(clientId),
    staleTime: 5 * 60_000,
  });
  if (client.data === undefined) {
    return <span className="skeleton-line" aria-label="Loading client" />;
  }
  return (
    <CellStack
      main={client.data.displayName}
      sub={client.data.clientCode ?? client.data.prospectCode}
    />
  );
}
