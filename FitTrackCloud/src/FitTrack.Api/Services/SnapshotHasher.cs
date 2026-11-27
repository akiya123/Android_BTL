using System.Globalization;
using System.Security.Cryptography;
using System.Text;
using FitTrack.Api.Dtos;

namespace FitTrack.Api.Services;

/// <summary>
/// Băm NỘI DUNG snapshot để biết "dữ liệu có thay đổi so với bản cloud mới nhất không".
/// Cố ý KHÔNG đưa LocalId vào: sau khi khôi phục sang máy khác, Room sinh lại id từ 1,
/// nhưng nội dung vẫn y hệt nên phải cho ra cùng hash.
/// Thứ tự dòng có ý nghĩa (app luôn gửi theo id tăng dần).
/// </summary>
public static class SnapshotHasher
{
    private const char Sep = '\u001F'; // ký tự phân tách "Unit Separator" - gần như không xuất hiện trong text người dùng nhập

    public static string Compute(SnapshotData d)
    {
        var inv = CultureInfo.InvariantCulture;
        using var sha = IncrementalHash.CreateHash(HashAlgorithmName.SHA256);
        var sb = new StringBuilder(256);

        void Row(char tag, params object[] parts)
        {
            sb.Clear();
            sb.Append(tag);
            foreach (var part in parts)
            {
                sb.Append(Sep);
                sb.Append(Convert.ToString(part, inv));
            }
            sb.Append('\n');
            sha.AppendData(Encoding.UTF8.GetBytes(sb.ToString()));
        }

        static string D(DateOnly date) => date.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);

        if (d.Profile is { } p)
        {
            Row('P', p.Name, p.Age, p.Gender, p.HeightCm, p.WeightKg, p.ActivityLevel, p.Goal,
                p.Bmr, p.Tdee, p.CalorieGoal, p.ProteinGoalG, p.CarbGoalG, p.FatGoalG);
        }

        foreach (var f in d.FoodLogs)
            Row('F', f.Name, f.Calories, f.ProteinG, f.CarbG, f.FatG, f.MealType, D(f.Date));

        foreach (var w in d.WorkoutLogs)
            Row('W', w.ExerciseName, w.Sets, w.Reps, w.WeightKg, w.CaloriesBurned, D(w.Date));

        foreach (var e in d.WeightEntries)
            Row('E', e.WeightKg, D(e.Date));

        foreach (var h in d.WaterLogs)
            Row('H', h.AmountMl, h.Hour, D(h.Date));

        return Convert.ToHexString(sha.GetHashAndReset()); // 64 ký tự HEX (chữ hoa)
    }
}
