// Renderiza las maquetas HTML generadas por generar_capturas.py a PNG (2x).
// Dependencias (instalar fuera del repo):  npm i @sparticuz/chromium puppeteer-core
// Uso:  node capturar.mjs /ruta/salida
import fs from "node:fs";
import path from "node:path";
import chromium from "@sparticuz/chromium";
import puppeteer from "puppeteer-core";

const htmlDir = process.env.CAPTURAS_HTML || "/tmp/capturas_html";
const outDir = process.argv[2] || "capturas";
fs.mkdirSync(outDir, { recursive: true });

const browser = await puppeteer.launch({
  executablePath: await chromium.executablePath(),
  args: chromium.args,
  headless: true,
});
const page = await browser.newPage();
await page.setViewport({ width: 360, height: 760, deviceScaleFactor: 2 });

for (const f of fs.readdirSync(htmlDir).filter((x) => x.endsWith(".html")).sort()) {
  await page.goto("file://" + path.join(htmlDir, f), { waitUntil: "load" });
  await page.evaluate(() => document.fonts.ready);
  const el = await page.$(".phone");
  const name = f.replace(/\.html$/, ".png");
  await el.screenshot({ path: path.join(outDir, name) });
  console.log("ok", name);
}
await browser.close();
