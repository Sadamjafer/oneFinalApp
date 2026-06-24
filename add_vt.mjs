import { promises as fs } from 'fs';

async function addVisualTransformation(filePath) {
  let content = await fs.readFile(filePath, 'utf8');
  const lines = content.split('\n');
  const newLines = [];
  
  for (let i = 0; i < lines.length; i++) {
    newLines.push(lines[i]);
    if (lines[i].includes('keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)')) {
      // Check if visualTransformation is already added
      if (i + 1 < lines.length && !lines[i+1].includes('visualTransformation')) {
          // get the same indentation
          const match = lines[i].match(/^(\s*)/);
          const indent = match ? match[1] : '';
          newLines.push(`${indent}visualTransformation = NumberCommaTransformation(),`);
      }
    }
  }
  
  let newContent = newLines.join('\n');
  await fs.writeFile(filePath, newContent, 'utf8');
  console.log(`Updated ${filePath}`);
}

async function run() {
  await addVisualTransformation('app/src/main/java/com/example/ui/IncomeScreenView.kt');
  await addVisualTransformation('app/src/main/java/com/example/ui/ExpenseScreenView.kt');
}

run();
