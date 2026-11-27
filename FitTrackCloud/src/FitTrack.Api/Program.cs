using System.Threading.RateLimiting;
using FitTrack.Api.Data;
using FitTrack.Api.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.HttpOverrides;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;

var builder = WebApplication.CreateBuilder(args);

// ---------------------------------------------------------------------------------------------
//  Cấu hình
// ---------------------------------------------------------------------------------------------
var connectionString = builder.Configuration.GetConnectionString("Default")
    ?? throw new InvalidOperationException("Thiếu ConnectionStrings:Default");

var firebaseProjectId = builder.Configuration["Firebase:ProjectId"]
    ?? throw new InvalidOperationException("Thiếu Firebase:ProjectId (lấy trong google-services.json -> project_id)");

builder.Services.Configure<CloudSaveOptions>(builder.Configuration.GetSection(CloudSaveOptions.SectionName));

// Snapshot có thể vài MB -> cho phép body tối đa 20 MB (nhớ chỉnh client_max_body_size của nginx tương ứng).
builder.WebHost.ConfigureKestrel(k => k.Limits.MaxRequestBodySize = 20 * 1024 * 1024);

// ---------------------------------------------------------------------------------------------
//  Database (EF Core + SQL Server)
// ---------------------------------------------------------------------------------------------
builder.Services.AddDbContext<FitTrackDbContext>(options =>
    options.UseSqlServer(connectionString, sql => sql.EnableRetryOnFailure(maxRetryCount: 3)));

builder.Services.AddScoped<ICloudSaveService, CloudSaveService>();

// ---------------------------------------------------------------------------------------------
//  Xác thực: kiểm tra Firebase ID token (app đã đăng nhập bằng Firebase Auth)
//  - Chữ ký được kiểm bằng khóa công khai Google (tự tải & tự xoay khóa qua OpenID metadata)
//  - Server không lưu mật khẩu, không cần service-account
// ---------------------------------------------------------------------------------------------
var firebaseAuthority = $"https://securetoken.google.com/{firebaseProjectId}";

builder.Services
    .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.Authority = firebaseAuthority;
        options.MapInboundClaims = false; // giữ nguyên tên claim: "sub", "user_id", "email", "name"
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = firebaseAuthority,
            ValidateAudience = true,
            ValidAudience = firebaseProjectId,
            ValidateLifetime = true,
            ClockSkew = TimeSpan.FromMinutes(2)
        };
    });

// Mặc định MỌI endpoint đều phải đăng nhập; endpoint công khai phải ghi rõ [AllowAnonymous] (an toàn mặc định).
builder.Services.AddAuthorizationBuilder()
    .SetFallbackPolicy(new AuthorizationPolicyBuilder().RequireAuthenticatedUser().Build());

// ---------------------------------------------------------------------------------------------
//  Giới hạn tần suất: 60 request/phút cho mỗi người dùng (chưa đăng nhập thì tính theo IP)
// ---------------------------------------------------------------------------------------------
builder.Services.AddRateLimiter(options =>
{
    options.RejectionStatusCode = StatusCodes.Status429TooManyRequests;
    options.GlobalLimiter = PartitionedRateLimiter.Create<HttpContext, string>(ctx =>
    {
        var key = ctx.User.FindFirst("sub")?.Value
                  ?? ctx.Connection.RemoteIpAddress?.ToString()
                  ?? "anonymous";

        return RateLimitPartition.GetFixedWindowLimiter(key, _ => new FixedWindowRateLimiterOptions
        {
            PermitLimit = 60,
            Window = TimeSpan.FromMinutes(1),
            QueueLimit = 0
        });
    });
});

// Chạy sau nginx: đọc IP/giao thức thật từ X-Forwarded-* (mặc định chỉ tin proxy nằm trên cùng máy).
builder.Services.Configure<ForwardedHeadersOptions>(o =>
    o.ForwardedHeaders = ForwardedHeaders.XForwardedFor | ForwardedHeaders.XForwardedProto);

builder.Services.AddControllers();
builder.Services.AddProblemDetails();

// ---------------------------------------------------------------------------------------------
//  Pipeline
// ---------------------------------------------------------------------------------------------
var app = builder.Build();

app.UseForwardedHeaders();
app.UseExceptionHandler();   // lỗi không lường trước -> 500 ProblemDetails, không lộ stack trace
app.UseAuthentication();
app.UseRateLimiter();        // đặt SAU UseAuthentication để biết user nào đang gọi
app.UseAuthorization();
app.MapControllers();

app.Run();
