'use strict';

// Set required environment variables for tests before any module is loaded
process.env.GITHUB_TOKEN = 'test-github-token';
process.env.GITHUB_REPO = 'test-org/test-repo';
process.env.GITHUB_WEBHOOK_SECRET = 'test-webhook-secret-32-chars-long!!';
process.env.PORT = '0'; // Use ephemeral port in tests
process.env.LOG_LEVEL = 'error'; // Suppress logs during tests
