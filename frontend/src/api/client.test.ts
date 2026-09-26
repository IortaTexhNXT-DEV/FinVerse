import { api, ApiError, fileNameOf, tokenStore, toQuery } from './client';

describe('api client', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    tokenStore.clear();
  });

  it('keeps the token and its expiry per tab and clears both', () => {
    tokenStore.set('abc', '2026-09-24T18:00:00Z');
    expect(tokenStore.get()).toBe('abc');
    expect(tokenStore.expiresAt()).toBe('2026-09-24T18:00:00Z');
    expect(localStorage.length).toBe(0);
    tokenStore.set('def');
    expect(tokenStore.expiresAt()).toBeNull();
    tokenStore.clear();
    expect(tokenStore.get()).toBeNull();
  });

  it('builds query strings without empty values', () => {
    expect(toQuery({ a: 1, b: '', c: undefined, d: 'x y' })).toBe('?a=1&d=x+y');
    expect(toQuery({})).toBe('');
  });

  it('sends the bearer token and parses JSON', async () => {
    tokenStore.set('abc');
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(new Response(JSON.stringify({ ok: true }), { status: 200 }));
    await expect(api.get('/x')).resolves.toEqual({ ok: true });
    const init = fetchMock.mock.calls[0]?.[1];
    expect((init?.headers as Record<string, string>).Authorization).toBe('Bearer abc');
  });

  it('sends multipart forms without a JSON content type and downloads files', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: 1 }), { status: 201 }))
      .mockResolvedValueOnce(
        new Response('a,b', {
          status: 200,
          headers: { 'Content-Disposition': 'attachment; filename="t.csv"' },
        }),
      );
    const form = new FormData();
    form.append('file', new Blob(['x']), 'x.csv');
    await expect(api.upload('/attachments', form)).resolves.toEqual({ id: 1 });
    const init = fetchMock.mock.calls[0]?.[1];
    expect(init?.body).toBe(form);
    expect((init?.headers as Record<string, string>)['Content-Type']).toBeUndefined();
    const file = await api.getFile('/journals/upload/template');
    expect(file.fileName).toBe('t.csv');
  });

  it('prefers the RFC 5987 filename* over the plain file name', () => {
    expect(
      fileNameOf(
        `attachment; filename="=?UTF-8?Q?E2E-IT-maintenance-invoice.pdf?="; filename*=UTF-8''E2E-IT-maintenance-invoice.pdf`,
        'download',
      ),
    ).toBe('E2E-IT-maintenance-invoice.pdf');
    expect(
      fileNameOf(
        `attachment; filename="Resume.pdf"; filename*=UTF-8''R%C3%A9sum%C3%A9%20v2.pdf`,
        'x',
      ),
    ).toBe('Résumé v2.pdf');
    expect(fileNameOf(`attachment; filename*=iso-8859-1'en'caf%E9.txt`, 'x')).toBe('café.txt');
  });

  it('falls back to the plain file name, then to the default', () => {
    expect(fileNameOf('attachment; filename="GL-TB.pdf"', 'report')).toBe('GL-TB.pdf');
    expect(fileNameOf('attachment; filename=GL-TB.csv', 'report')).toBe('GL-TB.csv');
    expect(fileNameOf('attachment; filename="a \\"quoted\\" name.txt"', 'x')).toBe(
      'a "quoted" name.txt',
    );
    expect(fileNameOf(`attachment; filename*=UTF-8''%E0%A4%A; filename="safe.txt"`, 'x')).toBe(
      'safe.txt',
    );
    expect(fileNameOf('attachment', 'report')).toBe('report');
    expect(fileNameOf(null, 'download')).toBe('download');
  });

  it('turns problem responses into ApiError with the backend code', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ detail: 'Period closed', code: 'PERIOD_NOT_OPEN' }), {
        status: 422,
      }),
    );
    const error = await api.post('/journals/1/approve').catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).code).toBe('PERIOD_NOT_OPEN');
    expect((error as ApiError).message).toBe('Period closed');
  });
});
