import { X } from 'lucide-react';
import { useEffect } from 'react';
import type { ReactNode } from 'react';

interface ModalProps {
  title: string;
  open: boolean;
  onClose: () => void;
  footer?: ReactNode;
  children: ReactNode;
}

/** Modal dialog with backdrop; Escape closes it. */
export function Modal({ title, open, onClose, footer, children }: Readonly<ModalProps>) {
  useEffect(() => {
    if (!open) {
      return undefined;
    }
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  if (!open) {
    return null;
  }
  return (
    <div className="modal-backdrop">
      <dialog className="modal" aria-label={title} aria-modal="true" open>
        <header className="card-header">
          <h2>{title}</h2>
          <div className="spacer" />
          <button
            type="button"
            className="btn btn-ghost btn-sm modal-close"
            onClick={onClose}
            aria-label="Close"
          >
            <X size={22} aria-hidden="true" />
          </button>
        </header>
        <div className="card-body">{children}</div>
        {footer !== undefined && <footer className="modal-footer">{footer}</footer>}
      </dialog>
    </div>
  );
}
