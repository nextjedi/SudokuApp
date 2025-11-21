# Branch Protection Guide

This document describes the branch protection rules configured for this repository.

## Overview

This repository uses a combination of **traditional branch protection** (for `master`) and **GitHub Rulesets** (for `release/*` branches) to enforce code quality while allowing solo developers to merge their own PRs.

## Current Configuration

### Protected Branches

| Branch      | Protection Type               | Bypass Enabled |
| ----------- | ----------------------------- | -------------- |
| `master`    | Traditional Branch Protection | Yes (admins)   |
| `release/*` | Repository Ruleset            | Yes (admins)   |

### What's Protected

- **Direct pushes blocked** - All changes must go through PRs
- **Force pushes blocked** - History cannot be rewritten
- **Branch deletion blocked** - Protected branches cannot be deleted
- **CI required** - "Code Quality & Tests" workflow must pass

### What's Allowed (via Bypass)

- Repository admins can bypass approval requirements
- Repository admins can bypass CI checks in emergencies

## How to Merge PRs (Solo Developer)

Since GitHub doesn't allow PR authors to approve their own PRs, use the bypass feature:

1. Create a PR from your feature branch to `master` or `release/*`
2. Wait for CI to pass (recommended)
3. Click the **"Merge pull request"** dropdown arrow
4. Select **"Bypass required checks and merge"**
5. Confirm the bypass in the dialog
6. PR merges

## Configuration Details

### Master Branch Protection

```json
{
  "required_status_checks": {
    "strict": true,
    "contexts": ["Code Quality & Tests"]
  },
  "required_pull_request_reviews": {
    "required_approving_review_count": 1,
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": true
  },
  "enforce_admins": false,
  "allow_force_pushes": false,
  "allow_deletions": false
}
```

**Key setting:** `enforce_admins: false` allows admins to bypass.

### Release Branch Ruleset

```json
{
  "name": "Release Branch Protection",
  "conditions": {
    "ref_name": {
      "include": ["refs/heads/release/*"]
    }
  },
  "rules": [
    { "type": "pull_request" },
    { "type": "required_status_checks" },
    { "type": "deletion" },
    { "type": "non_fast_forward" }
  ],
  "bypass_actors": [
    {
      "actor_id": 5,
      "actor_type": "RepositoryRole",
      "bypass_mode": "always"
    }
  ]
}
```

**Key setting:** `actor_id: 5` = repository admins can bypass.

### CODEOWNERS

Located at `.github/CODEOWNERS`:

```
* @nextjedi
/.github/workflows/ @nextjedi
/android/ @nextjedi
/ios/ @nextjedi
/src/ @nextjedi
```

## Modifying Protection Rules

### Via GitHub CLI

**Check current protection:**

```bash
gh api repos/nextjedi/SudokuApp/branches/master/protection
```

**Check rulesets:**

```bash
gh api repos/nextjedi/SudokuApp/rulesets
```

**Update master protection:**

```bash
gh api -X PUT repos/nextjedi/SudokuApp/branches/master/protection \
  --input protection-config.json
```

**Update ruleset:**

```bash
gh api -X PUT repos/nextjedi/SudokuApp/rulesets/RULESET_ID \
  --input ruleset-config.json
```

### Via GitHub Web UI

1. Go to repository **Settings**
2. Click **Branches** (for traditional protection)
3. Or click **Rules > Rulesets** (for rulesets)

## Common Tasks

### Temporarily Disable Protection (Emergency)

```bash
# Remove master protection
gh api -X DELETE repos/nextjedi/SudokuApp/branches/master/protection

# Do your emergency fix
git push origin master

# Re-enable protection
gh api -X PUT repos/nextjedi/SudokuApp/branches/master/protection \
  --input protection-config.json
```

### Add a New Required Status Check

Update the `contexts` array in master protection:

```json
{
  "required_status_checks": {
    "contexts": ["Code Quality & Tests", "New Check Name"]
  }
}
```

### Remove Bypass (For Team Projects)

Set `enforce_admins: true` for master branch:

```bash
gh api -X PUT repos/nextjedi/SudokuApp/branches/master/protection \
  -F enforce_admins=true
```

Remove bypass actors from ruleset:

```json
{
  "bypass_actors": []
}
```

## Troubleshooting

### Can't see "Bypass" button

1. Check you're a repository admin
2. Verify `enforce_admins: false` for traditional protection
3. Verify `bypass_actors` includes your role in rulesets

### CI check name doesn't match

Find the exact check name:

```bash
gh run list --limit 5 --json name
```

Update protection to use the exact name.

### "Branch not protected" error

The branch must exist before you can protect it:

```bash
git checkout -b release/alpha
git push -u origin release/alpha
```

## Related Files

- `.github/CODEOWNERS` - Code ownership definitions
- `.github/workflows/ci.yml` - CI workflow that must pass
- `docs/BRANCH_PROTECTION_GUIDE.md` - This file

## References

- [GitHub Branch Protection Docs](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches)
- [GitHub Rulesets Docs](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets)
