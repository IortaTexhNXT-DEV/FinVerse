import type { ReactNode } from 'react';
import { Button } from '@/components/ui/Button';

/** A small secondary card action, rendered only when the user may take it. */
export function ActionButton({
  shown,
  icon,
  label,
  onClick,
}: Readonly<{ shown: boolean; icon: ReactNode; label: string; onClick: () => void }>) {
  return shown ? (
    <Button variant="secondary" size="sm" icon={icon} onClick={onClick}>
      {label}
    </Button>
  ) : null;
}
