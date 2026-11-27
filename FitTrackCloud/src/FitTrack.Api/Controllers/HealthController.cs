using FitTrack.Api.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace FitTrack.Api.Controllers;

[ApiController]
[AllowAnonymous]
[Route("api/v1/health")]
public sealed class HealthController(FitTrackDbContext db) : ControllerBase
{
    /// <summary>Không cần đăng nhập. Dùng cho pm2/nginx/uptime monitor và để app kiểm tra "server có online không".</summary>
    [HttpGet]
    public async Task<IActionResult> Get(CancellationToken ct)
    {
        var dbOk = await db.Database.CanConnectAsync(ct);
        var body = new { status = dbOk ? "ok" : "degraded", database = dbOk, utc = DateTimeOffset.UtcNow };
        return dbOk ? Ok(body) : StatusCode(StatusCodes.Status503ServiceUnavailable, body);
    }
}
