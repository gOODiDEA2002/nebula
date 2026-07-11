import { chromium } from 'playwright'

const frontendUrl = process.argv[2] || 'http://localhost:3000'
const screenshotPath = process.argv[3] || 'websocket-ui-e2e.png'
const marker = 'nebula_e2e_ui_' + Date.now()
const consoleErrors = []

const browser = await chromium.launch({ channel: process.env.PLAYWRIGHT_CHANNEL || 'chrome', headless: true })
try {
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
  page.on('console', message => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })
  page.on('pageerror', error => consoleErrors.push(error.message))

  await page.goto(frontendUrl, { waitUntil: 'networkidle' })
  await page.getByPlaceholder('Enter nickname').fill(marker)
  await page.getByRole('button', { name: 'Connect' }).click()
  await page.getByText('Connected', { exact: true }).waitFor()
  await page.waitForFunction(() => document.querySelector('.info code')?.textContent?.trim() !== '-')

  await page.getByPlaceholder('Type a message...').fill(marker + '_message')
  await page.getByRole('button', { name: 'Send' }).click()
  await page.locator('.message .text', { hasText: marker + '_message' }).waitFor()
  await page.screenshot({ path: screenshotPath, fullPage: true })

  if (consoleErrors.length > 0) {
    throw new Error('浏览器控制台错误: ' + consoleErrors.join(' | '))
  }
  console.log(JSON.stringify({ connected: true, messageDelivered: true, screenshotPath }))
} finally {
  await browser.close()
}
