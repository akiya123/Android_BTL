# Bổ sung vào các DAO Room hiện có (app/src/main/java/com/fittrack/app/data/local/dao/)

Các DAO hiện chỉ có truy vấn LiveData theo ngày; để sao lưu/khôi phục cần đọc TOÀN BỘ bảng
(không LiveData, chạy trên luồng nền) và xóa/chèn hàng loạt. Thêm các dòng sau vào từng interface.
(Không đổi Entity, không đổi version Room, không cần migration.)

## FoodLogDao
```java
    @Query("SELECT * FROM food_log ORDER BY id ASC")
    List<FoodLog> getAllOnce();

    @Query("DELETE FROM food_log")
    void deleteAll();

    @Insert
    void insertAll(List<FoodLog> items);
```

## WorkoutLogDao
```java
    @Query("SELECT * FROM workout_log ORDER BY id ASC")
    List<WorkoutLog> getAllOnce();

    @Query("DELETE FROM workout_log")
    void deleteAll();

    @Insert
    void insertAll(List<WorkoutLog> items);
```

## WeightEntryDao
```java
    @Query("SELECT * FROM weight_entry ORDER BY id ASC")
    List<WeightEntry> getAllOnce();

    @Query("DELETE FROM weight_entry")
    void deleteAll();

    @Insert
    void insertAll(List<WeightEntry> items);
```

## WaterLogDao
```java
    @Query("SELECT * FROM water_log ORDER BY id ASC")
    List<WaterLog> getAllOnce();

    @Query("DELETE FROM water_log")
    void deleteAll();

    @Insert
    void insertAll(List<WaterLog> items);
```

## UserProfileDao
Đã đủ: dùng `getUserProfileOnce()` và `insertOrUpdate()` có sẵn.
