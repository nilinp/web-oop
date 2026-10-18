package library.web;

import java.util.List;

import library.model.Book;
import library.model.Member;
import library.service.ActiveLoan;
import library.service.HistoryRow;

public final class Pages {
    private Pages() { }

    public static final String CSS = """
        * { box-sizing: border-box; }
        body { font-family: "Sarabun", "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f4f6f8; color: #222; }
        nav { background: #2c3e50; padding: 0 24px; display: flex; align-items: center; gap: 4px; }
        nav .brand { color: #fff; font-weight: bold; margin-right: 24px; }
        nav a { color: #cfd8dc; text-decoration: none; padding: 14px 14px; }
        nav a:hover { background: #34495e; color: #fff; }
        main { max-width: 960px; margin: 24px auto; padding: 0 16px; }
        h1 { margin-top: 0; }
        table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 6px; overflow: hidden; }
        th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #eceff1; }
        th { background: #eceff1; }
        .card { background: #fff; padding: 20px; border-radius: 6px; margin-bottom: 20px; }
        label { display: block; margin: 10px 0 4px; font-weight: 600; }
        input, select { width: 100%; padding: 8px; border: 1px solid #b0bec5; border-radius: 4px; font: inherit; }
        button, .btn { background: #2980b9; color: #fff; border: 0; padding: 8px 16px; border-radius: 4px;
          cursor: pointer; text-decoration: none; display: inline-block; font: inherit; width: auto; }
        button.danger { background: #c0392b; }
        button.green, .btn.green { background: #27ae60; }
        .msg { padding: 10px 14px; border-radius: 4px; margin-bottom: 14px; }
        .msg.success { background: #d5f5e3; color: #1e8449; }
        .msg.error { background: #fadbd8; color: #922b21; }
        .tag { padding: 2px 8px; border-radius: 10px; font-size: 0.85em; }
        .tag.ok { background: #d5f5e3; color: #1e8449; }
        .tag.no { background: #fdebd0; color: #b9770e; }
        .tag.late { background: #fadbd8; color: #922b21; }
        .search { display: flex; gap: 8px; margin-bottom: 16px; }
        .inline { display: inline; }
        """;

    /** ป้องกัน HTML injection */
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static String layout(String title, String msg, String type, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html lang=\"th\"><head><meta charset=\"utf-8\">")
          .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
          .append("<title>").append(esc(title)).append(" - ระบบห้องสมุด</title>")
          .append("<link rel=\"stylesheet\" href=\"/style.css\"></head><body>")
          .append("<nav><span class=\"brand\">📚 ห้องสมุด</span>")
          .append("<a href=\"/books\">หนังสือ</a><a href=\"/members\">สมาชิก</a>")
          .append("<a href=\"/borrow\">ยืม</a><a href=\"/return\">คืน</a><a href=\"/history\">ประวัติ</a></nav><main>");
        if (msg != null && !msg.isEmpty()) {
            sb.append("<div class=\"msg ").append("error".equals(type) ? "error" : "success").append("\">")
              .append(esc(msg)).append("</div>");
        }
        sb.append(body).append("</main></body></html>");
        return sb.toString();
    }

    // ---------------- Books ----------------
    public static String books(List<Book> books, String q) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>รายการหนังสือ</h1>")
          .append("<form class=\"search\" method=\"get\" action=\"/books\">")
          .append("<input name=\"q\" value=\"").append(esc(q)).append("\" placeholder=\"ค้นหาชื่อหนังสือ ผู้แต่ง หรือ ISBN\">")
          .append("<button>ค้นหา</button><a class=\"btn green\" href=\"/books/add\">+ เพิ่มหนังสือ</a></form>")
          .append("<table><tr><th>#</th><th>ชื่อหนังสือ</th><th>ผู้แต่ง</th><th>ISBN</th><th>สถานะ</th><th></th></tr>");
        if (books.isEmpty()) {
            sb.append("<tr><td colspan=\"6\">ไม่พบหนังสือ</td></tr>");
        }
        for (Book b : books) {
            sb.append("<tr><td>").append(b.getBookId()).append("</td><td>").append(esc(b.getTitle()))
              .append("</td><td>").append(esc(b.getAuthor())).append("</td><td>")
              .append(b.getIsbn() == null ? "-" : esc(b.getIsbn())).append("</td><td>")
              .append(b.isAvailable() ? "<span class=\"tag ok\">ว่าง</span>" : "<span class=\"tag no\">ถูกยืม</span>")
              .append("</td><td><a class=\"btn\" href=\"/books/").append(b.getBookId()).append("/edit\">แก้ไข</a> ")
              .append("<form class=\"inline\" method=\"post\" action=\"/books/").append(b.getBookId())
              .append("/delete\" onsubmit=\"return confirm('ยืนยันการลบ?')\"><button class=\"danger\">ลบ</button></form>")
              .append("</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    public static String bookForm(Book book) {
        boolean edit = book != null;
        String action = edit ? "/books/" + book.getBookId() + "/edit" : "/books/add";
        return "<h1>" + (edit ? "แก้ไขหนังสือ" : "เพิ่มหนังสือ") + "</h1><div class=\"card\">"
             + "<form method=\"post\" action=\"" + action + "\">"
             + "<label>ชื่อหนังสือ</label><input name=\"title\" required value=\"" + esc(edit ? book.getTitle() : "") + "\">"
             + "<label>ผู้แต่ง</label><input name=\"author\" value=\"" + esc(edit ? book.getAuthor() : "") + "\">"
             + "<label>ISBN</label><input name=\"isbn\" value=\"" + esc(edit && book.getIsbn() != null ? book.getIsbn() : "") + "\">"
             + "<p><button class=\"green\">บันทึก</button> <a class=\"btn\" href=\"/books\">ยกเลิก</a></p>"
             + "</form></div>";
    }

    // ---------------- Members ----------------
    public static String members(List<Member> members) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>สมาชิก</h1><div class=\"card\"><form method=\"post\" action=\"/members\">")
          .append("<label>ชื่อ</label><input name=\"name\" required>")
          .append("<label>อีเมล</label><input name=\"email\" type=\"email\">")
          .append("<label>ประเภท</label><select name=\"member_type\">")
          .append("<option value=\"student\">นักเรียน/นักศึกษา</option><option value=\"teacher\">อาจารย์</option></select>")
          .append("<p><button class=\"green\">+ เพิ่มสมาชิก</button></p></form></div>")
          .append("<table><tr><th>#</th><th>ชื่อ</th><th>อีเมล</th><th>ประเภท</th><th>ยืมได้ (เล่ม)</th><th>วันยืม</th><th>ค่าปรับ/วัน</th></tr>");
        for (Member m : members) {
            sb.append("<tr><td>").append(m.getMemberId()).append("</td><td>").append(esc(m.getName()))
              .append("</td><td>").append(esc(m.getEmail())).append("</td><td>").append(m.getMemberType())
              .append("</td><td>").append(m.getMaxBooks()).append("</td><td>").append(m.getLoanDays())
              .append("</td><td>").append(m.getFineRate()).append(" ฿</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    // ---------------- Borrow / Return ----------------
    public static String borrow(List<Member> members, List<Book> available) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>ยืมหนังสือ</h1><div class=\"card\"><form method=\"post\" action=\"/borrow\">")
          .append("<label>สมาชิก</label><select name=\"member_id\" required>");
        for (Member m : members) {
            sb.append("<option value=\"").append(m.getMemberId()).append("\">")
              .append(esc(m.getName())).append(" (").append(m.getMemberType()).append(")</option>");
        }
        sb.append("</select><label>หนังสือ (เฉพาะเล่มที่ว่าง)</label><select name=\"book_id\" required>");
        for (Book b : available) {
            sb.append("<option value=\"").append(b.getBookId()).append("\">").append(esc(b.getTitle())).append("</option>");
        }
        sb.append("</select><p><button class=\"green\">ยืม</button></p></form></div>");
        return sb.toString();
    }

    public static String returnPage(List<ActiveLoan> loans) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>คืนหนังสือ</h1><table><tr><th>หนังสือ</th><th>ผู้ยืม</th><th>วันที่ยืม</th>")
          .append("<th>กำหนดคืน</th><th>สถานะ</th><th>ค่าปรับ</th><th></th></tr>");
        if (loans.isEmpty()) {
            sb.append("<tr><td colspan=\"7\">ไม่มีหนังสือที่ค้างคืน</td></tr>");
        }
        for (ActiveLoan l : loans) {
            sb.append("<tr><td>").append(esc(l.title())).append("</td><td>").append(esc(l.memberName()))
              .append("</td><td>").append(l.borrowDate()).append("</td><td>").append(l.dueDate()).append("</td><td>")
              .append(l.overdue() ? "<span class=\"tag late\">เกิน " + l.overdueDays() + " วัน</span>"
                                  : "<span class=\"tag ok\">ในกำหนด</span>")
              .append("</td><td>").append(l.fine()).append(" ฿</td><td>")
              .append("<form class=\"inline\" method=\"post\" action=\"/return\">")
              .append("<input type=\"hidden\" name=\"record_id\" value=\"").append(l.recordId()).append("\">")
              .append("<button class=\"green\">คืน</button></form></td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    public static String history(List<HistoryRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>ประวัติการยืม</h1><table><tr><th>#</th><th>หนังสือ</th><th>ผู้ยืม</th>")
          .append("<th>วันที่ยืม</th><th>กำหนดคืน</th><th>วันที่คืน</th><th>ค่าปรับ</th></tr>");
        if (rows.isEmpty()) {
            sb.append("<tr><td colspan=\"7\">ยังไม่มีประวัติ</td></tr>");
        }
        for (HistoryRow r : rows) {
            sb.append("<tr><td>").append(r.recordId()).append("</td><td>").append(esc(r.title()))
              .append("</td><td>").append(esc(r.memberName())).append("</td><td>").append(r.borrowDate())
              .append("</td><td>").append(r.dueDate()).append("</td><td>")
              .append(r.returnDate() == null ? "ยังไม่คืน" : r.returnDate())
              .append("</td><td>").append((int) r.fine()).append(" ฿</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }
}
