package library;

import library.db.Database;
import library.service.LibraryService;
import library.web.WebServer;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else if (System.getenv("PORT") != null) {
            port = Integer.parseInt(System.getenv("PORT"));
        }

        // ที่เก็บไฟล์ฐานข้อมูล ตั้งค่าได้ด้วยตัวแปร DB_PATH (ใช้ตอนมี disk ถาวรบนโฮสต์)
        String dbPath = System.getenv().getOrDefault("DB_PATH", "library.db");

        Database db = new Database(dbPath);
        db.initSchema();

        WebServer server = new WebServer(new LibraryService(db));
        server.start(port);
        System.out.println("Server started: http://localhost:" + server.getPort() + "  (press Ctrl+C to stop)");
    }
}
