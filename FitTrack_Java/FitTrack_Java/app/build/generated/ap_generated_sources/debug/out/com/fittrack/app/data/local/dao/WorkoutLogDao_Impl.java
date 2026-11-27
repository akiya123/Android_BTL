package com.fittrack.app.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.fittrack.app.data.local.entity.WorkoutLog;
import com.fittrack.app.data.local.model.DailyTotal;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WorkoutLogDao_Impl implements WorkoutLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WorkoutLog> __insertionAdapterOfWorkoutLog;

  private final EntityDeletionOrUpdateAdapter<WorkoutLog> __deletionAdapterOfWorkoutLog;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public WorkoutLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWorkoutLog = new EntityInsertionAdapter<WorkoutLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `workout_log` (`id`,`exerciseName`,`sets`,`reps`,`weightKg`,`caloriesBurned`,`date`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final WorkoutLog entity) {
        statement.bindLong(1, entity.id);
        if (entity.exerciseName == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.exerciseName);
        }
        statement.bindLong(3, entity.sets);
        statement.bindLong(4, entity.reps);
        statement.bindDouble(5, entity.weightKg);
        statement.bindDouble(6, entity.caloriesBurned);
        if (entity.date == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.date);
        }
      }
    };
    this.__deletionAdapterOfWorkoutLog = new EntityDeletionOrUpdateAdapter<WorkoutLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `workout_log` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final WorkoutLog entity) {
        statement.bindLong(1, entity.id);
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM workout_log";
        return _query;
      }
    };
  }

  @Override
  public void insertAll(final List<WorkoutLog> entries) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfWorkoutLog.insert(entries);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insert(final WorkoutLog workoutLog) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfWorkoutLog.insert(workoutLog);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final WorkoutLog workoutLog) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfWorkoutLog.handle(workoutLog);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteAll() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteAll.release(_stmt);
    }
  }

  @Override
  public List<WorkoutLog> getAllOnce() {
    final String _sql = "SELECT * FROM workout_log ORDER BY id";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfExerciseName = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseName");
      final int _cursorIndexOfSets = CursorUtil.getColumnIndexOrThrow(_cursor, "sets");
      final int _cursorIndexOfReps = CursorUtil.getColumnIndexOrThrow(_cursor, "reps");
      final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
      final int _cursorIndexOfCaloriesBurned = CursorUtil.getColumnIndexOrThrow(_cursor, "caloriesBurned");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final List<WorkoutLog> _result = new ArrayList<WorkoutLog>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final WorkoutLog _item;
        final String _tmpExerciseName;
        if (_cursor.isNull(_cursorIndexOfExerciseName)) {
          _tmpExerciseName = null;
        } else {
          _tmpExerciseName = _cursor.getString(_cursorIndexOfExerciseName);
        }
        final int _tmpSets;
        _tmpSets = _cursor.getInt(_cursorIndexOfSets);
        final int _tmpReps;
        _tmpReps = _cursor.getInt(_cursorIndexOfReps);
        final float _tmpWeightKg;
        _tmpWeightKg = _cursor.getFloat(_cursorIndexOfWeightKg);
        final double _tmpCaloriesBurned;
        _tmpCaloriesBurned = _cursor.getDouble(_cursorIndexOfCaloriesBurned);
        final String _tmpDate;
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _tmpDate = null;
        } else {
          _tmpDate = _cursor.getString(_cursorIndexOfDate);
        }
        _item = new WorkoutLog(_tmpExerciseName,_tmpSets,_tmpReps,_tmpWeightKg,_tmpCaloriesBurned,_tmpDate);
        _item.id = _cursor.getLong(_cursorIndexOfId);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LiveData<List<WorkoutLog>> getWorkoutLogsByDate(final String date) {
    final String _sql = "SELECT * FROM workout_log WHERE date = ? ORDER BY id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"workout_log"}, false, new Callable<List<WorkoutLog>>() {
      @Override
      @Nullable
      public List<WorkoutLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfExerciseName = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseName");
          final int _cursorIndexOfSets = CursorUtil.getColumnIndexOrThrow(_cursor, "sets");
          final int _cursorIndexOfReps = CursorUtil.getColumnIndexOrThrow(_cursor, "reps");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfCaloriesBurned = CursorUtil.getColumnIndexOrThrow(_cursor, "caloriesBurned");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final List<WorkoutLog> _result = new ArrayList<WorkoutLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkoutLog _item;
            final String _tmpExerciseName;
            if (_cursor.isNull(_cursorIndexOfExerciseName)) {
              _tmpExerciseName = null;
            } else {
              _tmpExerciseName = _cursor.getString(_cursorIndexOfExerciseName);
            }
            final int _tmpSets;
            _tmpSets = _cursor.getInt(_cursorIndexOfSets);
            final int _tmpReps;
            _tmpReps = _cursor.getInt(_cursorIndexOfReps);
            final float _tmpWeightKg;
            _tmpWeightKg = _cursor.getFloat(_cursorIndexOfWeightKg);
            final double _tmpCaloriesBurned;
            _tmpCaloriesBurned = _cursor.getDouble(_cursorIndexOfCaloriesBurned);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            _item = new WorkoutLog(_tmpExerciseName,_tmpSets,_tmpReps,_tmpWeightKg,_tmpCaloriesBurned,_tmpDate);
            _item.id = _cursor.getLong(_cursorIndexOfId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public LiveData<Integer> getWorkoutCountByDate(final String date) {
    final String _sql = "SELECT COUNT(*) FROM workout_log WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"workout_log"}, false, new Callable<Integer>() {
      @Override
      @Nullable
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public LiveData<Double> getTotalCaloriesBurnedByDate(final String date) {
    final String _sql = "SELECT COALESCE(SUM(caloriesBurned), 0) FROM workout_log WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"workout_log"}, false, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public LiveData<List<DailyTotal>> getDailyBurnedByDates(final List<String> dates) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT date, SUM(caloriesBurned) as total FROM workout_log WHERE date IN (");
    final int _inputSize = dates == null ? 1 : dates.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(") GROUP BY date");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    if (dates == null) {
      _statement.bindNull(_argIndex);
    } else {
      for (String _item : dates) {
        if (_item == null) {
          _statement.bindNull(_argIndex);
        } else {
          _statement.bindString(_argIndex, _item);
        }
        _argIndex++;
      }
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"workout_log"}, false, new Callable<List<DailyTotal>>() {
      @Override
      @Nullable
      public List<DailyTotal> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = 0;
          final int _cursorIndexOfTotal = 1;
          final List<DailyTotal> _result = new ArrayList<DailyTotal>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyTotal _item_1;
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final double _tmpTotal;
            _tmpTotal = _cursor.getDouble(_cursorIndexOfTotal);
            _item_1 = new DailyTotal(_tmpDate,_tmpTotal);
            _result.add(_item_1);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
