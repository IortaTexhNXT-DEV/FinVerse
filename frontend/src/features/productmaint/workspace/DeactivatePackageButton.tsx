import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { DeactivateDialog } from './DeactivateDialog';

interface DeactivatePackageButtonProps {
  product: {
    code: string;
    name: string;
    packageEndDate?: string | null;
    /** ACTIVE, EXPIRED or RETIRED; the button shows for an active package only. */
    lifecycleStatus?: string;
  };
}

/**
 * The Deactivate Package button of the Package Details screen (BDOI FRS FRPM.003.04), for an
 * active package and a user who may raise package requests or maintain products.
 */
export function DeactivatePackageButton({ product }: Readonly<DeactivatePackageButtonProps>) {
  const { can } = useAuth();
  const [open, setOpen] = useState(false);
  if (
    (product.lifecycleStatus ?? 'ACTIVE') !== 'ACTIVE' ||
    !(can('PKG_REQUEST') || can('PRODUCT_MAINTAIN'))
  ) {
    return null;
  }
  return (
    <>
      <Button variant="danger" onClick={() => setOpen(true)}>
        Deactivate Package
      </Button>
      {open && (
        <DeactivateDialog
          productCode={product.code}
          packageName={product.name}
          expiryDate={product.packageEndDate ?? null}
          onClose={() => setOpen(false)}
        />
      )}
    </>
  );
}
