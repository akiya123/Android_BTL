using System.ComponentModel.DataAnnotations;

namespace FitTrack.Api.Dtos;

/*
 * Quy ước JSON: camelCase (mặc định của ASP.NET Core). Ngày dùng chuỗi "yyyy-MM-dd"
 * (đúng định dạng cột `date` trong Room). Thời gian dùng ISO-8601, ví dụ "2026-11-11T03:25:10.123+00:00".
 *
 * Triết lý validate: đây là dịch vụ SAO LƯU, nên chỉ chặn dữ liệu rác/lạm dụng (âm, quá dài,
 * quá nhiều dòng) chứ KHÔNG chặn theo nghiệp vụ (ví dụ danh sách mealType) để sau này app thêm
 * giá trị mới thì bản sao lưu vẫn không bị từ chối.
 */

public static class SnapshotLimits
{
    /// <summary>Số dòng tối đa mỗi bảng trong 1 snapshot (chống lạm dụng).</summary>
    public const int MaxRows = 50_000;
}

// ---------------------------------------------------------------------------------------------
//  DỮ LIỆU SNAPSHOT  (mỗi lớp tương ứng 1 Entity của Room)
// ---------------------------------------------------------------------------------------------

/// <summary>Room: user_profile (bỏ firebaseUid và avatarUri).</summary>
public sealed class ProfileDto
{
    [Required(AllowEmptyStrings = true), StringLength(200)]
    public string Name { get; init; } = "";

    [Range(0, 200)]
    public int Age { get; init; }

    [Required(AllowEmptyStrings = true), StringLength(20)]
    public string Gender { get; init; } = "";

    [Range(0, float.MaxValue)] public float HeightCm { get; init; }
    [Range(0, float.MaxValue)] public float WeightKg { get; init; }

    [Required(AllowEmptyStrings = true), StringLength(50)]
    public string ActivityLevel { get; init; } = "";

    [Required(AllowEmptyStrings = true), StringLength(50)]
    public string Goal { get; init; } = "";

    public double Bmr { get; init; }
    public double Tdee { get; init; }
    public double CalorieGoal { get; init; }
    public double ProteinGoalG { get; init; }
    public double CarbGoalG { get; init; }
    public double FatGoalG { get; init; }
}

/// <summary>Room: food_log</summary>
public sealed class FoodLogDto
{
    /// <summary>`id` trong Room - dùng để giữ đúng thứ tự khi khôi phục.</summary>
    public long LocalId { get; init; }

    [Required(AllowEmptyStrings = true), StringLength(200)]
    public string Name { get; init; } = "";

    [Range(0, double.MaxValue)] public double Calories { get; init; }
    [Range(0, double.MaxValue)] public double ProteinG { get; init; }
    [Range(0, double.MaxValue)] public double CarbG { get; init; }
    [Range(0, double.MaxValue)] public double FatG { get; init; }

    [Required(AllowEmptyStrings = true), StringLength(20)]
    public string MealType { get; init; } = "";

    /// <summary>"yyyy-MM-dd"</summary>
    public required DateOnly Date { get; init; }
}

/// <summary>Room: workout_log</summary>
public sealed class WorkoutLogDto
{
    public long LocalId { get; init; }

    [Required(AllowEmptyStrings = true), StringLength(200)]
    public string ExerciseName { get; init; } = "";

    [Range(0, int.MaxValue)] public int Sets { get; init; }
    [Range(0, int.MaxValue)] public int Reps { get; init; }
    [Range(0, float.MaxValue)] public float WeightKg { get; init; }
    [Range(0, double.MaxValue)] public double CaloriesBurned { get; init; }

    public required DateOnly Date { get; init; }
}

/// <summary>Room: weight_entry</summary>
public sealed class WeightEntryDto
{
    public long LocalId { get; init; }

    [Range(0, float.MaxValue)] public float WeightKg { get; init; }

    public required DateOnly Date { get; init; }
}

/// <summary>Room: water_log</summary>
public sealed class WaterLogDto
{
    public long LocalId { get; init; }

    [Range(0, int.MaxValue)] public int AmountMl { get; init; }

    /// <summary>Giờ trong ngày 0-23 (cột `hour` của Room).</summary>
    [Range(0, 23)] public int Hour { get; init; }

    public required DateOnly Date { get; init; }
}

/// <summary>Toàn bộ nội dung 1 snapshot.</summary>
public sealed class SnapshotData
{
    public ProfileDto? Profile { get; init; }

    [MaxLength(SnapshotLimits.MaxRows)] public List<FoodLogDto> FoodLogs { get; init; } = [];
    [MaxLength(SnapshotLimits.MaxRows)] public List<WorkoutLogDto> WorkoutLogs { get; init; } = [];
    [MaxLength(SnapshotLimits.MaxRows)] public List<WeightEntryDto> WeightEntries { get; init; } = [];
    [MaxLength(SnapshotLimits.MaxRows)] public List<WaterLogDto> WaterLogs { get; init; } = [];
}

// ---------------------------------------------------------------------------------------------
//  REQUEST
// ---------------------------------------------------------------------------------------------

/// <summary>POST /api/v1/cloud-save</summary>
public sealed class UploadSnapshotRequest
{
    /// <summary>UUID do app tự sinh 1 lần và lưu SharedPreferences, để biết bản sao lưu đến từ máy nào.</summary>
    [Required, StringLength(64, MinimumLength = 1)]
    public string DeviceId { get; init; } = "";

    [Required, StringLength(100, MinimumLength = 1)]
    public string DeviceName { get; init; } = "";

    [Required, StringLength(20, MinimumLength = 1)]
    public string AppVersion { get; init; } = "";

    /// <summary>Version của Room AppDatabase (hiện là 12).</summary>
    [Range(1, 100_000)]
    public int SchemaVersion { get; init; }

    [StringLength(100)]
    public string? Label { get; init; }

    /// <summary>true nếu do WorkManager tự sao lưu, false nếu người dùng bấm nút.</summary>
    public bool IsAuto { get; init; }

    /// <summary>
    /// Id bản cloud mà máy này đã đồng bộ lần gần nhất (null = máy này chưa từng đồng bộ).
    /// Nếu khác bản mới nhất trên cloud -> server trả 409 để người dùng chọn.
    /// </summary>
    public long? BaseBackupId { get; init; }

    /// <summary>true = người dùng đã chọn "Ghi đè cloud bằng dữ liệu trên máy này".</summary>
    public bool Force { get; init; }

    [Required]
    public required SnapshotData Data { get; init; }
}

// ---------------------------------------------------------------------------------------------
//  RESPONSE
// ---------------------------------------------------------------------------------------------

/// <summary>Thông tin tóm tắt 1 bản sao lưu - dùng cho màn hình so sánh "máy này vs cloud".</summary>
public sealed class BackupInfoDto
{
    public required long Id { get; init; }
    public required DateTimeOffset CreatedAtUtc { get; init; }
    public required string DeviceId { get; init; }
    public required string DeviceName { get; init; }
    public required string AppVersion { get; init; }
    public required int SchemaVersion { get; init; }
    public required string? Label { get; init; }
    public required bool IsAuto { get; init; }
    public required bool HasProfile { get; init; }
    public required string? ProfileName { get; init; }
    public required float? LatestWeightKg { get; init; }
    public required int FoodLogCount { get; init; }
    public required int WorkoutLogCount { get; init; }
    public required int WeightEntryCount { get; init; }
    public required int WaterLogCount { get; init; }
    public required DateOnly? FirstLogDate { get; init; }
    public required DateOnly? LastLogDate { get; init; }
}

/// <summary>Kết quả upload thành công (201) hoặc không đổi (200).</summary>
public sealed class UploadResponseDto
{
    /// <summary>"created" | "unchanged"</summary>
    public required string Outcome { get; init; }
    public required BackupInfoDto Backup { get; init; }
    public required int PrunedBackups { get; init; }
}

/// <summary>HTTP 409: cloud đã có bản khác với bản máy này đang dựa vào.</summary>
public sealed class ConflictResponseDto
{
    public required string Code { get; init; }
    public required string Message { get; init; }
    public required BackupInfoDto Latest { get; init; }
}

/// <summary>GET /cloud-save/{id}/data - tải về để khôi phục.</summary>
public sealed class BackupDownloadDto
{
    public required BackupInfoDto Info { get; init; }
    public required SnapshotData Data { get; init; }
}

/// <summary>GET /me</summary>
public sealed class MeDto
{
    public required long Id { get; init; }
    public required string? Email { get; init; }
    public required string? DisplayName { get; init; }
    public required DateTimeOffset CreatedAtUtc { get; init; }
    public required int BackupCount { get; init; }
    public required int MaxBackups { get; init; }
    public required BackupInfoDto? Latest { get; init; }
}
