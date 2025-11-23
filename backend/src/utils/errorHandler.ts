import { Response } from 'express';
import type { ErrorResponse } from '../types/index.js';

/**
 * Extract error message from unknown error type
 */
export function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === 'string') {
    return error;
  }
  return 'Unknown error';
}

/**
 * Node.js error with optional code property
 */
export interface NodeError extends Error {
  code?: string;
  errno?: number;
}

/**
 * Check if error is a Node.js error with a code
 */
export function isNodeError(error: unknown): error is NodeError {
  return error instanceof Error && 'code' in error;
}

/**
 * Send standardized error response
 */
export function sendErrorResponse(
  res: Response,
  error: unknown,
  statusCode: number = 500
): void {
  const errorMessage = getErrorMessage(error);
  res.status(statusCode).json({ error: errorMessage } as ErrorResponse);
}

/**
 * Check if error is ENOENT (file not found)
 */
export function isFileNotFoundError(error: unknown): boolean {
  return isNodeError(error) && error.code === 'ENOENT';
}
