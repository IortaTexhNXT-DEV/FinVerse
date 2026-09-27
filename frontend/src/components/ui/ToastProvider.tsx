import { CircleAlert, CircleCheck, Info, TriangleAlert } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { useCallback, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { plainMessage } from './errorView';
import type { NoticeTone } from './Notice';
import { ToastContext } from './toastContext';

interface ToastMessage {
  id: number;
  text: string;
  tone: NoticeTone;
}

const DISMISS_AFTER_MS = 5000;
const ICONS: Record<NoticeTone, LucideIcon> = {
  success: CircleCheck,
  error: CircleAlert,
  warning: TriangleAlert,
  info: Info,
};
let nextId = 1;

/**
 * Transient notifications (announced to screen readers through a live region), in the semantic
 * colours of the notice standard: a white toast with the accent bar and icon of its tone. Technical
 * codes and identifiers are removed from the text, as in error notices.
 */
export function ToastProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [messages, setMessages] = useState<ToastMessage[]>([]);

  const push = useCallback((text: string, tone: NoticeTone) => {
    const id = nextId++;
    const shown = plainMessage(text) || text;
    setMessages((m) => [...m, { id, text: shown, tone }]);
    setTimeout(() => {
      setMessages((m) => m.filter((x) => x.id !== id));
    }, DISMISS_AFTER_MS);
  }, []);

  const value = useMemo(
    () => ({
      success: (t: string) => push(t, 'success'),
      error: (t: string) => push(t, 'error'),
      warning: (t: string) => push(t, 'warning'),
      info: (t: string) => push(t, 'info'),
    }),
    [push],
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-region" aria-live="polite">
        {messages.map((m) => {
          const Icon = ICONS[m.tone];
          return (
            <div key={m.id} className={`toast ${m.tone}`}>
              <Icon className="notice-icon" size={18} aria-hidden="true" />
              <span>{m.text}</span>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}
