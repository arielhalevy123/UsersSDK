/**
 * Captures Admin Portal login page and dashboard screenshots.
 * Run: node scripts/capture-admin-screenshots.js
 * Requires: backend running on localhost:8080, npm install puppeteer
 */
const http = require('http');
const path = require('path');

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
  const baseDir = path.join(__dirname, '..', 'docs', 'screenshots');

  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  await page.setViewport({ width: 1200, height: 900 });

  // 1. Login page screenshot
  await page.goto('http://localhost:8080/', { waitUntil: 'networkidle0' });
  await page.screenshot({
    path: path.join(baseDir, 'admin-portal-login.png'),
    fullPage: true
  });
  console.log('Saved: docs/screenshots/admin-portal-login.png');

  // 2. Login and show dashboard
  const { token, user } = await fetchLogin();
  if (!token || !user) throw new Error('Login failed');

  await page.evaluate((t) => localStorage.setItem('jwt', t), token);
  await page.evaluate(async (u) => {
    if (typeof showAdminInfoFromUser === 'function') showAdminInfoFromUser(u);
    if (u && u.role === 'ADMIN' && typeof loadUsersManagedByAdmin === 'function') {
      await loadUsersManagedByAdmin();
    }
  }, user);

  await page.waitForSelector('#userListSection', { timeout: 8000 }).catch(() => {});
  await new Promise(r => setTimeout(r, 800));

  // 3. Dashboard screenshot
  await page.screenshot({
    path: path.join(baseDir, 'admin-portal-dashboard.png'),
    fullPage: true
  });
  console.log('Saved: docs/screenshots/admin-portal-dashboard.png');

  await browser.close();
}

main().catch(err => { console.error(err); process.exit(1); });
