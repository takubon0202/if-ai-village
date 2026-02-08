#!/usr/bin/env node

/**
 * Codex CLI ヘルパースクリプト
 * プロジェクトコンテキストを自動埋め込みしてCodex CLIを実行
 */

import { spawn } from 'child_process';
import { readFileSync, existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(__dirname, '..');

// プロジェクトコンテキスト読み込み
function loadContext() {
  const codexMdPath = resolve(projectRoot, 'CODEX.md');
  if (existsSync(codexMdPath)) {
    return readFileSync(codexMdPath, 'utf-8');
  }
  return '';
}

// 引数解析
const args = process.argv.slice(2);
let mode = 'general';
let sandbox = 'read-only';
let targetFile = null;
let task = [];

for (let i = 0; i < args.length; i++) {
  switch (args[i]) {
    case '--error':
      mode = 'error';
      break;
    case '--file':
      mode = 'file';
      targetFile = args[++i];
      break;
    case '--minecraft':
      mode = 'minecraft';
      break;
    case '--write':
      sandbox = 'workspace-write';
      break;
    case '--interactive':
      mode = 'interactive';
      break;
    case '--help':
      console.log(`
Codex CLI ヘルパー - IF MineAI

使い方:
  node scripts/codex-helper.js "タスク"
  node scripts/codex-helper.js --error "エラーメッセージ"
  node scripts/codex-helper.js --file src/Main.java "修正内容"
  node scripts/codex-helper.js --minecraft "MCタスク"
  node scripts/codex-helper.js --write "コード生成タスク"
  node scripts/codex-helper.js --interactive

オプション:
  --error       エラー修正モード
  --file PATH   特定ファイル修正モード
  --minecraft   Minecraft特化モード
  --write       書き込みサンドボックス (workspace-write)
  --interactive 対話モード
  --help        ヘルプ表示
`);
      process.exit(0);
    default:
      task.push(args[i]);
  }
}

const taskStr = task.join(' ');

if (!taskStr && mode !== 'interactive') {
  console.error('エラー: タスクを指定してください');
  console.error('使い方: node scripts/codex-helper.js "タスク"');
  process.exit(1);
}

// プロンプト構築
const today = new Date().toISOString().split('T')[0];
const context = loadContext();
let prompt = `今日は${today}です。\n`;
prompt += `プロジェクト: IF MineAI (Minecraft Plugin, Java 21, Paper API 1.21.11, Gradle 8.12)\n\n`;

switch (mode) {
  case 'error':
    prompt += `以下のエラーを修正してください:\n${taskStr}`;
    sandbox = 'workspace-write';
    break;
  case 'file':
    prompt += `ファイル ${targetFile} について:\n${taskStr}`;
    break;
  case 'minecraft':
    prompt += `Minecraft Plugin開発タスク:\n${taskStr}`;
    break;
  default:
    prompt += taskStr;
}

// Codex CLI 実行
console.log(`[Codex Helper] モード: ${mode}, サンドボックス: ${sandbox}`);
console.log(`[Codex Helper] タスク: ${taskStr}`);
console.log('---');

if (mode === 'interactive') {
  const proc = spawn('codex', [], {
    stdio: 'inherit',
    cwd: projectRoot,
    shell: true
  });
  proc.on('close', (code) => process.exit(code));
} else {
  const proc = spawn('codex', ['exec', '-', '--sandbox', sandbox], {
    stdio: ['pipe', 'inherit', 'inherit'],
    cwd: projectRoot,
    shell: true
  });
  proc.stdin.write(prompt);
  proc.stdin.end();
  proc.on('close', (code) => process.exit(code));
}
