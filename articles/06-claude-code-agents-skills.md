# Claude Code Agents and Skills: The Features I Didn't Use But Should Have

_A retrospective on building without leveraging Claude's most powerful capabilities_

---

## Introduction

I built an entire app with Claude Code, deployed it to the Play Store, and set up CI/CD — all without using agents or skills. Looking back, I left significant productivity gains on the table.

This article explores what agents and skills are, how they could have helped, and how I plan to use them in future projects.

---

## What Are Agents?

Claude Code can spawn **sub-agents** — specialized Claude instances that handle specific tasks autonomously. Instead of:

```
You: Do complex task
Claude: [Does everything in one conversation]
```

You get:

```
You: Do complex task
Claude: I'll use specialized agents for this.
        [Launches Explore agent to analyze codebase]
        [Launches code-review agent after changes]
        [Reports back consolidated results]
```

### Available Agent Types

1. **Explore Agent**
   - Fast codebase exploration
   - File pattern matching
   - Code searching
   - Architecture understanding

2. **Plan Agent**
   - Task breakdown
   - Implementation planning
   - Dependency analysis

3. **General Purpose Agent**
   - Complex multi-step tasks
   - Research questions
   - Autonomous problem-solving

---

## What Are Skills?

Skills are pre-defined capabilities that Claude can invoke for specific tasks. Think of them as "macros" or "plugins" that provide specialized behavior.

Skills can be:

- Built into Claude Code
- Custom-defined in your project
- Shared across teams

---

## How I Should Have Used Agents

### Scenario 1: Codebase Exploration

**What I Did:**

```
Me: "Where is the game state managed?"
Claude: [Searches files one by one]
        "Let me check src/context... now src/hooks...
         now src/components..."
```

Multiple tool calls, slow, context-consuming.

**What I Should Have Done:**

```
Me: "Use the Explore agent to find where game state is managed"
Claude: [Launches Explore agent]
        [Agent searches in parallel, analyzes patterns]
        "Game state is managed in:
         - src/context/GameContext.tsx (main state)
         - src/hooks/useGameState.ts (state logic)
         - src/services/persistence.ts (storage)"
```

Faster, more thorough, less context consumed in main conversation.

### Scenario 2: Understanding Project Structure

**What I Did:**

```
Me: "What's the overall structure of this project?"
Claude: [Runs multiple ls, cat, grep commands]
        [Builds understanding incrementally]
```

**What I Should Have Done:**

```
Me: "Use Explore agent with 'very thorough' to analyze
     the complete project structure and architecture"
Claude: [Agent analyzes comprehensively]
        [Returns full architectural overview]
```

### Scenario 3: Multi-File Changes

**What I Did:**

```
Me: "Rename 'getCwd' to 'getCurrentWorkingDirectory' everywhere"
Claude: [Searches file by file]
        [Edits each occurrence manually]
```

**What I Should Have Done:**

```
Me: "Use a general-purpose agent to find and rename 'getCwd'
     to 'getCurrentWorkingDirectory' across the entire project"
Claude: [Agent handles systematically]
        [Returns summary of all changes]
```

---

## How I Should Have Used Skills

### Custom Slash Commands

Claude Code supports custom slash commands defined in `.claude/commands/`:

```markdown
<!-- .claude/commands/review-pr.md -->

Review pull request #$ARGUMENTS

1. Fetch PR details using gh CLI
2. Analyze all changed files
3. Check for:
   - Type safety issues
   - Missing error handling
   - Performance concerns
   - Security vulnerabilities
4. Provide summary with specific line references
```

Then use:

```
/review-pr 88
```

### Skills I Could Have Created

#### 1. Build Verification Skill

```markdown
<!-- .claude/commands/verify-build.md -->

Verify the project builds successfully:

1. Run npm run lint
2. Run npm run typecheck
3. Run npm test
4. Run Android build (./gradlew assembleRelease)
5. Report any failures with specific errors
```

#### 2. Release Preparation Skill

```markdown
<!-- .claude/commands/prepare-release.md -->

Prepare for release version $ARGUMENTS:

1. Update version in package.json
2. Update versionCode/versionName in build.gradle
3. Update CHANGELOG.md
4. Create git tag
5. Generate release notes from commits
```

#### 3. Component Generator Skill

```markdown
<!-- .claude/commands/new-component.md -->

Create a new React component named $ARGUMENTS:

1. Create component file with TypeScript
2. Create styles file
3. Add proper typing
4. Export from index
5. Follow existing project patterns
```

---

## The Explore Agent Deep Dive

The Explore agent is particularly powerful and I underutilized it completely.

### Thoroughness Levels

```
Quick: Basic file/pattern matching
Medium: Moderate exploration
Very Thorough: Comprehensive analysis across codebase
```

### Use Cases

| Question                            | Agent Setting |
| ----------------------------------- | ------------- |
| "Where is X defined?"               | Quick         |
| "How does feature Y work?"          | Medium        |
| "What's the complete architecture?" | Very Thorough |

### Example: Architecture Analysis

```
Me: "Use Explore agent (very thorough) to analyze
     how data flows from user input to persistence"

Agent: [Analyzes comprehensively]

Result:
1. User Input → TouchableCell component
2. → GameContext.dispatch(PLACE_NUMBER)
3. → gameReducer validates and updates state
4. → useEffect in GameContext triggers persistence
5. → AsyncStorage.setItem saves JSON state
6. → On app load, AsyncStorage.getItem restores

Files involved: [list]
Key functions: [list]
Potential issues found: [list]
```

This kind of analysis would have taken me 30+ minutes manually.

---

## Why I Didn't Use These Features

### 1. Didn't Know They Existed

Claude Code documentation is extensive. I jumped in and started coding without reading about agents and skills.

### 2. "Just Do It" Was Working

Direct commands worked fine for simple tasks. I didn't feel the pain until complex tasks.

### 3. Mental Model of AI = Chat

I thought of Claude as a chatbot, not as an orchestrator of specialized workers.

### 4. Upfront Investment

Creating custom skills requires initial effort. For a short project, it felt like overhead.

---

## What I'll Do Differently

### Project Setup Phase

1. **Create `.claude/` directory structure**

```
.claude/
├── commands/
│   ├── verify-build.md
│   ├── prepare-release.md
│   └── new-component.md
└── settings.json
```

2. **Define project-specific skills**
   - Common workflows as commands
   - Conventions encoded as instructions
   - Quality checks automated

### During Development

1. **Use Explore agent for any codebase questions**
   - "Use Explore agent to find..."
   - "Use Explore agent to understand..."

2. **Use Plan agent for complex features**
   - Before implementing, ask for a plan
   - Review plan before executing

3. **Run agents in parallel when possible**
   - "Launch agents in parallel to..."
   - Faster results, less waiting

### Before Commits

1. **Custom code review skill**
   - Runs linting
   - Checks for common issues
   - Verifies types

---

## Creating Your First Custom Skill

### Step 1: Create the Command File

```markdown
<!-- .claude/commands/check-quality.md -->

# Code Quality Check

Run comprehensive quality checks on the codebase:

## Steps

1. Run ESLint and report any errors
2. Run TypeScript compiler in strict mode
3. Check for console.log statements in production code
4. Verify all components have proper types
5. Check for TODO/FIXME comments that need addressing

## Output Format

Provide a summary with:

- Total issues found
- Critical issues (must fix)
- Warnings (should fix)
- Suggestions (nice to fix)
```

### Step 2: Use the Command

```
/check-quality
```

Claude expands the command and executes each step.

### Step 3: Iterate and Improve

As you find new checks, add them to the command.

---

## Agent Performance Tips

### 1. Be Specific About Thoroughness

```
# Vague
"Explore the codebase"

# Better
"Use Explore agent (medium thoroughness) to find
all API endpoints and their handlers"
```

### 2. Provide Context for Agents

```
"Use Explore agent to find authentication logic.
Context: We use JWT tokens stored in AsyncStorage,
and auth state is in AuthContext."
```

### 3. Use Parallel Agents for Independent Tasks

```
"Launch agents in parallel:
1. Explore agent: Find all database queries
2. Explore agent: Find all API calls
3. Plan agent: Design caching strategy based on findings"
```

---

## The ROI of Learning These Features

| Time Investment        | Payoff                      |
| ---------------------- | --------------------------- |
| 2 hours reading docs   | Weeks of faster development |
| 1 hour creating skills | Repeatable workflows        |
| Learning agent syntax  | Better task delegation      |

For my next project, I'll invest this upfront time. The compound benefits are clear in retrospect.

---

## Conclusion

Claude Code is more than a chat interface for code generation. It's an orchestration platform for AI workers. I used it like a chatbot when I should have used it like a team of specialists.

Key takeaways:

- **Explore agent** for codebase understanding
- **Plan agent** for complex implementations
- **Custom skills** for repeatable workflows
- **Parallel agents** for faster results

Don't make my mistake. Read the docs. Learn the features. Use Claude Code to its full potential.

---

_Next article: The complete software development flow — from first line of code to user feedback._

---

**Tags:** #ClaudeCode #AIAgents #DeveloperProductivity #Skills #Automation #AITools
