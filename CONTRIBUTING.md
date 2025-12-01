# Contributing to Upchaar Common Libraries

Thank you for considering a contribution! This document explains how to set up your environment, our development workflow, commit conventions, Git hooks, and how to propose changes without altering the library’s logical content and guarantees.

## Prerequisites
- Java 21 (Adoptium Temurin 21 or OpenJDK 21)
- Maven 3.9+
- Git
- Optional: Docker (if your adapters/tests need services)

Verify:
```bash
java -version
mvn -v
git --version
```

## Getting Started (Local Dev)
1. Fork and clone the repo:
   ```bash
   git clone https://github.com/<your-username>/upchaar-common-libs.git
   cd upchaar-common-libs
   ```
2. Install Git hooks (commit message lint + pre-push build):
   ```bash
   chmod +x scripts/setup-hooks.sh
   ./scripts/setup-hooks.sh
   ```
    - Pre-commit: validates commit messages (Conventional Commits style: feat:, fix:, docs:, chore:, refactor:, test:, perf:, build:, ci:, style:, revert:)
    - Pre-push: runs mvn -q -DskipTests=false -Dspotless.apply.skip=true clean verify (adjust inside script if needed)

3. Build:
   ```bash
   mvn clean install
   ```

4. Run tests:
   ```bash
   mvn -q -DskipTests=false test
   ```

For more details, see docs/GETTING_STARTED.md.

## Commit Message Convention
We follow Conventional Commits:
- feat: add a new feature
- fix: bug fix
- docs: documentation only changes
- style: formatting, missing semi colons, etc.; no code change
- refactor: code change that neither fixes a bug nor adds a feature
- perf: performance improvement
- test: adding missing tests or correcting existing tests
- build: changes that affect the build system or external dependencies
- ci: changes to CI configuration files and scripts
- chore: other changes that don’t modify src or test files
- revert: reverts a previous commit

Examples:
- feat(api): add ProblemDetailMapper for validation errors
- fix(outbox): handle null publishedAt during cleanup
- docs(readme): clarify Java 21 requirement

## Branching
- main: stable, released code
- develop (optional): integration branch
- feature/<topic>, fix/<ticket>, chore/<task>

## Code Style and Quality
- Keep modules small and cohesive
- Prefer interfaces in this repo; concrete adapters live in service repos
- Add JavaDoc for public APIs
- Write tests for new utilities and mappers
- If adding config properties, annotate with @ConfigurationProperties and document defaults

## Opening a Pull Request
1. Create a feature branch
2. Ensure:
    - mvn clean verify passes locally
    - public APIs have JavaDoc
    - docs updated (README, docs/* as relevant)
3. Submit PR with:
    - Clear description
    - Linked issue (if exists)
    - Screenshots/logs when relevant

## Release Process
See docs/VERSIONING_AND_RELEASES.md

## License
By contributing, you agree your contributions are licensed under the repository’s license (Proprietary). Contact the maintainer for exceptions.