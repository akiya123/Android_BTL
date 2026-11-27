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
import com.fittrack.app.data.local.entity.WeightEntry;
import java.lang.Class;
import java.lang.Exception;
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
public final class WeightEntryDao_Impl implements WeightEntryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WeightEntry> __insertionAdapterOfWeightEntry;

  private final EntityDeletionOrUpdateAdapter<WeightEntry> __deletionAdapterOfWeightEntry;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public WeightEntryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWeightEntry = new EntityInsertionAdapter<WeightEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `weight_entry` (`id`,`weightKg`,`date`) VALUES (nullif(?, 0),?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final WeightEntry entity) {
        statement.bindLong(1, entity.id);
        statement.bindDouble(2, entity.weightKg);
        if (entity.date == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.date);
        }
      }
    };
    this.__deletionAdapterOfWeightEntry = new EntityDeletionOrUpdateAdapter<WeightEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `weight_entry` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final WeightEntry entity) {
        statement.bindLong(1, entity.id);
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM weight_entry";
        return _query;
      }
    };
  }

  @Override
  public void insertAll(final List<WeightEntry> entries) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfWeightEntry.insert(entries);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insert(final WeightEntry weightEntry) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfWeightEntry.insert(weightEntry);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final WeightEntry weightEntry) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfWeightEntry.handle(weightEntry);
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
  public List<WeightEntry> getAllOnce() {
    final String _sql = "SELECT * FROM weight_entry ORDER BY id";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final List<WeightEntry> _result = new ArrayList<WeightEntry>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final WeightEntry _item;
        final float _tmpWeightKg;
        _tmpWeightKg = _cursor.getFloat(_cursorIndexOfWeightKg);
        final String _tmpDate;
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _tmpDate = null;
        } else {
          _tmpDate = _cursor.getString(_cursorIndexOfDate);
        }
        _item = new WeightEntry(_tmpWeightKg,_tmpDate);
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
  public LiveData<List<WeightEntry>> getAllWeights() {
    final String _sql = "SELECT * FROM weight_entry ORDER BY date ASC, id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"weight_entry"}, false, new Callable<List<WeightEntry>>() {
      @Override
      @Nullable
      public List<WeightEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final List<WeightEntry> _result = new ArrayList<WeightEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WeightEntry _item;
            final float _tmpWeightKg;
            _tmpWeightKg = _cursor.getFloat(_cursorIndexOfWeightKg);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            _item = new WeightEntry(_tmpWeightKg,_tmpDate);
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
  public LiveData<WeightEntry> getLatestWeight() {
    final String _sql = "SELECT * FROM weight_entry ORDER BY date DESC, id DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"weight_entry"}, false, new Callable<WeightEntry>() {
      @Override
      @Nullable
      public WeightEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final WeightEntry _result;
          if (_cursor.moveToFirst()) {
            final float _tmpWeightKg;
            _tmpWeightKg = _cursor.getFloat(_cursorIndexOfWeightKg);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            _result = new WeightEntry(_tmpWeightKg,_tmpDate);
            _result.id = _cursor.getLong(_cursorIndexOfId);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
