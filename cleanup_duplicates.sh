#!/bin/bash
# Script to clean up duplicate com.wifiguard structure
# This directory should not exist in a proper Android project

echo "🧹 Cleaning up duplicate com.wifiguard structure..."

# Remove all files from com.wifiguard structure
git rm -rf com.wifiguard/

echo "✅ Cleanup complete! Duplicate structure removed."
echo "💡 Remember to commit these changes:"
echo "   git commit -m 'Remove duplicate com.wifiguard directory structure'"
echo "   git push origin main"
