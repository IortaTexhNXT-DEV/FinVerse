import type { RowAction } from '@/components/ui/RowActions';

/**
 * The row menus of the certificate registers (screen standard: the actions on a record of a list
 * are one menu at the end of the row; a destructive action comes last, in red).
 */

/** A received certificate still recorded may be cancelled, with a reason asked in a dialog. */
export function receivedCertificateActions(
  c: { status: string },
  mayCancel: boolean,
  cancel: () => void,
): RowAction[] {
  return mayCancel && c.status === 'RECORDED'
    ? [{ label: 'Cancel Certificate', danger: true, onSelect: cancel }]
    : [];
}

/** An issued BIR Form 2307: download the form; the tax team may cancel it after a confirmation. */
export function issuedCertificateActions(
  c: { status: string; certificateNo?: string | null },
  mayCancel: boolean,
  on: { download: () => unknown; cancel: () => unknown },
): RowAction[] {
  const actions: RowAction[] = [{ label: 'Download PDF', onSelect: () => on.download() }];
  if (mayCancel && c.status === 'ISSUED') {
    actions.push({
      label: 'Cancel Certificate',
      danger: true,
      onSelect: () => on.cancel(),
      confirm: {
        title: 'Cancel Certificate',
        record: c.certificateNo ?? undefined,
        effect: 'The issued certificate is cancelled.',
        confirmLabel: 'Cancel Certificate',
        destructive: true,
      },
    });
  }
  return actions;
}
