#!/usr/bin/env bash
# =============================================================================
# PR Pilot — review.sh
# Entry-point script called by GitHub Actions (or any CI) to trigger a review.
#
# Usage:
#   ./scripts/review.sh <pr_number> <base_sha> <head_sha> [design_doc_path]
#
# Required environment variables:
#   GITHUB_TOKEN        — GitHub personal access token (for posting comments)
#   GITHUB_REPO         — owner/repo format (e.g., acme/library-app)
#   BOB_SHELL_PATH      — path to the bob executable (default: bob)
#
# Optional environment variables:
#   PR_PILOT_DEBUG      — set to "1" to print the full prompt before sending
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Argument validation
# ---------------------------------------------------------------------------
if [ "$#" -lt 3 ]; then
  echo "ERROR: Usage: $0 <pr_number> <base_sha> <head_sha> [design_doc_path]" >&2
  exit 1
fi

PR_NUMBER="$1"
BASE_SHA="$2"
HEAD_SHA="$3"
DESIGN_DOC_PATH="${4:-}"

# ---------------------------------------------------------------------------
# Environment validation
# ---------------------------------------------------------------------------
: "${GITHUB_TOKEN:?ERROR: GITHUB_TOKEN environment variable is required}"
: "${GITHUB_REPO:?ERROR: GITHUB_REPO environment variable is required}"

BOB_CMD="${BOB_SHELL_PATH:-bob}"

if ! command -v "$BOB_CMD" &>/dev/null; then
  echo "ERROR: Bob Shell not found at '${BOB_CMD}'. Set BOB_SHELL_PATH or ensure bob is in PATH." >&2
  exit 1
fi

# ---------------------------------------------------------------------------
# Workspace setup
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
WORK_DIR="${PROJECT_ROOT}/.pr-pilot-work"
mkdir -p "${WORK_DIR}"

DIFF_FILE="${WORK_DIR}/pr-${PR_NUMBER}.diff"
PROMPT_FILE="${WORK_DIR}/prompt-${PR_NUMBER}.md"
OUTPUT_FILE="${WORK_DIR}/review-${PR_NUMBER}.md"

# ---------------------------------------------------------------------------
# Step 1: Generate the diff
# ---------------------------------------------------------------------------
echo "[PR Pilot] Generating diff for PR #${PR_NUMBER} (${BASE_SHA}..${HEAD_SHA})"

git diff "${BASE_SHA}".."${HEAD_SHA}" \
  --unified=5 \
  --no-color \
  --ignore-blank-lines \
  -- ':!*.lock' ':!*.sum' ':!dist/*' ':!build/*' ':!target/*' \
  > "${DIFF_FILE}"

DIFF_SIZE=$(wc -l < "${DIFF_FILE}")
echo "[PR Pilot] Diff size: ${DIFF_SIZE} lines"

if [ "${DIFF_SIZE}" -eq 0 ]; then
  echo "[PR Pilot] Empty diff — nothing to review."
  exit 0
fi

# Warn on very large diffs
if [ "${DIFF_SIZE}" -gt 5000 ]; then
  echo "[PR Pilot] WARNING: Diff is ${DIFF_SIZE} lines. Consider splitting this PR for better review quality."
fi

# ---------------------------------------------------------------------------
# Step 2: Fetch PR metadata from GitHub API
# ---------------------------------------------------------------------------
echo "[PR Pilot] Fetching PR metadata from GitHub..."

PR_API_URL="https://api.github.com/repos/${GITHUB_REPO}/pulls/${PR_NUMBER}"
PR_META=$(curl --silent --fail \
  -H "Authorization: Bearer ${GITHUB_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "${PR_API_URL}")

PR_TITLE=$(echo "${PR_META}" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('title',''))" 2>/dev/null || echo "")
PR_DESCRIPTION=$(echo "${PR_META}" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('body','') or '')" 2>/dev/null || echo "")

echo "[PR Pilot] PR Title: ${PR_TITLE}"

# ---------------------------------------------------------------------------
# Step 3: Build the prompt by injecting values into the template
# ---------------------------------------------------------------------------
echo "[PR Pilot] Building prompt..."

DIFF_CONTENT=$(cat "${DIFF_FILE}")

# Read the prompt template
PROMPT_TEMPLATE=$(cat "${PROJECT_ROOT}/prompts/main-agent.md")

# Perform substitutions using Python for safe multiline handling
PR_TITLE="${PR_TITLE}" PR_DESCRIPTION="${PR_DESCRIPTION}" \
DESIGN_DOC_PATH="${DESIGN_DOC_PATH}" PR_NUMBER="${PR_NUMBER}" \
PROJECT_ROOT="${PROJECT_ROOT}" DIFF_FILE="${DIFF_FILE}" \
python3 - <<'PYEOF' > "${PROMPT_FILE}"
import os
from pathlib import Path

template = (Path(os.environ["PROJECT_ROOT"]) / "prompts/main-agent.md").read_text()
diff = Path(os.environ["DIFF_FILE"]).read_text()
result = template.replace("{{DIFF_CONTENT}}", diff)
result = result.replace("{{DESIGN_DOC_PATH}}", os.environ.get("DESIGN_DOC_PATH", ""))
result = result.replace("{{PR_NUMBER}}", os.environ["PR_NUMBER"])
result = result.replace("{{PR_TITLE}}", os.environ.get("PR_TITLE", ""))
result = result.replace("{{PR_DESCRIPTION}}", os.environ.get("PR_DESCRIPTION", ""))
result = result.replace("{{ACCEPTANCE_CRITERIA}}", "")
result = result.replace("{{CHANGED_FILES}}", diff[:2000])
print(result)
PYEOF

if [ "${PR_PILOT_DEBUG:-0}" = "1" ]; then
  echo "[PR Pilot] DEBUG: Prompt written to ${PROMPT_FILE}"
  echo "[PR Pilot] DEBUG: Prompt size: $(wc -l < "${PROMPT_FILE}") lines"
fi

PROMPT_BYTES=$(wc -c < "${PROMPT_FILE}")
if [ "${PROMPT_BYTES}" -eq 0 ]; then
  echo "[PR Pilot] ERROR: Generated review prompt is empty." >&2
  exit 1
fi
if [ "${PROMPT_BYTES}" -gt 100000 ]; then
  echo "[PR Pilot] ERROR: Generated review prompt is ${PROMPT_BYTES} bytes; split the PR so the prompt stays below 100000 bytes." >&2
  exit 1
fi

# ---------------------------------------------------------------------------
# Step 4: Run Bob Shell in non-interactive (pr-reviewer) mode
# ---------------------------------------------------------------------------
echo "[PR Pilot] Running Bob Shell review (pr-reviewer mode)..."

# Set the working directory to the repo root so Bob can use context mentions
cd "${PROJECT_ROOT}"

"${BOB_CMD}" --accept-license >/dev/null

if ! env -u GITHUB_TOKEN -u GH_TOKEN "${BOB_CMD}" run --mode pr-reviewer --trust \
  "$(cat "${PROMPT_FILE}")" > "${OUTPUT_FILE}" 2>&1; then
  echo "[PR Pilot] ERROR: Bob Shell review failed. Output follows:" >&2
  echo "[PR Pilot] Output:" >&2
  cat "${OUTPUT_FILE}" >&2
  exit 1
fi

echo "[PR Pilot] Review generated: ${OUTPUT_FILE}"
echo "[PR Pilot] Review size: $(wc -l < "${OUTPUT_FILE}") lines"

# ---------------------------------------------------------------------------
# Step 5: Post the review as a GitHub PR comment
# ---------------------------------------------------------------------------
echo "[PR Pilot] Posting review to GitHub PR #${PR_NUMBER}..."

"${SCRIPT_DIR}/post-comment.sh" "${PR_NUMBER}" "${OUTPUT_FILE}"

echo "[PR Pilot] Done. Review posted to PR #${PR_NUMBER}."

# ---------------------------------------------------------------------------
# Step 6: Determine exit code based on critical findings
# ---------------------------------------------------------------------------
if grep -q "🔴 Critical findings" "${OUTPUT_FILE}" && ! grep -q "None" "${OUTPUT_FILE}"; then
  echo "[PR Pilot] CRITICAL findings detected. Marking CI step as failed to block merge."
  exit 1
fi

exit 0
