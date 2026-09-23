import { useCallback, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { ToastContext } from './toastContext';

interface ToastMessage {
  id: number;
  text: string;
  error: boolean;
}

const DISMISS_AFTER_MS = 5000;
let nextId = 1;

/** Transient notifications (announced to screen readers through a live region). */
export function ToastProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [messages, setMessages] = useState<ToastMessage[]>([]);

  const push = useCallback((text: string, error: boolean) => {
    const id = nextId++;
    setMessages((m) => [...m, { id, text, error }]);
    setTimeout(() => {
      setMessages((m) => m.filter((x) => x.id !== id));
    }, DISMISS_AFTER_MS);
  }, []);

  const value = useMemo(
    () => ({ success: (t: string) => push(t, false), error: (t: string) => push(t, true) }),
    [push],
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-region" aria-live="polite">
        {messages.map((m) => (
          <div key={m.id} className={m.error ? 'toast error' : 'toast'}>
            {m.text}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
