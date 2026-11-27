using System.Linq.Expressions;
using System.Security.Claims;
using FitTrack.Api.Data;
using FitTrack.Api.Dtos;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;

namespace FitTrack.Api.Services;

public sealed class CloudSaveOptions
{
    public const string SectionName = "CloudSave";

    /// <summary>Số snapshot giữ lại cho mỗi người dùng. Upload bản mới -> bản cũ nhất vượt quá số này bị xóa.</summary>
    public int MaxBackupsPerUser { get; set; } = 5;
}

public enum UploadOutcome { Created, Unchanged, Conflict }

public sealed record UploadResult(UploadOutcome Outcome, BackupInfoDto Backup, int PrunedBackups = 0);

public interface ICloudSaveService
{
    Task<AppUser?> GetOrCreateUserAsync(ClaimsPrincipal principal, CancellationToken ct);
    Task<MeDto> GetMeAsync(AppUser user, CancellationToken ct);
    Task<List<BackupInfoDto>> ListAsync(long userId, CancellationToken ct);
    Task<BackupInfoDto?> GetInfoAsync(long userId, long? backupId, CancellationToken ct);
    Task<BackupDownloadDto?> GetDataAsync(long userId, long? backupId, CancellationToken ct);
    Task<UploadResult> UploadAsync(AppUser user, UploadSnapshotRequest request, CancellationToken ct);
    Task<bool> DeleteBackupAsync(long userId, long backupId, CancellationToken ct);
    Task DeleteAccountAsync(long userId, CancellationToken ct);
}

public sealed class CloudSaveService(
    FitTrackDbContext db,
    IOptions<CloudSaveOptions> options,
    ILogger<CloudSaveService> logger) : ICloudSaveService
{
    private readonly int _maxBackups = Math.Max(1, options.Value.MaxBackupsPerUser);

    /// <summary>
    /// Phép chiếu dùng chung: EF dịch thành SELECT chỉ các cột của bảng Backups,
    /// KHÔNG kéo theo các bảng con (food/workout/...) nên rất nhẹ.
    /// </summary>
    private static readonly Expression<Func<Backup, BackupInfoDto>> ToInfo = b => new BackupInfoDto
    {
        Id = b.Id,
        CreatedAtUtc = b.CreatedAtUtc,
        DeviceId = b.DeviceId,
        DeviceName = b.DeviceName,
        AppVersion = b.AppVersion,
        SchemaVersion = b.SchemaVersion,
        Label = b.Label,
        IsAuto = b.IsAuto,
        HasProfile = b.HasProfile,
        ProfileName = b.ProfileName,
        LatestWeightKg = b.LatestWeightKg,
        FoodLogCount = b.FoodLogCount,
        WorkoutLogCount = b.WorkoutLogCount,
        WeightEntryCount = b.WeightEntryCount,
        WaterLogCount = b.WaterLogCount,
        FirstLogDate = b.FirstLogDate,
        LastLogDate = b.LastLogDate
    };

    // ------------------------------------------------------------------------------------------
    //  USER
    // ------------------------------------------------------------------------------------------

    public async Task<AppUser?> GetOrCreateUserAsync(ClaimsPrincipal principal, CancellationToken ct)
    {
        // Firebase ID token: claim "user_id" và "sub" đều là UID. MapInboundClaims=false nên tên claim giữ nguyên.
        var uid = principal.FindFirst("user_id")?.Value ?? principal.FindFirst("sub")?.Value;
        if (string.IsNullOrWhiteSpace(uid) || uid.Length > 128) return null;

        var user = await db.Users.FirstOrDefaultAsync(u => u.FirebaseUid == uid, ct);
        if (user is not null) return user;

        user = new AppUser
        {
            FirebaseUid = uid,
            Email = Truncate(principal.FindFirst("email")?.Value, 320),
            DisplayName = Truncate(principal.FindFirst("name")?.Value, 200),
            CreatedAtUtc = DateTimeOffset.UtcNow
        };
        db.Users.Add(user);

        try
        {
            await db.SaveChangesAsync(ct);
            logger.LogInformation("Tạo mới user cloud {UserId}", user.Id);
            return user;
        }
        catch (DbUpdateException)
        {
            // 2 request đầu tiên của cùng 1 người chạy song song -> unique index UX_Users_FirebaseUid chặn 1 bên.
            db.ChangeTracker.Clear();
            return await db.Users.FirstOrDefaultAsync(u => u.FirebaseUid == uid, ct);
        }
    }

    public async Task<MeDto> GetMeAsync(AppUser user, CancellationToken ct)
    {
        var count = await db.Backups.CountAsync(b => b.UserId == user.Id, ct);
        var latest = await GetInfoAsync(user.Id, null, ct);

        return new MeDto
        {
            Id = user.Id,
            Email = user.Email,
            DisplayName = user.DisplayName,
            CreatedAtUtc = user.CreatedAtUtc,
            BackupCount = count,
            MaxBackups = _maxBackups,
            Latest = latest
        };
    }

    // ------------------------------------------------------------------------------------------
    //  ĐỌC
    // ------------------------------------------------------------------------------------------

    public Task<List<BackupInfoDto>> ListAsync(long userId, CancellationToken ct) =>
        db.Backups.AsNoTracking()
            .Where(b => b.UserId == userId)
            .OrderByDescending(b => b.CreatedAtUtc).ThenByDescending(b => b.Id)
            .Select(ToInfo)
            .ToListAsync(ct);

    /// <param name="backupId">null = bản mới nhất.</param>
    public Task<BackupInfoDto?> GetInfoAsync(long userId, long? backupId, CancellationToken ct)
    {
        IQueryable<Backup> q = db.Backups.AsNoTracking().Where(b => b.UserId == userId);

        q = backupId is { } id
            ? q.Where(b => b.Id == id)
            : q.OrderByDescending(b => b.CreatedAtUtc).ThenByDescending(b => b.Id);

        return q.Select(ToInfo).FirstOrDefaultAsync(ct);
    }

    public async Task<BackupDownloadDto?> GetDataAsync(long userId, long? backupId, CancellationToken ct)
    {
        var info = await GetInfoAsync(userId, backupId, ct);
        if (info is null) return null;
        var id = info.Id;

        // Mỗi bảng 1 truy vấn riêng (KHÔNG dùng Include lồng nhau để tránh "cartesian explosion").
        // Truy vấn chạy tuần tự vì 1 DbContext không cho chạy song song.
        var profile = await db.BackupProfiles.AsNoTracking()
            .Where(p => p.BackupId == id)
            .Select(p => new ProfileDto
            {
                Name = p.Name, Age = p.Age, Gender = p.Gender,
                HeightCm = p.HeightCm, WeightKg = p.WeightKg,
                ActivityLevel = p.ActivityLevel, Goal = p.Goal,
                Bmr = p.Bmr, Tdee = p.Tdee, CalorieGoal = p.CalorieGoal,
                ProteinGoalG = p.ProteinGoalG, CarbGoalG = p.CarbGoalG, FatGoalG = p.FatGoalG
            })
            .FirstOrDefaultAsync(ct);

        // Sắp theo LocalId để khi insert lại vào Room, thứ tự "mới/cũ" giống hệt máy gốc.
        var foods = await db.BackupFoodLogs.AsNoTracking()
            .Where(x => x.BackupId == id)
            .OrderBy(x => x.LocalId).ThenBy(x => x.Id)
            .Select(x => new FoodLogDto
            {
                LocalId = x.LocalId, Name = x.Name, Calories = x.Calories,
                ProteinG = x.ProteinG, CarbG = x.CarbG, FatG = x.FatG,
                MealType = x.MealType, Date = x.LogDate
            })
            .ToListAsync(ct);

        var workouts = await db.BackupWorkoutLogs.AsNoTracking()
            .Where(x => x.BackupId == id)
            .OrderBy(x => x.LocalId).ThenBy(x => x.Id)
            .Select(x => new WorkoutLogDto
            {
                LocalId = x.LocalId, ExerciseName = x.ExerciseName, Sets = x.Sets, Reps = x.Reps,
                WeightKg = x.WeightKg, CaloriesBurned = x.CaloriesBurned, Date = x.LogDate
            })
            .ToListAsync(ct);

        var weights = await db.BackupWeightEntries.AsNoTracking()
            .Where(x => x.BackupId == id)
            .OrderBy(x => x.LocalId).ThenBy(x => x.Id)
            .Select(x => new WeightEntryDto { LocalId = x.LocalId, WeightKg = x.WeightKg, Date = x.LogDate })
            .ToListAsync(ct);

        var waters = await db.BackupWaterLogs.AsNoTracking()
            .Where(x => x.BackupId == id)
            .OrderBy(x => x.LocalId).ThenBy(x => x.Id)
            .Select(x => new WaterLogDto { LocalId = x.LocalId, AmountMl = x.AmountMl, Hour = x.HourOfDay, Date = x.LogDate })
            .ToListAsync(ct);

        return new BackupDownloadDto
        {
            Info = info,
            Data = new SnapshotData
            {
                Profile = profile,
                FoodLogs = foods,
                WorkoutLogs = workouts,
                WeightEntries = weights,
                WaterLogs = waters
            }
        };
    }

    // ------------------------------------------------------------------------------------------
    //  GHI: upload snapshot
    // ------------------------------------------------------------------------------------------

    public async Task<UploadResult> UploadAsync(AppUser user, UploadSnapshotRequest req, CancellationToken ct)
    {
        var hash = SnapshotHasher.Compute(req.Data);

        // Chỉ lấy 2 cột cần thiết của bản mới nhất.
        var latest = await db.Backups.AsNoTracking()
            .Where(b => b.UserId == user.Id)
            .OrderByDescending(b => b.CreatedAtUtc).ThenByDescending(b => b.Id)
            .Select(b => new { b.Id, b.ContentHash })
            .FirstOrDefaultAsync(ct);

        // (1) Dữ liệu y hệt bản cloud mới nhất -> không tạo thêm, chỉ báo để app cập nhật baseBackupId.
        if (latest is not null && latest.ContentHash == hash)
        {
            var same = await GetInfoAsync(user.Id, latest.Id, ct);
            return new UploadResult(UploadOutcome.Unchanged, same!);
        }

        // (2) Cloud đang có bản mà máy này chưa từng đồng bộ (máy khác đã upload / máy mới cài lại)
        //     -> KHÔNG tự ghi đè, trả 409 để người dùng chọn: giữ máy này hay lấy bản cloud.
        if (latest is not null && !req.Force && req.BaseBackupId != latest.Id)
            return await ConflictAsync(user.Id, ct);

        // (3) Tạo snapshot mới.
        var now = DateTimeOffset.UtcNow;
        var backup = BuildBackup(user.Id, req, hash, now);
        db.Backups.Add(backup);

        // Sửa 1 cột của Users để EF sinh "UPDATE Users ... WHERE Id=@id AND RowVersion=@rv".
        // Nếu giữa chừng có request khác (máy khác) vừa upload xong -> RowVersion đã đổi -> 0 dòng bị ảnh hưởng
        // -> DbUpdateConcurrencyException -> trả 409. Toàn bộ nằm trong 1 transaction nên không bao giờ lưu dở.
        user.LastBackupAtUtc = now;

        try
        {
            await db.SaveChangesAsync(ct);
        }
        catch (DbUpdateConcurrencyException)
        {
            db.ChangeTracker.Clear();
            return await ConflictAsync(user.Id, ct);
        }

        var pruned = await PruneOldBackupsAsync(user.Id, ct);
        var info = await GetInfoAsync(user.Id, backup.Id, ct);

        logger.LogInformation(
            "User {UserId} upload backup {BackupId}: food={Food} workout={Workout} weight={Weight} water={Water}, pruned={Pruned}",
            user.Id, backup.Id, backup.FoodLogCount, backup.WorkoutLogCount, backup.WeightEntryCount, backup.WaterLogCount, pruned);

        return new UploadResult(UploadOutcome.Created, info!, pruned);
    }

    private async Task<UploadResult> ConflictAsync(long userId, CancellationToken ct)
    {
        var latest = await GetInfoAsync(userId, null, ct)
                     ?? throw new InvalidOperationException("Không tìm thấy bản sao lưu mới nhất để báo xung đột.");
        return new UploadResult(UploadOutcome.Conflict, latest);
    }

    /// <summary>Giữ N bản mới nhất, xóa phần còn lại (dòng con tự xóa nhờ ON DELETE CASCADE trong SQL).</summary>
    private async Task<int> PruneOldBackupsAsync(long userId, CancellationToken ct)
    {
        var staleIds = await db.Backups.AsNoTracking()
            .Where(b => b.UserId == userId)
            .OrderByDescending(b => b.CreatedAtUtc).ThenByDescending(b => b.Id)
            .Skip(_maxBackups)
            .Select(b => b.Id)
            .ToListAsync(ct);

        if (staleIds.Count == 0) return 0;

        return await db.Backups
            .Where(b => staleIds.Contains(b.Id))
            .ExecuteDeleteAsync(ct);
    }

    private static Backup BuildBackup(long userId, UploadSnapshotRequest req, string hash, DateTimeOffset now)
    {
        var d = req.Data;

        // LINQ-to-Objects: gom ngày của cả 4 bảng để biết khoảng thời gian dữ liệu bao phủ.
        // Min()/Max() trên DateOnly? trả về null nếu rỗng.
        var allDates = d.FoodLogs.Select(x => (DateOnly?)x.Date)
            .Concat(d.WorkoutLogs.Select(x => (DateOnly?)x.Date))
            .Concat(d.WeightEntries.Select(x => (DateOnly?)x.Date))
            .Concat(d.WaterLogs.Select(x => (DateOnly?)x.Date))
            .ToList();

        // Cân nặng mới nhất = bản ghi có ngày lớn nhất (cùng ngày thì lấy id lớn hơn).
        var latestWeight = d.WeightEntries
            .OrderByDescending(w => w.Date).ThenByDescending(w => w.LocalId)
            .Select(w => (float?)w.WeightKg)
            .FirstOrDefault();

        return new Backup
        {
            UserId = userId,
            CreatedAtUtc = now,
            DeviceId = req.DeviceId.Trim(),
            DeviceName = req.DeviceName.Trim(),
            AppVersion = req.AppVersion.Trim(),
            SchemaVersion = req.SchemaVersion,
            Label = string.IsNullOrWhiteSpace(req.Label) ? null : req.Label.Trim(),
            IsAuto = req.IsAuto,
            ContentHash = hash,

            HasProfile = d.Profile is not null,
            ProfileName = d.Profile?.Name,
            LatestWeightKg = latestWeight,
            FoodLogCount = d.FoodLogs.Count,
            WorkoutLogCount = d.WorkoutLogs.Count,
            WeightEntryCount = d.WeightEntries.Count,
            WaterLogCount = d.WaterLogs.Count,
            FirstLogDate = allDates.Min(),
            LastLogDate = allDates.Max(),

            Profile = d.Profile is null ? null : new BackupProfile
            {
                Name = d.Profile.Name, Age = d.Profile.Age, Gender = d.Profile.Gender,
                HeightCm = d.Profile.HeightCm, WeightKg = d.Profile.WeightKg,
                ActivityLevel = d.Profile.ActivityLevel, Goal = d.Profile.Goal,
                Bmr = d.Profile.Bmr, Tdee = d.Profile.Tdee, CalorieGoal = d.Profile.CalorieGoal,
                ProteinGoalG = d.Profile.ProteinGoalG, CarbGoalG = d.Profile.CarbGoalG, FatGoalG = d.Profile.FatGoalG
            },

            FoodLogs = d.FoodLogs.Select(x => new BackupFoodLog
            {
                LocalId = x.LocalId, Name = x.Name, Calories = x.Calories,
                ProteinG = x.ProteinG, CarbG = x.CarbG, FatG = x.FatG,
                MealType = x.MealType, LogDate = x.Date
            }).ToList(),

            WorkoutLogs = d.WorkoutLogs.Select(x => new BackupWorkoutLog
            {
                LocalId = x.LocalId, ExerciseName = x.ExerciseName, Sets = x.Sets, Reps = x.Reps,
                WeightKg = x.WeightKg, CaloriesBurned = x.CaloriesBurned, LogDate = x.Date
            }).ToList(),

            WeightEntries = d.WeightEntries.Select(x => new BackupWeightEntry
            {
                LocalId = x.LocalId, WeightKg = x.WeightKg, LogDate = x.Date
            }).ToList(),

            WaterLogs = d.WaterLogs.Select(x => new BackupWaterLog
            {
                LocalId = x.LocalId, AmountMl = x.AmountMl, HourOfDay = x.Hour, LogDate = x.Date
            }).ToList()
        };
    }

    // ------------------------------------------------------------------------------------------
    //  XÓA
    // ------------------------------------------------------------------------------------------

    public async Task<bool> DeleteBackupAsync(long userId, long backupId, CancellationToken ct)
    {
        // Điều kiện UserId bảo đảm người dùng chỉ xóa được bản của chính mình.
        var deleted = await db.Backups
            .Where(b => b.UserId == userId && b.Id == backupId)
            .ExecuteDeleteAsync(ct);
        return deleted > 0;
    }

    public async Task DeleteAccountAsync(long userId, CancellationToken ct)
    {
        // Xóa Users -> cascade: Backups -> BackupProfiles / FoodLogs / WorkoutLogs / WeightEntries / WaterLogs
        await db.Users.Where(u => u.Id == userId).ExecuteDeleteAsync(ct);
        logger.LogInformation("Đã xóa toàn bộ dữ liệu cloud của user {UserId}", userId);
    }

    private static string? Truncate(string? s, int max) =>
        string.IsNullOrEmpty(s) ? null : (s.Length <= max ? s : s[..max]);
}
