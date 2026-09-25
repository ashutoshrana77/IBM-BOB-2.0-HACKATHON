'use strict';

const express = require('express');
const router = express.Router();
const { getMissingEnvironment } = require('../utils/env-validator');

const startTime = Date.now();

/**
 * GET /health
 * Returns server health status and uptime.
 */
router.get('/', (req, res) => {
  res.status(200).json({
    status: 'ok',
    service: 'pr-pilot-server',
    runtime: process.env.VERCEL ? 'vercel-serverless' : 'node-server',
    configured: getMissingEnvironment().length === 0,
    missing_configuration: getMissingEnvironment().map(({ key }) => key),
    uptime_seconds: Math.floor((Date.now() - startTime) / 1000),
    timestamp: new Date().toISOString()
  });
});

router.get('/ready', (_req, res) => {
  const missing = getMissingEnvironment().map(({ key }) => key);
  const ready = missing.length === 0;
  res.status(ready ? 200 : 503).json({
    status: ready ? 'ready' : 'needs_configuration',
    service: 'pr-pilot-server',
    missing_configuration: missing
  });
});

module.exports = router;
