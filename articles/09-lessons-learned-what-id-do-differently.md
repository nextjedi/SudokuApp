# Lessons Learned: What I'd Do Differently Building My Next App with Claude

_Honest reflections on mistakes made, time wasted, and insights gained_

---

## Introduction

Hindsight is 20/20. After shipping Sudoku Streak to the Play Store, I can clearly see the mistakes I made, the time I wasted, and the approaches that would have been better.

This is my honest retrospective — not to dwell on errors, but to help you avoid them.

---

## Lesson 1: Set Up CI/CD First, Not Last

### What I Did

Built the entire app, then figured out deployment.

### What Happened

Spent 2+ days wrestling with:

- Gradle configuration
- Keystore setup
- GitHub Actions debugging
- Fastlane path issues

All while eager to ship and already mentally "done."

### What I'd Do Differently

**Day 1: Set up the entire pipeline**

```bash
# Create repo
# Add basic CI workflow
# Configure Fastlane
# Do first internal release
# THEN start building features
```

The first release should happen with a "Hello World" app, not a complete product.

### Why It Matters

- Pipeline issues surface early
- Every feature build is tested in real CI
- Mental shift from "coding" to "shipping"
- No late-stage surprises

---

## Lesson 2: Read the Documentation

### What I Did

Jumped into Claude Code and started prompting.

### What Happened

- Didn't know about agents until deep into the project
- Didn't use custom skills
- Used Claude as a chatbot, not an orchestration platform
- Left productivity gains on the table

### What I'd Do Differently

**Before starting, spend 2 hours on:**

- Claude Code documentation
- Agent capabilities
- Skill/command system
- Best practices

### The Cost of Ignorance

Tasks that could have been:

```
"Use Explore agent to find all API calls"
```

Instead were:

```
"Check src/services... now src/api... now src/utils..."
```

Hours of inefficient searching.

---

## Lesson 3: Start with Mobile-First Decisions

### What I Did

Started with web-friendly React patterns, adapted to mobile later.

### What Happened

- Had to refactor navigation structure
- State management needed adjustment
- Some web assumptions don't apply (no URL routing, different lifecycle)

### What I'd Do Differently

**Ask mobile-specific questions early:**

- How does navigation work on mobile?
- What's the app lifecycle (foreground/background)?
- How do we handle offline scenarios?
- What are platform-specific concerns?

### Example Decision

Web: Use React Router with URL-based navigation
Mobile: Use React Navigation with stack-based navigation

Different mental model from the start.

---

## Lesson 4: Define Conventions Early

### What I Did

Let conventions emerge organically as I built.

### What Happened

Inconsistent patterns across the codebase:

- Some components use inline styles
- Others have separate style files
- Mixed naming conventions
- Inconsistent file organization

### What I'd Do Differently

**Create a CONVENTIONS.md on Day 1:**

```markdown
# Project Conventions

## File Naming

- Components: PascalCase (GameGrid.tsx)
- Hooks: camelCase with use prefix (useTimer.ts)
- Utils: camelCase (formatTime.ts)

## Component Structure

- Functional components only
- Styles in separate file (ComponentName.styles.ts)
- Types defined in component file unless shared

## State Management

- Local state: useState
- Shared state: Context
- Complex state: useReducer

## Error Handling

- Always handle async errors
- User-facing errors go to Toast
- Log all errors for debugging
```

Then tell Claude: "Follow the conventions in CONVENTIONS.md"

---

## Lesson 5: Test on Real Devices Earlier

### What I Did

Developed entirely on emulators until final testing.

### What Happened

- Performance issues not visible on powerful dev machine
- Touch interactions feel different
- Real-world interruptions (calls, notifications) not tested
- Device-specific bugs discovered late

### What I'd Do Differently

**Test on physical device weekly:**

- Keep a test device charged and ready
- Build APK for testing (not just Expo Go)
- Test in real-world conditions (outside, poor lighting, interrupted)

### The Reality Check

What feels smooth on an M2 Mac emulator might stutter on a mid-range Android phone.

---

## Lesson 6: Plan the Release Metadata Early

### What I Did

Finished the app, then rushed to create:

- App description
- Screenshots
- Feature graphic
- Privacy policy
- Content rating

### What Happened

- Spent an entire day on non-code tasks
- Screenshots were an afterthought
- Description could have been better with more thought

### What I'd Do Differently

**Parallel track for store presence:**

Week 1: Draft description, plan screenshots
Week 2: Refine description, capture in-progress screenshots
Week 3: Finalize assets, write privacy policy
Release: Everything ready to upload

### The Realization

Store listing quality affects downloads. It deserves as much attention as code quality.

---

## Lesson 7: Smaller, More Frequent Commits

### What I Did

Sometimes worked for hours, then made one big commit.

### What Happened

- Harder to track what changed
- Difficult to revert specific changes
- Git history less useful
- Lost some work to unclear states

### What I'd Do Differently

**Commit after each logical change:**

```bash
# Good
feat: Add timer component
feat: Add timer start/pause logic
fix: Timer doesn't reset on new game
style: Timer display formatting

# Not good
feat: Add timer feature with all functionality
```

### The Rule

If you can describe it in one line, it's one commit.

---

## Lesson 8: Keep a Decision Log

### What I Did

Made decisions in conversation with Claude, then forgot why.

### What Happened

- Revisited same decisions multiple times
- Inconsistent choices without memory of reasoning
- Hard to explain decisions to others (or future self)

### What I'd Do Differently

**Maintain DECISIONS.md:**

```markdown
# Decision Log

## 2024-11-15: State Management

**Decision:** Use React Context instead of Redux
**Reason:** App is simple enough; Redux adds unnecessary complexity
**Tradeoffs:** May need to migrate if app grows significantly

## 2024-11-16: Puzzle Generation

**Decision:** Generate locally instead of fetching from API
**Reason:** Offline-first requirement, no server costs
**Tradeoffs:** App bundle slightly larger, all difficulties predetermined
```

---

## Lesson 9: Define "Done" Before Starting

### What I Did

Started building with a vague sense of "make a Sudoku app."

### What Happened

- Feature creep ("What if we added multiplayer?")
- Unclear when to stop and ship
- Perfectionism spiral

### What I'd Do Differently

**Write MVP spec before coding:**

```markdown
# MVP Scope (v1.0)

## Must Have

- [ ] Basic Sudoku gameplay
- [ ] 4 difficulty levels
- [ ] Save/resume game
- [ ] Basic statistics

## Nice to Have (v1.1+)

- [ ] Daily challenges
- [ ] Themes/dark mode
- [ ] Hint system
- [ ] Achievements

## Not In Scope

- Multiplayer
- Social features
- Accounts/sync
```

Ship the must-haves. Everything else is a future version.

---

## Lesson 10: Celebrate Small Wins

### What I Did

Heads-down building until "done."

### What Happened

- Burnout near the end
- Lost motivation during tedious deployment
- Forgot to appreciate progress

### What I'd Do Differently

**Mark milestones:**

```
Week 1: Game logic works! 🎉
Week 2: Navigation complete! 🎉
Week 3: First build on real device! 🎉
Week 4: Uploaded to Play Store! 🎉
```

Side projects die from lost motivation. Celebrate to maintain momentum.

---

## The Biggest Meta-Lesson

### What Actually Mattered

Not the code quality. Not the architecture. Not even the features.

**What mattered: Shipping.**

A shipped app with imperfect code teaches more than a perfect app that never launches.

### The Paradox

I spent hours optimizing things that didn't matter:

- Perfect TypeScript types for internal functions
- Over-engineered error handling
- Excessive code comments

Meanwhile, I under-invested in:

- Real device testing
- Store listing optimization
- User feedback mechanisms

### The New Priority Order

1. Does it ship?
2. Does it work?
3. Is it maintainable?
4. Is it elegant?

Elegance is a luxury for after you've shipped.

---

## If I Started Over Today

### Day 1

- Create repo with CI/CD pipeline
- First "Hello World" build to Play Store internal track
- Document conventions and decisions
- Set up CLAUDE.md

### Week 1

- Core gameplay loop
- Test on real device
- Ship to internal testers

### Week 2

- Essential features only (from defined MVP)
- Continue real device testing
- Draft store listing

### Week 3

- Polish and bug fixes
- Finalize store assets
- Beta release

### Week 4

- User feedback integration
- Final fixes
- Production release

**4 weeks to shipped.** Not 4 weeks of building plus surprise deployment hell.

---

## Advice for Future Me

1. **Pipeline first.** Always.
2. **Real devices early.** Always.
3. **Define done.** Before starting.
4. **Commit often.** After each change.
5. **Document decisions.** When you make them.
6. **Read the docs.** Before diving in.
7. **Celebrate progress.** Regularly.
8. **Ship imperfect.** Then iterate.

---

## Conclusion

Every project teaches lessons. The key is to actually learn them, not just acknowledge them.

My next project will be different — not because I'm smarter, but because I've been through this once and documented what I'd do differently.

That's the real value of side projects: they're tuition-free education with a portfolio piece at the end.

---

_Final article: The future of AI-assisted development — where is this all going?_

---

**Tags:** #Retrospective #LessonsLearned #SideProjects #SoftwareDevelopment #Productivity #IndieHacker
