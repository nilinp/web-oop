package library.model;

public class Teacher extends Member {
    public Teacher(int memberId, String name, String email) {
        super(memberId, name, email);
    }

    @Override public String getMemberType(){ 
        return "teacher"; 
    }
    @Override public int getMaxBooks(){ 
        return 5; 
    }
    @Override public int getFineRate(){ 
        return 2; 
    }
    @Override public int getLoanDays(){ 
        return 14; 
    }
}
