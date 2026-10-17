import { ConfirmDialog } from '@/components/ui/ConfirmDialog';

/** Asks for the remark of a dismissal or a resolution. */
export function RemarkDialog({
  title,
  record,
  effect,
  confirmLabel,
  busy,
  error,
  onConfirm,
  onClose,
}: Readonly<{
  title: string;
  record: string;
  effect: string;
  confirmLabel: string;
  busy: boolean;
  error: unknown;
  onConfirm: (remark: string) => void;
  onClose: () => void;
}>) {
  return (
    <ConfirmDialog
      title={title}
      record={record}
      effect={effect}
      confirmLabel={confirmLabel}
      reason="required"
      reasonMax={500}
      busy={busy}
      error={error}
      onConfirm={onConfirm}
      onClose={onClose}
    />
  );
}
