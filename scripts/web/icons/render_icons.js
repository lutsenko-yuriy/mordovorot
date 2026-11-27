#!/usr/bin/env node
// GH-357: renders the app icons (web/src/jsMain/resources/icons/*.png) from palette.css, so the
// icons and the board share their colours. Run by hand when the art or the palette changes; CI
// never runs it (the PNGs are committed, and AppIconsTest fails when they no longer match the
// palette). Needs Playwright's Chromium: `node scripts/web/icons/render_icons.js`.
//
// The art is the board in miniature: the frame colour holding 3x3 tiles coloured as board.css
// `.tile` does (hue row * 120 + 8, saturation 68%, lightness --tile-light - col * --tile-ramp / 2),
// the middle row shifted half a tile to the right and wrapping, clipped as `.tiles` clips mid-drag.
// The fractions in VARIANTS are repeated in AppIconsTest.
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const root = path.resolve(__dirname, '..', '..', '..');
const resources = path.join(root, 'web', 'src', 'jsMain', 'resources');
const outDir = path.join(resources, 'icons');

const palette = fs.readFileSync(path.join(resources, 'palette.css'), 'utf8').split('@media')[0];
const token = (name) => {
  const m = palette.match(new RegExp(`--${name}:\\s*([^;]+);`));
  if (!m) throw new Error(`palette.css has no --${name}`);
  return m[1].trim();
};
const frame = token('frame');
const tileLight = parseFloat(token('tile-light'));
const tileRamp = parseFloat(token('tile-ramp'));

// grid: the share of the icon the 3x3 board takes; radius: the frame's corner radius as a share
// of the icon (0 = full bleed, for icons a platform masks itself).
const VARIANTS = [
  { file: 'icon-192.png', size: 192, grid: 0.9, radius: 0.18 },
  { file: 'icon-512.png', size: 512, grid: 0.9, radius: 0.18 },
  { file: 'favicon-32.png', size: 32, grid: 0.9, radius: 0.18 },
  { file: 'apple-touch-icon.png', size: 180, grid: 0.72, radius: 0 },
  // The maskable safe zone is the circle of 80% of the icon; the board's corners sit inside it.
  { file: 'icon-maskable-512.png', size: 512, grid: 0.56, radius: 0 },
];

function svg({ size, grid, radius }) {
  const g = size * grid;
  const off = (size - g) / 2;
  const pitch = g / 3;
  const gap = pitch * 0.1;
  const tile = pitch - gap;
  const tiles = [];
  const put = (row, col, slot) => {
    const light = tileLight - (col * tileRamp) / 2;
    const x = off + slot * pitch + gap / 2;
    const y = off + row * pitch + gap / 2;
    tiles.push(`<rect x="${x}" y="${y}" width="${tile}" height="${tile}" rx="${tile * 0.16}" fill="hsl(${row * 120 + 8} 68% ${light}%)"/>`);
  };
  for (let row = 0; row < 3; row++) {
    for (let col = 0; col < 3; col++) put(row, col, row === 1 ? col + 0.5 : col);
  }
  put(1, 2, -0.5); // the wrapped tile, half back in on the left
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
<defs><clipPath id="box"><rect x="${off}" y="${off}" width="${g}" height="${g}" rx="${tile * 0.2}"/></clipPath></defs>
<rect width="${size}" height="${size}" rx="${size * radius}" fill="${frame}"/>
<g clip-path="url(#box)">${tiles.join('')}</g>
</svg>`;
}

(async () => {
  fs.mkdirSync(outDir, { recursive: true });
  const browser = await chromium.launch();
  const context = await browser.newContext({ deviceScaleFactor: 1 });
  const page = await context.newPage();
  for (const v of VARIANTS) {
    await page.setViewportSize({ width: v.size, height: v.size });
    await page.setContent(`<!doctype html><body style="margin:0;background:transparent">${svg(v)}</body>`);
    await page.screenshot({ path: path.join(outDir, v.file), omitBackground: true, clip: { x: 0, y: 0, width: v.size, height: v.size } });
    console.log(`wrote ${v.file}`);
  }
  await browser.close();
})();
