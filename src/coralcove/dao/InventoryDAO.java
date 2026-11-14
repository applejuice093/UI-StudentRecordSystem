package coralcove.dao;

import coralcove.model.InventoryItem;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class InventoryDAO {

    public List<InventoryItem> search(Connection connection, String text, String category) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT i.*, s.name AS supplier_name FROM inventory_items i
                LEFT JOIN suppliers s ON s.id = i.supplier_id WHERE 1=1
                """);
        List<String> args = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            sql.append(" AND (i.name LIKE ? OR i.sku LIKE ?)");
            args.add("%" + text.trim() + "%");
            args.add("%" + text.trim() + "%");
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND i.category=?");
            args.add(category);
        }
        Long resortFilter = coralcove.service.Session.resortFilter();
        if (resortFilter != null) {
            sql.append(" AND i.resort_id=").append(resortFilter.longValue());
        } else {
            sql.append(" AND i.resort_id IN (SELECT id FROM resorts WHERE active=1)");
        }
        sql.append(" ORDER BY i.name");
        try (var ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) {
                ps.setString(i + 1, args.get(i));
            }
            try (var rs = ps.executeQuery()) {
                List<InventoryItem> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(map(rs));
                }
                return rows;
            }
        }
    }

    public long addSupplier(Connection connection, String name, String phone, String email, boolean sample)
            throws SQLException {
        try (var ps = connection.prepareStatement(
                "INSERT INTO suppliers(name, phone, email, is_sample) VALUES (?,?,?,?)",
                java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.setInt(4, sample ? 1 : 0);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public List<String[]> suppliers(Connection connection) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT id, name FROM suppliers ORDER BY name");
                var rs = ps.executeQuery()) {
            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new String[] {rs.getString(1), rs.getString(2)});
            }
            return rows;
        }
    }

    public long insert(Connection connection, InventoryItem item) throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO inventory_items(resort_id, sku, name, category, supplier_id, quantity, min_quantity, unit, is_sample)
                VALUES (?,?,?,?,?,?,?,?,?)
                """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, item);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void update(Connection connection, InventoryItem item) throws SQLException {
        try (var ps = connection.prepareStatement("""
                UPDATE inventory_items SET resort_id=?, sku=?, name=?, category=?, supplier_id=?, quantity=?,
                    min_quantity=?, unit=?, is_sample=? WHERE id=?
                """)) {
            fill(ps, item);
            ps.setLong(10, item.getId());
            ps.executeUpdate();
        }
    }

    public void adjust(Connection connection, long itemId, int delta, String reason, String note, String at)
            throws SQLException {
        try (var ps = connection.prepareStatement(
                "UPDATE inventory_items SET quantity = quantity + ? WHERE id=?")) {
            ps.setInt(1, delta);
            ps.setLong(2, itemId);
            ps.executeUpdate();
        }
        try (var ps = connection.prepareStatement("""
                INSERT INTO stock_transactions(item_id, qty_delta, reason, note, created_at) VALUES (?,?,?,?,?)
                """)) {
            ps.setLong(1, itemId);
            ps.setInt(2, delta);
            ps.setString(3, reason);
            ps.setString(4, note);
            ps.setString(5, at);
            ps.executeUpdate();
        }
    }

    public List<String[]> history(Connection connection, long itemId) throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT created_at, reason, qty_delta, note FROM stock_transactions
                WHERE item_id=? ORDER BY id DESC
                """)) {
            ps.setLong(1, itemId);
            try (var rs = ps.executeQuery()) {
                List<String[]> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new String[] {rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)});
                }
                return rows;
            }
        }
    }

    public InventoryItem find(Connection connection, long id) throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT i.*, s.name AS supplier_name FROM inventory_items i
                LEFT JOIN suppliers s ON s.id=i.supplier_id WHERE i.id=?
                """)) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private void fill(java.sql.PreparedStatement ps, InventoryItem item) throws SQLException {
        ps.setLong(1, item.getResortId());
        ps.setString(2, item.getSku());
        ps.setString(3, item.getName());
        ps.setString(4, item.getCategory());
        Jdbc.nullableLong(ps, 5, item.getSupplierId());
        ps.setInt(6, item.getQuantity());
        ps.setInt(7, item.getMinQuantity());
        ps.setString(8, item.getUnit());
        ps.setInt(9, item.isSample() ? 1 : 0);
    }

    private InventoryItem map(ResultSet rs) throws SQLException {
        InventoryItem item = new InventoryItem();
        item.setId(rs.getLong("id"));
        item.setResortId(rs.getLong("resort_id"));
        item.setSku(rs.getString("sku"));
        item.setName(rs.getString("name"));
        item.setCategory(rs.getString("category"));
        item.setSupplierId(Jdbc.longOrNull(rs, "supplier_id"));
        item.setSupplierName(rs.getString("supplier_name"));
        item.setQuantity(rs.getInt("quantity"));
        item.setMinQuantity(rs.getInt("min_quantity"));
        item.setUnit(rs.getString("unit"));
        item.setSample(rs.getInt("is_sample") == 1);
        return item;
    }
}
