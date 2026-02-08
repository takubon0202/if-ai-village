#!/usr/bin/env node

/**
 * Gemini CLI ヘルパースクリプト
 * Gemini 3系のみ使用を強制し、プロジェクトコンテキストを自動埋め込み
 */

import { spawn } from 'child_process';
import { readFileSync, existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(__dirname, '..');

// 許可モデル（Gemini 3系のみ）
const ALLOWED_MODELS = {
  pro: 'gemini-3-pro-preview',
  flash: 'gemini-3-flash-preview'
};
const DEFAULT_MODEL = ALLOWED_MODELS.pro;

// プロジェクトコンテキスト読み込み
function loadContext() {
  const geminiMdPath = resolve(projectRoot, 'GEMINI.md');
  if (existsSync(geminiMdPath)) {
    return readFileSync(geminiMdPath, 'utf-8');
  }
  return '';
}

// モデル検証（Gemini 3系のみ許可）
function validateModel(model) {
  const allowedValues = Object.values(ALLOWED_MODELS);
  if (allowedValues.includes(model)) {
    return model;
  }
  if (model && model.includes('2.5')) {
    console.warn(`[Gemini Helper] ⚠ Gemini 2.5系は使用禁止です: ${model}`);
    console.warn(`[Gemini Helper] デフォルトモデル ${DEFAULT_MODEL} を使用します`);
    return DEFAULT_MODEL;
  }
  if (model) {
    console.warn(`[Gemini Helper] ⚠ 不明なモデル: ${model}`);
    console.warn(`[Gemini Helper] デフォルトモデル ${DEFAULT_MODEL} を使用します`);
  }
  return DEFAULT_MODEL;
}

// 引数解析
const args = process.argv.slice(2);
let mode = 'general';
let model = DEFAULT_MODEL;
let targetFile = null;
let yolo = true; // デフォルトでYOLOモード有効
let task = [];

for (let i = 0; i < args.length; i++) {
  switch (args[i]) {
    case '--flash':
      model = ALLOWED_MODELS.flash;
      break;
    case '--pro':
      model = ALLOWED_MODELS.pro;
      break;
    case '--model':
      model = validateModel(args[++i]);
      break;
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
    case '--interactive':
      mode = 'interactive';
      break;
    case '--yolo':
    case '-y':
      yolo = true;
      break;
    case '--no-yolo':
      yolo = false;
      break;
    case '--help':
      console.log(`
Gemini CLI ヘルパー - IF MineAI

使い方:
  node scripts/gemini-helper.js "タスク"
  node scripts/gemini-helper.js --flash "軽量タスク"
  node scripts/gemini-helper.js --error "エラーメッセージ"
  node scripts/gemini-helper.js --file src/Main.java "修正内容"
  node scripts/gemini-helper.js --minecraft "MCタスク"
  node scripts/gemini-helper.js --interactive
  node scripts/gemini-helper.js --yolo "タスク"

モデル:
  --pro         gemini-3-pro-preview (デフォルト)
  --flash       gemini-3-flash-preview (高速)

オプション:
  --error       エラー修正モード
  --file PATH   特定ファイル修正モード
  --minecraft   Minecraft特化モード
  --interactive 対話モード
  --yolo, -y    自動承認モード (デフォルト)
  --no-yolo     手動承認モード
  --help        ヘルプ表示

重要: Gemini 3系のみ使用。2.5系は禁止。
`);
      process.exit(0);
    default:
      task.push(args[i]);
  }
}

const taskStr = task.join(' ');

if (!taskStr && mode !== 'interactive') {
  console.error('エラー: タスクを指定してください');
  console.error('使い方: node scripts/gemini-helper.js "タスク"');
  process.exit(1);
}

// プロンプト構築（改行なし - Gemini CLIの非対話モード互換）
const today = new Date().toISOString().split('T')[0];
let prompt = `今日は${today}です。プロジェクト: IF MineAI (Minecraft Plugin, Java 21, Paper API 1.21.11)。`;

switch (mode) {
  case 'error':
    prompt += `以下のエラーを調査・修正してください: ${taskStr}`;
    break;
  case 'file':
    prompt += `ファイル ${targetFile} について: ${taskStr}`;
    break;
  case 'minecraft':
    prompt += `Minecraft Plugin開発に関する調査: ${taskStr}`;
    break;
  default:
    prompt += taskStr;
}

// Gemini CLI 実行
console.log(`[Gemini Helper] モデル: ${model}`);
console.log(`[Gemini Helper] モード: ${mode}, YOLO: ${yolo}`);
console.log(`[Gemini Helper] タスク: ${taskStr}`);
console.log('---');

const geminiArgs = ['-m', model];
if (yolo) {
  geminiArgs.push('-y');
}

if (mode === 'interactive') {
  const proc = spawn('gemini', geminiArgs, {
    stdio: 'inherit',
    cwd: projectRoot,
    shell: true
  });
  proc.on('close', (code) => process.exit(code));
} else {
  geminiArgs.push(prompt);
  const proc = spawn('gemini', geminiArgs, {
    stdio: 'inherit',
    cwd: projectRoot,
    shell: true
  });
  proc.on('close', (code) => process.exit(code));
}
