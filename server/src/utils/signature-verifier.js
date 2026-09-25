'use strict';

const crypto = require('crypto');

/**
 * Verifies a GitHub webhook HMAC-SHA256 signature.
 *
 * @param {string} secret   - The webhook secret configured in GitHub
 * @param {string} signature - The X-Hub-Signature-256 header value
 * @param {Buffer|string} payload - The raw request body
 * @returns {boolean} true if the signature is valid
 */
function verifyWebhookSignature(secret, signature, payload) {
  if (!secret || !signature || !signature.startsWith('sha256=')) {
    return false;
  }

  const expectedSignature = 'sha256=' + crypto
    .createHmac('sha256', secret)
    .update(payload)
    .digest('hex');

  // Use timingSafeEqual to prevent timing attacks.
  // Buffers must be equal length — pad/hash both to a fixed-length digest to avoid
  // "Input buffers must have the same byte length" errors on obviously-wrong signatures.
  try {
    const sigBuf = Buffer.from(signature);
    const expBuf = Buffer.from(expectedSignature);
    if (sigBuf.length !== expBuf.length) {
      return false;
    }
    return crypto.timingSafeEqual(sigBuf, expBuf);
  } catch {
    return false;
  }
}

module.exports = { verifyWebhookSignature };
