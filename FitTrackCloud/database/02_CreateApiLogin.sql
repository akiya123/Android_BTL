/* =============================================================================
   Tạo tài khoản SQL riêng cho API (KHÔNG dùng 'sa' trong chuỗi kết nối!)
   Chỉ cấp quyền đọc/ghi dữ liệu, không cấp quyền sửa cấu trúc bảng.

   Mật khẩu truyền qua biến sqlcmd để không nằm trong file:

     sqlcmd -S localhost -U sa -P "<mật_khẩu_sa>" -C -i 02_CreateApiLogin.sql -v API_PASSWORD="MatKhau#Manh2026"

   (SSMS: bật Query > SQLCMD Mode và thêm dòng  :setvar API_PASSWORD "MatKhau#Manh2026"  ở đầu file)
   Nếu quên truyền -v, sqlcmd sẽ báo lỗi "scripting variable not defined" -> an toàn.
   Mật khẩu phải đạt chính sách SQL Server: >= 8 ký tự, có chữ hoa, chữ thường, số/ký tự đặc biệt.
   ============================================================================= */

USE [master];
GO

IF NOT EXISTS (SELECT 1 FROM sys.sql_logins WHERE name = N'fittrack_api')
BEGIN
    CREATE LOGIN [fittrack_api]
        WITH PASSWORD = N'$(API_PASSWORD)',
             CHECK_POLICY = ON,
             DEFAULT_DATABASE = [FitTrackCloud];
END
GO

USE [FitTrackCloud];
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'fittrack_api')
    CREATE USER [fittrack_api] FOR LOGIN [fittrack_api];
GO

ALTER ROLE db_datareader ADD MEMBER [fittrack_api];
ALTER ROLE db_datawriter ADD MEMBER [fittrack_api];
GO

PRINT N'Đã tạo login fittrack_api (db_datareader + db_datawriter trên FitTrackCloud).';
GO
