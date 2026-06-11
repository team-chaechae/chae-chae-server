import { createReadStream, existsSync, statSync } from 'node:fs';
import { createServer } from 'node:http';
import { extname, join, normalize } from 'node:path';

const port = Number(process.env.PORT || 3000);
const distDir = join(process.cwd(), 'dist');

const contentTypes = new Map([
  ['.css', 'text/css; charset=utf-8'],
  ['.html', 'text/html; charset=utf-8'],
  ['.js', 'text/javascript; charset=utf-8'],
  ['.json', 'application/json; charset=utf-8'],
  ['.png', 'image/png'],
  ['.svg', 'image/svg+xml'],
  ['.ico', 'image/x-icon'],
  ['.woff', 'font/woff'],
  ['.woff2', 'font/woff2'],
]);

function resolveFile(urlPath) {
  const safePath = normalize(decodeURIComponent(urlPath.split('?')[0])).replace(/^(\.\.[/\\])+/, '');
  const requestedPath = join(distDir, safePath);

  if (existsSync(requestedPath) && statSync(requestedPath).isFile()) {
    return requestedPath;
  }

  return join(distDir, 'index.html');
}

createServer((request, response) => {
  const filePath = resolveFile(request.url || '/');
  const contentType = contentTypes.get(extname(filePath)) || 'application/octet-stream';

  response.writeHead(200, {
    'Cache-Control': filePath.endsWith('index.html') ? 'no-cache' : 'public, max-age=31536000, immutable',
    'Content-Type': contentType,
  });

  createReadStream(filePath).pipe(response);
}).listen(port, '0.0.0.0', () => {
  console.log(`frontend listening on ${port}`);
});

