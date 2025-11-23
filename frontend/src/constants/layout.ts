/**
 * Layout constants for consistent UI dimensions
 */

export const LAYOUT = {
  /** Height of the main header in pixels */
  HEADER_HEIGHT: 81,

  /** Width of the sidebar in rem */
  SIDEBAR_WIDTH: '28rem',

  /** Width of the queue sidebar in pixels */
  QUEUE_WIDTH: 288, // w-72 = 18rem = 288px
} as const;

export const BREAKPOINTS = {
  SM: '640px',
  MD: '768px',
  LG: '1024px',
  XL: '1280px',
  '2XL': '1536px',
} as const;
