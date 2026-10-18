package library.service;

//ข้อผิดพลาดของ business logic เช่น ยืมเกินจำนวน
public class LibraryException extends RuntimeException {
    public LibraryException(String message) {
        super(message);
    }
}
