# Claude Instructions for AnkiDeku Project

## Server Management

**IMPORTANT**: Do NOT start the backend or frontend dev servers automatically.

When servers need to be running, **ASK THE USER** to start them manually instead:
- Backend: `cd backend && npm run dev`
- Frontend: `cd frontend && npm run dev`

This prevents multiple background processes from accumulating.

## Project Structure

- **Backend**: TypeScript Express server on port 3001
  - Entry: `backend/src/index.ts`
  - Services: AnkiConnect, Cache
  - Routes: Health, Decks, Notes

- **Frontend**: TypeScript React + Vite on port 5173
  - Entry: `frontend/src/main.tsx`
  - Components: DeckSelector, PromptInput, Queue, ComparisonView
  - State: Zustand store

- **AnkiConnect**: Runs on port 8765 (external)

## Development Notes

- Full TypeScript codebase (backend + frontend)
- Persistent storage in `database/` directory:
  - `database/decks/` - Deck note caches
  - `database/settings.json` - User settings (field display config, etc.)
- Batch processing (100 cards at a time) for large decks
- All `.js` and `.jsx` files have been migrated to `.ts` and `.tsx`

## Common Commands

```bash
# Backend
cd backend && npm run dev    # Start dev server
cd backend && npm run build  # Build TypeScript

# Frontend
cd frontend && npm run dev   # Start dev server
cd frontend && npm run build # Build for production

# Testing
curl http://localhost:3001/api/health
curl http://localhost:3001/api/anki/ping
curl http://localhost:3001/api/decks
```

## Frontend Code Practices

**Component Organization:**
- All reusable UI icons are in `frontend/src/components/ui/Icons.tsx`
- Custom hooks are in `frontend/src/hooks/` for business logic separation
- UI components in `frontend/src/components/ui/` for reusable UI elements
- Main feature components in `frontend/src/components/`

**Code Quality Standards:**
- **No inline SVG duplication** - Always use icon components from `Icons.tsx`
- **Separate business logic from UI** - Extract logic into custom hooks
- **Use custom hooks for:**
  - State management patterns (e.g., `useNotification`)
  - API calls and data fetching (e.g., `useCardGeneration`)
  - Complex user interactions (e.g., `useCardReview`)
- **Reusable components** - Create shared components in `ui/` folder
- **Type safety** - All components and hooks have proper TypeScript types

**Key Files:**
- `frontend/src/components/ui/Icons.tsx` - 21 icon components
- `frontend/src/hooks/useNotification.ts` - Notification management
- `frontend/src/hooks/useCardGeneration.ts` - Card suggestion generation
- `frontend/src/hooks/useCardReview.ts` - Review actions (accept/reject/skip)

## Important Reminders

1. **Always ask user to start servers** - don't start them automatically
2. TypeScript is configured and working for both backend and frontend
3. **Database directory** - All persistent data is stored in `database/`:
   - Deck caches in `database/decks/`
   - User settings in `database/settings.json`
4. Refer to `TODO.md` for project status and next steps
5. **Keep frontend clean** - Use icon components and custom hooks, avoid duplication
