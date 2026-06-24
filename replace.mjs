import { promises as fs } from 'fs';

async function replaceInFile(filePath) {
  let content = await fs.readFile(filePath, 'utf8');
  content = content.replace(/د\.إ/g, 'ج.س');
  content = content.replace(/د\.ع/g, 'ج.س');
  content = content.replace(/بالدينار العراقي/g, 'بالجنيه السوداني');
  content = content.replace(/بالدينار/g, 'بالجنيه السوداني');
  await fs.writeFile(filePath, content, 'utf8');
  console.log(`Updated ${filePath}`);
}

async function run() {
  await replaceInFile('app/src/main/java/com/example/MainActivity.kt');
  await replaceInFile('app/src/main/java/com/example/ui/IncomeScreenView.kt');
  await replaceInFile('app/src/main/java/com/example/ui/ExpenseScreenView.kt');
}

run();
