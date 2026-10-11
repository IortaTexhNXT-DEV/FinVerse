import { FileUp, Repeat } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule, ScreenDef } from '@/navigation/types';

const journalAutomation: ScreenDef[] = [
  {
    path: '/gl/recurring',
    label: 'Recurring Journals',
    icon: Repeat,
    permission: 'JOURNAL_VIEW',
    component: lazyWithRetry(() => import('./RecurringJournalsPage')),
  },
  {
    path: '/gl/upload',
    label: 'Journal Upload',
    icon: FileUp,
    permission: 'JOURNAL_CREATE',
    component: lazyWithRetry(() => import('./JournalUploadPage')),
  },
];

/** General Ledger section with recurring journals and bulk upload after "New Journal". */
export function withJournalAutomation(gl: FeatureModule): FeatureModule {
  const anchor = gl.screens.findIndex((s) => s.path === '/gl/journals/new');
  const at = anchor < 0 ? gl.screens.length : anchor + 1;
  return {
    ...gl,
    screens: [...gl.screens.slice(0, at), ...journalAutomation, ...gl.screens.slice(at)],
  };
}
