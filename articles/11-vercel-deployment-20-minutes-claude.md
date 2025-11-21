# From Zero to Production: Deploying a React Native Web App to Vercel in 20 Minutes with Claude

**Date:** November 22, 2025
**Time to Complete:** ~20 minutes
**Session Duration:** 18:43 UTC - 19:05 UTC

## The Challenge

Deploy a React Native Expo Sudoku app to the web with:

- Vercel hosting
- Custom domain (`sudoku.arunabh.me`)
- Multi-environment setup (production, release, dev branches)
- Proper build configuration

## What Actually Happened

This article documents a real conversation where Claude Code handled the entire deployment pipeline. No copy-pasting from Stack Overflow, no hunting through documentation—just conversational instructions.

### Timeline

| Time  | Task                                                              |
| ----- | ----------------------------------------------------------------- |
| 0:00  | Started with "setup vercel project to deploy on web"              |
| 2:00  | Created `vercel.json` configuration                               |
| 3:00  | Added web dependencies (`react-dom`, `react-native-web`)          |
| 5:00  | First build test - identified missing dependencies                |
| 8:00  | Fixed peer dependency conflicts with `--legacy-peer-deps`         |
| 10:00 | Successful local web build                                        |
| 12:00 | Linked Vercel project via CLI                                     |
| 14:00 | First deployment failed (1.2GB upload - missing `.vercelignore`)  |
| 16:00 | Created `.vercelignore`, second deployment failed (npm peer deps) |
| 18:00 | Added `installCommand` to vercel.json, deployment successful      |
| 20:00 | Added custom domain, provided DNS instructions                    |

## Conversation Excerpts

### The Initial Request

```
User: yes and also i want to setup vercel project to deploy on the web
      i own the domain arunabh.me on hostinger and this will be hosted
      at games.arunabh.me/sudoku
```

### Claude's Approach

Claude immediately:

1. Read existing `package.json` and `app.json` to understand the project
2. Created necessary configuration files
3. Identified missing web dependencies
4. Handled errors iteratively

### Handling the First Failure

When the initial deployment tried to upload 1.2GB:

```
Error: File size limit exceeded (100 MB)
Uploading [===============-----] (934.3MB/1.2GB)
```

Claude immediately diagnosed the issue and created `.vercelignore`:

```
# Dependencies
node_modules/

# Build outputs (Vercel will build fresh)
dist/
web-build/
android/
ios/
.expo/
```

### Handling Peer Dependency Conflicts

When npm install failed on Vercel:

```
npm error ERESOLVE could not resolve
npm error peer react-test-renderer@">=16.0.0" from @testing-library/jest-native@5.4.3
```

Claude added to `vercel.json`:

```json
{
  "installCommand": "npm install --legacy-peer-deps"
}
```

### The Domain Decision

An interesting moment of collaboration:

```
User: this way games.arunabh.me will be empty do you think i shall
      make it sudoku.arunabh.me or shall i create this as microfrontend

Claude: Good point! You have two approaches:
        - Dedicated subdomain (sudoku.arunabh.me) - Simple, direct URL
        - Microfrontend portal (games.arunabh.me) - Single domain for all games

        My recommendation: If Sudoku is standalone → Dedicated subdomain
```

## Files Created/Modified

| File            | Purpose                                                   |
| --------------- | --------------------------------------------------------- |
| `vercel.json`   | Build config, install command, branch deployments         |
| `.vercelignore` | Exclude node_modules, build artifacts, android/ios        |
| `app.json`      | Web bundler config (later removed basePath)               |
| `package.json`  | Added `react-dom`, `react-native-web`, `build:web` script |

### Final `vercel.json`

```json
{
  "$schema": "https://openapi.vercel.sh/vercel.json",
  "installCommand": "npm install --legacy-peer-deps",
  "buildCommand": "npx expo export --platform web",
  "outputDirectory": "dist",
  "framework": null,
  "git": {
    "deploymentEnabled": {
      "master": true,
      "release/*": true,
      "dev": true
    }
  },
  "rewrites": [
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

## What Could Be Done Better

### 1. Pre-check Dependencies

Before starting, Claude could have checked if web dependencies were already installed:

```bash
npm ls react-dom react-native-web
```

This would have saved one iteration.

### 2. Create `.vercelignore` First

The 1.2GB upload attempt could have been avoided by creating `.vercelignore` before the first `vercel --prod` command. Always exclude:

- `node_modules/`
- Build directories (`android/`, `ios/`, `dist/`)
- Large assets not needed for web

### 3. Check for Peer Dependency Issues Earlier

Running `npm install` locally before deployment would have caught the `--legacy-peer-deps` requirement:

```bash
npm install 2>&1 | grep -i "peer"
```

### 4. Use Vercel MCP If Available

The user mentioned having Vercel MCP setup, but it wasn't configured in the session. With MCP, the entire flow could be:

```
User: deploy this to vercel at sudoku.arunabh.me
Claude: [Uses mcp__vercel__deploy tool directly]
```

### 5. Domain Decision Upfront

Asking about the domain structure (dedicated subdomain vs. microfrontend) at the start would have avoided the basePath configuration and subsequent removal.

## Key Takeaways

### Speed Wins

| Traditional Approach              | Claude-Assisted        |
| --------------------------------- | ---------------------- |
| Read Vercel docs (15 min)         | Instant                |
| Debug peer deps (30 min)          | 2 min                  |
| Figure out .vercelignore (20 min) | 1 min                  |
| Configure multi-branch (15 min)   | Included automatically |
| **Total: 1-2 hours**              | **20 minutes**         |

### What Made This Fast

1. **No context switching** - Everything in one terminal session
2. **Iterative problem solving** - Each error was diagnosed and fixed immediately
3. **Full-stack knowledge** - Claude understood Expo, Vercel, DNS, and npm simultaneously
4. **Git hygiene** - Commits were made at each logical step

### The Human's Role

The human provided:

- Clear intent ("deploy to web at this domain")
- Decision-making ("sudoku.arunabh.me" vs "games.arunabh.me")
- Domain knowledge ("TTL is 14400 by default")

Claude provided:

- Technical implementation
- Error diagnosis and fixes
- Best practices (branch deployments, caching headers)
- Trade-off explanations

## Final Result

```
Production URL: https://sudoku.arunabh.me (after DNS propagation)
Preview URL: https://sudoku-streak-ql88l9oxx-arunabh-priyadarshis-projects.vercel.app

Branches configured:
- master → Production
- release/* → Preview deployments
- dev → Preview deployments
```

## Conclusion

What would traditionally take an afternoon of documentation reading, Stack Overflow searches, and trial-and-error was completed in a single 20-minute conversation. The key wasn't that Claude knew everything perfectly—it made mistakes (the 1.2GB upload, missing peer deps flag). The key was **rapid iteration**: each failure was diagnosed and fixed in under a minute.

This is the new development workflow: describe what you want, let the AI attempt it, course-correct together, ship.

---

_This article was written based on an actual Claude Code session. The deployment is live at [sudoku.arunabh.me](https://sudoku.arunabh.me)._
