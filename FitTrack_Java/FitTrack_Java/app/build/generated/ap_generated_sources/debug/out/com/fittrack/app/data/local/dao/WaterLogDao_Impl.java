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
import androidx.sqlite.db.SupportSQLiteStatement;
import com.fittrack.app.data.local.entity.WaterLog;
import com.fittrack.app.data.local.model.HourlyTotal;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WaterLogDao_Impl implements WaterLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WaterLog> __insertionAdapterOfWaterLog;

  private final EntityDeletionOrUpdateAdapter<WaterLog> __deletionAdapterOfWaterLog;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  private final SharedSQLiteStatement __preparedStmtOfClearDay;

  public WaterLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWaterLog = new EntityInsertionAdapter<WaterLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `water_log` (`id`,`amountMl`,`hour`,`date`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final WaterLog entity) {
        statement.bindLong(1, entity.id);
        statement.bindLong(2, entity.amountMl);
        statement.bindLong(3, entity.hour);
        if (entity.date == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.date);
        }
      }
    };
    this.__deletionAdapterOfWaterLog = new EntityDeletionOrUpdateAdapter<WaterLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `water_log` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final WaterLog entity) {
        statement.bindLong(1, entity.id);
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM water_log";
        return _query;
      }
    };
    this.__preparedStmtOfClearDay = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM water_log WHERE date = ?";
        return _query;
      }
    };
  }

  @Override
  public void insertAll(final List<WaterLog> entries) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfWaterLog.insert(entries);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insert(final WaterLog waterLog) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfWaterLog.insert(waterLog);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final WaterLog waterLog) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfWaterLog.handle(waterLog);
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
  public void clearDay(final String date) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfClearDay.acquire();
    int _argIndex = 1;
    if (date == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, date);
    }
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfClearDay.release(_stmt);
    }
  }

  @Override
  public List<WaterLog> getAllOnce() {
    final String _sql = "SELECT * FROM water_log ORDER BY id";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfAmountMl = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMl");
      final int _cursorIndexOfHour = CursorUtil.getColumnIndexOrThrow(_cursor, "hour");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final List<WaterLog> _result = new ArrayList<WaterLog>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final WaterLog _item;
        final int _tmpAmountMl;
        _tmpAmountMl = _cursor.getInt(_cursorIndexOfAmountMl);
        final int _tmpHour;
        _tmpHour = _cursor.getInt(_cursorIndexOfHour);
        final String _tmpDate;
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _tmpDate = null;
        } else {
          _tmpDate = _cursor.getString(_cursorIndexOfDate);
        }
        _item = new WaterLog(_tmpAmountMl,_tmpHour,_tmpDate);
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
  public LiveData<List<WaterLog>> getWaterLogsByDate(final String date) {
    final String _sql = "SELECT * FROM water_log WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"water_log"}, false, new Callable<List<WaterLog>>() {
      @Override
      @Nullable
      public List<WaterLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmountMl = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMl");
          final int _cursorIndexOfHour = CursorUtil.getColumnIndexOrThrow(_cursor, "hour");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final List<WaterLog> _result = new ArrayList<WaterLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WaterLog _item;
            final int _tmpAmountMl;
            _tmpAmountMl = _cursor.getInt(_cursorIndexOfAmountMl);
            final int _tmpHour;
            _tmpHour = _cursor.getInt(_cursorIndexOfHour);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            _item = new WaterLog(_tmpAmountMl,_tmpHour,_tmpDate);
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
  public LiveData<Integer> getTotalWaterByDate(final String date) {
    final String _sql = "SELECT COALESCE(SUM(amountMl), 0) FROM water_log WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"water_log"}, false, new Callable<Integer>() {
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
  public LiveData<List<HourlyTotal>> getHourlyWaterByDate(final String date) {
    final String _sql = "SELECT hour, SUM(amountMl) as total FROM water_log WHERE date = ? GROUP BY hour";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"water_log"}, false, new Callable<List<HourlyTotal>>() {
      @Override
      @Nullable
      public List<HourlyTotal> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfHour = 0;
          final int _cursorIndexOfTotal = 1;
          final List<HourlyTotal> _result = new ArrayList<HourlyTotal>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HourlyTotal _item;
            final int _tmpHour;
            _tmpHour = _cursor.getInt(_cursorIndexOfHour);
            final double _tmpTotal;
            _tmpTotal = _cursor.getDouble(_cursorIndexOfTotal);
            _item = new HourlyTotal(_tmpHour,_tmpTotal);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
