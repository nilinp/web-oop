package library.model;

public class Student extends Member {
    public Student(int memberId, String name, String email) {
        super(memberId, name, email);
    }
    @Override public String getMemberType(){ 
        return "student"; 
    }
    @Override public int getMaxBooks(){ 
        return 3; 
    }
    @Override public int getFineRate(){ 
        return 5; 
    }
    @Override public int getLoanDays(){ 
        return 7; 
    }
}
