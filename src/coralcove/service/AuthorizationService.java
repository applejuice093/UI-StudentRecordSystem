package coralcove.service;

import coralcove.model.AppModule;
import coralcove.model.Role;
import coralcove.model.User;
import coralcove.util.AppException;

public final class AuthorizationService {

    private AuthorizationService() {
    }

    public static boolean allows(Role role, AppModule module) {
        if (role == null) {
            return false;
        }
        return switch (role) {
            case ADMIN -> true;
            case MANAGER -> module != AppModule.SETTINGS;
            case FRONT_DESK -> switch (module) {
                case DASHBOARD, STAYS, LODGES, GUESTS, ARRIVALS, BILLING, LODGE_CARE, EXPERIENCES -> true;
                default -> false;
            };
            case ATTENDANT -> module == AppModule.DASHBOARD || module == AppModule.LODGE_CARE;
        };
    }

    public static void require(AppModule module) {
        User user = Session.get();
        if (user == null || !user.isActive() || !allows(user.getRole(), module)) {
            throw new AppException("You do not have access to " + module.label() + ".");
        }
    }

    public static void requireRoomWrite() {
        require(AppModule.LODGES);
        if (Session.get().getRole() == Role.FRONT_DESK) {
            throw new AppException("Front desk can view lodges but cannot change lodge records.");
        }
    }

    public static void requireHousekeepingWrite() {
        User user = Session.get();
        if (user == null || (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER
                && user.getRole() != Role.ATTENDANT)) {
            throw new AppException("You cannot update lodge-care tasks.");
        }
    }
}
