import type { UploadResult } from '@/api/journalAutomation';
import {
  checkUploadFile,
  invalidRows,
  MAX_UPLOAD_BYTES,
  parseCsv,
  previewCsv,
  summarize,
} from './uploadHelpers';

describe('journal upload helpers', () => {
  it('parses quoted fields, doubled quotes, CRLF, BOM and skips blank lines', () => {
    const text = '\uFEFFa,b,c\r\n"x, y","say ""hi""","two\nlines"\r\n\r\n1,,3';
    expect(parseCsv(text)).toEqual([
      ['a', 'b', 'c'],
      ['x, y', 'say "hi"', 'two\nlines'],
      ['1', '', '3'],
    ]);
    expect(parseCsv('')).toEqual([]);
  });

  it('previews the header, first rows and missing columns', () => {
    const text =
      'Voucher Key,branch_code,value-date,currency,narration,account_code,debit\n' +
      'V1,HO,2026-01-31,PHP,Rent,5603,100\nV1,,,,,1111,\nV2,HO,2026-01-31,PHP,Fee,5605,5';
    const preview = previewCsv(text, 2);
    expect(preview.header[0]).toBe('voucher_key');
    expect(preview.header[2]).toBe('value_date');
    expect(preview.rows).toHaveLength(2);
    expect(preview.rows[1]).toEqual({ rowNumber: 3, cells: ['V1', '', '', '', '', '1111', ''] });
    expect(preview.totalRows).toBe(3);
    expect(preview.missingColumns).toEqual(['credit']);
    expect(previewCsv('').totalRows).toBe(0);
  });

  it('pre-checks the file type and size', () => {
    expect(checkUploadFile({ name: 'journals.xlsx', size: 100 })).toBeUndefined();
    expect(checkUploadFile({ name: 'journals.CSV', size: 100 })).toBeUndefined();
    expect(checkUploadFile({ name: 'journals.txt', size: 100 })).toContain('csv, xlsx');
    expect(checkUploadFile({ name: 'journals.csv', size: MAX_UPLOAD_BYTES + 1 })).toContain(
      'larger',
    );
  });

  it('summarizes a validation report', () => {
    const result: UploadResult = {
      fileName: 'j.csv',
      committed: true,
      totalRows: 5,
      vouchers: [
        {
          voucherKey: 'A',
          firstRow: 2,
          lineCount: 2,
          totalDebit: 10,
          status: 'CREATED',
          messages: [],
        },
        {
          voucherKey: 'B',
          firstRow: 4,
          lineCount: 2,
          totalDebit: 5,
          status: 'ERROR',
          messages: ['x'],
        },
        {
          voucherKey: 'C',
          firstRow: 6,
          lineCount: 2,
          totalDebit: 5,
          status: 'VALID',
          messages: [],
        },
      ],
      rows: [
        { rowNumber: 5, voucherKey: 'B', valid: false, messages: ['debit'] },
        { rowNumber: 2, voucherKey: 'A', valid: true, messages: [] },
        { rowNumber: 4, voucherKey: 'B', valid: false, messages: ['account'] },
      ],
    };
    expect(summarize(result)).toEqual({
      vouchers: 3,
      valid: 2,
      created: 1,
      rejected: 1,
      rowErrors: 2,
    });
    expect(invalidRows(result).map((r) => r.rowNumber)).toEqual([4, 5]);
  });
});
