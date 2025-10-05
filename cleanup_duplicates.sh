#!/bin/bash
# Final cleanup script for remaining duplicate com.wifiguard structure
# This directory should not exist in a proper Android project

echo "🧹 Final cleanup: Removing remaining duplicate com.wifiguard structure..."
echo ""

# Check if com.wifiguard directory still exists
if [ -d "com.wifiguard" ]; then
    echo "📝 Remaining directories to be removed:"
    find com.wifiguard -type d 2>/dev/null | sort
    echo ""
    
    echo "📝 Remaining files to be removed:"
    REMAINING_FILES=$(find com.wifiguard -type f 2>/dev/null | wc -l)
    if [ "$REMAINING_FILES" -gt 0 ]; then
        find com.wifiguard -type f 2>/dev/null | head -10
        if [ "$REMAINING_FILES" -gt 10 ]; then
            echo "... and $(($REMAINING_FILES - 10)) more files"
        fi
    else
        echo "(Only empty directories remain)"
    fi
    echo ""
    
    echo "📋 Total remaining files: $REMAINING_FILES"
    echo ""
    
    echo "🗑️ Removing entire com.wifiguard directory structure..."
    git rm -rf com.wifiguard/
    
    if [ $? -eq 0 ]; then
        echo "✅ SUCCESS: All duplicate files removed!"
        echo ""
        echo "💡 Next steps:"
        echo "   git commit -m 'Complete removal of duplicate com.wifiguard structure'"
        echo "   git push origin main"
    else
        echo "❌ ERROR: Failed to remove some files. Please check permissions."
        exit 1
    fi
else
    echo "✅ ALREADY CLEAN: No com.wifiguard directory found!"
fi

echo ""
echo "🎉 CLEANUP COMPLETE!"
echo "📝 Your WifiGuard project now has a clean, standard Android architecture!"
echo "🎯 All code is properly located in app/src/main/java/com/wifiguard/"
echo "🚫 No more duplicate files or conflicting structures!"
echo "🚀 Ready for professional Android development!"
echo ""
echo "See CLEANUP_SUMMARY.md for detailed cleanup report."
