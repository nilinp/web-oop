package library.service;

import java.time.LocalDate;
import java.util.List;

import library.db.Database;
import library.db.Database.RowMapper;
import library.db.Database.Sql;
import library.db.DatabaseException;
import library.model.Book;
import library.model.BorrowRecord;
import library.model.Member;

public class LibraryService {
    private final Database db;

    private static final RowMapper<Book> BOOK = rs -> new Book(
        rs.getInt("book_id"), rs.getString("title"), rs.getString("author"), rs.getString("isbn"), rs.getInt("is_available") == 1
    );
    private static final RowMapper<Member> MEMBER = rs -> Member.create(
        rs.getString("member_type"), rs.getInt("member_id"), rs.getString("name"), rs.getString("email")
    );
    private static final RowMapper<BorrowRecord> RECORD = rs -> {
        String ret = rs.getString("return_date");
        return new BorrowRecord(rs.getInt("record_id"), rs.getInt("member_id"), rs.getInt("book_id"), LocalDate.parse(rs.getString("borrow_date")), LocalDate.parse(rs.getString("due_date")), 
        ret == null ? null : LocalDate.parse(ret), rs.getDouble("fine_amount"));
    };
    public LibraryService(Database db) {
        this.db = db;
    }

    // ================= Books =================
    public List<Book> listBooks(String keyword){
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return db.queryAll("SELECT * FROM books WHERE title LIKE ? OR author LIKE ? OR isbn LIKE ? ORDER BY book_id", BOOK, like, like, like);
    }
    public Book getBook(int bookId) {
        return db.queryOne("SELECT * FROM books WHERE book_id=?", BOOK, bookId);
    }
    public void addBook(String title, String author, String isbn){
        Book book = new Book(0, title, author, isbn, true);
        try {
            db.insert("INSERT INTO books (title, author, isbn) VALUES (?,?,?)", book.getTitle(), book.getAuthor(), book.getIsbn());
        } catch (DatabaseException e) {
            throw translate(e);
        }
    }
    public void updateBook(int bookId, String title, String author, String isbn){
        Book book = new Book(bookId, title, author, isbn, true);
        try {
            db.update("UPDATE books SET title=?, author=?, isbn=? WHERE book_id=?", book.getTitle(), book.getAuthor(), book.getIsbn(), bookId);
        } catch (DatabaseException e) {
            throw translate(e);
        }
    }
    public void deleteBook(int bookId){
        Integer used = db.queryOne("SELECT 1 AS x FROM borrow_records WHERE book_id=?", rs -> 1, bookId);
        if (used != null) throw new LibraryException("ลบไม่ได้ เพราะหนังสือเล่มนี้ถูกยืมอยู่");
        db.update("DELETE FROM books WHERE book_id=?", bookId);
    }
    private LibraryException translate(DatabaseException e) {
        if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
            return new LibraryException("ISBN นี้มีอยู่ในระบบแล้ว");
        }
        throw e;
    }

    // ================= Members =================
    public List<Member> listMembers() {
        return db.queryAll("SELECT * FROM members ORDER BY member_id", MEMBER);
    }
    public Member getMember(int memberId) {
        return db.queryOne("SELECT * FROM members WHERE member_id=?", MEMBER, memberId);
    }
    public void addMember(String name, String email, String memberType) {
        Member member = Member.create(memberType, 0, name, email);
        db.insert("INSERT INTO members (name, email, member_type) VALUES (?,?,?)", member.getName(), member.getEmail(), member.getMemberType());
    }

    // ================= Borrow / Return =================
    //ยืมหนังสือ คืนวันกำหนดคืน
    public LocalDate borrowBook(int memberId, int bookId) {
        Member member = getMember(memberId);
        if (member == null) throw new LibraryException("ไม่พบสมาชิก");
        Book book = getBook(bookId);
        if (book == null) throw new LibraryException("ไม่พบหนังสือ");

        int active = db.queryOne(
                "SELECT COUNT(*) AS c FROM borrow_records WHERE member_id=? AND return_date IS NULL", rs -> rs.getInt("c"), memberId);
        if (active >= member.getMaxBooks()) throw new LibraryException(member.getName() + " ยืมครบ " + member.getMaxBooks() + " เล่มแล้ว");

        try {
            book.borrow(); // change obj status (ถ้าถูกยืมอยู่จะ throw)
        } catch (IllegalStateException e) {
            throw new LibraryException(e.getMessage());
        }

        LocalDate today = LocalDate.now();
        LocalDate due = BorrowRecord.newDueDate(member.getLoanDays(), today);
        db.executeAtomic(List.of(
                new Sql("INSERT INTO borrow_records (member_id, book_id, borrow_date, due_date) VALUES (?,?,?,?)", memberId, bookId, today.toString(), due.toString()),
                new Sql("UPDATE books SET is_available=0 WHERE book_id=?", bookId)));
        return due;
    }

    //คืนหนังสือ คืนค่าปรับ (บาท)
    public int returnBook(int recordId) {
        BorrowRecord record = db.queryOne("SELECT * FROM borrow_records WHERE record_id=?", RECORD, recordId);
        if (record == null || record.getReturnDate() != null) {
            throw new LibraryException("ไม่พบรายการยืมที่ยังไม่คืน");
        }
        Member member = getMember(record.getMemberId());
        int fine = record.calculateFine(member.getFineRate());

        db.executeAtomic(List.of(
                new Sql("UPDATE borrow_records SET return_date=?, fine_amount=? WHERE record_id=?",
                        LocalDate.now().toString(), fine, recordId),
                new Sql("UPDATE books SET is_available=1 WHERE book_id=?", record.getBookId())));
        return fine;
    }

    //รายการที่ยังไม่คืน พร้อมค่าปรับ ณ วันนี้
    public List<ActiveLoan> listActiveLoans(){
        return db.queryAll(
            "SELECT r.*, b.title, m.name, m.email, m.member_type FROM borrow_records r " + "JOIN books b ON b.book_id=r.book_id JOIN members m ON m.member_id=r.member_id "
            + "WHERE r.return_date IS NULL ORDER BY r.due_date",
            rs -> {
                BorrowRecord rec = RECORD.map(rs);
                Member m = MEMBER.map(rs);
                return new ActiveLoan(rec.getRecordId(), rs.getString("title"), m.getName(), rec.getBorrowDate(), rec.getDueDate(), rec.isOverdue(), rec.overdueDays(),
                rec.calculateFine(m.getFineRate()));
            }
        );
    }
    public List<HistoryRow> listHistory() {
        return db.queryAll(
            "SELECT r.*, b.title, m.name FROM borrow_records r " + "JOIN books b ON b.book_id=r.book_id JOIN members m ON m.member_id=r.member_id "
            + "ORDER BY r.record_id DESC",
            rs -> new HistoryRow(rs.getInt("record_id"), rs.getString("title"), rs.getString("name"), rs.getString("borrow_date"), rs.getString("due_date"),
                rs.getString("return_date"), rs.getDouble("fine_amount"))
        );
    }
}
