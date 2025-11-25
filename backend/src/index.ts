import express from 'express';
import cors from 'cors';
import { createServer } from 'http';
import { Server as SocketIOServer } from 'socket.io';
import { CONFIG, validateConfig } from './config.js';
import { sessionService } from './services/session/SessionService.js';
import { suggestionWriter } from './services/storage/SuggestionWriter.js';
import { sessionEventEmitter } from './services/session/SessionEventEmitter.js';
import { syncProgressEmitter } from './services/anki/SyncProgressEmitter.js';
import { SessionOrchestrator } from './services/session/SessionOrchestrator.js';
import { SocketEvent } from '../../contract/types.js';

// Route imports
import sessionsRouter, { initializeRouter } from './routes/sessions.js';
import historyRouter from './routes/history.js';
import healthRouter from './routes/health.js';
import ankiRouter from './routes/anki.js';
import decksRouter from './routes/decks.js';
import notesRouter from './routes/notes.js';
import settingsRouter from './routes/settings.js';
import llmRouter from './routes/llm.js';

// Validate configuration on startup
validateConfig();

const app = express();

// Create HTTP server for Socket.IO
const httpServer = createServer(app);

// Initialize Socket.IO
const io = new SocketIOServer(httpServer, {
  cors: {
    origin: CONFIG.server.corsOrigin,
    methods: ["GET", "POST"]
  }
});

// Wire up SessionEventEmitter with Socket.IO
sessionEventEmitter.setSocketIO(io);

// Wire up SyncProgressEmitter with Socket.IO
syncProgressEmitter.setSocketIO(io);

// Create SessionOrchestrator with dependencies
const sessionOrchestrator = new SessionOrchestrator(suggestionWriter, sessionEventEmitter);

// Initialize sessions router with service dependencies
initializeRouter({ sessionService, sessionOrchestrator });

// Middleware
app.use(cors());
app.use(express.json());

// WebSocket connection handling
io.on('connection', (socket) => {
  console.log('Client connected:', socket.id);

  socket.on(SocketEvent.SUBSCRIBE_SESSION, (sessionId: string) => {
    socket.join(sessionId);
    console.log(`Client ${socket.id} subscribed to session: ${sessionId}`);
  });

  socket.on(SocketEvent.UNSUBSCRIBE_SESSION, (sessionId: string) => {
    socket.leave(sessionId);
    console.log(`Client ${socket.id} unsubscribed from session: ${sessionId}`);
  });

  // Sync progress subscriptions
  socket.on(SocketEvent.SUBSCRIBE_SYNC, (deckName: string) => {
    const room = `sync:${deckName}`;
    socket.join(room);
    console.log(`Client ${socket.id} subscribed to sync: ${deckName}`);
  });

  socket.on(SocketEvent.UNSUBSCRIBE_SYNC, (deckName: string) => {
    const room = `sync:${deckName}`;
    socket.leave(room);
    console.log(`Client ${socket.id} unsubscribed from sync: ${deckName}`);
  });

  socket.on('disconnect', () => {
    console.log('Client disconnected:', socket.id);
  });
});

// Mount routes
app.use('/api/health', healthRouter);
app.use('/api/sessions', sessionsRouter);
app.use('/api', historyRouter);
app.use('/api/anki', ankiRouter);
app.use('/api/decks', decksRouter);
app.use('/api/notes', notesRouter);
app.use('/api/settings', settingsRouter);
app.use('/api/llm', llmRouter);

// Graceful shutdown handling
process.on('SIGTERM', async () => {
  console.log('SIGTERM received, shutting down gracefully...');
  httpServer.close(() => {
    console.log('Server closed');
    process.exit(0);
  });
});

process.on('SIGINT', async () => {
  console.log('SIGINT received, shutting down gracefully...');
  httpServer.close(() => {
    console.log('Server closed');
    process.exit(0);
  });
});

// Start server
httpServer.listen(CONFIG.server.port, () => {
  console.log(`AnkiDeku backend running on http://localhost:${CONFIG.server.port}`);
  console.log(`WebSocket server ready for connections`);
  console.log(`CORS enabled for: ${CONFIG.server.corsOrigin}`);
});
