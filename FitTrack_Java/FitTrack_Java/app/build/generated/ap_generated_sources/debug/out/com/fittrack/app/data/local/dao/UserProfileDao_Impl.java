package com.fittrack.app.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.fittrack.app.data.local.entity.UserProfile;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class UserProfileDao_Impl implements UserProfileDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserProfile> __insertionAdapterOfUserProfile;

  private final SharedSQLiteStatement __preparedStmtOfClear;

  public UserProfileDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserProfile = new EntityInsertionAdapter<UserProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_profile` (`id`,`firebaseUid`,`name`,`age`,`gender`,`heightCm`,`weightKg`,`activityLevel`,`goal`,`bmr`,`tdee`,`calorieGoal`,`proteinGoalG`,`carbGoalG`,`fatGoalG`,`avatarUri`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final UserProfile entity) {
        statement.bindLong(1, entity.id);
        if (entity.firebaseUid == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.firebaseUid);
        }
        if (entity.name == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.name);
        }
        statement.bindLong(4, entity.age);
        if (entity.gender == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.gender);
        }
        statement.bindDouble(6, entity.heightCm);
        statement.bindDouble(7, entity.weightKg);
        if (entity.activityLevel == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.activityLevel);
        }
        if (entity.goal == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.goal);
        }
        statement.bindDouble(10, entity.bmr);
        statement.bindDouble(11, entity.tdee);
        statement.bindDouble(12, entity.calorieGoal);
        statement.bindDouble(13, entity.proteinGoalG);
        statement.bindDouble(14, entity.carbGoalG);
        statement.bindDouble(15, entity.fatGoalG);
        if (entity.avatarUri == null) {
          statement.bindNull(16);
        } else {
          statement.bindString(16, entity.avatarUri);
        }
      }
    };
    this.__preparedStmtOfClear = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM user_profile";
        return _query;
      }
    };
  }

  @Override
  public void insertOrUpdate(final UserProfile profile) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfUserProfile.insert(profile);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void clear() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfClear.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfClear.release(_stmt);
    }
  }

  @Override
  public LiveData<UserProfile> getUserProfile() {
    final String _sql = "SELECT * FROM user_profile WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"user_profile"}, false, new Callable<UserProfile>() {
      @Override
      @Nullable
      public UserProfile call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFirebaseUid = CursorUtil.getColumnIndexOrThrow(_cursor, "firebaseUid");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "age");
          final int _cursorIndexOfGender = CursorUtil.getColumnIndexOrThrow(_cursor, "gender");
          final int _cursorIndexOfHeightCm = CursorUtil.getColumnIndexOrThrow(_cursor, "heightCm");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfActivityLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "activityLevel");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfBmr = CursorUtil.getColumnIndexOrThrow(_cursor, "bmr");
          final int _cursorIndexOfTdee = CursorUtil.getColumnIndexOrThrow(_cursor, "tdee");
          final int _cursorIndexOfCalorieGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "calorieGoal");
          final int _cursorIndexOfProteinGoalG = CursorUtil.getColumnIndexOrThrow(_cursor, "proteinGoalG");
          final int _cursorIndexOfCarbGoalG = CursorUtil.getColumnIndexOrThrow(_cursor, "carbGoalG");
          final int _cursorIndexOfFatGoalG = CursorUtil.getColumnIndexOrThrow(_cursor, "fatGoalG");
          final int _cursorIndexOfAvatarUri = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUri");
          final UserProfile _result;
          if (_cursor.moveToFirst()) {
            _result = new UserProfile();
            _result.id = _cursor.getInt(_cursorIndexOfId);
            if (_cursor.isNull(_cursorIndexOfFirebaseUid)) {
              _result.firebaseUid = null;
            } else {
              _result.firebaseUid = _cursor.getString(_cursorIndexOfFirebaseUid);
            }
            if (_cursor.isNull(_cursorIndexOfName)) {
              _result.name = null;
            } else {
              _result.name = _cursor.getString(_cursorIndexOfName);
            }
            _result.age = _cursor.getInt(_cursorIndexOfAge);
            if (_cursor.isNull(_cursorIndexOfGender)) {
              _result.gender = null;
            } else {
              _result.gender = _cursor.getString(_cursorIndexOfGender);
            }
            _result.heightCm = _cursor.getFloat(_cursorIndexOfHeightCm);
            _result.weightKg = _cursor.getFloat(_cursorIndexOfWeightKg);
            if (_cursor.isNull(_cursorIndexOfActivityLevel)) {
              _result.activityLevel = null;
            } else {
              _result.activityLevel = _cursor.getString(_cursorIndexOfActivityLevel);
            }
            if (_cursor.isNull(_cursorIndexOfGoal)) {
              _result.goal = null;
            } else {
              _result.goal = _cursor.getString(_cursorIndexOfGoal);
            }
            _result.bmr = _cursor.getDouble(_cursorIndexOfBmr);
            _result.tdee = _cursor.getDouble(_cursorIndexOfTdee);
            _result.calorieGoal = _cursor.getDouble(_cursorIndexOfCalorieGoal);
            _result.proteinGoalG = _cursor.getDouble(_cursorIndexOfProteinGoalG);
            _result.carbGoalG = _cursor.getDouble(_cursorIndexOfCarbGoalG);
            _result.fatGoalG = _cursor.getDouble(_cursorIndexOfFatGoalG);
            if (_cursor.isNull(_cursorIndexOfAvatarUri)) {
              _result.avatarUri = null;
            } else {
              _result.avatarUri = _cursor.getString(_cursorIndexOfAvatarUri);
            }
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
  public UserProfile getUserProfileOnce() {
    final String _sql = "SELECT * FROM user_profile WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfFirebaseUid = CursorUtil.getColumnIndexOrThrow(_cursor, "firebaseUid");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "age");
      final int _cursorIndexOfGender = CursorUtil.getColumnIndexOrThrow(_cursor, "gender");
      final int _cursorIndexOfHeightCm = CursorUtil.getColumnIndexOrThrow(_cursor, "heightCm");
      final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
      final int _cursorIndexOfActivityLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "activityLevel");
      final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
      final int _cursorIndexOfBmr = CursorUtil.getColumnIndexOrThrow(_cursor, "bmr");
      final int _cursorIndexOfTdee = CursorUtil.getColumnIndexOrThrow(_cursor, "tdee");
      final int _cursorIndexOfCalorieGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "calorieGoal");
      final int _cursorIndexOfProteinGoalG = CursorUtil.getColumnIndexOrThrow(_cursor, "proteinGoalG");
      final int _cursorIndexOfCarbGoalG = CursorUtil.getColumnIndexOrThrow(_cursor, "carbGoalG");
      final int _cursorIndexOfFatGoalG = CursorUtil.getColumnIndexOrThrow(_cursor, "fatGoalG");
      final int _cursorIndexOfAvatarUri = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUri");
      final UserProfile _result;
      if (_cursor.moveToFirst()) {
        _result = new UserProfile();
        _result.id = _cursor.getInt(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfFirebaseUid)) {
          _result.firebaseUid = null;
        } else {
          _result.firebaseUid = _cursor.getString(_cursorIndexOfFirebaseUid);
        }
        if (_cursor.isNull(_cursorIndexOfName)) {
          _result.name = null;
        } else {
          _result.name = _cursor.getString(_cursorIndexOfName);
        }
        _result.age = _cursor.getInt(_cursorIndexOfAge);
        if (_cursor.isNull(_cursorIndexOfGender)) {
          _result.gender = null;
        } else {
          _result.gender = _cursor.getString(_cursorIndexOfGender);
        }
        _result.heightCm = _cursor.getFloat(_cursorIndexOfHeightCm);
        _result.weightKg = _cursor.getFloat(_cursorIndexOfWeightKg);
        if (_cursor.isNull(_cursorIndexOfActivityLevel)) {
          _result.activityLevel = null;
        } else {
          _result.activityLevel = _cursor.getString(_cursorIndexOfActivityLevel);
        }
        if (_cursor.isNull(_cursorIndexOfGoal)) {
          _result.goal = null;
        } else {
          _result.goal = _cursor.getString(_cursorIndexOfGoal);
        }
        _result.bmr = _cursor.getDouble(_cursorIndexOfBmr);
        _result.tdee = _cursor.getDouble(_cursorIndexOfTdee);
        _result.calorieGoal = _cursor.getDouble(_cursorIndexOfCalorieGoal);
        _result.proteinGoalG = _cursor.getDouble(_cursorIndexOfProteinGoalG);
        _result.carbGoalG = _cursor.getDouble(_cursorIndexOfCarbGoalG);
        _result.fatGoalG = _cursor.getDouble(_cursorIndexOfFatGoalG);
        if (_cursor.isNull(_cursorIndexOfAvatarUri)) {
          _result.avatarUri = null;
        } else {
          _result.avatarUri = _cursor.getString(_cursorIndexOfAvatarUri);
        }
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
