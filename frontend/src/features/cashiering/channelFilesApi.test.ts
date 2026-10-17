import { describe, expect, it } from 'vitest';
import { FILE_STATUS_LABELS, FILE_TYPE_LABELS, filesPerType, pickedFiles } from './channelFilesApi';

const file = (name: string) => new File(['1|00001'], name);

describe('Payment files upload (FRS.CSH.05.01.03)', () => {
  it('keeps several files of several types in one upload and lists them per type', () => {
    const rows = [
      { type: 'BILLS_PAYMENT' as const, file: file('BDOI202510091.TXT') },
      { type: 'BILLS_PAYMENT' as const, file: file('BDOI202510092.TXT') },
      { type: 'CLPC' as const, file: file('CLPC-Oct092025.xlsx') },
      { type: 'TRADE' as const },
    ];
    expect(pickedFiles(rows)).toHaveLength(3);
    expect(filesPerType(rows)).toEqual({
      'Bills Payment': ['BDOI202510091.TXT', 'BDOI202510092.TXT'],
      CLPC: ['CLPC-Oct092025.xlsx'],
    });
  });

  it('names every file type and status', () => {
    expect(Object.keys(FILE_TYPE_LABELS)).toHaveLength(6);
    expect(FILE_STATUS_LABELS.PARTIAL).toBe('Processed with failed rows');
  });
});
