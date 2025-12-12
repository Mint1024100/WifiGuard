#!/bin/bash
# Быстрое восстановление потерянных коммитов

echo "🔍 Проверка потерянного коммита..."
git show aab8e9a30ee5aa3038520e864f2c83cb61937d8b --stat | head -30

echo ""
echo "📋 Список измененных файлов в потерянном коммите:"
git diff main aab8e9a30ee5aa3038520e864f2c83cb61937d8b --name-only | head -20

echo ""
echo "✅ Для восстановления выполните:"
echo "   git checkout -b restore-lost aab8e9a30ee5aa3038520e864f2c83cb61937d8b"
echo "   git checkout main"
echo "   git merge restore-lost --no-ff -m 'Восстановление потерянных изменений'"
echo "   git push origin main"

