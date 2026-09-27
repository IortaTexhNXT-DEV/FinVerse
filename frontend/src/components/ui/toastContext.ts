import { createContext, useContext } from 'react';

export interface ToastApi {
  success: (text: string) => void;
  error: (text: string) => void;
}

export const ToastContext = createContext<ToastApi | null>(null);

/** Shows transient notifications (see ToastProvider). */
export function useToast(): ToastApi {
  const ctx = useContext(ToastContext);
  if (ctx === null) {
    throw new Error('useToast must be used inside ToastProvider');
  }
  return ctx;
}
