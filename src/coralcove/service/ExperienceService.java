package coralcove.service;

import coralcove.dao.StayBookingDAO;
import coralcove.dao.ExperienceDAO;
import coralcove.database.DatabaseManager;
import coralcove.database.Transactions;
import coralcove.model.AppModule;
import coralcove.model.StayBooking;
import coralcove.model.StayStatus;
import coralcove.model.Experience;
import coralcove.model.ExperienceBooking;
import coralcove.util.AppException;
import coralcove.util.DateUtils;
import coralcove.util.Money;
import coralcove.util.ValidationUtils;
import java.sql.SQLException;
import java.util.List;

public final class ExperienceService {

    private final ExperienceDAO orders = new ExperienceDAO();
    private final BillingService billing = new BillingService();

    public List<Experience> menu() {
        AuthorizationService.require(AppModule.EXPERIENCES);
        try (var connection = DatabaseManager.getConnection()) {
            return orders.menu(connection, false);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public void saveItem(Experience item, boolean create) {
        AuthorizationService.require(AppModule.EXPERIENCES);
        item.setName(ValidationUtils.required(item.getName(), "Item name"));
        item.setCategory(ValidationUtils.required(item.getCategory(), "Category"));
        Money.requireNonNegative(item.getPrice(), "Price");
        Transactions.in(connection -> {
            if (!create) {
                Experience current = orders.menu(connection, false).stream()
                        .filter(row -> row.getId() == item.getId())
                        .findFirst()
                        .orElseThrow(() -> new AppException("Experience was not found."));
                ResortScope.assertOwns(current.getResortId());
            }
            item.setResortId(ResortScope.resolve(connection, item.getResortId()));
            if (create) {
                orders.insertItem(connection, item);
            } else {
                orders.updateItem(connection, item);
            }
            return null;
        });
    }

    public List<ExperienceBooking> orders(String status) {
        AuthorizationService.require(AppModule.EXPERIENCES);
        try (var connection = DatabaseManager.getConnection()) {
            return orders.orders(connection, status);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public void place(Long experienceId, String description, int qty, String orderType, Long lodgeId) {
        AuthorizationService.require(AppModule.EXPERIENCES);
        if (qty <= 0) {
            throw new AppException("Quantity must be greater than zero.");
        }
        Transactions.in(connection -> {
            ExperienceBooking order = new ExperienceBooking();
            order.setOrderType(orderType);
            order.setQty(qty);
            order.setStatus("NEW");
            order.setCreatedAt(DateUtils.nowStamp());
            if (experienceId != null) {
                Experience item = orders.menu(connection, true).stream()
                        .filter(row -> row.getId() == experienceId)
                        .findFirst()
                        .orElseThrow(() -> new AppException("Menu item was not found."));
                order.setServiceId(item.getId());
                order.setDescription(item.getName());
                order.setUnitPrice(item.getPrice());
            } else {
                order.setDescription(ValidationUtils.required(description, "Description"));
                throw new AppException("Choose a menu item.");
            }
            order.setLineTotal(Money.multiply(order.getUnitPrice(), qty));
            if ("ROOM_SERVICE".equals(orderType)) {
                if (lodgeId == null) {
                    throw new AppException("Lodge service needs a room.");
                }
                StayBooking stay = activeStay(connection, lodgeId);
                order.setLodgeId(lodgeId);
                order.setReservationId(stay.getId());
                order.setGuestId(stay.getGuestId());
            }
            orders.insertOrder(connection, order);
            return null;
        });
        Session.dataChanged();
    }

    public void advance(long orderId, String status) {
        AuthorizationService.require(AppModule.EXPERIENCES);
        Transactions.in(connection -> {
            ExperienceBooking order = orders.find(connection, orderId);
            if (order == null) {
                throw new AppException("Order was not found.");
            }
            if ("SERVED".equals(status) && order.getReservationId() != null) {
                orders.updateStatus(connection, orderId, "SERVED", null);
                billing.attachServedOrders(connection, order.getReservationId());
            } else {
                orders.updateStatus(connection, orderId, status, order.getInvoiceItemId());
            }
            return null;
        });
        Session.dataChanged();
    }

    private StayBooking activeStay(java.sql.Connection connection, long lodgeId) throws SQLException {
        for (StayBooking reservation : new StayBookingDAO().search(connection, null, null, null, null, "ARRIVED")) {
            if (reservation.getLodgeId() == lodgeId) {
                return reservation;
            }
        }
        throw new AppException("Lodge service can be charged only to a room with a checked-in guest.");
    }
}
