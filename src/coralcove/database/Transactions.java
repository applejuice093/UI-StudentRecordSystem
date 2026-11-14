package coralcove.database;

import coralcove.util.AppException;
import java.sql.Connection;
import java.sql.SQLException;

public final class Transactions {

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private Transactions() {
    }

    public static <T> T in(Work<T> work) {
        try (Connection connection = DatabaseManager.getConnection()) {
            boolean previous = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (RuntimeException ex) {
                connection.rollback();
                throw ex;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(previous);
            }
        } catch (SQLException ex) {
            throw new AppException(sqlMessage(ex), ex);
        }
    }

    public static String sqlMessage(SQLException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        String lower = message.toLowerCase();
        if (lower.contains("unique")) {
            return "That value is already in use.";
        }
        if (lower.contains("foreign key")) {
            return "This record has related history and cannot be deleted.";
        }
        return "Database error: " + message;
    }
}
