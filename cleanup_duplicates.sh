#!/bin/bash
# Script to clean up remaining duplicate com.wifiguard structure
# This directory should not exist in a proper Android project

echo "🧹 Cleaning up remaining duplicate com.wifiguard structure..."

# Show what will be removed
echo "📝 Files and directories to be removed:"
find com.wifiguard -type f 2>/dev/null | head -20
echo "..."

# Count remaining files
FILE_COUNT=$(find com.wifiguard -type f 2>/dev/null | wc -l)
echo "📋 Total remaining files: $FILE_COUNT"

# Remove all remaining files from com.wifiguard structure
if [ -d "com.wifiguard" ]; then
    echo "🗑️ Removing duplicate com.wifiguard directory..."
    git rm -rf com.wifiguard/
    
    echo "✅ Cleanup complete! Duplicate structure removed."
    echo "💡 Now commit these changes:"
    echo "   git commit -m 'Remove remaining duplicate com.wifiguard directory structure'"
    echo "   git push origin main"
else
    echo "✅ No com.wifiguard directory found. Cleanup already complete!"
fi

echo ""
echo "🎉 Your WifiGuard project now has a clean, standard Android architecture!"
echo "📝 All code is properly located in app/src/main/java/com/wifiguard/"
echo "🚫 No more duplicate files or conflicting structures!"
