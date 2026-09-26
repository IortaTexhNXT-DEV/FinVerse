/** Query key of a record's workflow panel (invalidate it after a business action). */
export function workflowKey(entityType: string, entityId: string | number) {
  return ['workflow', entityType, String(entityId)] as const;
}
