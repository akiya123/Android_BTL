namespace FitTrack.Api.Data;

/// <summary>Bảng Users - tài khoản Firebase đã từng dùng cloud.</summary>
public class AppUser
{
    public long Id { get; set; }
    public string FirebaseUid { get; set; } = "";
    public string? Email { get; set; }
    public string? DisplayName { get; set; }
    public DateTimeOffset CreatedAtUtc { get; set; }
    public DateTimeOffset? LastBackupAtUtc { get; set; }

    /// <summary>Concurrency token (ROWVERSION). EF tự đưa vào mệnh đề WHERE của UPDATE.</summary>
    public byte[] RowVersion { get; set; } = [];

    public List<Backup> Backups { get; set; } = [];
}

/// <summary>Bảng Backups - 1 dòng = 1 snapshot do người dùng bấm "Save to cloud".</summary>
public class Backup
{
    public long Id { get; set; }
    public long UserId { get; set; }
    public AppUser User { get; set; } = null!;
    public DateTimeOffset CreatedAtUtc { get; set; }

    public string DeviceId { get; set; } = "";
    public string DeviceName { get; set; } = "";
    public string AppVersion { get; set; } = "";
    public int SchemaVersion { get; set; }
    public string? Label { get; set; }
    public bool IsAuto { get; set; }

    public string ContentHash { get; set; } = "";

    public bool HasProfile { get; set; }
    public string? ProfileName { get; set; }
    public float? LatestWeightKg { get; set; }
    public int FoodLogCount { get; set; }
    public int WorkoutLogCount { get; set; }
    public int WeightEntryCount { get; set; }
    public int WaterLogCount { get; set; }
    public DateOnly? FirstLogDate { get; set; }
    public DateOnly? LastLogDate { get; set; }

    public BackupProfile? Profile { get; set; }
    public List<BackupFoodLog> FoodLogs { get; set; } = [];
    public List<BackupWorkoutLog> WorkoutLogs { get; set; } = [];
    public List<BackupWeightEntry> WeightEntries { get; set; } = [];
    public List<BackupWaterLog> WaterLogs { get; set; } = [];
}

/// <summary>Room: user_profile</summary>
public class BackupProfile
{
    public long BackupId { get; set; }
    public Backup Backup { get; set; } = null!;

    public string Name { get; set; } = "";
    public int Age { get; set; }
    public string Gender { get; set; } = "";
    public float HeightCm { get; set; }
    public float WeightKg { get; set; }
    public string ActivityLevel { get; set; } = "";
    public string Goal { get; set; } = "";
    public double Bmr { get; set; }
    public double Tdee { get; set; }
    public double CalorieGoal { get; set; }
    public double ProteinGoalG { get; set; }
    public double CarbGoalG { get; set; }
    public double FatGoalG { get; set; }
}

/// <summary>Room: food_log</summary>
public class BackupFoodLog
{
    public long Id { get; set; }
    public long BackupId { get; set; }
    public Backup Backup { get; set; } = null!;

    public long LocalId { get; set; }
    public string Name { get; set; } = "";
    public double Calories { get; set; }
    public double ProteinG { get; set; }
    public double CarbG { get; set; }
    public double FatG { get; set; }
    public string MealType { get; set; } = "";
    public DateOnly LogDate { get; set; }
}

/// <summary>Room: workout_log</summary>
public class BackupWorkoutLog
{
    public long Id { get; set; }
    public long BackupId { get; set; }
    public Backup Backup { get; set; } = null!;

    public long LocalId { get; set; }
    public string ExerciseName { get; set; } = "";
    public int Sets { get; set; }
    public int Reps { get; set; }
    public float WeightKg { get; set; }
    public double CaloriesBurned { get; set; }
    public DateOnly LogDate { get; set; }
}

/// <summary>Room: weight_entry</summary>
public class BackupWeightEntry
{
    public long Id { get; set; }
    public long BackupId { get; set; }
    public Backup Backup { get; set; } = null!;

    public long LocalId { get; set; }
    public float WeightKg { get; set; }
    public DateOnly LogDate { get; set; }
}

/// <summary>Room: water_log</summary>
public class BackupWaterLog
{
    public long Id { get; set; }
    public long BackupId { get; set; }
    public Backup Backup { get; set; } = null!;

    public long LocalId { get; set; }
    public int AmountMl { get; set; }
    public int HourOfDay { get; set; }
    public DateOnly LogDate { get; set; }
}
