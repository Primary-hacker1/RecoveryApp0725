package com.rick.recoveryapp.greendao;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

import com.rick.recoveryapp.greendao.entity.RecordDetailed;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "RECORD_DETAILED".
*/
public class RecordDetailedDao extends AbstractDao<RecordDetailed, Long> {

    public static final String TABLENAME = "RECORD_DETAILED";

    /**
     * Properties of entity RecordDetailed.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property RecordID = new Property(1, long.class, "RecordID", false, "RECORD_ID");
        public final static Property ActivtType = new Property(2, String.class, "ActivtType", false, "ACTIVT_TYPE");
        public final static Property RecordTime = new Property(3, String.class, "RecordTime", false, "RECORD_TIME");
        public final static Property Speed = new Property(4, int.class, "Speed", false, "SPEED");
        public final static Property Resistance = new Property(5, int.class, "Resistance", false, "RESISTANCE");
        public final static Property LeftLimb = new Property(6, int.class, "LeftLimb", false, "LEFT_LIMB");
        public final static Property RightLimb = new Property(7, int.class, "RightLimb", false, "RIGHT_LIMB");
        public final static Property HeartRate = new Property(8, int.class, "heartRate", false, "HEART_RATE");
        public final static Property Hbo2 = new Property(9, int.class, "Hbo2", false, "HBO2");
        public final static Property Spasm = new Property(10, int.class, "spasm", false, "SPASM");
    }


    public RecordDetailedDao(DaoConfig config) {
        super(config);
    }
    
    public RecordDetailedDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"RECORD_DETAILED\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                "\"RECORD_ID\" INTEGER NOT NULL ," + // 1: RecordID
                "\"ACTIVT_TYPE\" TEXT," + // 2: ActivtType
                "\"RECORD_TIME\" TEXT," + // 3: RecordTime
                "\"SPEED\" INTEGER NOT NULL ," + // 4: Speed
                "\"RESISTANCE\" INTEGER NOT NULL ," + // 5: Resistance
                "\"LEFT_LIMB\" INTEGER NOT NULL ," + // 6: LeftLimb
                "\"RIGHT_LIMB\" INTEGER NOT NULL ," + // 7: RightLimb
                "\"HEART_RATE\" INTEGER NOT NULL ," + // 8: heartRate
                "\"HBO2\" INTEGER NOT NULL ," + // 9: Hbo2
                "\"SPASM\" INTEGER NOT NULL );"); // 10: spasm
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"RECORD_DETAILED\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, RecordDetailed entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindLong(2, entity.getRecordID());
 
        String ActivtType = entity.getActivtType();
        if (ActivtType != null) {
            stmt.bindString(3, ActivtType);
        }
 
        String RecordTime = entity.getRecordTime();
        if (RecordTime != null) {
            stmt.bindString(4, RecordTime);
        }
        stmt.bindLong(5, entity.getSpeed());
        stmt.bindLong(6, entity.getResistance());
        stmt.bindLong(7, entity.getLeftLimb());
        stmt.bindLong(8, entity.getRightLimb());
        stmt.bindLong(9, entity.getHeartRate());
        stmt.bindLong(10, entity.getHbo2());
        stmt.bindLong(11, entity.getSpasm());
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, RecordDetailed entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindLong(2, entity.getRecordID());
 
        String ActivtType = entity.getActivtType();
        if (ActivtType != null) {
            stmt.bindString(3, ActivtType);
        }
 
        String RecordTime = entity.getRecordTime();
        if (RecordTime != null) {
            stmt.bindString(4, RecordTime);
        }
        stmt.bindLong(5, entity.getSpeed());
        stmt.bindLong(6, entity.getResistance());
        stmt.bindLong(7, entity.getLeftLimb());
        stmt.bindLong(8, entity.getRightLimb());
        stmt.bindLong(9, entity.getHeartRate());
        stmt.bindLong(10, entity.getHbo2());
        stmt.bindLong(11, entity.getSpasm());
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public RecordDetailed readEntity(Cursor cursor, int offset) {
        RecordDetailed entity = new RecordDetailed( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.getLong(offset + 1), // RecordID
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // ActivtType
            cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3), // RecordTime
            cursor.getInt(offset + 4), // Speed
            cursor.getInt(offset + 5), // Resistance
            cursor.getInt(offset + 6), // LeftLimb
            cursor.getInt(offset + 7), // RightLimb
            cursor.getInt(offset + 8), // heartRate
            cursor.getInt(offset + 9), // Hbo2
            cursor.getInt(offset + 10) // spasm
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, RecordDetailed entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setRecordID(cursor.getLong(offset + 1));
        entity.setActivtType(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setRecordTime(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
        entity.setSpeed(cursor.getInt(offset + 4));
        entity.setResistance(cursor.getInt(offset + 5));
        entity.setLeftLimb(cursor.getInt(offset + 6));
        entity.setRightLimb(cursor.getInt(offset + 7));
        entity.setHeartRate(cursor.getInt(offset + 8));
        entity.setHbo2(cursor.getInt(offset + 9));
        entity.setSpasm(cursor.getInt(offset + 10));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(RecordDetailed entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(RecordDetailed entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(RecordDetailed entity) {
        return entity.getId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}
