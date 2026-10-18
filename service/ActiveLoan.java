package library.service;

import java.time.LocalDate;

//รายการที่ยังไม่คืน (ใช้แสดงในหน้าคืนหนังสือ) 
public record ActiveLoan(int recordId, String title, String memberName, LocalDate borrowDate, LocalDate dueDate, boolean overdue, int overdueDays, int fine) { }
