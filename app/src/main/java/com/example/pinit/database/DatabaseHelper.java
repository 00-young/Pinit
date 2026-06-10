package com.example.pinit.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.pinit.model.Budget;
import com.example.pinit.model.Record;
import com.example.pinit.model.Schedule;
import com.example.pinit.model.Trip;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "traveltracker.db";
    private static final int DB_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE trips (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, destination TEXT, start_date TEXT, end_date TEXT, budget REAL, memo TEXT, cover_image TEXT)");
        db.execSQL("CREATE TABLE schedules (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "trip_id INTEGER, " +
                "title TEXT, " +
                "date TEXT, " +
                "time TEXT, " +
                "place_name TEXT, " +
                "memo TEXT, " +
                "color TEXT, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "google_place_id TEXT, " +
                "category TEXT)");
        db.execSQL("CREATE TABLE budgets (id INTEGER PRIMARY KEY AUTOINCREMENT, trip_id INTEGER, title TEXT, amount REAL, category TEXT, date TEXT, type TEXT, memo TEXT)");
        db.execSQL("CREATE TABLE records (id INTEGER PRIMARY KEY AUTOINCREMENT, trip_id INTEGER, title TEXT, date TEXT, content TEXT, image_path TEXT, place_name TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // schedules 테이블에 누락된 컬럼 추가 (기존 데이터 유지하고 싶을 경우 ALTER 사용)
            // 여기서는 단순함을 위해 테이블 재생성 방식을 사용합니다.
            db.execSQL("DROP TABLE IF EXISTS schedules");
            db.execSQL("CREATE TABLE schedules (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "trip_id INTEGER, " +
                    "title TEXT, " +
                    "date TEXT, " +
                    "time TEXT, " +
                    "place_name TEXT, " +
                    "memo TEXT, " +
                    "color TEXT, " +
                    "latitude REAL, " +
                    "longitude REAL, " +
                    "google_place_id TEXT, " +
                    "category TEXT)");
        }
    }

    // ========== TRIP ==========
    public long insertTrip(Trip t) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", t.getTitle()); v.put("destination", t.getDestination());
        v.put("start_date", t.getStartDate()); v.put("end_date", t.getEndDate());
        v.put("budget", t.getBudget()); v.put("memo", t.getMemo());
        v.put("cover_image", t.getCoverImage());
        long id = db.insert("trips", null, v);
        db.close(); return id;
    }

    public List<Trip> getAllTrips() {
        List<Trip> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("trips", null, null, null, null, null, "id DESC");
        if (c.moveToFirst()) do { list.add(cursorToTrip(c)); } while (c.moveToNext());
        c.close(); db.close(); return list;
    }

    public Trip getTripById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("trips", null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        Trip t = null;
        if (c.moveToFirst()) t = cursorToTrip(c);
        c.close(); db.close(); return t;
    }

    public void deleteTrip(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("trips", "id=?", new String[]{String.valueOf(id)});
        db.delete("schedules", "trip_id=?", new String[]{String.valueOf(id)});
        db.delete("budgets", "trip_id=?", new String[]{String.valueOf(id)});
        db.delete("records", "trip_id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void updateTrip(Trip t) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues v = new ContentValues();

        v.put("title", t.getTitle());
        v.put("start_date", t.getStartDate());
        v.put("end_date", t.getEndDate());
        v.put("budget", t.getBudget());

        db.update(
                "trips",
                v,
                "id=?",
                new String[]{String.valueOf(t.getId())}
        );

        db.close();
    }

    private Trip cursorToTrip(Cursor c) {
        Trip t = new Trip();
        t.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        t.setTitle(c.getString(c.getColumnIndexOrThrow("title")));
        t.setDestination(c.getString(c.getColumnIndexOrThrow("destination")));
        t.setStartDate(c.getString(c.getColumnIndexOrThrow("start_date")));
        t.setEndDate(c.getString(c.getColumnIndexOrThrow("end_date")));
        t.setBudget(c.getDouble(c.getColumnIndexOrThrow("budget")));
        t.setMemo(c.getString(c.getColumnIndexOrThrow("memo")));
        t.setCoverImage(c.getString(c.getColumnIndexOrThrow("cover_image")));
        return t;
    }

    // ========== SCHEDULE ==========
    public long insertSchedule(Schedule s) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("trip_id", s.getTripId()); v.put("title", s.getTitle());
        v.put("date", s.getDate()); v.put("time", s.getTime());
        v.put("place_name", s.getPlaceName()); v.put("memo", s.getMemo());
        v.put("color", s.getColor());
        v.put("latitude", s.getLatitude());
        v.put("longitude", s.getLongitude());
        v.put("google_place_id", s.getGooglePlaceId());
        v.put("category", s.getCategory());
        long id = db.insert("schedules", null, v);
        db.close();
        return id;
    }

    public List<Schedule> getSchedulesByTrip(int tripId) {
        List<Schedule> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("schedules", null, "trip_id=?", new String[]{String.valueOf(tripId)}, null, null, "date ASC, time ASC");
        if (c.moveToFirst()) do { list.add(cursorToSchedule(c)); } while (c.moveToNext());
        c.close(); db.close(); return list;
    }

    public void deleteSchedule(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("schedules", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void updateSchedule(Schedule s) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", s.getTitle());
        cv.put("date", s.getDate());
        cv.put("time", s.getTime());
        cv.put("place_name", s.getPlaceName());
        cv.put("memo", s.getMemo());
        db.update("schedules", cv, "id=?", new String[]{String.valueOf(s.getId())});
        db.close();
    }

    private Schedule cursorToSchedule(Cursor c) {
        Schedule s = new Schedule();
        s.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        s.setTripId(c.getInt(c.getColumnIndexOrThrow("trip_id")));
        s.setTitle(c.getString(c.getColumnIndexOrThrow("title")));
        s.setDate(c.getString(c.getColumnIndexOrThrow("date")));
        s.setTime(c.getString(c.getColumnIndexOrThrow("time")));
        s.setPlaceName(c.getString(c.getColumnIndexOrThrow("place_name")));
        s.setMemo(c.getString(c.getColumnIndexOrThrow("memo")));
        s.setColor(c.getString(c.getColumnIndexOrThrow("color")));
        s.setLatitude(c.getDouble(c.getColumnIndexOrThrow("latitude")));
        s.setLongitude(c.getDouble(c.getColumnIndexOrThrow("longitude")));
        s.setGooglePlaceId(c.getString(c.getColumnIndexOrThrow("google_place_id")));
        s.setCategory(c.getString(c.getColumnIndexOrThrow("category")));
        return s;
    }

    // ========== BUDGET ==========
    public long insertBudget(Budget b) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("trip_id", b.getTripId()); v.put("title", b.getTitle());
        v.put("amount", b.getAmount()); v.put("category", b.getCategory());
        v.put("date", b.getDate()); v.put("type", b.getType()); v.put("memo", b.getMemo());
        long id = db.insert("budgets", null, v);
        db.close(); return id;
    }

    public List<Budget> getBudgetsByTrip(int tripId) {
        List<Budget> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("budgets", null, "trip_id=?", new String[]{String.valueOf(tripId)}, null, null, "date DESC");
        if (c.moveToFirst()) do { list.add(cursorToBudget(c)); } while (c.moveToNext());
        c.close(); db.close(); return list;
    }

    public double getTotalExpense(int tripId) {
        SQLiteDatabase db = getReadableDatabase();
        double total = 0;
        Cursor c = db.rawQuery("SELECT SUM(amount) FROM budgets WHERE trip_id=? AND type='expense'", new String[]{String.valueOf(tripId)});
        if (c.moveToFirst() && !c.isNull(0)) total = c.getDouble(0);
        c.close(); db.close(); return total;
    }

    public void deleteBudget(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("budgets", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    private Budget cursorToBudget(Cursor c) {
        Budget b = new Budget();
        b.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        b.setTripId(c.getInt(c.getColumnIndexOrThrow("trip_id")));
        b.setTitle(c.getString(c.getColumnIndexOrThrow("title")));
        b.setAmount(c.getDouble(c.getColumnIndexOrThrow("amount")));
        b.setCategory(c.getString(c.getColumnIndexOrThrow("category")));
        b.setDate(c.getString(c.getColumnIndexOrThrow("date")));
        b.setType(c.getString(c.getColumnIndexOrThrow("type")));
        b.setMemo(c.getString(c.getColumnIndexOrThrow("memo")));
        return b;
    }

    // ========== RECORD ==========
    public long insertRecord(Record r) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("trip_id", r.getTripId()); v.put("title", r.getTitle());
        v.put("date", r.getDate()); v.put("content", r.getContent());
        v.put("image_path", r.getImagePath()); v.put("place_name", r.getPlaceName());
        long id = db.insert("records", null, v);
        db.close(); return id;
    }

    public List<Record> getRecordsByTrip(int tripId) {
        List<Record> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("records", null, "trip_id=?", new String[]{String.valueOf(tripId)}, null, null, "date DESC");
        if (c.moveToFirst()) do { list.add(cursorToRecord(c)); } while (c.moveToNext());
        c.close(); db.close(); return list;
    }

    public void deleteRecord(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("records", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    private Record cursorToRecord(Cursor c) {
        Record r = new Record();
        r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        r.setTripId(c.getInt(c.getColumnIndexOrThrow("trip_id")));
        r.setTitle(c.getString(c.getColumnIndexOrThrow("title")));
        r.setDate(c.getString(c.getColumnIndexOrThrow("date")));
        r.setContent(c.getString(c.getColumnIndexOrThrow("content")));
        r.setImagePath(c.getString(c.getColumnIndexOrThrow("image_path")));
        r.setPlaceName(c.getString(c.getColumnIndexOrThrow("place_name")));
        return r;
    }
}
