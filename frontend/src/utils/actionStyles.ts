import { CheckIcon, CloseIcon, ClockIcon } from '../components/ui/Icons';
import type { ComponentType } from 'react';

export type ActionType = 'accept' | 'reject' | 'skip';

interface ActionStyle {
  bgClass: string;
  textClass: string;
  iconClass: string;
  Icon: ComponentType<{ className?: string }>;
  label: string;
}

export const ACTION_STYLES: Record<ActionType, ActionStyle> = {
  accept: {
    bgClass: 'bg-green-100 dark:bg-green-900',
    textClass: 'text-green-600 dark:text-green-400',
    iconClass: 'text-green-600 dark:text-green-400',
    Icon: CheckIcon,
    label: 'Accepted'
  },
  reject: {
    bgClass: 'bg-red-100 dark:bg-red-900',
    textClass: 'text-red-600 dark:text-red-400',
    iconClass: 'text-red-600 dark:text-red-400',
    Icon: CloseIcon,
    label: 'Rejected'
  },
  skip: {
    bgClass: 'bg-gray-100 dark:bg-gray-600',
    textClass: 'text-gray-600 dark:text-gray-300',
    iconClass: 'text-gray-600 dark:text-gray-300',
    Icon: ClockIcon,
    label: 'Skipped'
  }
};

/**
 * Get action style for a given action type
 */
export function getActionStyle(action: string): ActionStyle {
  return ACTION_STYLES[action as ActionType] || ACTION_STYLES.skip;
}
