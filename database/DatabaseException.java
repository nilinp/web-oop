package library.db;

import java.sql.SQLException;

public class DatabaseException extends RuntimeException {
    public DatabaseException(SQLException cause) {
        super(cause.getMessage(), cause);
    }
}
