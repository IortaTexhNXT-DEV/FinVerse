import { useQuery } from '@tanstack/react-query';
import { bookingApi } from '@/api/booking';
import type { ServiceInvoiceType } from '@/api/booking';
import { humanize } from '@/utils/format';

/** The name of a service invoice type ("Insurer commission"): the set-up name, else the code in words. */
export function serviceInvoiceTypeName(
  types: readonly ServiceInvoiceType[] | undefined,
  code: string,
): string {
  return types?.find((t) => t.code === code)?.name ?? humanize(code);
}

/** Name lookup of the service invoice types of the booking set-up. */
export function useServiceInvoiceTypeName(): (code: string) => string {
  const types = useQuery({
    queryKey: ['booking', 'service-invoice-types'],
    queryFn: bookingApi.serviceInvoiceTypes,
    staleTime: 5 * 60_000,
    retry: false,
  });
  return (code) => serviceInvoiceTypeName(types.data, code);
}
