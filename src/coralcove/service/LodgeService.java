package coralcove.service;

import coralcove.dao.AuditDAO;
import coralcove.dao.LodgeDAO;
import coralcove.database.DatabaseManager;
import coralcove.database.Transactions;
import coralcove.model.Lodge;
import coralcove.model.LodgeStatus;
import coralcove.model.LodgeCategory;
import coralcove.util.AppException;
import coralcove.util.Money;
import coralcove.util.ValidationUtils;
import java.sql.SQLException;
import java.util.List;

public final class LodgeService {

    private final LodgeDAO lodges = new LodgeDAO();
    private final AuditDAO audit = new AuditDAO();
    private final LodgeStatusSync sync = new LodgeStatusSync();

    public List<LodgeCategory> types() {
        AuthorizationService.require(coralcove.model.AppModule.LODGES);
        try (var connection = DatabaseManager.getConnection()) {
            return lodges.types(connection);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public List<Lodge> search(String text, String zone, Long typeId, String status) {
        AuthorizationService.require(coralcove.model.AppModule.LODGES);
        try (var connection = DatabaseManager.getConnection()) {
            return lodges.search(connection, text, zone, typeId, status);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public List<Lodge> availableForStay(java.time.LocalDate checkIn, java.time.LocalDate checkOut, Long ignoreReservation) {
        AuthorizationService.require(coralcove.model.AppModule.LODGES);
        try (var connection = DatabaseManager.getConnection()) {
            List<Lodge> all = lodges.search(connection, null, null, null, null);
            var stay_bookings = new coralcove.dao.StayBookingDAO();
            List<Lodge> free = new java.util.ArrayList<>();
            for (Lodge room : all) {
                if (room.getStatus() == LodgeStatus.CLOSED || room.getStatus() == LodgeStatus.REPAIR) {
                    continue;
                }
                if (!stay_bookings.overlaps(connection, room.getId(), checkIn, checkOut, ignoreReservation)) {
                    free.add(room);
                }
            }
            return free;
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public Lodge save(Lodge room, boolean create) {
        AuthorizationService.requireRoomWrite();
        validate(room);
        Lodge saved = Transactions.in(connection -> {
            room.setResortId(ResortScope.resolve(connection, room.getResortId()));
            if (!create) {
                Lodge current = lodges.findById(connection, room.getId());
                if (current == null) {
                    throw new AppException("Lodge was not found.");
                }
                ResortScope.assertOwns(current.getResortId());
            }
            Lodge existing = lodges.findByNumber(connection, room.getResortId(), room.getUnitCode());
            if (existing != null && (create || existing.getId() != room.getId())) {
                throw new AppException("Lodge number " + room.getUnitCode() + " already exists.");
            }
            if (create) {
                if (room.getStatus() == null) {
                    room.setStatus(LodgeStatus.READY);
                }
                room.setId(lodges.insert(connection, room));
            } else {
                lodges.update(connection, room);
                if (room.getStatus() != LodgeStatus.CLOSED) {
                    sync.apply(connection, room.getId());
                }
            }
            audit.log(connection, Session.get().getId(), create ? "CREATE" : "UPDATE", "lodges",
                    String.valueOf(room.getId()), room.getUnitCode());
            return lodges.findById(connection, room.getId());
        });
        Session.dataChanged();
        return saved;
    }

    public void changeStatus(long lodgeId, LodgeStatus status) {
        AuthorizationService.requireRoomWrite();
        if (status == LodgeStatus.IN_STAY) {
            throw new AppException("In stay is set by arrival, not by a manual status change.");
        }
        Transactions.in(connection -> {
            Lodge room = lodges.findById(connection, lodgeId);
            if (room == null) {
                throw new AppException("Lodge was not found.");
            }
            ResortScope.assertOwns(room.getResortId());
            var stay_bookings = new coralcove.dao.StayBookingDAO();
            boolean checkedIn = stay_bookings.hasActiveCheckedIn(connection, lodgeId, null);
            boolean hold = stay_bookings.hasUpcomingHold(connection, lodgeId, coralcove.util.DateUtils.today());
            if (checkedIn) {
                throw new AppException("This lodge has a guest in stay. Depart the guest before changing status.");
            }
            if ((status == LodgeStatus.READY || status == LodgeStatus.CLOSED) && hold) {
                throw new AppException("This lodge has a stay covering today. Cancel or move that booking first.");
            }
            lodges.updateStatus(connection, lodgeId, status);
            if (status != LodgeStatus.CLOSED) {
                sync.apply(connection, lodgeId);
            }
            return null;
        });
        Session.dataChanged();
    }

    public void delete(long lodgeId) {
        AuthorizationService.requireRoomWrite();
        Transactions.in(connection -> {
            Lodge current = lodges.findById(connection, lodgeId);
            if (current == null) {
                throw new AppException("Lodge was not found.");
            }
            ResortScope.assertOwns(current.getResortId());
            if (lodges.countReservations(connection, lodgeId) > 0) {
                throw new AppException("This lodge has stay history and cannot be deleted.");
            }
            lodges.delete(connection, lodgeId);
            audit.log(connection, Session.get().getId(), "DELETE", "lodges", String.valueOf(lodgeId), "");
            return null;
        });
        Session.dataChanged();
    }

    private void validate(Lodge room) {
        room.setUnitCode(ValidationUtils.required(room.getUnitCode(), "Lodge number"));
        ValidationUtils.maxLength(room.getUnitCode(), 10, "Lodge number");
        if (!java.util.Set.of("BEACH", "LAGOON", "GARDEN", "CLIFF").contains(room.getZone())) {
            throw new AppException("Zone must be Beach, Lagoon, Garden, or Cliff.");
        }
        if (!java.util.Set.of("SEA", "GARDEN", "POOL").contains(room.getViewAspect())) {
            throw new AppException("View must be Sea, Garden, or Pool.");
        }
        if (room.getLodgeCategoryId() <= 0) {
            throw new AppException("Lodge type is required.");
        }
        if (room.getMaxGuests() <= 0) {
            throw new AppException("Maximum guests must be greater than zero.");
        }
        Money.requireNonNegative(room.getPricePerNight(), "Price per night");
        if (room.getPricePerNight().signum() == 0) {
            throw new AppException("Price per night must be greater than zero.");
        }
        room.setBedConfig(ValidationUtils.optional(room.getBedConfig()));
        room.setDescription(ValidationUtils.optional(room.getDescription()));
        room.setAmenities(ValidationUtils.optional(room.getAmenities()));
        if (room.getStatus() == null) {
            room.setStatus(LodgeStatus.READY);
        }
    }
}
