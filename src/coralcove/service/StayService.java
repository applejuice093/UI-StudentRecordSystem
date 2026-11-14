package coralcove.service;

import coralcove.dao.AuditDAO;
import coralcove.dao.StayBookingDAO;
import coralcove.dao.LodgeDAO;
import coralcove.database.DatabaseManager;
import coralcove.database.Transactions;
import coralcove.model.StayBooking;
import coralcove.model.StayStatus;
import coralcove.model.Lodge;
import coralcove.model.LodgeStatus;
import coralcove.util.AppException;
import coralcove.util.DateUtils;
import coralcove.util.Money;
import coralcove.util.ValidationUtils;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class StayService {

    private final StayBookingDAO stay_bookings = new StayBookingDAO();
    private final LodgeDAO lodges = new LodgeDAO();
    private final BillingService billing = new BillingService();
    private final LodgeStatusSync sync = new LodgeStatusSync();
    private final AuditDAO audit = new AuditDAO();

    public List<StayBooking> search(String reference, String guest, LocalDate from, LocalDate to, String status) {
        AuthorizationService.require(coralcove.model.AppModule.STAYS);
        try (var connection = DatabaseManager.getConnection()) {
            return stay_bookings.search(connection, reference, guest, from, to, status);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public StayBooking find(long id) {
        AuthorizationService.require(coralcove.model.AppModule.STAYS);
        try (var connection = DatabaseManager.getConnection()) {
            return stay_bookings.findById(connection, id);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public StayBooking create(StayBooking draft) {
        AuthorizationService.require(coralcove.model.AppModule.STAYS);
        validate(draft, true);
        StayBooking saved = Transactions.in(connection -> {
            prepare(connection, draft, null);
            if (draft.getBookingReference() == null || draft.getBookingReference().isBlank()) {
                draft.setBookingReference(nextReference(connection));
            } else if (stay_bookings.referenceExists(connection, draft.getBookingReference(), null)) {
                throw new AppException("Booking reference already exists.");
            }
            draft.setCreatedAt(DateUtils.nowStamp());
            draft.setId(stay_bookings.insert(connection, draft));
            stay_bookings.linkGuest(connection, draft.getId(), draft.getGuestId(), true);
            sync.apply(connection, draft.getLodgeId());
            audit.log(connection, Session.get().getId(), "CREATE", "stay_bookings",
                    draft.getBookingReference(), DateUtils.iso(draft.getCheckIn()));
            return stay_bookings.findById(connection, draft.getId());
        });
        Session.dataChanged();
        return saved;
    }

    public StayBooking update(StayBooking draft) {
        AuthorizationService.require(coralcove.model.AppModule.STAYS);
        validate(draft, false);
        StayBooking saved = Transactions.in(connection -> {
            StayBooking current = stay_bookings.findById(connection, draft.getId());
            if (current == null) {
                throw new AppException("StayBooking was not found.");
            }
            ResortScope.assertOwns(current.getResortId());
            if (current.getStatus() == StayStatus.DEPARTED || current.getStatus() == StayStatus.CANCELLED) {
                throw new AppException("This reservation can no longer be edited.");
            }
            if (current.getStatus() == StayStatus.ARRIVED) {
                current.setSpecialRequests(draft.getSpecialRequests());
                stay_bookings.update(connection, current);
                return stay_bookings.findById(connection, current.getId());
            }
            long previousRoom = current.getLodgeId();
            prepare(connection, draft, draft.getId());
            draft.setCreatedAt(current.getCreatedAt());
            draft.setSample(current.isSample());
            if (stay_bookings.referenceExists(connection, draft.getBookingReference(), draft.getId())) {
                throw new AppException("Booking reference already exists.");
            }
            stay_bookings.update(connection, draft);
            stay_bookings.linkGuest(connection, draft.getId(), draft.getGuestId(), true);
            sync.apply(connection, draft.getLodgeId());
            if (previousRoom != draft.getLodgeId()) {
                sync.apply(connection, previousRoom);
            }
            return stay_bookings.findById(connection, draft.getId());
        });
        Session.dataChanged();
        return saved;
    }

    public void cancel(long id) {
        AuthorizationService.require(coralcove.model.AppModule.STAYS);
        Transactions.in(connection -> {
            StayBooking reservation = stay_bookings.findById(connection, id);
            if (reservation == null) {
                throw new AppException("StayBooking was not found.");
            }
            ResortScope.assertOwns(reservation.getResortId());
            if (reservation.getStatus() == StayStatus.ARRIVED
                    || reservation.getStatus() == StayStatus.DEPARTED) {
                throw new AppException("Checked-in stays must be checked out instead of cancelled.");
            }
            stay_bookings.updateStatus(connection, id, StayStatus.CANCELLED);
            sync.apply(connection, reservation.getLodgeId());
            audit.log(connection, Session.get().getId(), "CANCEL", "stay_bookings",
                    reservation.getBookingReference(), "");
            return null;
        });
        Session.dataChanged();
    }

    public void markNoShow(long id) {
        AuthorizationService.require(coralcove.model.AppModule.STAYS);
        Transactions.in(connection -> {
            StayBooking reservation = stay_bookings.findById(connection, id);
            if (reservation.getStatus() != StayStatus.CONFIRMED && reservation.getStatus() != StayStatus.PENDING) {
                throw new AppException("Only a pending or confirmed reservation can be marked as a no-show.");
            }
            stay_bookings.updateStatus(connection, id, StayStatus.NO_SHOW);
            sync.apply(connection, reservation.getLodgeId());
            return null;
        });
        Session.dataChanged();
    }

    private void prepare(java.sql.Connection connection, StayBooking draft, Long excludeId) throws SQLException {
        Lodge room = lodges.findById(connection, draft.getLodgeId());
        if (room == null) {
            throw new AppException("Lodge was not found.");
        }
        ResortScope.assertOwns(room.getResortId());
        if (draft.getResortId() > 0 && draft.getResortId() != room.getResortId()) {
            throw new AppException("That record belongs to another resort.");
        }
        draft.setResortId(room.getResortId());
        if (room.getStatus() == LodgeStatus.CLOSED || room.getStatus() == LodgeStatus.REPAIR) {
            throw new AppException("That lodge is not available for booking.");
        }
        if (draft.getAdults() + draft.getChildren() > room.getMaxGuests()) {
            throw new AppException("Guest count exceeds the lodge maximum of " + room.getMaxGuests() + ".");
        }
        if (stay_bookings.overlaps(connection, draft.getLodgeId(), draft.getCheckIn(), draft.getCheckOut(), excludeId)) {
            throw new AppException("That lodge is already booked for overlapping dates.");
        }
        draft.setRoomPrice(room.getPricePerNight());
        if (draft.getDiscount() == null) {
            draft.setDiscount(Money.zero());
        }
        billing.priceStay(draft);
        if (draft.getStatus() != StayStatus.PENDING && draft.getStatus() != StayStatus.CONFIRMED) {
            draft.setStatus(StayStatus.CONFIRMED);
        }
    }

    private void validate(StayBooking draft, boolean create) {
        if (!create && draft.getId() <= 0) {
            throw new AppException("StayBooking was not found.");
        }
        if (draft.getGuestId() <= 0 || draft.getLodgeId() <= 0) {
            throw new AppException("Guest and lodge are required.");
        }
        if (draft.getCheckIn() == null || draft.getCheckOut() == null) {
            throw new AppException("Check-in and check-out dates are required.");
        }
        DateUtils.nights(draft.getCheckIn(), draft.getCheckOut());
        if (draft.getAdults() <= 0) {
            throw new AppException("At least one adult is required.");
        }
        if (draft.getChildren() < 0) {
            throw new AppException("Children cannot be negative.");
        }
        Money.requireNonNegative(draft.getDiscount() == null ? Money.zero() : draft.getDiscount(), "Discount");
        if (draft.getSource() == null) {
            throw new AppException("Booking source is required.");
        }
        draft.setSpecialRequests(ValidationUtils.optional(draft.getSpecialRequests()));
        if (draft.getBookingReference() != null) {
            draft.setBookingReference(draft.getBookingReference().trim());
            ValidationUtils.maxLength(draft.getBookingReference(), 30, "Booking reference");
        }
    }

    private String nextReference(java.sql.Connection connection) throws SQLException {
        String reference;
        do {
            reference = "GS" + DateUtils.today().toString().replace("-", "") + "-"
                    + (1000 + ThreadLocalRandom.current().nextInt(9000));
        } while (stay_bookings.referenceExists(connection, reference, null));
        return reference;
    }
}
