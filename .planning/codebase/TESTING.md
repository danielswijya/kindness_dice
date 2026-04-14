# Testing Patterns

**Analysis Date:** 2026-04-14

## Test Framework

**Runner:**
- Jest 29.x
- Config: `jest.config.js`
- Preset: `@react-native/jest-preset` (handles transforms, module mocking for React Native)

**Assertion Library:**
- Jest built-in (`expect`)

**Renderer:**
- `react-test-renderer` 19.2.3 — used for component snapshot/render testing

**Run Commands:**
```bash
npm test           # Run all tests
```

No watch mode or coverage script is defined in `package.json`. To run manually:
```bash
npx jest --watch           # Watch mode
npx jest --coverage        # Coverage report
```

## Test File Organization

**Location:**
- Separate `__tests__/` directory at project root

**Naming:**
- `[ComponentName].test.tsx` — mirrors the source file name
- Example: `App.tsx` → `__tests__/App.test.tsx`

**Structure:**
```
KindnessDice/
├── __tests__/
│   └── App.test.tsx
├── App.tsx
└── jest.config.js
```

## Test Structure

**Suite Organization:**

There are no `describe` blocks in the current test file. Tests are written as top-level `test()` calls:

```typescript
/**
 * @format
 */

import React from 'react';
import ReactTestRenderer from 'react-test-renderer';
import App from '../App';

test('renders correctly', async () => {
  await ReactTestRenderer.act(() => {
    ReactTestRenderer.create(<App />);
  });
});
```

**Patterns:**
- Async/await used for rendering to handle concurrent React updates
- `ReactTestRenderer.act()` wraps renders to flush state and effects
- Import path from `__tests__/` back to root: `'../App'`
- `React` is imported explicitly in test files (required for JSX in test context)

## Mocking

**Framework:** Jest built-in mocking via `@react-native/jest-preset`

The preset automatically mocks React Native modules. No manual `jest.mock()` calls are present in the existing test file.

**Patterns:**
- Rely on `@react-native/jest-preset` for native module mocking
- For custom mocks, use `jest.mock()` at the top of the test file before imports

**What the preset handles:**
- Native modules (NativeModules)
- Platform-specific code
- Animations

## Fixtures and Factories

**Test Data:**
- Not applicable at current project stage. No fixture files or factory functions exist.

**Location:**
- If added, place in `__tests__/fixtures/` or `__tests__/helpers/`

## Coverage

**Requirements:** None enforced. No `coverageThreshold` in `jest.config.js`.

**View Coverage:**
```bash
npx jest --coverage
```

## Test Types

**Unit Tests:**
- Scope: Individual component render verification
- Approach: `react-test-renderer` creates component tree; test passes if render does not throw

**Integration Tests:**
- Not present. No multi-component integration tests exist.

**E2E Tests:**
- Not configured. No Detox or similar framework is installed.

## Common Patterns

**Async Rendering:**
```typescript
test('renders correctly', async () => {
  await ReactTestRenderer.act(() => {
    ReactTestRenderer.create(<App />);
  });
});
```

**Error Testing:**
- No error testing patterns established yet.

**Snapshot Testing:**
- Not currently used. `ReactTestRenderer.create()` result is not captured or asserted against a snapshot. To add snapshot testing:

```typescript
test('matches snapshot', async () => {
  let tree;
  await ReactTestRenderer.act(() => {
    tree = ReactTestRenderer.create(<App />);
  });
  expect(tree.toJSON()).toMatchSnapshot();
});
```

## Jest Configuration

**`jest.config.js`:**
```javascript
module.exports = {
  preset: '@react-native/jest-preset',
};
```

The entire configuration is delegated to `@react-native/jest-preset`, which provides:
- Babel transform via `@react-native/babel-preset`
- Module name mapper for assets (images, fonts)
- Setup files for React Native environment
- `testEnvironment: 'node'`

## TypeScript in Tests

- Test files use `.tsx` extension
- `tsconfig.json` includes `"types": ["jest"]` to provide Jest global types (`test`, `expect`, etc.) without explicit imports

---

*Testing analysis: 2026-04-14*
