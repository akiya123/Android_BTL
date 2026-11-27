#!/usr/bin/env bash
# Kiểm tra nhanh API sau khi triển khai (cần curl). Không cần jq.
#
#   API_URL=https://api.ten-mien-cua-ban.com TOKEN="<Firebase ID token>" ./deploy/smoke-test.sh
#
# Lấy Firebase ID token để test (thay API_KEY bằng "current_key" trong app/google-services.json,
# và email/mật khẩu của 1 tài khoản đã đăng ký trong app):
#
#   curl -s "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=API_KEY" \
#     -H 'Content-Type: application/json' \
#     -d '{"email":"you@example.com","password":"******","returnSecureToken":true}'
#   -> copy giá trị "idToken" (sống 1 giờ)
#
# Script tạo 1 bản sao lưu thử (force=true) và xóa nó cuối cùng nếu CLEANUP=1 (mặc định).
set -euo pipefail

: "${API_URL:?Cần đặt API_URL, ví dụ https://api.example.com}"
: "${TOKEN:?Cần đặt TOKEN = Firebase ID token}"
CLEANUP="${CLEANUP:-1}"
API="${API_URL%/}/api/v1"
AUTH=(-H "Authorization: Bearer $TOKEN")
JSON=(-H "Content-Type: application/json")

pass() { echo "  ✔ $*"; }
fail() { echo "  ✘ $*"; exit 1; }
code() { curl -s -o /tmp/smoke.out -w "%{http_code}" "$@"; }

SNAP='{"deviceId":"smoke-test","deviceName":"Smoke Test","appVersion":"0.0","schemaVersion":12,"force":true,
"data":{"profile":{"name":"Smoke","age":30,"gender":"Male","heightCm":175.5,"weightKg":70.2,"activityLevel":"Sedentary","goal":"Maintain","bmr":1700.5,"tdee":2040.6,"calorieGoal":2040.6,"proteinGoalG":153.0,"carbGoalG":204.0,"fatGoalG":68.0},
"foodLogs":[{"localId":1,"name":"Smoke Pho","calories":450,"proteinG":25,"carbG":60,"fatG":15,"mealType":"Lunch","date":"2026-11-10"}],
"workoutLogs":[{"localId":1,"exerciseName":"Squats","sets":3,"reps":20,"weightKg":20,"caloriesBurned":180.5,"date":"2026-11-10"}],
"weightEntries":[{"localId":1,"weightKg":70.2,"date":"2026-11-10"}],
"waterLogs":[{"localId":1,"amountMl":250,"hour":8,"date":"2026-11-10"}]}}'

echo "1) Health (không cần token)"
[ "$(code "$API/health")" = "200" ] && pass "200 - server + database OK" || { cat /tmp/smoke.out; fail "health lỗi (kiểm tra chuỗi kết nối SQL Server)"; }

echo "2) Token hợp lệ?"
c="$(code "${AUTH[@]}" "$API/me")"
[ "$c" = "200" ] && pass "GET /me = 200" || { cat /tmp/smoke.out; fail "GET /me = $c (401 = token sai/hết hạn hoặc sai Firebase:ProjectId)"; }

echo "3) Upload snapshot (force=true)"
c="$(code -X POST "${AUTH[@]}" "${JSON[@]}" -d "$SNAP" "$API/cloud-save")"
{ [ "$c" = "201" ] || [ "$c" = "200" ]; } && pass "POST = $c" || { cat /tmp/smoke.out; fail "POST = $c"; }
ID="$(grep -o '"id":[0-9]*' /tmp/smoke.out | head -1 | cut -d: -f2)"
echo "     backupId = $ID"

echo "4) Upload lại y hệt -> phải là 'unchanged'"
c="$(code -X POST "${AUTH[@]}" "${JSON[@]}" -d "$SNAP" "$API/cloud-save")"
{ [ "$c" = "200" ] && grep -q '"outcome":"unchanged"' /tmp/smoke.out; } && pass "200 unchanged (không tạo bản trùng)" || { cat /tmp/smoke.out; fail "kỳ vọng 200 unchanged, nhận $c"; }

echo "5) Máy 'mới' (baseBackupId=null, force=false) + dữ liệu khác -> phải 409"
OTHER="${SNAP/Smoke Pho/Other Pho}"; OTHER="${OTHER/\"force\":true/\"force\":false}"
c="$(code -X POST "${AUTH[@]}" "${JSON[@]}" -d "$OTHER" "$API/cloud-save")"
[ "$c" = "409" ] && pass "409 Conflict (không bị ghi đè âm thầm)" || { cat /tmp/smoke.out; fail "kỳ vọng 409, nhận $c"; }

echo "6) Tải dữ liệu bản mới nhất để khôi phục"
c="$(code "${AUTH[@]}" "$API/cloud-save/latest/data")"
{ [ "$c" = "200" ] && grep -q 'Smoke Pho' /tmp/smoke.out && grep -q '"date":"2026-11-10"' /tmp/smoke.out; } \
  && pass "200, dữ liệu và ngày khớp" || { cat /tmp/smoke.out; fail "download lỗi ($c)"; }

echo "7) Không token -> 401"
[ "$(code "$API/cloud-save/latest")" = "401" ] && pass "401" || fail "endpoint không được bảo vệ!"

if [ "$CLEANUP" = "1" ] && [ -n "$ID" ]; then
  echo "8) Dọn dẹp: xóa bản thử"
  [ "$(code -X DELETE "${AUTH[@]}" "$API/cloud-save/$ID")" = "204" ] && pass "204" || fail "xóa thất bại"
fi
echo; echo "TẤT CẢ ĐẠT ✔"
