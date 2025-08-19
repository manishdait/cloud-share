const fs = require('fs');

const envConfig = fs.readFileSync('.env', 'utf-8').split('\n');
const config = {};

envConfig.forEach(line => {
  const [key, value] = line.split('=');
  if (key && value) {
    config[key.trim()] = value.trim();
  }
});

const envFileContent = `export const env = {
  RAZORPAY_API_KEY: '${config.RAZORPAY_API_KEY}'
}`;

fs.writeFileSync('src/environments/env.ts', envFileContent);
