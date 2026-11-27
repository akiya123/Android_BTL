using FitTrack.Api.Dtos;
using FitTrack.Api.Services;
using Microsoft.AspNetCore.Mvc;

namespace FitTrack.Api.Controllers;

[ApiController]
[Route("api/v1/me")]
public sealed class MeController(ICloudSaveService svc) : ControllerBase
{
    /// <summary>
    /// Thông tin tài khoản cloud + bản sao lưu mới nhất.
    /// Gọi ngay sau khi đăng nhập Firebase để (1) kiểm tra token hợp lệ, (2) biết cloud đã có dữ liệu chưa.
    /// </summary>
    [HttpGet]
    public async Task<ActionResult<MeDto>> Get(CancellationToken ct)
    {
        var user = await svc.GetOrCreateUserAsync(User, ct);
        if (user is null) return Unauthorized();

        return await svc.GetMeAsync(user, ct);
    }

    /// <summary>
    /// Xóa VĨNH VIỄN toàn bộ dữ liệu cloud của tôi (mọi bản sao lưu + dòng Users).
    /// Không ảnh hưởng dữ liệu offline trên máy và tài khoản Firebase.
    /// </summary>
    [HttpDelete]
    public async Task<IActionResult> Delete(CancellationToken ct)
    {
        var user = await svc.GetOrCreateUserAsync(User, ct);
        if (user is null) return Unauthorized();

        await svc.DeleteAccountAsync(user.Id, ct);
        return NoContent();
    }
}
