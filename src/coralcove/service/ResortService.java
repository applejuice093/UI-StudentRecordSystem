package coralcove.service;

import coralcove.dao.AuditDAO;
import coralcove.dao.ResortDAO;
import coralcove.database.DatabaseManager;
import coralcove.database.Transactions;
import coralcove.model.AppModule;
import coralcove.model.Resort;
import coralcove.util.AppException;
import coralcove.util.ValidationUtils;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.List;

public final class ResortService {

    private final ResortDAO resorts = new ResortDAO();

    public List<Resort> list(boolean activeOnly) {
        try (var connection = DatabaseManager.getConnection()) {
            return resorts.list(connection, activeOnly);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public Resort save(Resort resort) {
        AuthorizationService.require(AppModule.SETTINGS);
        resort.setName(ValidationUtils.required(resort.getName(), "Resort name"));
        resort.setLocation(ValidationUtils.required(resort.getLocation(), "Location"));
        resort.setCoastline(ValidationUtils.optional(resort.getCoastline()));
        try {
            ZoneId.of(resort.getTimezone());
        } catch (RuntimeException ex) {
            throw new AppException("Timezone must be a valid zone id, such as Asia/Kolkata.");
        }
        BigDecimal tax = ValidationUtils.money(resort.getTaxPercent(), "Tax percent", true);
        if (tax.compareTo(new BigDecimal("100")) > 0) {
            throw new AppException("Tax percent cannot exceed 100.");
        }
        resort.setTaxPercent(tax.toPlainString());
        Resort saved = Transactions.in(connection -> {
            if (resort.getId() <= 0) {
                resort.setId(resorts.insert(connection, resort));
            } else {
                resorts.update(connection, resort);
            }
            new AuditDAO().log(connection, Session.get().getId(), "UPDATE", "resorts",
                    String.valueOf(resort.getId()), resort.getName());
            SettingsService.clearCache();
            return resorts.find(connection, resort.getId());
        });
        Session.dataChanged();
        return saved;
    }
}
