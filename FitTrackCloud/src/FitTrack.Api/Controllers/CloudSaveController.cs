using FitTrack.Api.Dtos;
using FitTrack.Api.Services;
using Microsoft.AspNetCore.Mvc;

namespace FitTrack.Api.Controllers;

/// <summary>
/// Cloud Save kiểu Soul Knight: app luôn chạy offline, các endpoint này chỉ được gọi
/// khi người dùng CHỦ ĐỘNG bấm "Sao lưu lên cloud" / "Khôi phục từ cloud"
/// (hoặc khi bật tự động sao lưu bằng WorkManager).
/// Mọi endpoint yêu cầu header:  Authorization: Bearer &lt;Firebase ID token&gt;
/// </summary>
[ApiController]
[Route("api/v1/cloud-save")]
public sealed class CloudSaveController(ICloudSaveService svc) : ControllerBase
{
    /// <summary>Danh sách các bản sao lưu (mới nhất trước) - chỉ thông tin tóm tắt.</summary>
    [HttpGet]
    public async Task<ActionResult<List<BackupInfoDto>>> List(CancellationToken ct)
    {
        var user = await svc.GetOrCreateUserAsync(User, ct);
        if (user is null) return Unauthorized();

        return await svc.ListAsync(user.Id, ct);
    }

    /// <summary>Thông tin bản mới nhất - dùng để hiện hộp thoại so sánh "máy này vs cloud". 404 nếu chưa có.</summary>
    [HttpGet("latest")]
    public Task<ActionResult<BackupInfoDto>> GetLatestInfo(CancellationToken ct) => InfoAsync(null, ct);

    [HttpGet("{id:long}")]
    public Task<ActionResult<BackupInfoDto>> GetInfo(long id, CancellationToken ct) => InfoAsync(id, ct);

    /// <summary>Tải toàn bộ dữ liệu bản mới nhất để khôi phục vào Room.</summary>
    [HttpGet("latest/data")]
    public Task<ActionResult<BackupDownloadDto>> GetLatestData(CancellationToken ct) => DataAsync(null, ct);

    [HttpGet("{id:long}/data")]
    public Task<ActionResult<BackupDownloadDto>> GetData(long id, CancellationToken ct) => DataAsync(id, ct);

    /// <summary>
    /// Tải lên 1 snapshot.
    ///   201 Created   - đã tạo bản mới
    ///   200 OK        - dữ liệu y hệt bản cloud mới nhất, không tạo thêm (outcome = "unchanged")
    ///   409 Conflict  - cloud có bản khác mà máy này chưa đồng bộ; body chứa thông tin bản cloud.
    ///                   App hiện hộp thoại cho người dùng chọn rồi gửi lại với force=true nếu muốn ghi đè.
    /// </summary>
    [HttpPost]
    [ProducesResponseType<UploadResponseDto>(StatusCodes.Status201Created)]
    [ProducesResponseType<UploadResponseDto>(StatusCodes.Status200OK)]
    [ProducesResponseType<ConflictResponseDto>(StatusCodes.Status409Conflict)]
    public async Task<IActionResult> Upload([FromBody] UploadSnapshotRequest request, CancellationToken ct)
    {
        var user = await svc.GetOrCreateUserAsync(User, ct);
        if (user is null) return Unauthorized();

        var result = await svc.UploadAsync(user, request, ct);

        switch (result.Outcome)
        {
            case UploadOutcome.Created:
                return CreatedAtAction(nameof(GetInfo), new { id = result.Backup.Id },
                    new UploadResponseDto { Outcome = "created", Backup = result.Backup, PrunedBackups = result.PrunedBackups });

            case UploadOutcome.Unchanged:
                return Ok(new UploadResponseDto { Outcome = "unchanged", Backup = result.Backup, PrunedBackups = 0 });

            default:
                return Conflict(new ConflictResponseDto
                {
                    Code = "CLOUD_HAS_DIFFERENT_DATA",
                    Message = "Cloud đang có dữ liệu khác với bản mà máy này đã đồng bộ. Hãy chọn giữ dữ liệu trên máy hoặc tải bản cloud.",
                    Latest = result.Backup
                });
        }
    }

    /// <summary>Xóa 1 bản sao lưu của chính mình.</summary>
    [HttpDelete("{id:long}")]
    public async Task<IActionResult> Delete(long id, CancellationToken ct)
    {
        var user = await svc.GetOrCreateUserAsync(User, ct);
        if (user is null) return Unauthorized();

        return await svc.DeleteBackupAsync(user.Id, id, ct) ? NoContent() : NotFound();
    }

    // ----------------------------------------------------------------------------------------

    private async Task<ActionResult<BackupInfoDto>> InfoAsync(long? id, CancellationToken ct)
    {
        var user = await svc.GetOrCreateUserAsync(User, ct);
        if (user is null) return Unauthorized();

        var info = await svc.GetInfoAsync(user.Id, id, ct);
        return info is null ? NotFoundCloud() : info;
    }

    private async Task<ActionResult<BackupDownloadDto>> DataAsync(long? id, CancellationToken ct)
    {
        var user = await svc.GetOrCreateUserAsync(User, ct);
        if (user is null) return Unauthorized();

        var data = await svc.GetDataAsync(user.Id, id, ct);
        return data is null ? NotFoundCloud() : data;
    }

    private ObjectResult NotFoundCloud() =>
        Problem(title: "Chưa có bản sao lưu nào trên cloud.", statusCode: StatusCodes.Status404NotFound);
}
