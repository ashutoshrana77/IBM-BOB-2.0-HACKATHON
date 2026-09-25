'use strict';

const request = require('supertest');
const app = require('../src/index');

afterAll(() => {
  if (app.closeServer) app.closeServer();
});

describe('Health endpoint', () => {
  test('GET /health returns 200 with status ok', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('ok');
    expect(res.body.service).toBe('pr-pilot-server');
    expect(typeof res.body.uptime_seconds).toBe('number');
    expect(res.body.timestamp).toBeDefined();
  });
});

describe('Unknown routes', () => {
  test('GET /unknown returns 404', async () => {
    const res = await request(app).get('/unknown-route');
    expect(res.status).toBe(404);
    expect(res.body.error).toBe('Not found');
  });
});
