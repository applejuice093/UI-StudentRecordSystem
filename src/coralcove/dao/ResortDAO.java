package coralcove.dao;

import coralcove.model.Resort;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ResortDAO {

    public List<Resort> list(Connection connection, boolean activeOnly) throws SQLException {
        String sql = "SELECT * FROM resorts" + (activeOnly ? " WHERE active=1" : "") + " ORDER BY name";
        try (var ps = connection.prepareStatement(sql); var rs = ps.executeQuery()) {
            List<Resort> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(map(rs));
            }
            return rows;
        }
    }

    public Resort find(Connection connection, long id) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT * FROM resorts WHERE id=?")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public long insert(Connection connection, Resort resort) throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO resorts(name, location, coastline, timezone, tax_percent, active, is_sample)
                VALUES (?,?,?,?,?,?,?)
                """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, resort);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void update(Connection connection, Resort resort) throws SQLException {
        try (var ps = connection.prepareStatement("""
                UPDATE resorts SET name=?, location=?, coastline=?, timezone=?, tax_percent=?, active=?, is_sample=?
                WHERE id=?
                """)) {
            fill(ps, resort);
            ps.setLong(8, resort.getId());
            ps.executeUpdate();
        }
    }

    private void fill(java.sql.PreparedStatement ps, Resort resort) throws SQLException {
        ps.setString(1, resort.getName());
        ps.setString(2, resort.getLocation());
        ps.setString(3, resort.getCoastline());
        ps.setString(4, resort.getTimezone());
        ps.setString(5, resort.getTaxPercent());
        ps.setInt(6, resort.isActive() ? 1 : 0);
        ps.setInt(7, resort.isSample() ? 1 : 0);
    }

    private Resort map(ResultSet rs) throws SQLException {
        Resort resort = new Resort();
        resort.setId(rs.getLong("id"));
        resort.setName(rs.getString("name"));
        resort.setLocation(rs.getString("location"));
        resort.setCoastline(rs.getString("coastline"));
        resort.setTimezone(rs.getString("timezone"));
        resort.setTaxPercent(rs.getString("tax_percent"));
        resort.setActive(rs.getInt("active") == 1);
        resort.setSample(rs.getInt("is_sample") == 1);
        return resort;
    }
}
