const http = require('http');
const fs = require('fs');
const path = require('path');

process.on('uncaughtException', (err) => {
  console.error('Unhandled server exception caught:', err);
});

process.on('unhandledRejection', (reason) => {
  console.error('Unhandled server rejection caught:', reason);
});

const ZIP_PATH = path.join(__dirname, 'public', 'infinix_gt50pro_charge_boost.zip');
const APK_PATH = fs.existsSync(path.join(__dirname, 'public', 'GT_50_Pro_Charge_Mod.apk')) 
  ? path.join(__dirname, 'public', 'GT_50_Pro_Charge_Mod.apk')
  : (fs.existsSync(path.join(__dirname, '.build-outputs', 'app-debug.apk'))
      ? path.join(__dirname, '.build-outputs', 'app-debug.apk')
      : path.join(__dirname, 'public', 'app.apk'));
const PORT = 3000;

const server = http.createServer((req, res) => {
  req.on('error', (err) => console.error('Req error:', err));
  res.on('error', (err) => console.error('Res error:', err));

  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = url.pathname;

  // Serve Magisk / KernelSU Flashable Module ZIP
  if (pathname === '/infinix_gt50pro_charge_boost.zip' || pathname === '/module.zip' || pathname === '/gt50pro-module.zip') {
    if (!fs.existsSync(ZIP_PATH)) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      return res.end('Module ZIP not found on server.');
    }

    const stat = fs.statSync(ZIP_PATH);
    res.writeHead(200, {
      'Content-Length': stat.size,
      'Content-Type': 'application/zip',
      'Content-Disposition': 'attachment; filename="infinix_gt50pro_charge_boost.zip"',
      'Accept-Ranges': 'bytes',
      'Cache-Control': 'no-cache'
    });
    const file = fs.createReadStream(ZIP_PATH);
    file.pipe(res);
    file.on('error', () => res.end());
    return;
  }

  // Serve APK
  if (pathname === '/app.apk' || pathname === '/download' || pathname === '/download.apk' || pathname === '/ONE_STI_Made_by_MJ.apk' || pathname === '/GT_50_Pro_Charge_Mod.apk') {
    const targetApk = fs.existsSync(APK_PATH) ? APK_PATH : path.join(__dirname, 'public', 'app.apk');
    if (!fs.existsSync(targetApk)) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      return res.end('APK not found on server.');
    }

    const stat = fs.statSync(targetApk);
    res.writeHead(200, {
      'Content-Length': stat.size,
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Disposition': 'attachment; filename="GT_50_Pro_Charge_Mod.apk"',
      'Accept-Ranges': 'bytes',
      'Cache-Control': 'no-cache'
    });
    const file = fs.createReadStream(targetApk);
    file.pipe(res);
    file.on('error', () => res.end());
    return;
  }

  let zipSizeKB = '4 KB';
  try {
    if (fs.existsSync(ZIP_PATH)) {
      zipSizeKB = (fs.statSync(ZIP_PATH).size / 1024).toFixed(1) + ' KB';
    }
  } catch(e) {}

  const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Infinix GT 50 Pro Charging Speed Module</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
    body {
      background: radial-gradient(circle at top, #1A1F2C 0%, #090B10 100%);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 18px;
      color: #fff;
    }
    .card {
      background: #11141D;
      border: 1px solid #283144;
      color: #f1f5f9;
      width: 100%;
      max-width: 440px;
      border-radius: 28px;
      padding: 32px 24px;
      text-align: center;
      box-shadow: 0 25px 60px rgba(0, 0, 0, 0.7);
    }
    .badge {
      display: inline-block;
      background: rgba(255, 87, 34, 0.15);
      color: #FF5722;
      border: 1px solid #FF5722;
      font-weight: 800;
      font-size: 11px;
      padding: 5px 14px;
      border-radius: 9999px;
      text-transform: uppercase;
      letter-spacing: 1px;
      margin-bottom: 16px;
    }
    .icon-badge {
      width: 80px;
      height: 80px;
      background: linear-gradient(135deg, #FF5722, #FF8A65);
      border-radius: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 16px;
      font-size: 38px;
      box-shadow: 0 10px 25px rgba(255, 87, 34, 0.4);
    }
    h1 {
      font-size: 24px;
      font-weight: 800;
      color: #fff;
      margin-bottom: 6px;
      letter-spacing: -0.5px;
    }
    .subtitle {
      color: #94A3B8;
      font-size: 14px;
      margin-bottom: 24px;
      line-height: 1.4;
    }
    .specs-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
      margin-bottom: 24px;
      text-align: left;
    }
    .spec-item {
      background: #191F2C;
      border: 1px solid #283144;
      padding: 12px;
      border-radius: 14px;
    }
    .spec-label {
      font-size: 11px;
      color: #94A3B8;
      text-transform: uppercase;
    }
    .spec-val {
      font-size: 15px;
      font-weight: 700;
      color: #00E5FF;
      margin-top: 4px;
    }
    .btn-zip {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      width: 100%;
      background: linear-gradient(135deg, #FF5722 0%, #E64A19 100%);
      color: #ffffff;
      font-size: 17px;
      font-weight: 700;
      padding: 16px 20px;
      border-radius: 16px;
      text-decoration: none;
      box-shadow: 0 10px 25px rgba(255, 87, 34, 0.35);
      margin-bottom: 12px;
      transition: transform 0.15s;
    }
    .btn-zip:active {
      transform: scale(0.98);
    }
    .btn-apk {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      width: 100%;
      background: #1E2532;
      border: 1px solid #283144;
      color: #00E5FF;
      font-size: 15px;
      font-weight: 600;
      padding: 14px 18px;
      border-radius: 14px;
      text-decoration: none;
      margin-bottom: 20px;
    }
    .guide-box {
      background: #161B26;
      border: 1px solid #283144;
      border-radius: 16px;
      padding: 16px;
      text-align: left;
      font-size: 13px;
      color: #cbd5e1;
      line-height: 1.5;
    }
    .guide-box h4 {
      color: #FF5722;
      font-size: 13px;
      margin-bottom: 8px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .guide-box ol {
      margin-left: 20px;
    }
    .guide-box li {
      margin-bottom: 6px;
    }
  </style>
</head>
<body>
  <div class="card">
    <div class="badge">Infinix GT HyperCharge Engine</div>
    <div class="icon-badge">⚡</div>
    <h1>GT 50 Pro Charge Mod</h1>
    <div class="subtitle">Transforms <strong>Infinix Hot 60 Pro</strong> charging algorithms to <strong>Infinix GT 50 Pro</strong> speeds.</div>

    <div class="specs-grid">
      <div class="spec-item">
        <div class="spec-label">Target Max Current</div>
        <div class="spec-val">12,000 mA</div>
      </div>
      <div class="spec-item">
        <div class="spec-label">Bypass Charging</div>
        <div class="spec-val">Enabled (Direct SoC)</div>
      </div>
      <div class="spec-item">
        <div class="spec-label">Thermal Throttle</div>
        <div class="spec-val">Bypassed</div>
      </div>
      <div class="spec-item">
        <div class="spec-label">Protocol Spoof</div>
        <div class="spec-val">GT 50 Pro (X6711)</div>
      </div>
    </div>

    <a href="/infinix_gt50pro_charge_boost.zip" class="btn-zip" download="infinix_gt50pro_charge_boost.zip">
      ⚡ Download Magisk/KSU Module (${zipSizeKB})
    </a>

    <a href="/app.apk" class="btn-apk" download="GT_50_Pro_Charge_Mod.apk">
      📱 Download Companion APK
    </a>

    <div class="guide-box">
      <h4>⚡ How to Flash in Magisk / KernelSU:</h4>
      <ol>
        <li>Download the <strong>infinix_gt50pro_charge_boost.zip</strong> above.</li>
        <li>Open <strong>Magisk</strong> or <strong>KernelSU</strong> app.</li>
        <li>Go to <strong>Modules</strong> &rarr; tap <strong>Install from storage</strong>.</li>
        <li>Select <code>infinix_gt50pro_charge_boost.zip</code> and wait for flash.</li>
        <li>Reboot your Infinix phone and enjoy GT 50 Pro charging!</li>
      </ol>
    </div>
  </div>
</body>
</html>`;

  res.writeHead(200, {
    'Content-Type': 'text/html; charset=utf-8',
    'Content-Length': Buffer.byteLength(html)
  });
  res.end(html);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`GT 50 Pro Charge Server running on port ${PORT}`);
});
