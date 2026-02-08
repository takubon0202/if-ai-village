#!/usr/bin/env node

/**
 * セットアップ確認スクリプト
 * すべてのCLIと設定ファイルが正しくセットアップされているか確認
 */

import { execSync } from 'child_process';
import { existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(__dirname, '..');

const checks = [];
let hasError = false;

function check(name, fn) {
  try {
    const result = fn();
    if (result) {
      checks.push({ name, status: 'OK', detail: result });
    } else {
      checks.push({ name, status: 'FAIL', detail: 'Not found' });
      hasError = true;
    }
  } catch (e) {
    checks.push({ name, status: 'FAIL', detail: e.message });
    hasError = true;
  }
}

// CLI チェック
check('Java', () => {
  const ver = execSync('java -version 2>&1', { encoding: 'utf-8' });
  const match = ver.match(/version "([^"]+)"/);
  return match ? `v${match[1]}` : ver.trim().split('\n')[0];
});

check('javac', () => {
  const ver = execSync('javac -version 2>&1', { encoding: 'utf-8' });
  return ver.trim();
});

check('Gradle Wrapper', () => {
  const wrapperPath = resolve(projectRoot, 'gradlew.bat');
  return existsSync(wrapperPath) ? 'gradlew.bat exists' : null;
});

check('Node.js', () => {
  const ver = execSync('node --version', { encoding: 'utf-8' });
  return ver.trim();
});

check('npm', () => {
  const ver = execSync('npm --version', { encoding: 'utf-8' });
  return `v${ver.trim()}`;
});

check('Codex CLI', () => {
  const ver = execSync('codex --version 2>&1', { encoding: 'utf-8' });
  return ver.trim();
});

check('Gemini CLI', () => {
  const ver = execSync('gemini --version 2>&1', { encoding: 'utf-8' });
  return ver.trim();
});

check('Git', () => {
  const ver = execSync('git --version', { encoding: 'utf-8' });
  return ver.trim();
});

// ファイルチェック
const requiredFiles = [
  'CLAUDE.md',
  'CODEX.md',
  'GEMINI.md',
  'build.gradle.kts',
  'settings.gradle.kts',
  '.claude/settings.json',
  '.claude/commands/codex.md',
  '.claude/commands/gemini.md',
  '.gitignore',
  'package.json'
];

for (const file of requiredFiles) {
  check(file, () => {
    return existsSync(resolve(projectRoot, file)) ? 'exists' : null;
  });
}

// 結果表示
console.log('\n=== IF MineAI セットアップ確認 ===\n');

const maxNameLen = Math.max(...checks.map(c => c.name.length));

for (const c of checks) {
  const icon = c.status === 'OK' ? '[OK]' : '[!!]';
  const padding = ' '.repeat(maxNameLen - c.name.length);
  console.log(`  ${icon} ${c.name}${padding}  ${c.detail}`);
}

console.log('');

if (hasError) {
  console.log('  !! 一部のチェックが失敗しました。上記を確認してください。');
  process.exit(1);
} else {
  console.log('  すべてのチェックに合格しました!');
}
