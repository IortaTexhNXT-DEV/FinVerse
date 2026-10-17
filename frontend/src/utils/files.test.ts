import { checkFile, extensionOf, formatBytes, screenFiles } from './files';

describe('file helpers', () => {
  it('formats sizes', () => {
    expect(formatBytes(512)).toBe('512 B');
    expect(formatBytes(1536)).toBe('1.5 KB');
    expect(formatBytes(10 * 1024 * 1024)).toBe('10.0 MB');
    expect(formatBytes(5 * 1024 ** 4)).toBe('5120.0 GB');
  });

  it('extracts extensions', () => {
    expect(extensionOf('Report.PDF')).toBe('pdf');
    expect(extensionOf('archive.tar.gz')).toBe('gz');
    expect(extensionOf('README')).toBe('');
  });

  it('pre-checks uploads', () => {
    const allowed = ['pdf', 'csv'];
    expect(checkFile({ name: 'a.pdf', size: 10 }, allowed, 100)).toBeUndefined();
    expect(checkFile({ name: 'a.pdf', size: 0 }, allowed, 100)).toBe('The file is empty.');
    expect(checkFile({ name: 'a.pdf', size: 200 }, allowed, 100)).toContain('larger');
    expect(checkFile({ name: 'a.exe', size: 10 }, allowed, 100)).toContain('pdf, csv');
  });

  it('pre-checks several files at once', () => {
    const allowed = ['pdf'];
    const ok = { name: 'a.pdf', size: 10 };
    expect(screenFiles([ok, ok], allowed, 100, 2)).toBeUndefined();
    expect(screenFiles([ok, ok, ok], allowed, 100, 2)).toBe('Choose at most 2 files at a time.');
    expect(screenFiles([ok, { name: 'b.exe', size: 5 }], allowed, 100, 5)).toMatch(/^b\.exe: /);
  });
});
