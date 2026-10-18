package library;

import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import library.db.Database;
import library.model.Book;
import library.model.BorrowRecord;
import library.model.Member;
import library.model.Student;
import library.model.Teacher;
import library.service.LibraryException;
import library.service.LibraryService;
import library.web.WebServer;

/** ชุดทดสอบ: รันด้วย  java -cp "out:lib/*" library.TestRunner  (ไม่ต้องใช้ไลบรารีเพิ่ม) */
public class TestRunner {
    interface Check { void run() throws Exception; }

    static int passed = 0, failed = 0;

    static void test(String name, Check c) {
        try {
            c.run();
            passed++;
            System.out.println("  ok    " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("  FAIL  " + name + "  -> " + t);
        }
    }

    static void assertTrue(boolean cond, String what) {
        if (!cond) throw new AssertionError(what);
    }

    static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected " + expected + " but was " + actual);
    }

    static void assertThrows(Class<? extends Throwable> type, Check c) {
        try {
            c.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + t);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " but nothing was thrown");
    }

    static Path tempDb() throws Exception {
        Path p = Files.createTempFile("library-test", ".db");
        return p;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("OOP tests");
        test("1. Abstraction: Member is abstract, unknown type rejected", () -> {
            assertTrue(Modifier.isAbstract(Member.class.getModifiers()), "Member must be abstract");
            assertThrows(IllegalArgumentException.class, () -> Member.create("alien", 1, "x", ""));
        });
        test("2. Polymorphism: Student / Teacher", () -> {
            Member s = new Student(1, "A", ""), t = new Teacher(2, "B", "");
            assertEquals(3, s.getMaxBooks()); assertEquals(5, s.getFineRate());
            assertEquals(5, t.getMaxBooks()); assertEquals(2, t.getFineRate());
        });
        test("3. Encapsulation: blank name rejected by setter", () ->
            assertThrows(IllegalArgumentException.class, () -> new Student(1, "   ", "")));
        test("4. Fine calculation (3 days x 5 = 15, early = 0)", () -> {
            BorrowRecord r = new BorrowRecord(1, 1, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 8), null, 0);
            assertEquals(15, r.calculateFine(5, LocalDate.of(2026, 1, 11)));
            assertEquals(0, r.calculateFine(5, LocalDate.of(2026, 1, 5)));
        });
        test("5. Book.borrow() twice throws", () -> {
            Book b = new Book(1, "T", "", "", true);
            b.borrow();
            assertThrows(IllegalStateException.class, b::borrow);
        });

        System.out.println("Service tests");
        test("6. Borrow and return via LibraryService", () -> withService((db, s) -> {
            s.borrowBook(1, 1);
            assertTrue(!s.getBook(1).isAvailable(), "book should be borrowed");
            assertThrows(LibraryException.class, () -> s.borrowBook(2, 1));
            int rid = s.listActiveLoans().get(0).recordId();
            assertEquals(0, s.returnBook(rid));
            assertTrue(s.getBook(1).isAvailable(), "book should be available");
        }));
        test("7. Student cannot borrow a 4th book", () -> withService((db, s) -> {
            for (int id = 1; id <= 3; id++) s.borrowBook(1, id);
            assertThrows(LibraryException.class, () -> s.borrowBook(1, 4));
        }));
        test("8. Return 10 days late = fine 50", () -> withService((db, s) -> {
            s.borrowBook(1, 1);
            db.update("UPDATE borrow_records SET due_date=?", LocalDate.now().minusDays(10).toString());
            int rid = s.listActiveLoans().get(0).recordId();
            assertEquals(50, s.returnBook(rid));
        }));
        test("9. Cannot delete a book that has history", () -> withService((db, s) -> {
            s.borrowBook(1, 1);
            assertThrows(LibraryException.class, () -> s.deleteBook(1));
        }));

        System.out.println("Web test");
        test("10. All pages return 200, borrow via form works", () -> {
            Path p = tempDb();
            try {
                Database db = new Database(p.toString());
                db.initSchema();
                WebServer web = new WebServer(new LibraryService(db));
                web.start(0);
                try {
                    HttpClient c = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
                    String base = "http://localhost:" + web.getPort();
                    for (String u : new String[]{"/books", "/members", "/borrow", "/return", "/history"}) {
                        int code = c.send(HttpRequest.newBuilder(URI.create(base + u)).build(),
                                HttpResponse.BodyHandlers.ofString()).statusCode();
                        assertEquals(200, code);
                    }
                    HttpResponse<String> r = c.send(HttpRequest.newBuilder(URI.create(base + "/borrow"))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString("member_id=1&book_id=1")).build(),
                            HttpResponse.BodyHandlers.ofString());
                    assertTrue(r.body().contains("ยืมสำเร็จ"), "should show success message");
                } finally {
                    web.stop();
                }
            } finally {
                Files.deleteIfExists(p);
            }
        });

        System.out.println();
        System.out.println("Passed: " + passed + "   Failed: " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    interface ServiceCheck { void run(Database db, LibraryService s) throws Exception; }

    static void withService(ServiceCheck check) throws Exception {
        Path p = tempDb();
        try {
            Database db = new Database(p.toString());
            db.initSchema();
            check.run(db, new LibraryService(db));
        } finally {
            Files.deleteIfExists(p);
        }
    }
}
