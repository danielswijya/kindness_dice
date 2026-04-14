# Coding Conventions

**Analysis Date:** 2026-04-14

## Naming Patterns

**Files:**
- PascalCase for React component files: `App.tsx`
- camelCase for config files: `babel.config.js`, `jest.config.js`, `metro.config.js`
- Test files mirror source file name with `.test.` suffix: `App.test.tsx`

**Functions / Components:**
- PascalCase for React function components: `App`, `AppContent`
- camelCase for variables and hooks: `isDarkMode`, `safeAreaInsets`

**Variables:**
- camelCase for all local variables and destructured values
- Boolean variable names use `is` prefix: `isDarkMode`

**Types:**
- TypeScript types inferred from React Native and third-party library types; no custom type aliases or interfaces defined yet in application code

## Code Style

**Formatting:**
- Tool: Prettier 2.8.8
- Config: `.prettierrc.js`
- Key settings:
  - `singleQuote: true` — use single quotes for strings
  - `trailingComma: 'all'` — trailing commas everywhere valid
  - `arrowParens: 'avoid'` — omit parens for single-param arrow functions

**Linting:**
- Tool: ESLint 8.x
- Config: `.eslintrc.js` extends `@react-native` (official React Native ESLint config)
- Run with: `npm run lint`

## Import Organization

**Order observed in `App.tsx` and `index.js`:**
1. Third-party library imports (e.g., `react`, `react-native`, `@react-native/new-app-screen`, `react-native-safe-area-context`)
2. Local relative imports (e.g., `./App`)

**Path Aliases:**
- None configured. All local imports use relative paths (e.g., `'../App'`)

**React import:**
- React is not imported explicitly in `App.tsx` (JSX transform handles it)
- React is imported explicitly in test files: `import React from 'react'`

## File Header Comments

All source files carry a JSDoc-style header with `@format` tag:

```typescript
/**
 * @format
 */
```

`App.tsx` additionally carries a descriptive comment block:

```typescript
/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */
```

## Component Design

**Pattern:** Function components only. No class components observed.

**Component splitting:** Sub-components are defined in the same file when tightly coupled. Example: `AppContent` is defined in `App.tsx` alongside `App`.

**Styles:**
- Styles use `StyleSheet.create({})` at the bottom of the file, after component definitions
- Style object named `styles` (camelCase)

```typescript
const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});
```

**Hooks:**
- Custom hook results assigned to descriptive camelCase variables: `const isDarkMode = useColorScheme() === 'dark'`
- Hooks called at the top of function component bodies

## Error Handling

**Pattern:** No custom error handling or error boundaries observed in current application code. The project is at initial scaffold stage.

**Recommendation for new code:** Follow React Native conventions — use `try/catch` for async operations, `ErrorBoundary` components for UI-level errors.

## Logging

**Framework:** Not configured. No `console.log` or logging library present in application code.

## Comments

**When to Comment:**
- File-level JSDoc header with `@format` tag on every file
- Inline comments used sparingly (none in application code)
- Config files include URL references for documentation: `// https://reactnative.dev/docs/metro`

**JSDoc/TSDoc:**
- `@format` tag used universally in file headers
- `@type` used in `metro.config.js` for IDE type support on config objects

## Module Design

**Exports:**
- Single default export per component file: `export default App`
- Named exports not used in application code

**Barrel Files:**
- None. Each module is imported directly by path.

## TypeScript

**Config:**
- Extends `@react-native/typescript-config`
- `types: ["jest"]` added to support Jest globals in tests
- Includes all `**/*.ts` and `**/*.tsx`, excludes `node_modules` and `Pods`
- Config: `tsconfig.json`

**Strictness:**
- Inherits strictness settings from `@react-native/typescript-config` (strict mode enabled by default in that preset)

---

*Convention analysis: 2026-04-14*
