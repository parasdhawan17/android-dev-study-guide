# Android Dev Study Guide

Android Dev Study Guide is a MkDocs Material site for Android interview preparation and practical Android engineering review. It turns the source notes in `notes/` into a browsable documentation site under `docs/`, then publishes it with GitHub Pages.

Live site: https://parasdhawan17.github.io/android-dev-study-guide/

## What This Repo Contains

- Structured reading notes for Kotlin, Android lifecycle, Compose, background work, coroutines, architecture, testing, performance, debugging, and interview practice.
- Kotlin-heavy examples in `notes/**/*.kt`, converted into Markdown pages by `scripts/generate_mkdocs_docs.py`.
- Cheat sheets and practice exercises for each study section.
- A MkDocs Material configuration with search, navigation, code highlighting, and copyable code blocks.
- A GitHub Actions workflow that regenerates the docs and deploys the site to GitHub Pages on every push to `main`.

## Reading Material Index

### Day 1-2: Core Kotlin Concepts

- Cheat Sheet
- Data Classes
- Sealed Classes
- Extension Functions
- Higher Order Functions
- Null Safety
- Generics Variance
- Practice Exercises

### Day 3-4: Android Lifecycle And Navigation

- Cheat Sheet
- Activity Lifecycle
- Fragment Lifecycle
- Lifecycle Components
- ViewModel Lifecycle
- Memory Leaks And Process Death
- Practice Exercises
- Navigation Component

### Day 5-7: Compose, State, And Background Work

- Cheat Sheet
- Compose Lifecycle
- Compose vs Views
- Background Execution
- Saved State And Process Death
- Practice Exercises

### Day 8-14: Coroutines, Flow, And Threading

- Cheat Sheet
- Coroutines Fundamentals
- Flow And Reactive State
- Thread Safety And Synchronization
- Android Threading
- Practice Exercises

### Day 15-21: Architecture And Design Patterns

- Cheat Sheet
- MVVM
- MVI
- Clean Architecture
- Dependency Injection
- Practice Exercises

### Day 22-28: Testing

- Cheat Sheet
- Unit Testing Fundamentals
- ViewModel And Coroutine Testing
- Use Case And Repository Testing
- Testable Code Design
- Practice Exercises

### Day 29-34: Performance

- Cheat Sheet
- RecyclerView Performance
- Layout Performance
- Memory And Profiling
- Startup Jank And ANR
- Practice Exercises

### Day 35-42: Debugging And Final Interview Prep

- Cheat Sheet
- Debugging Techniques
- Edge Cases And Error Handling
- Coding Interview Patterns
- Final Mock Interview Prep
- Practice Exercises

## Repository Structure

- `notes/`: Source of truth for study material. Markdown files are copied as-is; Kotlin files are converted into Markdown pages.
- `docs/`: Generated MkDocs documentation source.
- `site/`: Local MkDocs build output. This directory is ignored by git.
- `scripts/generate_mkdocs_docs.py`: Generates `docs/`, `docs/index.md`, `docs/stylesheets/extra.css`, and `mkdocs.yml` from `notes/`.
- `.github/workflows/deploy-mkdocs.yml`: Builds and deploys the site to GitHub Pages.
- `requirements.txt`: Python dependencies for building the site.

## Local Development

Create a virtual environment, install dependencies, regenerate docs, and run the site locally:

```bash
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
python3 scripts/generate_mkdocs_docs.py
.venv/bin/mkdocs serve
```

Build the site strictly, matching the deploy workflow:

```bash
python3 scripts/generate_mkdocs_docs.py
.venv/bin/mkdocs build --strict
```

## Publishing

Pushing to `main` triggers the `Deploy MkDocs site` GitHub Actions workflow. The workflow:

1. Installs Python dependencies from `requirements.txt`.
2. Runs `python scripts/generate_mkdocs_docs.py`.
3. Builds the site with `mkdocs build --strict`.
4. Deploys the generated site to GitHub Pages.

Because the workflow regenerates `docs/` from `notes/`, make source content changes in `notes/` first, then regenerate before committing.
