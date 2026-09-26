import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { ConfirmDialog } from './ConfirmDialog';

/** The in-app path a plain left click follows, if the click is on such a link. */
function inAppLink(e: MouseEvent): string | undefined {
  if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey) {
    return undefined;
  }
  const anchor = e.target instanceof Element ? e.target.closest('a[href]') : null;
  const href = anchor?.getAttribute('href');
  if (!href?.startsWith('/') || anchor?.getAttribute('target') === '_blank') {
    return undefined;
  }
  return href;
}

/**
 * Unsaved-changes guard of a form: while `dirty`, closing or reloading the tab asks the browser's
 * confirmation, and following an in-app link opens a themed dialog (Leave Page / Go Back).
 * Returns the dialog to render in the form.
 */
export function useUnsavedChangesGuard(dirty: boolean): ReactNode {
  const navigate = useNavigate();
  const [target, setTarget] = useState<string | null>(null);

  useEffect(() => {
    if (!dirty) {
      return undefined;
    }
    const beforeUnload = (e: BeforeUnloadEvent) => {
      e.preventDefault();
    };
    const click = (e: MouseEvent) => {
      const href = inAppLink(e);
      if (href === undefined) {
        return;
      }
      e.preventDefault();
      e.stopPropagation();
      setTarget(href);
    };
    window.addEventListener('beforeunload', beforeUnload);
    document.addEventListener('click', click, true);
    return () => {
      window.removeEventListener('beforeunload', beforeUnload);
      document.removeEventListener('click', click, true);
    };
  }, [dirty]);

  if (target === null) {
    return null;
  }
  return (
    <ConfirmDialog
      title="Leave Without Saving?"
      effect="The changes on this page are not saved and will be lost."
      confirmLabel="Leave Page"
      destructive
      onClose={() => setTarget(null)}
      onConfirm={() => {
        const to = target;
        setTarget(null);
        void navigate(to);
      }}
    />
  );
}
