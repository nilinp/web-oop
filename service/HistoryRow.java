package library.service;

public record HistoryRow(int recordId, String title, String memberName, String borrowDate, String dueDate, String returnDate, double fine) { }
