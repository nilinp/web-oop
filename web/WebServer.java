package library.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import library.model.Book;
import library.service.LibraryException;
import library.service.LibraryService;

public class WebServer {
    private static final Pattern BOOK_ACTION = Pattern.compile("^/books/(\\d+)/(edit|delete)$");

    private interface Action<T> {
        T run();
    }

    private final LibraryService service;
    private HttpServer server;

    public WebServer(LibraryService service) {
        this.service = service;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    // ================= Routing =================
    private void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            Map<String, String> query = parseParams(ex.getRequestURI().getRawQuery());
            Map<String, String> form = "POST".equals(method)
                    ? parseParams(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8))
                    : Map.of();
            boolean post = "POST".equals(method);
            String msg = query.get("msg");
            String type = query.getOrDefault("type", "success");

            Matcher bookAction = BOOK_ACTION.matcher(path);

            if (path.equals("/style.css")) {
                send(ex, 200, "text/css; charset=utf-8", Pages.CSS);
            } else if (path.equals("/")) {
                redirect(ex, "/books");
            } else if (path.equals("/books")) {
                String q = query.getOrDefault("q", "");
                page(ex, "หนังสือ", msg, type, Pages.books(service.listBooks(q), q));
            } else if (path.equals("/books/add")) {
                if (post) {
                    doAction(ex, "/books/add", "/books", "เพิ่มหนังสือเรียบร้อย", () -> {
                        service.addBook(form.getOrDefault("title", ""), form.getOrDefault("author", ""),
                                form.getOrDefault("isbn", ""));
                        return null;
                    });
                } else {
                    page(ex, "เพิ่มหนังสือ", msg, type, Pages.bookForm(null));
                }
            } else if (bookAction.matches()) {
                int id = Integer.parseInt(bookAction.group(1));
                if (bookAction.group(2).equals("edit")) {
                    if (post) {
                        doAction(ex, "/books/" + id + "/edit", "/books", "แก้ไขเรียบร้อย", () -> {
                            service.updateBook(id, form.getOrDefault("title", ""), form.getOrDefault("author", ""),
                                    form.getOrDefault("isbn", ""));
                            return null;
                        });
                    } else {
                        Book book = service.getBook(id);
                        if (book == null) redirectMsg(ex, "/books", "ไม่พบหนังสือ", "error");
                        else page(ex, "แก้ไขหนังสือ", msg, type, Pages.bookForm(book));
                    }
                } else if (post) {
                    doAction(ex, "/books", "/books", "ลบหนังสือแล้ว", () -> {
                        service.deleteBook(id);
                        return null;
                    });
                } else {
                    notFound(ex);
                }
            } else if (path.equals("/members")) {
                if (post) {
                    doAction(ex, "/members", "/members", "เพิ่มสมาชิกเรียบร้อย", () -> {
                        service.addMember(form.getOrDefault("name", ""), form.getOrDefault("email", ""),
                                form.getOrDefault("member_type", ""));
                        return null;
                    });
                } else {
                    page(ex, "สมาชิก", msg, type, Pages.members(service.listMembers()));
                }
            } else if (path.equals("/borrow")) {
                if (post) {
                    doAction(ex, "/borrow", "/borrow", null, () -> {
                        LocalDate due = service.borrowBook(toInt(form.get("member_id")), toInt(form.get("book_id")));
                        return "ยืมสำเร็จ กำหนดคืน " + due;
                    });
                } else {
                    List<Book> available = service.listBooks("").stream().filter(Book::isAvailable).toList();
                    page(ex, "ยืมหนังสือ", msg, type, Pages.borrow(service.listMembers(), available));
                }
            } else if (path.equals("/return")) {
                if (post) {
                    doAction(ex, "/return", "/return", null, () -> {
                        int fine = service.returnBook(toInt(form.get("record_id")));
                        return "คืนสำเร็จ" + (fine > 0 ? " ค่าปรับ " + fine + " บาท" : " ไม่มีค่าปรับ");
                    });
                } else {
                    page(ex, "คืนหนังสือ", msg, type, Pages.returnPage(service.listActiveLoans()));
                }
            } else if (path.equals("/history")) {
                page(ex, "ประวัติ", msg, type, Pages.history(service.listHistory()));
            } else {
                notFound(ex);
            }
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "text/plain; charset=utf-8", "เกิดข้อผิดพลาดภายในระบบ: " + e.getMessage());
        } finally {
            ex.close();
        }
    }

    private void doAction(HttpExchange ex, String errorPath, String okPath, String okMsg, Action<String> action)
            throws IOException {
        try {
            String result = action.run();
            redirectMsg(ex, okPath, okMsg != null ? okMsg : result, "success");
        } catch (LibraryException | IllegalArgumentException e) {
            redirectMsg(ex, errorPath, e.getMessage(), "error");
        }
    }

    // ================= Helpers =================
    private void page(HttpExchange ex, String title, String msg, String type, String body) throws IOException {
        send(ex, 200, "text/html; charset=utf-8", Pages.layout(title, msg, type, body));
    }

    private void notFound(HttpExchange ex) throws IOException {
        send(ex, 404, "text/plain; charset=utf-8", "404 ไม่พบหน้านี้");
    }

    private void send(HttpExchange ex, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
    }

    private void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().set("Location", location);
        ex.sendResponseHeaders(303, -1);
    }

    private void redirectMsg(HttpExchange ex, String path, String msg, String type) throws IOException {
        redirect(ex, path + "?msg=" + URLEncoder.encode(msg, StandardCharsets.UTF_8) + "&type=" + type);
    }

    private static int toInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("ข้อมูลที่ส่งมาไม่ถูกต้อง");
        }
    }

    private static Map<String, String> parseParams(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            String k = i < 0 ? pair : pair.substring(0, i);
            String v = i < 0 ? "" : pair.substring(i + 1);
            map.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return map;
    }
}
