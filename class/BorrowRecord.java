package library.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class BorrowRecord {
    private final int recordId;
    private final int memberId;
    private final int bookId;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private final LocalDate returnDate; //null = ยังไม่คืน
    private final double fineAmount;

    public BorrowRecord(int recordId, int memberId, int bookId, LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate, double fineAmount) {
        this.recordId = recordId;
        this.memberId = memberId;
        this.bookId = bookId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fineAmount = fineAmount;
    }

    public int getRecordId(){ 
        return recordId; 
    }
    public int getMemberId(){ 
        return memberId; 
    }
    public int getBookId(){ 
        return bookId; 
    }
    public LocalDate getBorrowDate(){ 
        return borrowDate; 
    }
    public LocalDate getDueDate(){ 
        return dueDate; 
    }
    public LocalDate getReturnDate(){ 
        return returnDate; 
    }
    public double getFineAmount(){ 
        return fineAmount; 
    }

    public static LocalDate newDueDate(int loanDays, LocalDate start){
        return start.plusDays(loanDays);
    }

    public int overdueDays(LocalDate on){
        long days = ChronoUnit.DAYS.between(dueDate, on);
        return (int) Math.max(days, 0);
    }

    public int overdueDays(){ 
        return overdueDays(LocalDate.now()); 
    }
    public boolean isOverdue(){ 
        return overdueDays() > 0; 
    }

    public int calculateFine(int fineRate, LocalDate on) {
        return overdueDays(on) * fineRate;
    }
    public int calculateFine(int fineRate){ 
        return calculateFine(fineRate, LocalDate.now()); 
    }
}
