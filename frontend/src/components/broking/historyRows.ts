import type { HistoryEntry } from '@/api/workflow';
import { formatDuration } from '@/utils/format';

/** A row of the history table with the time spent in the stage it entered. */
export interface HistoryRow {
  entry: HistoryEntry;
  /** Time spent in the stage this row entered: until the next change, or until now. */
  duration: string;
  order: number;
}

/** Builds the history rows with the duration in each stage, oldest first. */
export function historyRows(history: HistoryEntry[], terminal = false): HistoryRow[] {
  return history.map((entry, i) => {
    const next = history[i + 1];
    let duration = '';
    if (next !== undefined) {
      duration = formatDuration(entry.occurredAt, next.occurredAt);
    } else if (!terminal) {
      duration = formatDuration(entry.occurredAt, undefined);
    }
    return { entry, duration, order: i };
  });
}
