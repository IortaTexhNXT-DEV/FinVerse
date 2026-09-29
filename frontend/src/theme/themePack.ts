/**
 * A theme pack: the branding of one client deployment (product name, client name and short name,
 * logo, sign-in photo, colour and font tokens, print title). The pack is chosen at build time with
 * VITE_THEME_PACK (default "bdoi") and resolved as the module "@theme-pack" (vite.config.ts); its
 * theme.json also fills the page title, description and icon of index.html. No client name, logo
 * or colour is written into the components.
 */
export interface ThemePack {
  /** Pack id, the folder name under src/theme/packs. */
  id: string;
  /** Product name shown to users, e.g. in the header and the page titles. */
  product: string;
  /** Long product name. */
  productName: string;
  /** Client name under its logo. */
  client: string;
  /** Short name of the client in texts, until the company master gives one. */
  clientShortName: string;
  /** Group of the client in labels of group concepts, until the company master gives one. */
  groupName: string;
  /** Description of the web page. */
  description: string;
  /** Title printed above every page. */
  printTitle: string;
  /** Client logo (URL of the bundled image). */
  clientLogo: string;
  /** Sign-in photo (URL of the bundled image). */
  loginPhoto: string;
}
