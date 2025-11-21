# Vibe Coding a Sudoku App in React Native with Claude: My Journey into AI-Assisted Development

_How I built a complete mobile game by having conversations with an AI_

---

## Introduction

"Vibe coding" — it's a term that perfectly captures the new paradigm of software development where you describe what you want, and AI helps you build it. No more endless Stack Overflow searches, no more copy-pasting boilerplate code, no more spending hours debugging cryptic error messages alone.

I recently built **Sudoku Streak**, a complete React Native mobile game, using Claude Code as my AI pair programmer. What would have taken me weeks of solo development compressed into days of collaborative coding sessions. Here's my honest experience of what worked, what didn't, and what I learned about this new way of building software.

---

## What is Vibe Coding?

Vibe coding is conversational development. Instead of writing every line of code yourself, you:

1. **Describe your intent** in natural language
2. **Review and iterate** on AI-generated solutions
3. **Guide the direction** while AI handles implementation details
4. **Learn as you go** from the code being written

It's not about being lazy or not knowing how to code. It's about leveraging AI to handle repetitive tasks while you focus on architecture, user experience, and the creative aspects of your app.

---

## The Project: Sudoku Streak

I wanted to build a mobile Sudoku game with these features:

- Clean, minimalist UI
- Multiple difficulty levels
- Streak tracking (consecutive days played)
- Offline-first functionality
- Cross-platform (iOS and Android)
- Eventually deployable to app stores

### Tech Stack Chosen

```
- React Native (Expo managed workflow initially, then bare)
- TypeScript for type safety
- AsyncStorage for local persistence
- React Navigation for routing
- Custom Sudoku generation algorithm
```

---

## Day 1: Setting Up and Initial Development

### The First Conversation

My first prompt to Claude was simple:

> "I want to build a Sudoku mobile app using React Native. Help me set up the project structure and create the basic game logic."

Within minutes, Claude had:

1. Created the Expo project structure
2. Set up TypeScript configuration
3. Generated a complete Sudoku puzzle generator
4. Built the basic grid component
5. Added touch handling for cell selection

### What Surprised Me

The Sudoku generation algorithm Claude produced was sophisticated — it used backtracking with constraint propagation, the same technique used in professional Sudoku software. I didn't have to explain the algorithm; Claude understood what "valid Sudoku puzzle" meant.

```typescript
// Claude generated this complete puzzle generator
const generatePuzzle = (difficulty: Difficulty): SudokuGrid => {
  const solution = generateSolvedGrid();
  const puzzle = removeCells(solution, getDifficultyCount(difficulty));
  return { puzzle, solution };
};

const generateSolvedGrid = (): number[][] => {
  const grid = Array(9)
    .fill(null)
    .map(() => Array(9).fill(0));
  fillGrid(grid);
  return grid;
};

const fillGrid = (grid: number[][]): boolean => {
  const emptyCell = findEmptyCell(grid);
  if (!emptyCell) return true;

  const [row, col] = emptyCell;
  const numbers = shuffle([1, 2, 3, 4, 5, 6, 7, 8, 9]);

  for (const num of numbers) {
    if (isValidPlacement(grid, row, col, num)) {
      grid[row][col] = num;
      if (fillGrid(grid)) return true;
      grid[row][col] = 0;
    }
  }
  return false;
};
```

---

## Day 2-3: Building the UI

### Conversational Iteration

The beauty of vibe coding is iteration. I'd say things like:

> "The grid looks too cramped. Add more spacing between the 3x3 boxes."

> "Make the selected cell more obvious — maybe a blue highlight?"

> "Add haptic feedback when the user places a correct number."

Each request was understood and implemented. No need to look up React Native's Vibration API or StyleSheet documentation — Claude knew it all.

### The UI Evolution

```
Initial → Basic gray grid
  ↓
Iteration 1 → Added box borders for 3x3 sections
  ↓
Iteration 2 → Color-coded conflicts (red for errors)
  ↓
Iteration 3 → Smooth animations for number placement
  ↓
Final → Polished, app-store ready UI
```

---

## The Vibe Coding Workflow

Here's the pattern that emerged:

### 1. High-Level Direction

```
Me: "Add a feature to track the user's daily streak"
```

### 2. Claude's Implementation

Claude would:

- Create the data structure
- Build the persistence logic
- Add UI components
- Handle edge cases (timezone changes, missed days)

### 3. My Review and Feedback

```
Me: "The streak should reset at midnight in the user's timezone, not UTC"
```

### 4. Refinement

Claude would update the logic, explaining the changes.

---

## What Vibe Coding Does Well

### Speed of Development

Tasks that would take hours of research and implementation took minutes:

- Setting up navigation: 5 minutes
- Implementing AsyncStorage persistence: 10 minutes
- Creating the settings screen: 15 minutes
- Adding dark mode support: 20 minutes

### Handling Boilerplate

React Native has a lot of boilerplate. Claude handled:

- TypeScript interfaces and types
- Navigation type definitions
- Platform-specific code (`Platform.select`)
- Style definitions

### Debugging

When errors occurred, I'd paste them directly:

```
Me: "Getting this error: 'Cannot read property 'map' of undefined' in SudokuGrid.tsx"
```

Claude would analyze the stack trace, identify the issue, and provide a fix — often pointing out the root cause I would have spent an hour finding myself.

---

## What Vibe Coding Struggles With

### Context Limits

Long conversations lose context. Claude might forget earlier decisions or re-implement things differently. I learned to:

- Keep sessions focused on specific features
- Summarize decisions at conversation starts
- Use clear file naming conventions

### Opinionated Decisions

Sometimes Claude would make architectural choices I disagreed with. You need enough knowledge to:

- Recognize when a solution isn't optimal
- Guide toward better patterns
- Know when to accept "good enough"

### Platform-Specific Quirks

Some React Native issues require deep platform knowledge:

- Android keystore signing
- iOS provisioning profiles
- Gradle configuration

These required more back-and-forth than pure JavaScript logic.

---

## Tips for Effective Vibe Coding

### 1. Be Specific About Intent

```
Bad: "Make it look better"
Good: "Add 8px padding to the grid cells and use a subtle shadow on the container"
```

### 2. Provide Context

```
"In the context of our Sudoku app where users can play daily challenges,
add a notification reminder feature"
```

### 3. Review Generated Code

Don't blindly accept everything. I caught several issues:

- Unused imports
- Overly complex solutions
- Missing error handling

### 4. Learn From the Output

Vibe coding is a learning opportunity. When Claude used a pattern I didn't know, I'd ask:

```
"Explain why you used useMemo here instead of useState"
```

---

## The Result

After about a week of vibe coding sessions, I had:

- A fully functional Sudoku game
- Multiple difficulty levels
- Daily challenges with streak tracking
- Clean, responsive UI
- Local persistence
- Ready for app store deployment

The code was clean, typed, and well-organized — arguably better than what I would have written alone, because Claude followed best practices consistently.

---

## Conclusion

Vibe coding isn't replacing traditional development — it's augmenting it. You still need to understand:

- What you're building
- How to evaluate solutions
- When something is wrong

But the speed boost is undeniable. Tasks that required research, trial-and-error, and debugging now happen through conversation. It's like having a senior developer available 24/7 who never gets tired of your questions.

For personal projects especially, vibe coding removes the friction that often kills side projects. No more getting stuck on configuration issues or losing momentum to documentation rabbit holes.

If you haven't tried AI-assisted development yet, start with a small project. You might be surprised how much you can build through conversation.

---

_Next in this series: The journey of deploying a React Native app to Android and the Play Store — all the headaches and how Claude helped navigate them._

---

**Tags:** #ReactNative #Claude #AI #VibeCoding #MobileDevelopment #Sudoku #AIPairProgramming
