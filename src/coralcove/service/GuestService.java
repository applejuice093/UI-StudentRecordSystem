package coralcove.service;

import coralcove.dao.GuestDAO;
import coralcove.dao.InvoiceDAO;
import coralcove.dao.StayBookingDAO;
import coralcove.database.DatabaseManager;
import coralcove.database.Transactions;
import coralcove.model.Guest;
import coralcove.model.StayBooking;
import coralcove.model.StayStatus;
import coralcove.util.AppException;
import coralcove.util.DateUtils;
import coralcove.util.Money;
import coralcove.util.ValidationUtils;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public final class GuestService {

    private final GuestDAO guests = new GuestDAO();

    public List<Guest> search(String text) {
        AuthorizationService.require(coralcove.model.AppModule.GUESTS);
        try (var connection = DatabaseManager.getConnection()) {
            return guests.search(connection, text);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public Guest save(Guest guest, boolean create) {
        AuthorizationService.require(coralcove.model.AppModule.GUESTS);
        guest.setFullName(ValidationUtils.required(guest.getFullName(), "Full name"));
        ValidationUtils.maxLength(guest.getFullName(), 120, "Full name");
        ValidationUtils.email(guest.getEmail(), false);
        guest.setPhone(ValidationUtils.optional(guest.getPhone()));
        if (guest.getRegisteredOn() == null || guest.getRegisteredOn().isBlank()) {
            guest.setRegisteredOn(DateUtils.iso(DateUtils.today()));
        }
        if (guest.getVip() == null || guest.getVip().isBlank()) {
            guest.setVip("STANDARD");
        }
        Guest saved = Transactions.in(connection -> {
            if (create) {
                guest.setId(guests.insert(connection, guest));
            } else {
                guests.update(connection, guest);
            }
            return guests.findById(connection, guest.getId());
        });
        Session.dataChanged();
        return saved;
    }

    public void delete(long id) {
        AuthorizationService.require(coralcove.model.AppModule.GUESTS);
        Transactions.in(connection -> {
            if (guests.countReservations(connection, id) > 0) {
                throw new AppException("This guest has reservation history and cannot be deleted.");
            }
            guests.delete(connection, id);
            return null;
        });
        Session.dataChanged();
    }

    public GuestHistory history(long guestId) {
        AuthorizationService.require(coralcove.model.AppModule.GUESTS);
        try (var connection = DatabaseManager.getConnection()) {
            List<StayBooking> stays = new StayBookingDAO().forGuest(connection, guestId);
            int visits = 0;
            for (StayBooking stay : stays) {
                if (stay.getStatus() == StayStatus.ARRIVED || stay.getStatus() == StayStatus.DEPARTED) {
                    visits++;
                }
            }
            BigDecimal spent = Money.zero();
            InvoiceDAO invoices = new InvoiceDAO();
            for (StayBooking stay : stays) {
                var invoice = invoices.findByReservation(connection, stay.getId());
                if (invoice != null) {
                    spent = spent.add(invoice.getAmountPaid());
                }
            }
            return new GuestHistory(visits, spent, stays);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public void applyVipRule(long guestId) {
        GuestHistory history = history(guestId);
        boolean vip = history.visits() >= 3 || history.spent().compareTo(new BigDecimal("50000.00")) >= 0;
        Transactions.in(connection -> {
            Guest guest = guests.findById(connection, guestId);
            guest.setVip(vip ? "VIP" : "STANDARD");
            guests.update(connection, guest);
            return null;
        });
        Session.dataChanged();
    }

    public record GuestHistory(int visits, BigDecimal spent, List<StayBooking> stays) {
    }
}
