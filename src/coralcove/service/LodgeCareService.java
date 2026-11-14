package coralcove.service;

import coralcove.dao.LodgeCareDAO;
import coralcove.dao.LodgeDAO;
import coralcove.database.DatabaseManager;
import coralcove.database.Transactions;
import coralcove.model.LodgeCareTask;
import coralcove.model.Lodge;
import coralcove.model.LodgeStatus;
import coralcove.util.AppException;
import coralcove.util.DateUtils;
import coralcove.util.ValidationUtils;
import java.sql.SQLException;
import java.util.List;

public final class LodgeCareService {

    private final LodgeCareDAO tasks = new LodgeCareDAO();
    private final LodgeDAO lodges = new LodgeDAO();
    private final LodgeStatusSync sync = new LodgeStatusSync();

    public List<LodgeCareTask> search(String status, String priority) {
        AuthorizationService.require(coralcove.model.AppModule.LODGE_CARE);
        try (var connection = DatabaseManager.getConnection()) {
            return tasks.search(connection, status, priority);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public void create(long lodgeId, Long assigneeId, String priority, String notes) {
        AuthorizationService.requireHousekeepingWrite();
        Transactions.in(connection -> {
            LodgeCareTask task = new LodgeCareTask();
            task.setLodgeId(lodgeId);
            task.setAssigneeId(assigneeId);
            task.setPriority(priority == null ? "NORMAL" : priority);
            task.setStatus(assigneeId == null ? "PENDING" : "ASSIGNED");
            task.setNotes(ValidationUtils.optional(notes));
            task.setInspectionStatus("NONE");
            task.setCreatedAt(DateUtils.nowStamp());
            tasks.insert(connection, task);
            sync.apply(connection, lodgeId);
            return null;
        });
        Session.dataChanged();
    }

    public void update(long taskId, Long assigneeId, String priority, String status, String inspection, String notes) {
        AuthorizationService.requireHousekeepingWrite();
        Transactions.in(connection -> {
            LodgeCareTask task = tasks.find(connection, taskId);
            if (task == null) {
                throw new AppException("Task was not found.");
            }
            task.setAssigneeId(assigneeId);
            task.setPriority(priority);
            task.setStatus(status);
            task.setInspectionStatus(inspection == null ? "NONE" : inspection);
            task.setNotes(notes);
            if ("COMPLETED".equals(status)) {
                if (!"PASSED".equals(task.getInspectionStatus())) {
                    task.setStatus("INSPECTION_REQUIRED");
                    task.setCompletedAt(null);
                } else {
                    task.setCompletedAt(DateUtils.nowStamp());
                }
            } else {
                task.setCompletedAt(null);
            }
            if (assigneeId != null && "PENDING".equals(task.getStatus())) {
                task.setStatus("ASSIGNED");
            }
            tasks.update(connection, task);
            sync.apply(connection, task.getLodgeId());
            return null;
        });
        Session.dataChanged();
    }

    public void reportMaintenance(long lodgeId, String description) {
        AuthorizationService.requireHousekeepingWrite();
        ValidationUtils.required(description, "Description");
        Transactions.in(connection -> {
            Lodge room = lodges.findById(connection, lodgeId);
            if (room == null) {
                throw new AppException("Lodge was not found.");
            }
            tasks.addMaintenance(connection, lodgeId, Session.get().getUsername(), description.trim(), "OPEN",
                    DateUtils.nowStamp(), false);
            if (room.getStatus() != LodgeStatus.IN_STAY) {
                sync.apply(connection, lodgeId);
            }
            return null;
        });
        Session.dataChanged();
    }

    public List<Lodge> lodges() {
        AuthorizationService.require(coralcove.model.AppModule.LODGE_CARE);
        try (var connection = DatabaseManager.getConnection()) {
            return lodges.search(connection, null, null, null, null);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public List<coralcove.model.Employee> staff() {
        AuthorizationService.require(coralcove.model.AppModule.LODGE_CARE);
        try (var connection = DatabaseManager.getConnection()) {
            return new coralcove.dao.EmployeeDAO().search(connection, null, "Housekeeping");
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public List<String[]> openMaintenance() {
        AuthorizationService.require(coralcove.model.AppModule.LODGE_CARE);
        try (var connection = DatabaseManager.getConnection()) {
            return tasks.openMaintenance(connection);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public void resolveMaintenance(long id, long lodgeId) {
        AuthorizationService.requireHousekeepingWrite();
        Transactions.in(connection -> {
            tasks.resolveMaintenance(connection, id);
            sync.apply(connection, lodgeId);
            return null;
        });
        Session.dataChanged();
    }
}
