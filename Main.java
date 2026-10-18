package library;

import library.db.Database;
import library.service.LibraryService;
import library.web.WebServer;

/** จุดเริ่มต้นของโปรแกรม: สร้างฐานข้อมูล บริการ และเว็บเซิร์ฟเวอร์ */
public class Main {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        Database db = new Database("library.db");
        db.initSchema();

        WebServer server = new WebServer(new LibraryService(db));
        server.start(port);
        System.out.println("Server started: http://localhost:" + server.getPort() + "  (press Ctrl+C to stop)");
    }
}
