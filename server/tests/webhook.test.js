'use strict';

const request = require('supertest');
const crypto = require('crypto');

// Must be at module level — jest.mock is hoisted to the top of the file
jest.mock('../src/services/review-service', () => ({
  triggerReview: jest.fn().mockResolvedValue({ exitCode: 0, stdout: '', stderr: '' })
}));

const app = require('../src/index');

afterAll(() => {
  if (app.closeServer) app.closeServer();
});

const WEBHOOK_SECRET = process.env.GITHUB_WEBHOOK_SECRET;

function makeSignature(body, secret) {
  const s = secret || WEBHOOK_SECRET;
  // body can be a string or Buffer — normalise to Buffer for consistent hashing
  const buf = Buffer.isBuffer(body) ? body : Buffer.from(body);
  return 'sha256=' + crypto.createHmac('sha256', s).update(buf).digest('hex');
}

function makePayload(action, prNumber) {
  return JSON.stringify({
    action,
    pull_request: {
      number: prNumber,
      title: 'Test PR',
      body: 'Test description',
      base: { sha: 'abc1234567890' },
      head: { sha: 'def9876543210' }
    }
  });
}

describe('POST /webhook', () => {
  test('returns 401 when signature is missing', async () => {
    const payload = makePayload('opened', 1);
    const res = await request(app)
      .post('/webhook')
      .set('Content-Type', 'application/json')
      .set('x-github-event', 'pull_request')
      .send(payload);
    expect(res.status).toBe(401);
    expect(res.body.error).toBe('Invalid webhook signature');
  });

  test('returns 401 when signature is wrong', async () => {
    const payload = makePayload('opened', 2);
    const res = await request(app)
      .post('/webhook')
      .set('Content-Type', 'application/json')
      .set('x-github-event', 'pull_request')
      .set('x-hub-signature-256', 'sha256=invalidsignature000000000000000000000000000000000000000000000000')
      .send(payload);
    expect(res.status).toBe(401);
    expect(res.body.error).toBe('Invalid webhook signature');
  });

  test('returns 200 and ignores non pull_request events', async () => {
    const payload = JSON.stringify({ zen: 'test', hook_id: 123 });
    const sig = makeSignature(payload);
    const res = await request(app)
      .post('/webhook')
      .set('Content-Type', 'application/json')
      .set('x-github-event', 'ping')
      .set('x-hub-signature-256', sig)
      .send(payload);
    expect(res.status).toBe(200);
    expect(res.body.message).toContain("'ping' ignored");
  });

  test('returns 200 and ignores non-relevant PR actions', async () => {
    const payload = makePayload('closed', 3);
    const sig = makeSignature(payload);
    const res = await request(app)
      .post('/webhook')
      .set('Content-Type', 'application/json')
      .set('x-github-event', 'pull_request')
      .set('x-hub-signature-256', sig)
      .send(payload);
    expect(res.status).toBe(200);
    expect(res.body.message).toContain("'closed' ignored");
  });

  test('returns 202 and queues review for opened PR', async () => {
    const payload = makePayload('opened', 42);
    const sig = makeSignature(payload);
    const res = await request(app)
      .post('/webhook')
      .set('Content-Type', 'application/json')
      .set('x-github-event', 'pull_request')
      .set('x-hub-signature-256', sig)
      .send(payload);

    expect(res.status).toBe(202);
    expect(res.body.pr_number).toBe(42);
    expect(res.body.message).toContain('queued');
  });
});
