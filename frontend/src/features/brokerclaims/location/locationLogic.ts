/** Pure rules of the insurer location references (FR-CM-023). */

export interface RefErrors {
  arn?: string;
  item?: string;
  insurer?: string;
  reference?: string;
}

/** Errors of a new insurer location reference. */
export function refErrors(
  arn: string,
  item: string,
  insurer: string,
  reference: string,
): RefErrors {
  return {
    arn: arn.trim() === '' ? 'Enter the ARN of the cover' : undefined,
    item: item === '' ? 'Select the location' : undefined,
    insurer: insurer === '' ? 'Select the insurer' : undefined,
    reference: reference.trim() === '' ? 'Enter the insurer location reference' : undefined,
  };
}

/** Whether a set of field errors is empty. */
export function noErrors(errors: object): boolean {
  return Object.values(errors).every((e) => e === undefined);
}
