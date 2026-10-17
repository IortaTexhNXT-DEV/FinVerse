import { useContext } from 'react';
import { BRAND } from '@/branding';
import { WorkspaceContext } from './workspaceContext';

/**
 * Names of the client in texts and labels: the short name of the company ("Via <short name>",
 * "<short name> Only") and the name of its group ("<group> Bank Client"). They come from the
 * client profile of the selected company (company master), else from the theme pack; no client
 * name is written into the screens.
 */
export interface ClientNames {
  /** Short name of the company. */
  shortName: string;
  /** Group of the company; '' when it has none. */
  groupName: string;
}

interface CompanyNames {
  shortName?: string;
  groupName?: string;
}

const FALLBACK: ClientNames = { shortName: BRAND.clientShortName, groupName: BRAND.groupName };

let current: ClientNames = FALLBACK;

/**
 * The names of a company, with the theme pack names where the company master has none.
 *
 * @param company company, undefined while loading
 * @returns names
 */
export function namesOf(company: CompanyNames | undefined): ClientNames {
  if (company === undefined) {
    return FALLBACK;
  }
  return {
    shortName: company.shortName ?? FALLBACK.shortName,
    groupName: company.groupName ?? FALLBACK.groupName,
  };
}

/**
 * Keeps the names of the selected company for the labels built outside components (label maps
 * of codes); called by the workspace when the company changes.
 *
 * @param company selected company
 */
export function useClientNamesOf(company: CompanyNames | undefined): void {
  current = namesOf(company);
}

/** Short name of the selected company, for label maps of codes. */
export function clientShortName(): string {
  return current.shortName;
}

/**
 * A label of a concept of the company's group: "<group> bank client" for the concept "bank
 * client"; the concept alone, capitalised, when the company has no group.
 *
 * @param concept concept
 * @param names client names, the selected company's by default
 * @returns label
 */
export function groupLabel(concept: string, names: ClientNames = current): string {
  return names.groupName === ''
    ? concept.charAt(0).toUpperCase() + concept.slice(1)
    : `${names.groupName} ${concept}`;
}

/** The client names of the selected company. */
export function useClientNames(): ClientNames {
  return namesOf(useContext(WorkspaceContext)?.company);
}
