import { useCallback, useEffect, useRef } from 'react';
import { AUTOSAVE_MS } from './accountForm';

/**
 * Saves a value every `intervalMs` while `enabled`, only when it changed since the last save
 * (BRNB.051 draft autosave). Returns `markSaved`, to call after an explicit save so the same
 * content is not saved again.
 */
export function useAutosave<T>(
  value: T,
  enabled: boolean,
  save: (value: T) => Promise<unknown>,
  intervalMs: number = AUTOSAVE_MS,
): (saved: T) => void {
  const latest = useRef(value);
  const saver = useRef(save);
  const lastSaved = useRef<string | undefined>(undefined);
  useEffect(() => {
    latest.current = value;
    saver.current = save;
  });
  useEffect(() => {
    if (!enabled) {
      return undefined;
    }
    const timer = setInterval(() => {
      const snapshot = JSON.stringify(latest.current);
      if (snapshot !== lastSaved.current) {
        lastSaved.current = snapshot;
        saver.current(latest.current).catch(() => {
          lastSaved.current = undefined;
        });
      }
    }, intervalMs);
    return () => clearInterval(timer);
  }, [enabled, intervalMs]);
  return useCallback((saved: T) => {
    lastSaved.current = JSON.stringify(saved);
  }, []);
}
