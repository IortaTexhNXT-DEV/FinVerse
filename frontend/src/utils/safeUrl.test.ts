import { describe, expect, it } from 'vitest';
import { safeUrl } from './safeUrl';

describe('safeUrl', () => {
  it('keeps web addresses', () => {
    expect(safeUrl('https://legacy.example.com/app')).toBe('https://legacy.example.com/app');
    expect(safeUrl(' http://intranet.example.com ')).toBe('http://intranet.example.com');
  });

  it('drops script, data, relative and empty addresses', () => {
    expect(safeUrl('javascript:alert(1)')).toBeUndefined();
    expect(safeUrl('JaVaScRiPt:alert(1)')).toBeUndefined();
    expect(safeUrl('data:text/html,<script>alert(1)</script>')).toBeUndefined();
    expect(safeUrl('//evil.example.com')).toBeUndefined();
    expect(safeUrl('https://')).toBeUndefined();
    expect(safeUrl('')).toBeUndefined();
    expect(safeUrl(undefined)).toBeUndefined();
    expect(safeUrl(null)).toBeUndefined();
  });
});
