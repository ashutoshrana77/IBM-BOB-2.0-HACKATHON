'use strict';

const express = require('express');
const router = express.Router();

const logger = require('../logger');
const { verifyWebhookSignature } = require('../utils/signature-verifier');
const { triggerReview } = require('../services/review-service');

// In-memory set to track PRs currently being reviewed (prevents duplicate runs)
const activeReviews = new Set();

/**
 * POST /webhook
 * Receives GitHub pull_request webhook events.
 */
router.post('/', express.raw({ type: '*/*' }), async (req, res) => {
  // -------------------------------------------------------------------------
  // Step 1: Verify GitHub webhook signature
  // -------------------------------------------------------------------------
  // req.body is a Buffer when express.raw() is used; normalise it.
  const rawBody = Buffer.isBuffer(req.body)
    ? req.body
    : Buffer.from(typeof req.body === 'string' ? req.body : JSON.stringify(req.body));

  const signature = req.headers['x-hub-signature-256'];
  const webhookSecret = process.env.GITHUB_WEBHOOK_SECRET;

  if (!verifyWebhookSignature(webhookSecret, signature, rawBody)) {
    logger.warn('Webhook signature verification failed', {
      ip: req.ip,
      signature: signature ? signature.substring(0, 20) + '...' : 'missing'
    });
    return res.status(401).json({ error: 'Invalid webhook signature' });
  }

  // -------------------------------------------------------------------------
  // Step 2: Parse the payload
  // -------------------------------------------------------------------------
  let payload;
  try {
    payload = JSON.parse(rawBody.toString('utf8'));
  } catch (err) {
    logger.error('Failed to parse webhook payload', { error: err.message });
    return res.status(400).json({ error: 'Invalid JSON payload' });
  }

  const eventType = req.headers['x-github-event'];

  // -------------------------------------------------------------------------
  // Step 3: Filter relevant events
  // -------------------------------------------------------------------------
  if (eventType !== 'pull_request') {
    logger.debug(`Ignoring non-pull_request event: ${eventType}`);
    return res.status(200).json({ message: `Event '${eventType}' ignored` });
  }

  const action = payload.action;
  const relevantActions = ['opened', 'synchronize', 'reopened'];

  if (!relevantActions.includes(action)) {
    logger.debug(`Ignoring pull_request action: ${action}`);
    return res.status(200).json({ message: `Action '${action}' ignored` });
  }

  // -------------------------------------------------------------------------
  // Step 4: Extract PR details
  // -------------------------------------------------------------------------
  const pr = payload.pull_request;
  if (!pr) {
    return res.status(400).json({ error: 'Missing pull_request object in payload' });
  }

  const prNumber = pr.number;
  const baseSha = pr.base && pr.base.sha;
  const headSha = pr.head && pr.head.sha;

  if (!prNumber || !baseSha || !headSha) {
    logger.error('Missing required PR fields', { prNumber, baseSha, headSha });
    return res.status(400).json({ error: 'Missing required PR fields (number, base.sha, head.sha)' });
  }

  // -------------------------------------------------------------------------
  // Step 5: Prevent duplicate reviews for the same PR
  // -------------------------------------------------------------------------
  const reviewKey = `${prNumber}-${headSha}`;
  if (activeReviews.has(reviewKey)) {
    logger.info(`Review already in progress for PR #${prNumber} at ${headSha}`);
    return res.status(202).json({ message: 'Review already in progress' });
  }

  activeReviews.add(reviewKey);

  // -------------------------------------------------------------------------
  // Step 6: Acknowledge the webhook immediately (GitHub expects < 10s response)
  // -------------------------------------------------------------------------
  res.status(202).json({
    message: `PR Pilot review queued for PR #${prNumber}`,
    pr_number: prNumber,
    head_sha: headSha
  });

  // -------------------------------------------------------------------------
  // Step 7: Run review asynchronously (after response is sent)
  // -------------------------------------------------------------------------
  setImmediate(async () => {
    try {
      logger.info(`Starting PR Pilot review for PR #${prNumber}`, { action, baseSha, headSha });

      await triggerReview({
        prNumber,
        baseSha,
        headSha
      });

      logger.info(`PR Pilot review completed for PR #${prNumber}`);
    } catch (err) {
      logger.error(`PR Pilot review failed for PR #${prNumber}`, {
        error: err.message
      });
    } finally {
      activeReviews.delete(reviewKey);
    }
  });
});

module.exports = router;
