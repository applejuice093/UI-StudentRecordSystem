package coralcove.service;

import coralcove.util.AppException;
import java.sql.Connection;
import java.sql.SQLException;

/** Decides which resort a write belongs to, and blocks cross-resort edits. */
public final class ResortScope {

    private ResortScope() {
    }

    public static void assertOwns(long resortId) {
        Long filter = Session.resortFilter();
        if (filter != null && filter.longValue() != resortId) {
            throw new AppException("That record belongs to another resort.");
        }
    }

    public static long resolve(Connection connection, long chosen) throws SQLException {
        if (chosen > 0) {
            assertOwns(chosen);
            return chosen;
        }
        Long filter = Session.resortFilter();
        if (filter != null) {
            return filter;
        }
        try (var ps = connection.prepareStatement("SELECT id FROM resorts WHERE active=1 ORDER BY id LIMIT 1");
                var rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        throw new AppException("Choose a resort before saving.");
    }
}
