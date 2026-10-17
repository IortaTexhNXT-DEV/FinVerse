import { Button } from '@/components/ui/Button';

/** The footer of an EB dialog: Cancel, then the action. */
export function DialogFooter({
  busy,
  label,
  onClose,
  onSave,
}: Readonly<{ busy: boolean; label: string; onClose: () => void; onSave: () => void }>) {
  return (
    <>
      <Button variant="secondary" onClick={onClose}>
        Cancel
      </Button>
      <Button busy={busy} onClick={onSave}>
        {label}
      </Button>
    </>
  );
}
