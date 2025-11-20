#!/bin/sh

echo "🔧 Installing Git hooks..."
git config core.hooksPath .git-hooks

chmod +x .git-hooks/pre-commit
chmod +x .git-hooks/commit-msg
chmod +x .git-hooks/pre-push

echo "🎉 Git hooks installed successfully!"