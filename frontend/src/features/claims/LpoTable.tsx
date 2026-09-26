import { useMutation } from '@tanstack/react-query';
import { claimsApi } from '@/api/claims';
import type { Lpo } from '@/api/claims';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate } from '@/utils/format';

interface Props {
  lpos: Lpo[];
  loading: boolean;
  showClaim?: boolean;
  onChange: () => Promise<void>;
  onOpenClaim?: (lpo: Lpo) => void;
}

/** LPO list shared by the claim LPO tab and the LPO register, with cancellation. */
export function LpoTable({
  lpos,
  loading,
  showClaim = false,
  onChange,
  onOpenClaim,
}: Readonly<Props>) {
  const { can } = useAuth();
  const toast = useToast();
  const cancel = useMutation({
    mutationFn: (lpo: Lpo) => claimsApi.cancelLpo(lpo.id, 'Cancelled by the claims officer'),
    onSuccess: async (lpo) => {
      toast.success(`${lpo.lpoNo} cancelled`);
      await onChange();
    },
  });
  const columns = [
    { key: 'no', header: 'LPO No.', render: (l: Lpo) => <strong>{l.lpoNo}</strong> },
    ...(showClaim ? [{ key: 'claim', header: 'Claim', render: (l: Lpo) => l.claimNo }] : []),
    { key: 'garage', header: 'Garage', render: (l: Lpo) => l.garageName },
    { key: 'cover', header: 'Cover', render: (l: Lpo) => l.coverType },
    { key: 'date', header: 'Issued', render: (l: Lpo) => formatDate(l.issueDate) },
    {
      key: 'gross',
      header: 'Gross',
      numeric: true,
      render: (l: Lpo) => <Amount value={l.grossAmount} />,
    },
    {
      key: 'disc',
      header: 'Discount',
      numeric: true,
      render: (l: Lpo) => <Amount value={l.discountAmount} />,
    },
    {
      key: 'net',
      header: 'Net',
      numeric: true,
      render: (l: Lpo) => <Amount value={l.netAmount} />,
    },
    { key: 'st', header: 'Status', render: (l: Lpo) => <StatusBadge status={l.status} /> },
    {
      key: 'act',
      header: '',
      render: (l: Lpo) =>
        l.status === 'ISSUED' &&
        can('CLAIM_MAINTAIN') && (
          <Button
            size="sm"
            variant="ghost"
            busy={cancel.isPending}
            onClick={(e) => {
              e.stopPropagation();
              cancel.mutate(l);
            }}
          >
            Cancel
          </Button>
        ),
    },
  ];
  return (
    <>
      <ErrorAlert error={cancel.error} />
      <DataTable<Lpo>
        loading={loading}
        rows={lpos}
        rowKey={(l) => l.id}
        caption="Local purchase orders"
        onRowClick={onOpenClaim}
        columns={columns}
      />
    </>
  );
}
