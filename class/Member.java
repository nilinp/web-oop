package library.model;

public abstract class Member{
    //Encapsulation : private
    private final int memberId;
    private String name;
    private final String email;

    protected Member(int memberId, String name, String email){
        this.memberId = memberId;
        setName(name);
        this.email = (email == null) ? "" : email;
    }
    public int getMemberId(){
        return memberId; 
    }
    public String getName(){ 
        return name; 
    }
    public String getEmail(){ 
        return email; 
    }
    public final void setName(String name){
        if (name == null || name.isBlank()) throw new IllegalArgumentException("ชื่อสมาชิกห้ามว่าง");
        this.name = name.trim();
    }

    //Abstraction : implement
    public abstract String getMemberType();
    public abstract int getMaxBooks();
    public abstract int getFineRate();
    public abstract int getLoanDays();

    public static Member create(String type, int memberId, String name, String email) {
        return switch (type) {
            case "student" -> new Student(memberId, name, email);
            case "teacher" -> new Teacher(memberId, name, email);
            default -> throw new IllegalArgumentException("ประเภทสมาชิกไม่ถูกต้อง: " + type);
        };
    }
}
