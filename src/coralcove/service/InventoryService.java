package coralcove.service;

import coralcove.dao.InventoryDAO;
import coralcove.database.DatabaseManager;
import coralcove.database.Transactions;
import coralcove.model.AppModule;
import coralcove.model.InventoryItem;
import coralcove.util.AppException;
import coralcove.util.DateUtils;
import coralcove.util.ValidationUtils;
import java.sql.SQLException;
import java.util.List;

public final class InventoryService {

    public static final List<String> CATEGORIES = List.of(
            "Housekeeping supplies", "Food and beverages", "Maintenance supplies", "Guest amenities", "Office supplies");
    private final InventoryDAO items = new InventoryDAO();

    public List<InventoryItem> search(String text, String category) {
        AuthorizationService.require(AppModule.INVENTORY);
        try (var connection = DatabaseManager.getConnection()) {
            return items.search(connection, text, category);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public List<String[]> suppliers() {
        AuthorizationService.require(AppModule.INVENTORY);
        try (var connection = DatabaseManager.getConnection()) {
            return items.suppliers(connection);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public long addSupplier(String name, String phone, String email) {
        AuthorizationService.require(AppModule.INVENTORY);
        ValidationUtils.required(name, "Supplier name");
        return Transactions.in(connection -> items.addSupplier(connection, name.trim(), phone, email, false));
    }

    public InventoryItem save(InventoryItem item, boolean create) {
        AuthorizationService.require(AppModule.INVENTORY);
        item.setSku(ValidationUtils.required(item.getSku(), "SKU"));
        item.setName(ValidationUtils.required(item.getName(), "Item name"));
        if (!CATEGORIES.contains(item.getCategory())) {
            throw new AppException("Choose an inventory category.");
        }
        if (item.getQuantity() < 0 || item.getMinQuantity() < 0) {
            throw new AppException("Quantities cannot be negative.");
        }
        InventoryItem saved = Transactions.in(connection -> {
            if (!create) {
                InventoryItem current = items.find(connection, item.getId());
                if (current == null) {
                    throw new AppException("Stock item was not found.");
                }
                ResortScope.assertOwns(current.getResortId());
            }
            item.setResortId(ResortScope.resolve(connection, item.getResortId()));
            if (create) {
                item.setId(items.insert(connection, item));
            } else {
                items.update(connection, item);
            }
            return items.find(connection, item.getId());
        });
        Session.dataChanged();
        return saved;
    }

    public void adjust(long itemId, int delta, String reason, String note) {
        AuthorizationService.require(AppModule.INVENTORY);
        if (delta == 0) {
            throw new AppException("Enter a quantity change other than zero.");
        }
        Transactions.in(connection -> {
            InventoryItem item = items.find(connection, itemId);
            if (item.getQuantity() + delta < 0) {
                throw new AppException("Stock cannot fall below zero.");
            }
            items.adjust(connection, itemId, delta, reason, note, DateUtils.nowStamp());
            return null;
        });
        Session.dataChanged();
    }

    public List<String[]> history(long itemId) {
        AuthorizationService.require(AppModule.INVENTORY);
        try (var connection = DatabaseManager.getConnection()) {
            return items.history(connection, itemId);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }
}
