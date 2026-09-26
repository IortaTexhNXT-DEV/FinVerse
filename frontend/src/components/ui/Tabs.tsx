import { useRef } from 'react';
import type { KeyboardEvent } from 'react';

interface TabDef<T extends string> {
  id: T;
  label: string;
  /** Number of items behind the tab (records, documents...), shown as a small count. */
  count?: number;
}

interface TabsProps<T extends string> {
  tabs: readonly TabDef<T>[];
  active: T;
  onChange: (id: T) => void;
}

const NEXT_KEYS: Record<string, number> = {
  ArrowRight: 1,
  ArrowDown: 1,
  ArrowLeft: -1,
  ArrowUp: -1,
};

/**
 * Accessible tab strip (BDO boxed tabs) with optional counts. Keyboard: the arrow keys move
 * between tabs, Home and End go to the first and last; only the active tab is in the tab order.
 */
export function Tabs<T extends string>({ tabs, active, onChange }: Readonly<TabsProps<T>>) {
  const strip = useRef<HTMLDivElement>(null);
  const focusTab = (index: number) => {
    const wrapped = (index + tabs.length) % tabs.length;
    const target = tabs[wrapped];
    if (target !== undefined) {
      onChange(target.id);
      const buttons = strip.current?.querySelectorAll<HTMLButtonElement>('[role="tab"]');
      buttons?.[wrapped]?.focus();
    }
  };
  const onKeyDown = (e: KeyboardEvent<HTMLButtonElement>) => {
    const current = tabs.findIndex((t) => t.id === active);
    const step = NEXT_KEYS[e.key];
    if (step !== undefined) {
      e.preventDefault();
      focusTab(current + step);
    } else if (e.key === 'Home') {
      e.preventDefault();
      focusTab(0);
    } else if (e.key === 'End') {
      e.preventDefault();
      focusTab(tabs.length - 1);
    }
  };
  return (
    <div className="tabs" role="tablist" ref={strip}>
      {tabs.map((t) => (
        <button
          key={t.id}
          type="button"
          role="tab"
          className="tab"
          aria-selected={t.id === active}
          tabIndex={t.id === active ? 0 : -1}
          onClick={() => onChange(t.id)}
          onKeyDown={onKeyDown}
        >
          {t.label}
          {t.count !== undefined && (
            <span className="tab-count" aria-label={`${String(t.count)} items`}>
              {t.count}
            </span>
          )}
        </button>
      ))}
    </div>
  );
}
