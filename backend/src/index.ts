import express from 'express';
import cors from 'cors';
import { createServer } from 'http';
import { Server as SocketIOServer } from 'socket.io';
import { CONFIG, validateConfig } from './config.js';
import { sessionService } from './services/sessionService.js';
import { FileWatcherService } from './services/fileWatcher.js';
import { ClaudeSpawnerService } from './services/claudeSpawner.js';

// Route imports
import sessionsRouter, { initializeRouter } from './routes/sessions.js';
import historyRouter from './routes/history.js';
import healthRouter from './routes/health.js';
import ankiRouter from './routes/anki.js';
import decksRouter from './routes/decks.js';
import notesRouter from './routes/notes.js';
import settingsRouter from './routes/settings.js';

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

// Initialize AI workflow services (sessionService is already a singleton)
const fileWatcher = new FileWatcherService();
const claudeSpawner = new ClaudeSpawnerService();

// Initialize sessions router with service dependencies
initializeRouter({ sessionService, fileWatcher, claudeSpawner });

// Middleware
app.use(cors());
app.use(express.json());

// WebSocket connection handling
io.on('connection', (socket) => {
  console.log('Client connected:', socket.id);

  // Subscribe to session updates
  socket.on('subscribe:session', (sessionId: string) => {
    socket.join(sessionId);
    console.log(`Client ${socket.id} subscribed to session: ${sessionId}`);
  });

  // Unsubscribe from session
  socket.on('unsubscribe:session', (sessionId: string) => {
    socket.leave(sessionId);
    console.log(`Client ${socket.id} unsubscribed from session: ${sessionId}`);
  });

  socket.on('disconnect', () => {
    console.log('Client disconnected:', socket.id);
  });
});

// Listen to file watcher events and emit via WebSocket
fileWatcher.on('suggestion:new', ({ sessionId, suggestion }) => {
  io.to(sessionId).emit('suggestion:new', suggestion);
  console.log(`Sent suggestion to session ${sessionId}: note ${suggestion.noteId}`);
});

fileWatcher.on('state:change', ({ sessionId, state }) => {
  console.log(`[WebSocket] Emitting state:change to session ${sessionId}:`, state);
  io.to(sessionId).emit('state:change', state);
  console.log(`State changed for session ${sessionId}:`, state.state);
});

fileWatcher.on('session:complete', ({ sessionId, totalSuggestions }) => {
  io.to(sessionId).emit('session:complete', { totalSuggestions });
  console.log(`Session ${sessionId} complete: ${totalSuggestions} suggestions`);
});

fileWatcher.on('error', ({ sessionId, error }) => {
  io.to(sessionId).emit('session:error', { error });
  console.error(`Session ${sessionId} error:`, error);
});

// Mount routes
app.use('/api/health', healthRouter);
app.use('/api/sessions', sessionsRouter);
app.use('/api', historyRouter);
app.use('/api/anki', ankiRouter);
app.use('/api/decks', decksRouter);
app.use('/api/notes', notesRouter);
app.use('/api/settings', settingsRouter);

// Graceful shutdown handling
process.on('SIGTERM', async () => {
  console.log('SIGTERM received, shutting down gracefully...');
  await fileWatcher.unwatchAll();
  claudeSpawner.killAll();
  httpServer.close(() => {
    console.log('Server closed');
    process.exit(0);
  });
});

process.on('SIGINT', async () => {
  console.log('SIGINT received, shutting down gracefully...');
  await fileWatcher.unwatchAll();
  claudeSpawner.killAll();
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
