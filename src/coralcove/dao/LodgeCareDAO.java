package coralcove.dao;

import coralcove.model.LodgeCareTask;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class LodgeCareDAO {

    public List<LodgeCareTask> search(Connection connection, String status, String priority) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT t.*, r.unit_code, e.name AS assignee_name
                FROM lodge_care_tasks t
                JOIN lodges r ON r.id = t.lodge_id
                LEFT JOIN employees e ON e.id = t.assignee_employee_id
                WHERE 1=1
                """);
        List<String> args = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append(" AND t.status=?");
            args.add(status);
        }
        if (priority != null && !priority.isBlank()) {
            sql.append(" AND t.priority=?");
            args.add(priority);
        }
        sql.append(scope("r.resort_id"));
        sql.append(" ORDER BY CASE t.priority WHEN 'HIGH' THEN 0 WHEN 'NORMAL' THEN 1 ELSE 2 END, t.id DESC");
        try (var ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) {
                ps.setString(i + 1, args.get(i));
            }
            try (var rs = ps.executeQuery()) {
                List<LodgeCareTask> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(map(rs));
                }
                return rows;
            }
        }
    }

    public LodgeCareTask find(Connection connection, long id) throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT t.*, r.unit_code, e.name AS assignee_name
                FROM lodge_care_tasks t
                JOIN lodges r ON r.id = t.lodge_id
                LEFT JOIN employees e ON e.id = t.assignee_employee_id
                WHERE t.id=?
                """)) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean hasOpenTask(Connection connection, long lodgeId) throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT COUNT(*) FROM lodge_care_tasks
                WHERE lodge_id=? AND status <> 'COMPLETED'
                """)) {
            ps.setLong(1, lodgeId);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public long insert(Connection connection, LodgeCareTask task) throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO lodge_care_tasks(lodge_id, assignee_employee_id, priority, status, notes,
                    inspection_status, created_at, completed_at, is_sample)
                VALUES (?,?,?,?,?,?,?,?,?)
                """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, task.getLodgeId());
            Jdbc.nullableLong(ps, 2, task.getAssigneeId());
            ps.setString(3, task.getPriority());
            ps.setString(4, task.getStatus());
            ps.setString(5, task.getNotes());
            ps.setString(6, task.getInspectionStatus());
            ps.setString(7, task.getCreatedAt());
            ps.setString(8, task.getCompletedAt());
            ps.setInt(9, task.isSample() ? 1 : 0);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void update(Connection connection, LodgeCareTask task) throws SQLException {
        try (var ps = connection.prepareStatement("""
                UPDATE lodge_care_tasks SET assignee_employee_id=?, priority=?, status=?, notes=?,
                    inspection_status=?, completed_at=? WHERE id=?
                """)) {
            Jdbc.nullableLong(ps, 1, task.getAssigneeId());
            ps.setString(2, task.getPriority());
            ps.setString(3, task.getStatus());
            ps.setString(4, task.getNotes());
            ps.setString(5, task.getInspectionStatus());
            ps.setString(6, task.getCompletedAt());
            ps.setLong(7, task.getId());
            ps.executeUpdate();
        }
    }

    public void addMaintenance(Connection connection, long lodgeId, String reportedBy, String description, String status,
            String createdAt, boolean sample) throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO estate_repairs(lodge_id, reported_by, description, status, created_at, is_sample)
                VALUES (?,?,?,?,?,?)
                """)) {
            ps.setLong(1, lodgeId);
            ps.setString(2, reportedBy);
            ps.setString(3, description);
            ps.setString(4, status);
            ps.setString(5, createdAt);
            ps.setInt(6, sample ? 1 : 0);
            ps.executeUpdate();
        }
    }

    public List<String[]> openMaintenance(Connection connection) throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT m.id, r.unit_code, m.description, m.status, m.created_at
                FROM estate_repairs m JOIN lodges r ON r.id=m.lodge_id
                WHERE m.status <> 'RESOLVED'
                """ + scope("r.resort_id") + " ORDER BY m.id DESC");
                var rs = ps.executeQuery()) {
            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new String[] {
                        rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)
                });
            }
            return rows;
        }
    }

    private static String scope(String column) {
        Long filter = coralcove.service.Session.resortFilter();
        if (filter != null) {
            return " AND " + column + " = " + filter.longValue();
        }
        return " AND " + column + " IN (SELECT id FROM resorts WHERE active = 1)";
    }

    public boolean hasOpenMaintenance(Connection connection, long lodgeId) throws SQLException {
        try (var ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM estate_repairs WHERE lodge_id=? AND status<>'RESOLVED'")) {
            ps.setLong(1, lodgeId);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public void resolveMaintenance(Connection connection, long id) throws SQLException {
        try (var ps = connection.prepareStatement("UPDATE estate_repairs SET status='RESOLVED' WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private LodgeCareTask map(ResultSet rs) throws SQLException {
        LodgeCareTask task = new LodgeCareTask();
        task.setId(rs.getLong("id"));
        task.setLodgeId(rs.getLong("lodge_id"));
        task.setUnitCode(rs.getString("unit_code"));
        task.setAssigneeId(Jdbc.longOrNull(rs, "assignee_employee_id"));
        task.setAssigneeName(rs.getString("assignee_name"));
        task.setPriority(rs.getString("priority"));
        task.setStatus(rs.getString("status"));
        task.setNotes(rs.getString("notes"));
        task.setInspectionStatus(rs.getString("inspection_status"));
        task.setCreatedAt(rs.getString("created_at"));
        task.setCompletedAt(rs.getString("completed_at"));
        task.setSample(rs.getInt("is_sample") == 1);
        return task;
    }
}
