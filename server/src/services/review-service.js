'use strict';

const { execFile } = require('child_process');
const path = require('path');
const fs = require('fs');
const logger = require('../logger');

const SCRIPTS_DIR = path.resolve(__dirname, '..', '..', '..', 'scripts');
const REVIEW_SCRIPT = path.join(SCRIPTS_DIR, 'review.sh');

/**
 * Triggers a PR Pilot review by spawning the review.sh script.
 *
 * @param {object} params
 * @param {number} params.prNumber  - Pull request number
 * @param {string} params.baseSha   - Base branch commit SHA
 * @param {string} params.headSha   - Head branch commit SHA
 * @param {string} [params.designDocPath] - Optional path to a design document
 * @returns {Promise<{stdout: string, stderr: string, exitCode: number}>}
 */
async function triggerReview({ prNumber, baseSha, headSha, designDocPath = '' }) {
  return new Promise((resolve, reject) => {
    if (!fs.existsSync(REVIEW_SCRIPT)) {
      return reject(new Error(`review.sh not found at: ${REVIEW_SCRIPT}`));
    }

    const args = [
      String(prNumber),
      baseSha,
      headSha
    ];

    if (designDocPath) {
      args.push(designDocPath);
    }

    const env = {
      ...process.env,
      GITHUB_TOKEN: process.env.GITHUB_TOKEN,
      GITHUB_REPO: process.env.GITHUB_REPO,
      BOB_SHELL_PATH: process.env.BOB_SHELL_PATH || 'bob'
    };

    logger.info(`Spawning review.sh for PR #${prNumber}`, { baseSha, headSha });

    execFile('/bin/bash', [REVIEW_SCRIPT, ...args], { env, maxBuffer: 10 * 1024 * 1024 }, (error, stdout, stderr) => {
      const exitCode = error ? (error.code || 1) : 0;

      if (stderr) {
        logger.warn(`review.sh stderr for PR #${prNumber}:`, { stderr });
      }

      if (exitCode !== 0 && exitCode !== 1) {
        // exitCode 1 is expected when critical findings block the merge — not a failure
        logger.error(`review.sh failed for PR #${prNumber} with code ${exitCode}`, { stderr });
        return reject(new Error(`review.sh exited with code ${exitCode}: ${stderr}`));
      }

      logger.info(`review.sh completed for PR #${prNumber}`, { exitCode });
      resolve({ stdout, stderr, exitCode });
    });
  });
}

module.exports = { triggerReview };
