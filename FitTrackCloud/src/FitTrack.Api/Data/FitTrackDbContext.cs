using Microsoft.EntityFrameworkCore;

namespace FitTrack.Api.Data;

/// <summary>
/// Database-first: cấu trúc bảng do file SQL (database/01_FitTrackCloud_SqlServer.sql) quyết định.
/// Lớp này chỉ khai báo ánh xạ để LINQ biết tên bảng/cột. Không dùng EF Migrations.
/// </summary>
public class FitTrackDbContext(DbContextOptions<FitTrackDbContext> options) : DbContext(options)
{
    public DbSet<AppUser> Users => Set<AppUser>();
    public DbSet<Backup> Backups => Set<Backup>();
    public DbSet<BackupProfile> BackupProfiles => Set<BackupProfile>();
    public DbSet<BackupFoodLog> BackupFoodLogs => Set<BackupFoodLog>();
    public DbSet<BackupWorkoutLog> BackupWorkoutLogs => Set<BackupWorkoutLog>();
    public DbSet<BackupWeightEntry> BackupWeightEntries => Set<BackupWeightEntry>();
    public DbSet<BackupWaterLog> BackupWaterLogs => Set<BackupWaterLog>();

    protected override void OnModelCreating(ModelBuilder mb)
    {
        mb.Entity<AppUser>(e =>
        {
            e.ToTable("Users");
            e.HasKey(x => x.Id);
            e.Property(x => x.FirebaseUid).HasMaxLength(128).IsRequired();
            e.Property(x => x.Email).HasMaxLength(320);
            e.Property(x => x.DisplayName).HasMaxLength(200);
            e.Property(x => x.CreatedAtUtc).HasPrecision(3);
            e.Property(x => x.LastBackupAtUtc).HasPrecision(3);
            e.Property(x => x.RowVersion).IsRowVersion();
            e.HasIndex(x => x.FirebaseUid).IsUnique();
        });

        mb.Entity<Backup>(e =>
        {
            e.ToTable("Backups");
            e.HasKey(x => x.Id);
            e.Property(x => x.CreatedAtUtc).HasPrecision(3);
            e.Property(x => x.DeviceId).HasMaxLength(64).IsRequired();
            e.Property(x => x.DeviceName).HasMaxLength(100).IsRequired();
            e.Property(x => x.AppVersion).HasMaxLength(20).IsRequired();
            e.Property(x => x.Label).HasMaxLength(100);
            e.Property(x => x.ContentHash).HasMaxLength(64).IsFixedLength().IsUnicode(false).IsRequired();
            e.Property(x => x.ProfileName).HasMaxLength(200);

            e.HasOne(x => x.User).WithMany(u => u.Backups)
                .HasForeignKey(x => x.UserId).OnDelete(DeleteBehavior.Cascade);
        });

        mb.Entity<BackupProfile>(e =>
        {
            e.ToTable("BackupProfiles");
            e.HasKey(x => x.BackupId);
            e.Property(x => x.BackupId).ValueGeneratedNever();
            e.Property(x => x.Name).HasMaxLength(200).IsRequired();
            e.Property(x => x.Gender).HasMaxLength(20).IsRequired();
            e.Property(x => x.ActivityLevel).HasMaxLength(50).IsRequired();
            e.Property(x => x.Goal).HasMaxLength(50).IsRequired();

            e.HasOne(x => x.Backup).WithOne(b => b.Profile)
                .HasForeignKey<BackupProfile>(x => x.BackupId).OnDelete(DeleteBehavior.Cascade);
        });

        mb.Entity<BackupFoodLog>(e =>
        {
            e.ToTable("BackupFoodLogs");
            e.HasKey(x => x.Id);
            e.Property(x => x.Name).HasMaxLength(200).IsRequired();
            e.Property(x => x.MealType).HasMaxLength(20).IsRequired();
            e.HasOne(x => x.Backup).WithMany(b => b.FoodLogs)
                .HasForeignKey(x => x.BackupId).OnDelete(DeleteBehavior.Cascade);
        });

        mb.Entity<BackupWorkoutLog>(e =>
        {
            e.ToTable("BackupWorkoutLogs");
            e.HasKey(x => x.Id);
            e.Property(x => x.ExerciseName).HasMaxLength(200).IsRequired();
            e.HasOne(x => x.Backup).WithMany(b => b.WorkoutLogs)
                .HasForeignKey(x => x.BackupId).OnDelete(DeleteBehavior.Cascade);
        });

        mb.Entity<BackupWeightEntry>(e =>
        {
            e.ToTable("BackupWeightEntries");
            e.HasKey(x => x.Id);
            e.HasOne(x => x.Backup).WithMany(b => b.WeightEntries)
                .HasForeignKey(x => x.BackupId).OnDelete(DeleteBehavior.Cascade);
        });

        mb.Entity<BackupWaterLog>(e =>
        {
            e.ToTable("BackupWaterLogs");
            e.HasKey(x => x.Id);
            e.HasOne(x => x.Backup).WithMany(b => b.WaterLogs)
                .HasForeignKey(x => x.BackupId).OnDelete(DeleteBehavior.Cascade);
        });
    }
}
