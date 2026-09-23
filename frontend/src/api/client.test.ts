import { api, ApiError, tokenStore, toQuery } from './client';

describe('api client', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    tokenStore.clear();
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
