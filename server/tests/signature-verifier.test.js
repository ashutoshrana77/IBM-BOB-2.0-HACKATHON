'use strict';

const { verifyWebhookSignature } = require('../src/utils/signature-verifier');
const crypto = require('crypto');

describe('verifyWebhookSignature', () => {
  const secret = 'test-webhook-secret';
  const payload = Buffer.from(JSON.stringify({ action: 'opened' }));

  function makeSignature(body) {
    return 'sha256=' + crypto.createHmac('sha256', secret).update(body).digest('hex');
  }

  test('returns true for a valid signature', () => {
    const sig = makeSignature(payload);
    expect(verifyWebhookSignature(secret, sig, payload)).toBe(true);
  });

  test('returns false for a tampered payload', () => {
    const sig = makeSignature(payload);
    const tampered = Buffer.from(JSON.stringify({ action: 'deleted' }));
    expect(verifyWebhookSignature(secret, sig, tampered)).toBe(false);
  });

  test('returns false when signature is missing', () => {
    expect(verifyWebhookSignature(secret, null, payload)).toBe(false);
  });

  test('returns false when signature has wrong prefix', () => {
    const sig = 'sha1=' + crypto.createHmac('sha1', secret).update(payload).digest('hex');
    expect(verifyWebhookSignature(secret, sig, payload)).toBe(false);
  });

  test('returns false for empty string signature', () => {
    expect(verifyWebhookSignature(secret, '', payload)).toBe(false);
  });

  test('returns false when secret is wrong', () => {
    const sig = makeSignature(payload);
    expect(verifyWebhookSignature('wrong-secret', sig, payload)).toBe(false);
  });
});
