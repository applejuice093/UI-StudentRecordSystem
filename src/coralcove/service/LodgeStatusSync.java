package coralcove.service;

import coralcove.dao.LodgeCareDAO;
import coralcove.dao.StayBookingDAO;
import coralcove.dao.LodgeDAO;
import coralcove.model.Lodge;
import coralcove.model.LodgeStatus;
import coralcove.util.DateUtils;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Derives operational room status from stays, open cleaning tasks, and open maintenance.
 * A room becomes AVAILABLE only when it is not occupied, not waiting on housekeeping,
 * not under open maintenance, and no non-cancelled hold covers today.
 */
public final class LodgeStatusSync {

    private final LodgeDAO lodges = new LodgeDAO();
    private final StayBookingDAO stay_bookings = new StayBookingDAO();
    private final LodgeCareDAO housekeeping = new LodgeCareDAO();

    public void apply(Connection connection, long lodgeId) throws SQLException {
        Lodge room = lodges.findById(connection, lodgeId);
        if (room == null || room.getStatus() == LodgeStatus.CLOSED) {
            return;
        }
        if (stay_bookings.hasActiveCheckedIn(connection, lodgeId, null)) {
            lodges.updateStatus(connection, lodgeId, LodgeStatus.IN_STAY);
            return;
        }
        if (housekeeping.hasOpenTask(connection, lodgeId)) {
            lodges.updateStatus(connection, lodgeId, LodgeStatus.TURNDOWN);
            return;
        }
        if (housekeeping.hasOpenMaintenance(connection, lodgeId)) {
            lodges.updateStatus(connection, lodgeId, LodgeStatus.REPAIR);
            return;
        }
        if (stay_bookings.hasUpcomingHold(connection, lodgeId, DateUtils.today())) {
            lodges.updateStatus(connection, lodgeId, LodgeStatus.HELD);
            return;
        }
        lodges.updateStatus(connection, lodgeId, LodgeStatus.READY);
    }
}
