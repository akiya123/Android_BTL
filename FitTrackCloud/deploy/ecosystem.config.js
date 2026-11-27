/**
 * pm2 cho FitTrack Cloud API (ASP.NET Core / .NET 10).
 *
 *   pm2 start ecosystem.config.js        # chạy lần đầu
 *   pm2 restart fittrack-api --update-env # sau khi đổi file api.env
 *   pm2 logs fittrack-api                 # xem log (Console của ASP.NET Core)
 *
 * Bí mật (chuỗi kết nối SQL Server...) KHÔNG nằm trong file này mà ở file ngoài thư mục code,
 * chmod 600, đọc ở bên dưới. Tên biến theo quy ước .NET: dấu "__" thay cho ":" trong appsettings.json.
 */
const fs = require('fs');

const ENV_FILE = process.env.FITTRACK_ENV_FILE || '/etc/fittrack-api/api.env';

function loadEnvFile(file) {
  const out = {};
  if (!fs.existsSync(file)) {
    console.warn(`[ecosystem] Không thấy ${file} - API sẽ thiếu chuỗi kết nối DB!`);
    return out;
  }
  for (const raw of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const line = raw.trim();
    if (!line || line.startsWith('#')) continue;
    const i = line.indexOf('=');               // tách ở dấu "=" ĐẦU TIÊN (giá trị có thể chứa "=" và ";")
    if (i < 1) continue;
    let value = line.slice(i + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    out[line.slice(0, i).trim()] = value;
  }
  return out;
}

module.exports = {
  apps: [
    {
      name: 'fittrack-api',
      script: 'dotnet',                         // chạy runtime .NET
      args: 'FitTrack.Api.dll',
      interpreter: 'none',                      // QUAN TRỌNG: không dùng node để chạy
      cwd: '/opt/fittrack-api/current',         // symlink tới bản release hiện tại (xem deploy.sh)

      exec_mode: 'fork',                        // .NET tự đa luồng; KHÔNG dùng cluster của pm2
      instances: 1,

      autorestart: true,
      min_uptime: '10s',                        // chạy chưa đủ 10s mà chết = crash loop
      max_restarts: 10,
      restart_delay: 3000,
      max_memory_restart: '600M',
      kill_timeout: 15000,                      // cho ASP.NET Core thời gian tắt êm (hoàn tất request đang chạy)

      out_file: '/var/log/fittrack-api/out.log',
      error_file: '/var/log/fittrack-api/error.log',
      merge_logs: true,
      time: true,                               // gắn timestamp vào mỗi dòng log

      env: {
        ASPNETCORE_ENVIRONMENT: 'Production',
        ASPNETCORE_URLS: 'http://127.0.0.1:5080', // chỉ nghe localhost; nginx đứng trước và mở ra ngoài
        DOTNET_NOLOGO: '1',
        DOTNET_CLI_TELEMETRY_OPTOUT: '1',
        ...loadEnvFile(ENV_FILE),
      },
    },
  ],
};
