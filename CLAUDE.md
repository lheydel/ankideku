# Claude Instructions for AnkiDeku Project

## Server Management

**IMPORTANT**: Do NOT start the backend or frontend dev servers automatically.

When servers need to be running, **ASK THE USER** to start them manually instead:
- Backend: `cd backend && npm run dev`
- Frontend: `cd frontend && npm run dev`

This prevents multiple background processes from accumulating.

## Project Structure

- **Backend**: TypeScript Express server on port 3000
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
- Filesystem cache in `cache/decks/` for fast loading
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
curl http://localhost:3000/api/health
curl http://localhost:3000/api/anki/ping
curl http://localhost:3000/api/decks
```

## Important Reminders

1. **Always ask user to start servers** - don't start them automatically
2. TypeScript is configured and working for both backend and frontend
3. Cache files are stored in `cache/decks/` directory
4. Refer to `TODO.md` for project status and next steps
