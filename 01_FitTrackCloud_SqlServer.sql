/* =============================================================================
   FitTrack Cloud Save  -  SQL Server 2019+ (chạy được trên Windows và Linux)
   -----------------------------------------------------------------------------
   Mô hình: "Cloud Save" kiểu Soul Knight
     - App Android (Room/SQLite) luôn là bản chính, chạy offline 100%.
     - Người dùng chủ động bấm "Sao lưu lên cloud" -> server lưu 1 SNAPSHOT.
     - Mỗi snapshot = 1 dòng ở Backups + các dòng con (profile, food, workout...).
     - Server chỉ giữ N snapshot mới nhất / người dùng (mặc định 5, cấu hình ở API).
     - Xóa Backups -> tự xóa toàn bộ dòng con nhờ ON DELETE CASCADE.

   Cách chạy:  SSMS / Azure Data Studio: mở file -> Execute
               sqlcmd:  sqlcmd -S localhost -U sa -P "<pass>" -C -i 01_FitTrackCloud_SqlServer.sql
   File an toàn khi chạy lại nhiều lần (idempotent).
   ============================================================================= */

SET NOCOUNT ON;
GO

IF DB_ID(N'FitTrackCloud') IS NULL
BEGIN
    CREATE DATABASE [FitTrackCloud];
END
GO

USE [FitTrackCloud];
GO

/* -----------------------------------------------------------------------------
   1. Users  -  ánh xạ 1-1 với tài khoản Firebase của app (UserProfile.firebaseUid)
      Server KHÔNG lưu mật khẩu: app đăng nhập bằng Firebase Auth, gửi ID token,
      API xác thực token rồi tra/ tạo dòng Users theo FirebaseUid.
   ----------------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.Users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Users
    (
        Id              BIGINT            IDENTITY(1,1) NOT NULL,
        FirebaseUid     NVARCHAR(128)     NOT NULL,
        Email           NVARCHAR(320)     NULL,
        DisplayName     NVARCHAR(200)     NULL,
        CreatedAtUtc    DATETIMEOFFSET(3) NOT NULL
            CONSTRAINT DF_Users_CreatedAtUtc DEFAULT (CONVERT(DATETIMEOFFSET(3), SYSUTCDATETIME())),
        LastBackupAtUtc DATETIMEOFFSET(3) NULL,
        -- Token chống ghi đè đồng thời: 2 thiết bị upload cùng lúc thì 1 bên bị 409
        [RowVersion]    ROWVERSION        NOT NULL,
        CONSTRAINT PK_Users PRIMARY KEY CLUSTERED (Id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_Users_FirebaseUid' AND object_id = OBJECT_ID(N'dbo.Users'))
    CREATE UNIQUE NONCLUSTERED INDEX UX_Users_FirebaseUid ON dbo.Users (FirebaseUid);
GO

/* -----------------------------------------------------------------------------
   2. Backups  -  1 dòng = 1 snapshot (1 lần "Save to cloud")
      Các cột thống kê (Count, LatestWeightKg, LastLogDate...) được tính lúc upload
      để màn hình "so sánh bản local / bản cloud" không phải đếm lại các bảng con.
   ----------------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.Backups', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Backups
    (
        Id                BIGINT            IDENTITY(1,1) NOT NULL,
        UserId            BIGINT            NOT NULL,
        CreatedAtUtc      DATETIMEOFFSET(3) NOT NULL
            CONSTRAINT DF_Backups_CreatedAtUtc DEFAULT (CONVERT(DATETIMEOFFSET(3), SYSUTCDATETIME())),

        -- Thiết bị đã tạo snapshot (hiển thị: "Samsung S23 - 11/11/2026 10:30")
        DeviceId          NVARCHAR(64)      NOT NULL,
        DeviceName        NVARCHAR(100)     NOT NULL,
        AppVersion        NVARCHAR(20)      NOT NULL,
        SchemaVersion     INT               NOT NULL,   -- = version của Room AppDatabase (hiện là 12)
        [Label]           NVARCHAR(100)     NULL,       -- ghi chú tuỳ chọn của người dùng
        IsAuto            BIT               NOT NULL CONSTRAINT DF_Backups_IsAuto DEFAULT (0),

        -- SHA-256 của NỘI DUNG (không gồm id cục bộ) -> phát hiện "không có gì thay đổi"
        ContentHash       CHAR(64)          NOT NULL,

        -- Thống kê nhanh
        HasProfile        BIT               NOT NULL,
        ProfileName       NVARCHAR(200)     NULL,
        LatestWeightKg    REAL              NULL,
        FoodLogCount      INT               NOT NULL,
        WorkoutLogCount   INT               NOT NULL,
        WeightEntryCount  INT               NOT NULL,
        WaterLogCount     INT               NOT NULL,
        FirstLogDate      DATE              NULL,
        LastLogDate       DATE              NULL,

        CONSTRAINT PK_Backups PRIMARY KEY CLUSTERED (Id),
        CONSTRAINT FK_Backups_Users FOREIGN KEY (UserId)
            REFERENCES dbo.Users (Id) ON DELETE CASCADE
    );
END
GO

-- Truy vấn chính: "bản mới nhất / danh sách bản của 1 user"
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Backups_User_Created' AND object_id = OBJECT_ID(N'dbo.Backups'))
    CREATE NONCLUSTERED INDEX IX_Backups_User_Created ON dbo.Backups (UserId, CreatedAtUtc DESC, Id DESC);
GO

/* -----------------------------------------------------------------------------
   3. BackupProfiles  -  tương ứng bảng Room `user_profile` (luôn id = 1 -> tối đa 1 dòng / snapshot)
      Cố ý KHÔNG lưu: firebaseUid (server đã biết qua token) và avatarUri
      (đó là đường dẫn file cục bộ, đổi máy là vô nghĩa).
   ----------------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.BackupProfiles', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.BackupProfiles
    (
        BackupId        BIGINT        NOT NULL,
        Name            NVARCHAR(200) NOT NULL,
        Age             INT           NOT NULL,
        Gender          NVARCHAR(20)  NOT NULL,
        HeightCm        REAL          NOT NULL,
        WeightKg        REAL          NOT NULL,
        ActivityLevel   NVARCHAR(50)  NOT NULL,
        Goal            NVARCHAR(50)  NOT NULL,
        Bmr             FLOAT         NOT NULL,
        Tdee            FLOAT         NOT NULL,
        CalorieGoal     FLOAT         NOT NULL,
        ProteinGoalG    FLOAT         NOT NULL,
        CarbGoalG       FLOAT         NOT NULL,
        FatGoalG        FLOAT         NOT NULL,
        CONSTRAINT PK_BackupProfiles PRIMARY KEY CLUSTERED (BackupId),
        CONSTRAINT FK_BackupProfiles_Backups FOREIGN KEY (BackupId)
            REFERENCES dbo.Backups (Id) ON DELETE CASCADE
    );
END
GO

/* -----------------------------------------------------------------------------
   4. BackupFoodLogs  -  tương ứng Room `food_log`
      LocalId = `id` trong Room, giữ lại để khôi phục đúng thứ tự thêm vào
      (DAO của app sắp xếp theo id DESC).
   ----------------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.BackupFoodLogs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.BackupFoodLogs
    (
        Id         BIGINT        IDENTITY(1,1) NOT NULL,
        BackupId   BIGINT        NOT NULL,
        LocalId    BIGINT        NOT NULL,
        Name       NVARCHAR(200) NOT NULL,
        Calories   FLOAT         NOT NULL,
        ProteinG   FLOAT         NOT NULL,
        CarbG      FLOAT         NOT NULL,
        FatG       FLOAT         NOT NULL,
        MealType   NVARCHAR(20)  NOT NULL,   -- Breakfast | Lunch | Dinner | Snack
        LogDate    DATE          NOT NULL,   -- Room: "date" (yyyy-MM-dd)
        CONSTRAINT PK_BackupFoodLogs PRIMARY KEY CLUSTERED (Id),
        CONSTRAINT FK_BackupFoodLogs_Backups FOREIGN KEY (BackupId)
            REFERENCES dbo.Backups (Id) ON DELETE CASCADE,
        CONSTRAINT CK_BackupFoodLogs_NonNegative
            CHECK (Calories >= 0 AND ProteinG >= 0 AND CarbG >= 0 AND FatG >= 0)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_BackupFoodLogs_Backup_Date' AND object_id = OBJECT_ID(N'dbo.BackupFoodLogs'))
    CREATE NONCLUSTERED INDEX IX_BackupFoodLogs_Backup_Date ON dbo.BackupFoodLogs (BackupId, LogDate);
GO

/* -----------------------------------------------------------------------------
   5. BackupWorkoutLogs  -  tương ứng Room `workout_log`
   ----------------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.BackupWorkoutLogs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.BackupWorkoutLogs
    (
        Id              BIGINT        IDENTITY(1,1) NOT NULL,
        BackupId        BIGINT        NOT NULL,
        LocalId         BIGINT        NOT NULL,
        ExerciseName    NVARCHAR(200) NOT NULL,
        Sets            INT           NOT NULL,
        Reps            INT           NOT NULL,
        WeightKg        REAL          NOT NULL,
        CaloriesBurned  FLOAT         NOT NULL,
        LogDate         DATE          NOT NULL,
        CONSTRAINT PK_BackupWorkoutLogs PRIMARY KEY CLUSTERED (Id),
        CONSTRAINT FK_BackupWorkoutLogs_Backups FOREIGN KEY (BackupId)
            REFERENCES dbo.Backups (Id) ON DELETE CASCADE,
        CONSTRAINT CK_BackupWorkoutLogs_NonNegative
            CHECK (Sets >= 0 AND Reps >= 0 AND WeightKg >= 0 AND CaloriesBurned >= 0)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_BackupWorkoutLogs_Backup_Date' AND object_id = OBJECT_ID(N'dbo.BackupWorkoutLogs'))
    CREATE NONCLUSTERED INDEX IX_BackupWorkoutLogs_Backup_Date ON dbo.BackupWorkoutLogs (BackupId, LogDate);
GO

/* -----------------------------------------------------------------------------
   6. BackupWeightEntries  -  tương ứng Room `weight_entry`
   ----------------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.BackupWeightEntries', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.BackupWeightEntries
    (
        Id        BIGINT IDENTITY(1,1) NOT NULL,
        BackupId  BIGINT NOT NULL,
        LocalId   BIGINT NOT NULL,
        WeightKg  REAL   NOT NULL,
        LogDate   DATE   NOT NULL,
        CONSTRAINT PK_BackupWeightEntries PRIMARY KEY CLUSTERED (Id),
        CONSTRAINT FK_BackupWeightEntries_Backups FOREIGN KEY (BackupId)
            REFERENCES dbo.Backups (Id) ON DELETE CASCADE,
        CONSTRAINT CK_BackupWeightEntries_NonNegative CHECK (WeightKg >= 0)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_BackupWeightEntries_Backup_Date' AND object_id = OBJECT_ID(N'dbo.BackupWeightEntries'))
    CREATE NONCLUSTERED INDEX IX_BackupWeightEntries_Backup_Date ON dbo.BackupWeightEntries (BackupId, LogDate);
GO

/* -----------------------------------------------------------------------------
   7. BackupWaterLogs  -  tương ứng Room `water_log`   (HourOfDay = cột `hour`, 0-23)
   ----------------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.BackupWaterLogs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.BackupWaterLogs
    (
        Id         BIGINT IDENTITY(1,1) NOT NULL,
        BackupId   BIGINT NOT NULL,
        LocalId    BIGINT NOT NULL,
        AmountMl   INT    NOT NULL,
        HourOfDay  INT    NOT NULL,
        LogDate    DATE   NOT NULL,
        CONSTRAINT PK_BackupWaterLogs PRIMARY KEY CLUSTERED (Id),
        CONSTRAINT FK_BackupWaterLogs_Backups FOREIGN KEY (BackupId)
            REFERENCES dbo.Backups (Id) ON DELETE CASCADE,
        CONSTRAINT CK_BackupWaterLogs_Valid CHECK (AmountMl >= 0 AND HourOfDay BETWEEN 0 AND 23)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_BackupWaterLogs_Backup_Date' AND object_id = OBJECT_ID(N'dbo.BackupWaterLogs'))
    CREATE NONCLUSTERED INDEX IX_BackupWaterLogs_Backup_Date ON dbo.BackupWaterLogs (BackupId, LogDate);
GO

/* -----------------------------------------------------------------------------
   8. View thống kê dung lượng cloud theo người dùng (dành cho quản trị / báo cáo)
   ----------------------------------------------------------------------------- */
CREATE OR ALTER VIEW dbo.vw_UserCloudUsage
AS
SELECT
    u.Id                AS UserId,
    u.FirebaseUid,
    u.Email,
    COUNT(b.Id)         AS BackupCount,
    MAX(b.CreatedAtUtc) AS LastBackupAtUtc,
    COALESCE(SUM(b.FoodLogCount + b.WorkoutLogCount + b.WeightEntryCount + b.WaterLogCount), 0)
                        AS TotalRowsStored
FROM dbo.Users AS u
LEFT JOIN dbo.Backups AS b ON b.UserId = u.Id
GROUP BY u.Id, u.FirebaseUid, u.Email;
GO

PRINT N'FitTrackCloud: tạo schema thành công.';
GO
