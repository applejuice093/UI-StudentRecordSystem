package coralcove.service;

import coralcove.dao.LodgeCareDAO;
import coralcove.dao.StayBookingDAO;
import coralcove.database.DatabaseManager;
import coralcove.database.Transactions;
import coralcove.model.LodgeCareTask;
import coralcove.model.StayBooking;
import coralcove.model.StayStatus;
import coralcove.util.AppException;
import coralcove.util.DateUtils;
import java.sql.SQLException;
import java.time.LocalDate;

public final class ArrivalService {

    private final StayBookingDAO stay_bookings = new StayBookingDAO();
    private final BillingService billing = new BillingService();
    private final LodgeCareDAO housekeeping = new LodgeCareDAO();
    private final LodgeStatusSync sync = new LodgeStatusSync();

    public StayBooking findByReference(String reference) {
        AuthorizationService.require(coralcove.model.AppModule.ARRIVALS);
        try (var connection = DatabaseManager.getConnection()) {
            StayBooking reservation = stay_bookings.findByReference(connection, reference == null ? "" : reference.trim());
            if (reservation == null) {
                throw new AppException("No reservation uses that booking reference.");
            }
            return reservation;
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public StayBooking checkIn(long reservationId) {
        AuthorizationService.require(coralcove.model.AppModule.ARRIVALS);
        StayBooking saved = Transactions.in(connection -> {
            StayBooking reservation = required(connection, reservationId);
            if (reservation.getStatus() != StayStatus.CONFIRMED
                    && reservation.getStatus() != StayStatus.PENDING) {
                throw new AppException("Only a pending or confirmed reservation can be checked in.");
            }
            LocalDate today = DateUtils.today();
            if (today.isBefore(reservation.getCheckIn()) || !today.isBefore(reservation.getCheckOut())) {
                throw new AppException("Check-in is allowed only on dates covered by this reservation.");
            }
            if (stay_bookings.hasActiveCheckedIn(connection, reservation.getLodgeId(), reservation.getId())) {
                throw new AppException("Another guest is already checked into this room.");
            }
            try (var ps = connection.prepareStatement("""
                    INSERT INTO arrivals(reservation_id, actual_arrival, performed_by) VALUES (?,?,?)
                    """)) {
                ps.setLong(1, reservation.getId());
                ps.setString(2, DateUtils.nowStamp());
                ps.setLong(3, Session.get().getId());
                ps.executeUpdate();
            }
            stay_bookings.updateStatus(connection, reservation.getId(), StayStatus.ARRIVED);
            billing.ensureForReservation(connection, reservation);
            sync.apply(connection, reservation.getLodgeId());
            return stay_bookings.findById(connection, reservation.getId());
        });
        Session.dataChanged();
        return saved;
    }

    public StayBooking checkOut(long reservationId) {
        AuthorizationService.require(coralcove.model.AppModule.ARRIVALS);
        StayBooking saved = Transactions.in(connection -> {
            StayBooking reservation = required(connection, reservationId);
            if (reservation.getStatus() != StayStatus.ARRIVED) {
                throw new AppException("Check-out is allowed only after the guest has checked in.");
            }
            billing.ensureForReservation(connection, reservation);
            billing.attachServedOrders(connection, reservation.getId());
            try (var ps = connection.prepareStatement(
                    "UPDATE arrivals SET actual_departure=? WHERE reservation_id=?")) {
                ps.setString(1, DateUtils.nowStamp());
                ps.setLong(2, reservation.getId());
                ps.executeUpdate();
            }
            stay_bookings.updateStatus(connection, reservation.getId(), StayStatus.DEPARTED);
            if (!housekeeping.hasOpenTask(connection, reservation.getLodgeId())) {
                LodgeCareTask task = new LodgeCareTask();
                task.setLodgeId(reservation.getLodgeId());
                task.setPriority("NORMAL");
                task.setStatus("PENDING");
                task.setNotes("Departure clean for " + reservation.getBookingReference());
                task.setInspectionStatus("NONE");
                task.setCreatedAt(DateUtils.nowStamp());
                task.setSample(reservation.isSample());
                housekeeping.insert(connection, task);
            }
            sync.apply(connection, reservation.getLodgeId());
            return stay_bookings.findById(connection, reservation.getId());
        });
        Session.dataChanged();
        return saved;
    }

    private StayBooking required(java.sql.Connection connection, long id) throws SQLException {
        StayBooking reservation = stay_bookings.findById(connection, id);
        if (reservation == null) {
            throw new AppException("StayBooking was not found.");
        }
        ResortScope.assertOwns(reservation.getResortId());
        return reservation;
    }

    public ListStay stay(long reservationId) {
        AuthorizationService.require(coralcove.model.AppModule.ARRIVALS);
        try (var connection = DatabaseManager.getConnection()) {
            return new ListStay(stay_bookings.findById(connection, reservationId),
                    billing.findForReservation(reservationId));
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public record ListStay(StayBooking reservation, coralcove.model.Invoice invoice) {
    }
}
