// Locate the nested squares framing the 25-column by 32-row grid.
export function findCorners({pixels, width, height}, compact=false) {
 const stride = pixels.length / (width * height);
 const gray = new Uint8Array(width * height);
 for (let i = 0, j = 0; i < gray.length; i++, j += stride) {
  gray[i] = (77 * pixels[j] + 150 * pixels[j + 1] + 29 * pixels[j + 2]) >> 8;
 }

 const groups = [];
 const rowStep = Math.max(1, Math.floor(height / 1500));
 for (const threshold of [85, 115, 145]) {
  const dark = (x, y) => gray[y * width + x] < threshold;
  for (let y = 0; y < height; y += rowStep) {
   const runs = [];
   let start = 0;
   let shade = dark(0, y);
   for (let x = 1; x <= width; x++) {
    const next = x < width && dark(x, y);
    if (x < width && next === shade) continue;

    runs.push(x - start);
    if (runs.length > 5) runs.shift();
    start = x;
    const endsInDark = shade;
    shade = next;
    if (!endsInDark || runs.length !== 5) continue;

    // The outer dark bands can be thin on photographs, so estimate the
    // module from the two light bands and the dark center.
    const [outerLeft, left, center, right, outerRight] = runs;
    const unit = (left + center + right) / 5;
    if (unit < 1 || center < 2 * unit || center > 4 * unit) continue;
    if (left < .35 * unit || right < .35 * unit || left > 1.8 * unit || right > 1.8 * unit) continue;
    if (outerLeft < .25 * unit || outerRight < .25 * unit || outerLeft > 2 * unit || outerRight > 2 * unit) continue;

    const cx = x - outerRight - right - center / 2;
    const column = Math.floor(cx);
    let top = y;
    let bottom = y;
    while (top > 0 && dark(column, top - 1)) top--;
    while (bottom < height - 1 && dark(column, bottom + 1)) bottom++;
    const cy = (top + bottom + 1) / 2;
    const verticalUnit = (bottom - top + 1) / 3;
    if (verticalUnit < .8 * unit || verticalUnit > 1.8 * unit) continue;

    const hasShade = (offset, expected) => {
     const row = Math.round(cy + offset * verticalUnit);
     return row >= 0 && row < height && dark(column, row) === expected;
    };
    if (!hasShade(-2, false) || !hasShade(2, false)) continue;
    if (![-1, 1].every(side => [2.7, 3, 3.36].some(offset => hasShade(side * offset, true)))) continue;

    const match = groups.find(p => Math.abs(p.x / p.hits - cx) < unit && Math.abs(p.y / p.hits - cy) < verticalUnit);
    if (match) {
     match.x += cx;
     match.y += cy;
     match.unit += unit;
     match.hits++;
    } else if (groups.length < 1500) {
     groups.push({x: cx, y: cy, unit, hits: 1});
    }
   }
  }
 }

 const candidates = groups.filter(p => p.hits >= 3).map(({x, y, unit, hits}) => ({
  x: x / hits, y: y / hits, unit: unit / hits, hits
 }));
 const targets = [[0, 0], [width, 0], [width, height], [0, height]];
 const ranked = targets.map(([x, y]) => candidates
  .map(p => ({point: p, distance: ((p.x - x) / width) ** 2 + ((p.y - y) / height) ** 2}))
  .sort((a, b) => a.distance - b.distance).slice(0, 16).map(entry => entry.point));

 let best = null;
 let bestScore = -Infinity;
 for (const a of ranked[0]) for (const b of ranked[1]) for (const c of ranked[2]) for (const d of ranked[3]) {
  if (a.x >= b.x || d.x >= c.x || a.y >= d.y || b.y >= c.y) continue;
  const quad = [a, b, c, d];
  if (new Set(quad).size !== 4) continue;
  const scales = quad.map(p => p.unit);
  if (Math.max(...scales) > 1.8 * Math.min(...scales)) continue;

  let area = 0;
  let perimeter = 0;
  for (let i = 0; i < 4; i++) {
   const p = quad[i];
   const q = quad[(i + 1) % 4];
   area += p.x * q.y - q.x * p.y;
   perimeter += Math.hypot(p.x - q.x, p.y - q.y);
  }
  const meanSideInModules = perimeter / scales.reduce((sum, unit) => sum + unit, 0);
  // Marker bands are 4 pixels wide in the source layout. The grid sides
  // span 192 and 248 modules, so an unrelated border typically scores poorly.
  if (meanSideInModules < (compact?30:155) || meanSideInModules > 285) continue;

  const dx = (a.x - b.x + c.x - d.x) / width;
  const dy = (a.y - b.y + c.y - d.y) / height;
  const support = quad.reduce((sum, p) => sum + Math.min(p.hits, 30), 0);
  const score = area / (2 * width * height) - 2 * (dx * dx + dy * dy)
   - .8 * (compact?0:Math.abs(Math.log(meanSideInModules / 220))) + .008 * support;
  if (score > bestScore) {
   best = quad;
   bestScore = score;
  }
 }
 return best?.map(p => [p.x, p.y]) || null;
}
