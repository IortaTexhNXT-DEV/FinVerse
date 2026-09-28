import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { productMaintApi } from '@/api/productmaint';
import { useAuth } from '@/auth/authContext';
import { useCompanyId } from '@/context/workspaceContext';

/**
 * The package request a version came from, as a link to the request when the user may open it,
 * else its number.
 */
export function PackageRequestLink({ requestNo }: Readonly<{ requestNo: string }>) {
  const { can } = useAuth();
  const companyId = useCompanyId();
  const found = useQuery({
    queryKey: ['productmaint', 'request-by-no', companyId, requestNo],
    queryFn: () => productMaintApi.search(companyId, { text: requestNo }, 0, 5),
    enabled: can('PRODUCT_VIEW'),
  });
  const request = found.data?.content.find((r) => r.requestNo === requestNo);
  if (request === undefined) {
    return <span className="nowrap">{requestNo}</span>;
  }
  return (
    <Link className="nowrap" to={`/product-maintenance/requests/${String(request.id)}`}>
      {requestNo}
    </Link>
  );
}
