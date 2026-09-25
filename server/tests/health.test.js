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
    expect(res.body.configured).toBe(true);
    expect(res.body.missing_configuration).toEqual([]);
    expect(typeof res.body.uptime_seconds).toBe('number');
    expect(res.body.timestamp).toBeDefined();
  });

  test('GET /health/ready returns 200 when required settings are present', async () => {
    const res = await request(app).get('/health/ready');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('ready');
  });

  test('GET /health/ready reports missing settings without crashing', async () => {
    const originalToken = process.env.GITHUB_TOKEN;
    delete process.env.GITHUB_TOKEN;
    try {
      const res = await request(app).get('/health/ready');
      expect(res.status).toBe(503);
      expect(res.body.missing_configuration).toContain('GITHUB_TOKEN');
    } finally {
      process.env.GITHUB_TOKEN = originalToken;
    }
  });
});

describe('Unknown routes', () => {
  test('GET / returns service links', async () => {
    const res = await request(app).get('/');
    expect(res.status).toBe(200);
    expect(res.body.service).toBe('pr-pilot-server');
    expect(res.body.health).toBe('/health');
  });

  test('GET /unknown returns 404', async () => {
    const res = await request(app).get('/unknown-route');
    expect(res.status).toBe(404);
    expect(res.body.error).toBe('Not found');
  });
});
