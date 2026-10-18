package library.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Database {
    /** แปลงแถวผลลัพธ์ (ResultSet) เป็น obj */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
    public record Sql(String text, Object... params) { }

    private static final String[] SCHEMA = {
        """
        CREATE TABLE IF NOT EXISTS members (
            member_id   INTEGER PRIMARY KEY AUTOINCREMENT,
            name        TEXT NOT NULL,
            email       TEXT,
            member_type TEXT NOT NULL CHECK (member_type IN ('student','teacher'))
        )""",
        """
        CREATE TABLE IF NOT EXISTS books (
            book_id      INTEGER PRIMARY KEY AUTOINCREMENT,
            title        TEXT NOT NULL,
            author       TEXT,
            isbn         TEXT UNIQUE,
            is_available INTEGER DEFAULT 1
        )""",
        """
        CREATE TABLE IF NOT EXISTS borrow_records (
            record_id   INTEGER PRIMARY KEY AUTOINCREMENT,
            member_id   INTEGER NOT NULL,
            book_id     INTEGER NOT NULL,
            borrow_date TEXT NOT NULL,
            due_date    TEXT NOT NULL,
            return_date TEXT,
            fine_amount REAL DEFAULT 0,
            FOREIGN KEY (member_id) REFERENCES members(member_id),
            FOREIGN KEY (book_id)   REFERENCES books(book_id)
        )"""
    };

    private final String url;
    public Database(String path) {
        this.url = "jdbc:sqlite:" + path;
    }
    public Connection connect() {
        try {
            Connection conn = java.sql.DriverManager.getConnection(url);
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            return conn;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }
    public void initSchema() {
        for (String ddl : SCHEMA) {
            update(ddl);
        }
        seed();
    }

    /** INSERT: คืนค่า id ของแถวที่เพิ่ม */
    public long insert(String sql, Object... params) {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    /** UPDATE / DELETE / DDL: คืนจำนวนแถวที่ได้รับผลกระทบ */
    public int update(String sql, Object... params) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }
    public void executeAtomic(List<Sql> statements) {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                for (Sql s : statements) {
                    try (PreparedStatement ps = c.prepareStatement(s.text())) {
                        bind(ps, s.params());
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }
    public <T> List<T> queryAll(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    /** คืนแถวแรก หรือ null ถ้าไม่พบ */
    public <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = queryAll(sql, mapper, params);
        return rows.isEmpty() ? null : rows.get(0);
    }
    private void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
  
    /** ใส่ข้อมูลตัวอย่างครั้งแรก */
    private void seed() {
        if (queryOne("SELECT 1 AS x FROM books LIMIT 1", rs -> 1) != null) {
            return;
        }
        String[][] books = {
            {"Python Crash Course", "Eric Matthes", "9781593279288"},
            {"Clean Code", "Robert C. Martin", "9780132350884"},
            {"Design Patterns", "Erich Gamma", "9780201633610"},
            {"Introduction to Algorithms", "Thomas H. Cormen", "9780262033848"},
        };
        for (String[] b : books) {
            insert("INSERT INTO books (title, author, isbn) VALUES (?,?,?)", (Object[]) b);
        }
        String[][] members = {
            {"ปังปอนด์ ณราวิชญ์", "pangauam@example.com", "student"},
            {"ชุน อาเชน", "mooood@example.com", "student"},
            {"อาจารย์ใจดี", "่jaidee@example.com", "teacher"},
            {"อาจารย์เพิ่มพูน", "่permpoon@example.com", "teacher"},
        };
        for (String[] m : members) {
            insert("INSERT INTO members (name, email, member_type) VALUES (?,?,?)", (Object[]) m);
        }
    }
}
