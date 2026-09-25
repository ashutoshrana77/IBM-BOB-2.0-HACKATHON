// =============================================================================
// PR Pilot — Webhook Server Entry Point
// Receives GitHub webhook events and dispatches PR reviews via Bob Shell.
// =============================================================================

'use strict';

require('dotenv').config();

const express = require('express');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');

const logger = require('./logger');
const webhookRouter = require('./routes/webhook');
const healthRouter = require('./routes/health');
const { validateEnvironment } = require('./utils/env-validator');

// ---------------------------------------------------------------------------
// Validate required environment variables before starting
// ---------------------------------------------------------------------------
// Vercel loads this module as a serverless handler. It must stay importable even
// before secrets are configured so /health can report the missing settings.
const isVercel = Boolean(process.env.VERCEL);
if (!isVercel) validateEnvironment();

// ---------------------------------------------------------------------------
// Express app setup
// ---------------------------------------------------------------------------
const app = express();

// Security headers
app.use(helmet());

// Request logging
app.use(morgan('combined', {
  stream: { write: (msg) => logger.http(msg.trim()) }
}));

// Body parsing — limit size to prevent large payload attacks
app.use(express.json({ limit: '10mb' }));
app.use(express.raw({ type: 'application/json', limit: '10mb' }));

// Rate limiting — 60 requests per minute per IP
const limiter = rateLimit({
  windowMs: 60 * 1000,
  max: 60,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests, please try again later.' }
});
app.use(limiter);

// ---------------------------------------------------------------------------
// Routes
// ---------------------------------------------------------------------------
app.use('/health', healthRouter);
app.use('/webhook', webhookRouter);

app.get('/', (_req, res) => {
  res.status(200).json({
    service: 'pr-pilot-server',
    status: 'ok',
    health: '/health',
    readiness: '/health/ready',
    webhook: '/webhook'
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({ error: 'Not found' });
});

// Global error handler
app.use((err, req, res, _next) => {
  logger.error('Unhandled error:', { message: err.message, stack: err.stack });
  res.status(500).json({ error: 'Internal server error' });
});

// ---------------------------------------------------------------------------
// Start server
// ---------------------------------------------------------------------------
if (!isVercel) {
  const PORT = parseInt(process.env.PORT || '3000', 10);
  const HOST = process.env.HOST || '0.0.0.0';
  const server = app.listen(PORT, HOST, () => {
    logger.info(`PR Pilot webhook server listening on ${HOST}:${PORT}`);
  });

  // Expose server close for test teardown.
  app.closeServer = () => server.close();

  // Graceful shutdown for long-running hosts.
  process.on('SIGTERM', () => server.close(() => process.exit(0)));
  process.on('SIGINT', () => server.close(() => process.exit(0)));
} else {
  // Vercel invokes the exported Express app directly; app.listen() must not run.
  app.closeServer = () => Promise.resolve();
}

module.exports = app; // exported for testing
