package library.model;

public class Book {
    private final int bookId;
    private String title;
    private final String author;
    private final String isbn;
    private boolean available;
  
    public Book(int bookId, String title, String author, String isbn, boolean available){
        this.bookId = bookId;
        setTitle(title);
        this.author = (author == null) ? "" : author;
        this.isbn = (isbn == null || isbn.isBlank()) ? null : isbn.trim();
        this.available = available;
    }
    public int getBookId(){ 
        return bookId; 
    }
    public String getTitle(){ 
        return title; 
    }
    public String getAuthor(){ 
        return author; 
    }
    public String getIsbn(){ 
        return isbn; 
    }
    public boolean isAvailable(){ 
        return available; 
    }
    public final void setTitle(String title){
        if (title == null || title.isBlank()) throw new IllegalArgumentException("ชื่อหนังสือห้ามว่าง");
        this.title = title.trim();
    }
    public void borrow() {
        if (!available) throw new IllegalStateException("หนังสือ '" + title + "' ถูกยืมอยู่");
        available = false;
    }
    public void returnBook() {
        available = true;
    }
}
