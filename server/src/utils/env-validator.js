'use strict';

/**
 * Validates required environment variables on startup.
 * Throws an error with a clear message if any are missing.
 */
const required = [
    { key: 'GITHUB_TOKEN', description: 'GitHub personal access token with repo scope' },
    { key: 'GITHUB_REPO', description: 'GitHub repository in owner/repo format (e.g., acme/library-app)' },
    { key: 'GITHUB_WEBHOOK_SECRET', description: 'GitHub webhook secret for signature verification' }
];

function getMissingEnvironment() {
  return required.filter(({ key }) => !process.env[key]);
}

function validateEnvironment() {

  const optional = [
    { key: 'BOB_SHELL_PATH', description: 'Path to bob executable', default: 'bob' },
    { key: 'BOB_API_KEY', description: 'IBM Bob API key (if required by your Bob installation)' },
    { key: 'PORT', description: 'Server port', default: '3000' },
    { key: 'LOG_LEVEL', description: 'Logging level (info/debug/warn/error)', default: 'info' },
    { key: 'REPO_CLONE_PATH', description: 'Local path where the repository is cloned for diff generation', default: '/tmp/pr-pilot-repo' }
  ];

  const missing = getMissingEnvironment();

  if (missing.length > 0) {
    const details = missing.map(({ key, description }) => `  - ${key}: ${description}`).join('\n');
    throw new Error(`Missing required environment variables:\n${details}\n\nCopy .env.example to .env and fill in the values.`);
  }

  // Set defaults for optional variables
  for (const { key, default: defaultValue } of optional) {
    if (!process.env[key] && defaultValue !== undefined) {
      process.env[key] = defaultValue;
    }
  }
}

module.exports = { getMissingEnvironment, validateEnvironment };
