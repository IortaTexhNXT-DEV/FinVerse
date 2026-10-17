/** Query key of a client's banner (invalidate it after a tag or instruction change). */
export function bannerKey(clientId: number) {
  return ['crm', 'banner', clientId] as const;
}
