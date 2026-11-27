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
import com.fittrack.app.data.local.entity.FoodLog;
import com.fittrack.app.data.local.model.DailyTotal;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
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
public final class FoodLogDao_Impl implements FoodLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FoodLog> __insertionAdapterOfFoodLog;

  private final EntityDeletionOrUpdateAdapter<FoodLog> __deletionAdapterOfFoodLog;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public FoodLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFoodLog = new EntityInsertionAdapter<FoodLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `food_log` (`id`,`name`,`calories`,`proteinG`,`carbG`,`fatG`,`mealType`,`date`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final FoodLog entity) {
        statement.bindLong(1, entity.id);
        if (entity.name == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.name);
        }
        statement.bindDouble(3, entity.calories);
        statement.bindDouble(4, entity.proteinG);
        statement.bindDouble(5, entity.carbG);
        statement.bindDouble(6, entity.fatG);
        if (entity.mealType == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.mealType);
        }
        if (entity.date == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.date);
        }
      }
    };
    this.__deletionAdapterOfFoodLog = new EntityDeletionOrUpdateAdapter<FoodLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `food_log` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final FoodLog entity) {
        statement.bindLong(1, entity.id);
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM food_log";
        return _query;
      }
    };
  }

  @Override
  public void insertAll(final List<FoodLog> entries) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfFoodLog.insert(entries);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insert(final FoodLog foodLog) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfFoodLog.insert(foodLog);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final FoodLog foodLog) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfFoodLog.handle(foodLog);
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
  public List<FoodLog> getAllOnce() {
    final String _sql = "SELECT * FROM food_log ORDER BY id";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "calories");
      final int _cursorIndexOfProteinG = CursorUtil.getColumnIndexOrThrow(_cursor, "proteinG");
      final int _cursorIndexOfCarbG = CursorUtil.getColumnIndexOrThrow(_cursor, "carbG");
      final int _cursorIndexOfFatG = CursorUtil.getColumnIndexOrThrow(_cursor, "fatG");
      final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final List<FoodLog> _result = new ArrayList<FoodLog>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final FoodLog _item;
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        final double _tmpCalories;
        _tmpCalories = _cursor.getDouble(_cursorIndexOfCalories);
        final double _tmpProteinG;
        _tmpProteinG = _cursor.getDouble(_cursorIndexOfProteinG);
        final double _tmpCarbG;
        _tmpCarbG = _cursor.getDouble(_cursorIndexOfCarbG);
        final double _tmpFatG;
        _tmpFatG = _cursor.getDouble(_cursorIndexOfFatG);
        final String _tmpMealType;
        if (_cursor.isNull(_cursorIndexOfMealType)) {
          _tmpMealType = null;
        } else {
          _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
        }
        final String _tmpDate;
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _tmpDate = null;
        } else {
          _tmpDate = _cursor.getString(_cursorIndexOfDate);
        }
        _item = new FoodLog(_tmpName,_tmpCalories,_tmpProteinG,_tmpCarbG,_tmpFatG,_tmpMealType,_tmpDate);
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
  public LiveData<List<FoodLog>> getFoodLogsByDate(final String date) {
    final String _sql = "SELECT * FROM food_log WHERE date = ? ORDER BY id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"food_log"}, false, new Callable<List<FoodLog>>() {
      @Override
      @Nullable
      public List<FoodLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "calories");
          final int _cursorIndexOfProteinG = CursorUtil.getColumnIndexOrThrow(_cursor, "proteinG");
          final int _cursorIndexOfCarbG = CursorUtil.getColumnIndexOrThrow(_cursor, "carbG");
          final int _cursorIndexOfFatG = CursorUtil.getColumnIndexOrThrow(_cursor, "fatG");
          final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final List<FoodLog> _result = new ArrayList<FoodLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FoodLog _item;
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final double _tmpCalories;
            _tmpCalories = _cursor.getDouble(_cursorIndexOfCalories);
            final double _tmpProteinG;
            _tmpProteinG = _cursor.getDouble(_cursorIndexOfProteinG);
            final double _tmpCarbG;
            _tmpCarbG = _cursor.getDouble(_cursorIndexOfCarbG);
            final double _tmpFatG;
            _tmpFatG = _cursor.getDouble(_cursorIndexOfFatG);
            final String _tmpMealType;
            if (_cursor.isNull(_cursorIndexOfMealType)) {
              _tmpMealType = null;
            } else {
              _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
            }
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            _item = new FoodLog(_tmpName,_tmpCalories,_tmpProteinG,_tmpCarbG,_tmpFatG,_tmpMealType,_tmpDate);
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
  public LiveData<Double> getTotalCaloriesByDate(final String date) {
    final String _sql = "SELECT COALESCE(SUM(calories), 0) FROM food_log WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"food_log"}, false, new Callable<Double>() {
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
  public LiveData<Double> getTotalProteinByDate(final String date) {
    final String _sql = "SELECT COALESCE(SUM(proteinG), 0) FROM food_log WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"food_log"}, false, new Callable<Double>() {
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
  public LiveData<Double> getTotalCarbByDate(final String date) {
    final String _sql = "SELECT COALESCE(SUM(carbG), 0) FROM food_log WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"food_log"}, false, new Callable<Double>() {
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
  public LiveData<Double> getTotalFatByDate(final String date) {
    final String _sql = "SELECT COALESCE(SUM(fatG), 0) FROM food_log WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"food_log"}, false, new Callable<Double>() {
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
  public int getCount() {
    final String _sql = "SELECT COUNT(*) FROM food_log";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LiveData<List<DailyTotal>> getDailyCaloriesByDates(final List<String> dates) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT date, SUM(calories) as total FROM food_log WHERE date IN (");
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
    return __db.getInvalidationTracker().createLiveData(new String[] {"food_log"}, false, new Callable<List<DailyTotal>>() {
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
