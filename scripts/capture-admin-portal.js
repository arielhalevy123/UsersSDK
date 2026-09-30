/**
 * One-off script: logs in to Admin Portal and saves a screenshot of the logged-in view.
 * Run: node scripts/capture-admin-portal.js
 * Requires: npm install puppeteer (or run with npx puppeteer scripts/capture-admin-portal.js)
 */
const http = require('http');

function fetchLogin() {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify({ email: 'admin@example.com', password: 'admin123' });
    const req = http.request({
      hostname: 'localhost',
      port: 8080,
      path: '/api/auth/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) }
    }, (res) => {
      let data = '';
      res.on('data', (ch) => data += ch);
      res.on('end', () => {
        try { resolve(JSON.parse(data)); } catch (e) { reject(e); }
      });
    });
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

async function main() {
  const puppeteer = require('puppeteer');
  const path = require('path');

  const { token } = await fetchLogin();
  if (!token) throw new Error('Login failed');

  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  await page.setViewport({ width: 1200, height: 800 });
  await page.goto('http://localhost:8080/', { waitUntil: 'networkidle0' });
  await page.evaluate((t) => localStorage.setItem('jwt', t), token);
  await page.evaluate(async () => { if (typeof getAdminInfo === 'function') await getAdminInfo(); });
  await page.waitForSelector('#adminInfo:not(.hidden)', { timeout: 5000 }).catch(() => {});
  await new Promise(r => setTimeout(r, 500));

  const outPath = path.join(__dirname, '..', 'docs', 'screenshots', 'admin-portal-logged-in.png');
  await page.screenshot({ path: outPath, fullPage: true });
  console.log('Saved:', outPath);
  await browser.close();
}

main().catch(err => { console.error(err); process.exit(1); });
