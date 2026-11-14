package coralcove.dao;

import coralcove.util.Money;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

final class Jdbc {

    private Jdbc() {
    }

    static void money(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        ps.setString(index, Money.store(value));
    }

    static BigDecimal money(ResultSet rs, String column) throws SQLException {
        return Money.parse(rs.getString(column));
    }

    static void nullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setLong(index, value);
        }
    }

    static Long longOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
