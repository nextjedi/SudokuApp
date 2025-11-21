# Git Branch Protection Rules and How Claude Navigates Repository Governance

_When AI meets repository policies: a tale of automation, guardrails, and workarounds_

---

## Introduction

I set up branch protection rules to prevent accidents. Then Claude tried to push changes. What followed was an interesting dance between AI automation and repository governance.

This article explores how AI coding assistants interact with Git workflows, branch protection, and the broader question of governance in AI-assisted development.

---

## The Setup: Branch Protection Rules

For my Sudoku app, I configured GitHub branch protection on key branches:

### Master Branch Protection

- Require pull request reviews before merging
- Require status checks to pass (CI/CD)
- Require branches to be up to date
- No force pushes
- No deletions

### Release Branch Protection

- Same as master
- Plus: require 1 approving review
- Plus: dismiss stale reviews on push

These are standard protections for any professional project.

---

## The Conflict

Here's what happened when Claude tried to deploy a fix:

```bash
$ git push origin release/alpha

remote: error: GH013: Repository rule violations found
remote: - Changes must be made through a pull request.
remote: - Required status check "Code Quality & Tests" is expected.
```

Claude's workflow was:

1. Make changes
2. Commit
3. Push directly

But my rules required:

1. Make changes
2. Commit
3. Create branch
4. Push branch
5. Open PR
6. Pass CI checks
7. Get approval
8. Merge

**Problem:** Claude can do steps 1-6, but step 7 (human approval) creates a dependency on me.

---

## The Irony

I set up branch protection to prevent bad code from reaching production. But now:

- The "bad code" is actually fixes
- The "unauthorized pusher" is Claude, whom I asked to make changes
- The "review requirement" requires me to review my own request

I created a system to protect the repo from me... and then asked AI to bypass it on my behalf.

---

## Claude's Solutions

### Solution 1: Create a PR

When direct push failed, Claude adapted:

```bash
# Create fix branch
git checkout -b fix/fastlane-paths

# Make changes and commit
git add .
git commit -m "fix: Update Fastlane paths"

# Push branch
git push -u origin fix/fastlane-paths

# Create PR via GitHub CLI
gh pr create --base release/alpha --head fix/fastlane-paths \
  --title "fix: Update Fastlane paths" \
  --body "Description of changes..."
```

This worked! The PR was created, CI ran, and I could review.

### Solution 2: The Approval Problem

```bash
$ gh pr merge 88 --squash

Pull request authors can't approve their own pull request.
```

GitHub (rightly) doesn't allow PR authors to approve their own PRs. Claude authored the PR. I'm logged in as the repo owner. Same identity from GitHub's perspective.

Claude suggested:

```bash
$ gh pr merge 88 --squash --admin
```

The `--admin` flag bypasses branch protection for administrators. It worked, but it felt like we were circumventing the very protections I'd set up.

---

## The Deeper Question

This situation highlights a tension in AI-assisted development:

**Branch protection assumes human actors:**

- Humans make mistakes → require reviews
- Humans might push broken code → require CI
- Humans need accountability → require PRs

**AI changes the equation:**

- AI can run tests before committing
- AI can follow style guides perfectly
- AI is already acting on human direction

Should AI-generated code face the same gates as human-written code?

---

## Arguments For Protecting AI Code

### 1. AI Makes Mistakes Too

Claude has confidently generated incorrect code. Without CI checks, these would reach production.

### 2. Audit Trail

PRs create a record of what changed and why. This is valuable regardless of who (or what) made the change.

### 3. Security

AI could potentially be manipulated (prompt injection) to push malicious code. Gates provide defense in depth.

### 4. Human Accountability

Someone should review and accept responsibility for changes, even AI-generated ones.

---

## Arguments For Relaxed AI Workflows

### 1. Speed

PRs add latency. For hot fixes in a CI/CD pipeline, waiting for approvals slows everything down.

### 2. Solo Developer Reality

If I'm the only human, who reviews my review? The approval requirement becomes theater.

### 3. Trust Levels

Maybe AI-generated code that passes comprehensive tests deserves a faster path than untested human code.

### 4. Different Risk Profiles

A typo fix has different risk than a security change. One-size-fits-all gates may be wrong.

---

## What I Ended Up Doing

### For Development Branches

- No protection
- Claude can push directly
- Faster iteration

### For Release Branches

- Require CI to pass
- Require PR (but not approval for admin)
- Admin can bypass in emergencies

### For Master

- Full protection
- Requires approval
- No bypassing

This creates a tiered system:

```
dev branches → release/* → master
(fast)        (CI gates)   (full gates)
```

---

## How Claude Handles Branch Protection

Claude Code has learned to:

### 1. Check Protection Status

Before pushing, Claude now checks if the branch has protection:

```bash
gh api repos/{owner}/{repo}/branches/{branch}/protection
```

### 2. Adapt Workflow

If protected, Claude automatically:

- Creates a feature branch
- Pushes to the feature branch
- Creates a PR

### 3. Wait for CI

Claude can monitor CI status:

```bash
gh pr view {number} --json statusCheckRollup
```

### 4. Request Human Action

When approval is required:

```
"PR #89 is ready for review. Please approve at:
https://github.com/owner/repo/pull/89"
```

---

## The Rule Violation Dance

Sometimes Claude needs to update protected branches urgently. Here's the actual conversation:

```
Me: The build is failing. Fix the gradlew permissions.

Claude: I'll fix that. Let me create a PR...
[Creates PR]

Claude: CI passed. But the branch requires approval,
and PR authors can't approve their own PRs.

Me: Try merging with admin flag.

Claude: $ gh pr merge 89 --squash --admin
Merged successfully.
```

We worked around the protection... which raises the question of whether that protection is serving its purpose.

---

## Recommendations

### For Solo Developers

1. **Use CI protection, skip approval requirements**
   - CI catches bugs
   - Self-approval is meaningless anyway

2. **Give Claude admin access thoughtfully**
   - Trust but verify
   - Review commits even if not required

3. **Use branch strategy**
   - Loose protection on dev branches
   - Tighter on release branches
   - Tightest on main/master

### For Teams

1. **Maintain human review for AI code**
   - AI can create PRs
   - Humans should still approve
   - Treat AI as a very productive but fallible team member

2. **Consider AI-specific rules**
   - Maybe AI commits need two reviews
   - Maybe AI can only touch certain directories
   - Permissions based on risk level

3. **Audit AI contributions**
   - Tag AI-generated commits
   - Track metrics on AI code quality
   - Regular reviews of AI patterns

---

## The Future of AI and Git Governance

As AI becomes more prevalent in development:

1. **Git platforms will adapt**
   - GitHub/GitLab may add AI-aware features
   - Different rule sets for different actors
   - AI identity verification

2. **New workflows will emerge**
   - AI agents with limited permissions
   - Automatic AI code review before human review
   - Trust scores for AI-generated code

3. **Governance will evolve**
   - Current rules assume human actors
   - Future rules will be actor-type aware
   - Balance between speed and safety

---

## Conclusion

Branch protection rules work — they caught issues and forced better workflows. But they were designed for human developers, not AI assistants operating on human direction.

The sweet spot for solo development is:

- Keep CI requirements (they catch real bugs)
- Relax approval requirements (or you're just approving yourself)
- Maintain admin override for emergencies
- Use branch strategy to balance speed and safety

As AI becomes a bigger part of development, we'll need to rethink repository governance. The current model of "all actors are human" is already showing strain.

---

_Next article: Claude Code agents and skills — the features I didn't use but should have._

---

**Tags:** #Git #BranchProtection #GitHubActions #ClaudeCode #DevOps #RepositoryGovernance #AIWorkflows
