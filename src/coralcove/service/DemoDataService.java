package coralcove.service;

import coralcove.dao.AuditDAO;
import coralcove.dao.EmployeeDAO;
import coralcove.dao.GuestDAO;
import coralcove.dao.LodgeCareDAO;
import coralcove.dao.InventoryDAO;
import coralcove.dao.InvoiceDAO;
import coralcove.dao.StayBookingDAO;
import coralcove.dao.LodgeDAO;
import coralcove.dao.ExperienceDAO;
import coralcove.dao.ResortDAO;
import coralcove.dao.UserDAO;
import coralcove.database.DatabaseManager;
import coralcove.database.Transactions;
import coralcove.model.AppModule;
import coralcove.model.BookingSource;
import coralcove.model.Employee;
import coralcove.model.Guest;
import coralcove.model.LodgeCareTask;
import coralcove.model.InventoryItem;
import coralcove.model.Payment;
import coralcove.model.PaymentMethod;
import coralcove.model.StayBooking;
import coralcove.model.StayStatus;
import coralcove.model.Lodge;
import coralcove.model.LodgeStatus;
import coralcove.model.Resort;
import coralcove.model.Role;
import coralcove.model.ExperienceBooking;
import coralcove.model.User;
import coralcove.util.AppException;
import coralcove.util.PasswordHasher;
import coralcove.util.DateUtils;
import coralcove.util.Money;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Optional demonstration rows. Every inserted business row is marked is_sample=1.
 * Loading never runs automatically, and reset deletes only sample rows.
 */
public final class DemoDataService {

    public void load() {
        AuthorizationService.require(AppModule.SETTINGS);
        if (SettingsService.demoLoaded()) {
            throw new AppException("Demonstration data is already loaded. Reset it before loading again.");
        }
        Transactions.in(connection -> {
            LodgeDAO lodges = new LodgeDAO();
            GuestDAO guests = new GuestDAO();
            EmployeeDAO employees = new EmployeeDAO();
            StayBookingDAO stay_bookings = new StayBookingDAO();
            InvoiceDAO invoices = new InvoiceDAO();
            LodgeCareDAO housekeeping = new LodgeCareDAO();
            InventoryDAO inventory = new InventoryDAO();
            ExperienceDAO orders = new ExperienceDAO();
            BillingService billing = new BillingService();
            LodgeStatusSync sync = new LodgeStatusSync();
            ResortDAO resortDao = new ResortDAO();
            long beachId = resortDao.list(connection, false).get(0).getId();
            Resort cliff = new Resort();
            cliff.setName("CoralCove Cliff");
            cliff.setLocation("Varkala");
            cliff.setCoastline("Malabar coast");
            cliff.setTimezone("Asia/Kolkata");
            cliff.setTaxPercent("12");
            cliff.setActive(true);
            cliff.setSample(true);
            long cliffId = resortDao.insert(connection, cliff);
            for (var item : orders.menu(connection, false)) {
                if (item.getResortId() != beachId) {
                    continue;
                }
                item.setResortId(cliffId);
                orders.insertItem(connection, item);
            }
            var types = lodges.types(connection);
            String[] beds = {"1 King", "2 Twin", "1 Queen", "1 King + sofa"};
            BigDecimal[] prices = {
                    new BigDecimal("3500"), new BigDecimal("5500"), new BigDecimal("8000"),
                    new BigDecimal("14000"), new BigDecimal("45000"), new BigDecimal("28000"), new BigDecimal("6500")
            };
            String[] beachCodes = new String[16];
            String[] beachZones = new String[16];
            for (int i = 0; i < 10; i++) {
                beachCodes[i] = String.format("B%02d", i + 1);
                beachZones[i] = "BEACH";
            }
            for (int i = 0; i < 6; i++) {
                beachCodes[10 + i] = String.format("L%02d", i + 1);
                beachZones[10 + i] = "LAGOON";
            }
            String[] cliffCodes = new String[14];
            String[] cliffZones = new String[14];
            for (int i = 0; i < 8; i++) {
                cliffCodes[i] = String.format("G%02d", i + 1);
                cliffZones[i] = "GARDEN";
            }
            for (int i = 0; i < 6; i++) {
                cliffCodes[8 + i] = String.format("C%02d", i + 1);
                cliffZones[8 + i] = "CLIFF";
            }
            long[] roomIds = new long[30];
            long[] resortOfRoom = new long[30];
            for (int i = 0; i < 30; i++) {
                boolean onBeach = i < 16;
                Lodge room = new Lodge();
                room.setResortId(onBeach ? beachId : cliffId);
                room.setUnitCode(onBeach ? beachCodes[i] : cliffCodes[i - 16]);
                room.setZone(onBeach ? beachZones[i] : cliffZones[i - 16]);
                room.setViewAspect("LAGOON".equals(room.getZone()) ? "POOL" : "GARDEN".equals(room.getZone()) ? "GARDEN" : "SEA");
                room.setLodgeCategoryId(types.get(i % types.size()).getId());
                room.setBedConfig(beds[i % beds.length]);
                room.setMaxGuests(2 + (i % 3));
                room.setPricePerNight(prices[i % prices.length]);
                room.setDescription("Demonstration lodge");
                room.setAmenities("Wi-Fi, fan, kettle");
                room.setStatus(LodgeStatus.READY);
                room.setSample(true);
                roomIds[i] = lodges.insert(connection, room);
                resortOfRoom[i] = room.getResortId();
            }
            String[] names = {
                    "Asha Menon", "Rahul Iyer", "Priya Nair", "Vikram Shah", "Neha Kulkarni",
                    "Arjun Das", "Meera Joshi", "Kabir Khan", "Ananya Rao", "Sanjay Patel",
                    "Divya Singh", "Omar Farooq", "Lakshmi Bai", "Harsh Vardhan", "Tara D'Souza"
            };
            long[] guestIds = new long[names.length];
            for (int i = 0; i < names.length; i++) {
                Guest guest = new Guest();
                guest.setFullName(names[i]);
                guest.setPhone("98" + String.format("%08d", 10000000 + i * 17));
                guest.setEmail(names[i].toLowerCase().replace(" ", ".").replace("'", "") + "@example.com");
                guest.setNationality("India");
                guest.setAddress("Demonstration address, Kochi");
                guest.setIdDocType("Aadhaar");
                guest.setIdDocNumber("XXXX-XXXX-" + (1000 + i));
                guest.setVip(i % 5 == 0 ? "VIP" : "STANDARD");
                guest.setRegisteredOn(DateUtils.iso(DateUtils.today().minusDays(30 + i)));
                guest.setSample(true);
                guestIds[i] = guests.insert(connection, guest);
            }
            String[][] staff = {
                    {"Anita George", "Front office lead", "Reception"},
                    {"Ravi Kumar", "Receptionist", "Reception"},
                    {"Suma P", "Housekeeping supervisor", "Housekeeping"},
                    {"Jithin M", "Lodge attendant", "Housekeeping"},
                    {"Biju Thomas", "Technician", "Maintenance"},
                    {"Chef Leela", "Sous chef", "Restaurant"},
                    {"Naveen Roy", "Steward", "Restaurant"},
                    {"Imran Ali", "Security officer", "Security"},
                    {"Fathima Noor", "Duty manager", "Management"},
                    {"Paul Mathew", "Store keeper", "Management"}
            };
            long housekeeper = 0;
            for (int i = 0; i < staff.length; i++) {
                Employee employee = new Employee();
                employee.setName(staff[i][0]);
                employee.setDesignation(staff[i][1]);
                employee.setDepartment(staff[i][2]);
                employee.setPhone("97" + String.format("%08d", 20000000 + i));
                employee.setEmail(staff[i][0].toLowerCase().replace(" ", ".") + "@coralcove.example");
                employee.setSalary(new BigDecimal(25000 + i * 1500));
                employee.setJoiningDate(DateUtils.iso(DateUtils.today().minusMonths(8)));
                employee.setStatus("ACTIVE");
                employee.setSample(true);
                employee.setResortId(i < 5 ? beachId : cliffId);
                long id = employees.insert(connection, employee);
                if (i == 3) {
                    housekeeper = id;
                }
                employees.markAttendance(connection, id, DateUtils.iso(DateUtils.today()), i % 7 == 0 ? "LEAVE" : "PRESENT", "Sample");
            }
            LocalDate today = DateUtils.today();
            Object[][] stays = {
                    {0, 0, today.minusDays(10), today.minusDays(7), StayStatus.DEPARTED, BookingSource.WEBSITE},
                    {1, 1, today.minusDays(2), today.plusDays(2), StayStatus.ARRIVED, BookingSource.WALK_IN},
                    {2, 2, today, today.plusDays(3), StayStatus.CONFIRMED, BookingSource.PHONE},
                    {3, 3, today.plusDays(5), today.plusDays(8), StayStatus.CONFIRMED, BookingSource.TRAVEL_AGENT},
                    {4, 4, today.plusDays(1), today.plusDays(4), StayStatus.PENDING, BookingSource.WEBSITE},
                    {5, 5, today.minusDays(20), today.minusDays(18), StayStatus.CANCELLED, BookingSource.PHONE},
                    {6, 6, today.minusDays(1), today.plusDays(1), StayStatus.ARRIVED, BookingSource.WALK_IN},
                    {7, 7, today.plusDays(12), today.plusDays(15), StayStatus.CONFIRMED, BookingSource.WEBSITE},
                    {8, 8, today.minusDays(4), today.minusDays(1), StayStatus.NO_SHOW, BookingSource.PHONE},
                    {9, 9, today, today.plusDays(1), StayStatus.CONFIRMED, BookingSource.WALK_IN}
            };
            for (Object[] stay : stays) {
                int guestIndex = (int) stay[0];
                int roomIndex = (int) stay[1];
                StayBooking reservation = new StayBooking();
                reservation.setGuestId(guestIds[guestIndex]);
                reservation.setLodgeId(roomIds[roomIndex]);
                reservation.setCheckIn((LocalDate) stay[2]);
                reservation.setCheckOut((LocalDate) stay[3]);
                reservation.setAdults(2);
                reservation.setChildren(guestIndex % 2);
                reservation.setStatus((StayStatus) stay[4]);
                reservation.setSource((BookingSource) stay[5]);
                reservation.setSpecialRequests("Demonstration booking");
                reservation.setSample(true);
                reservation.setDiscount(guestIndex == 0 ? new BigDecimal("500.00") : Money.zero());
                Lodge room = lodges.findById(connection, reservation.getLodgeId());
                reservation.setResortId(room.getResortId());
                reservation.setRoomPrice(room.getPricePerNight());
                reservation.setBookingReference("CC-" + room.getUnitCode() + "-" + reservation.getCheckIn().toString().replace("-", ""));
                reservation.setCreatedAt(DateUtils.nowStamp());
                billing.priceStay(reservation);
                reservation.setId(stay_bookings.insert(connection, reservation));
                stay_bookings.linkGuest(connection, reservation.getId(), reservation.getGuestId(), true);
                if (reservation.getStatus() == StayStatus.ARRIVED
                        || reservation.getStatus() == StayStatus.DEPARTED) {
                    try (var ps = connection.prepareStatement(
                            "INSERT INTO arrivals(reservation_id, actual_arrival, actual_departure, performed_by) VALUES (?,?,?,?)")) {
                        ps.setLong(1, reservation.getId());
                        ps.setString(2, DateUtils.iso(reservation.getCheckIn()) + "T14:00:00");
                        if (reservation.getStatus() == StayStatus.DEPARTED) {
                            ps.setString(3, DateUtils.iso(reservation.getCheckOut()) + "T11:00:00");
                        } else {
                            ps.setString(3, null);
                        }
                        ps.setLong(4, Session.get().getId());
                        ps.executeUpdate();
                    }
                    var invoice = billing.ensureForReservation(connection, stay_bookings.findById(connection, reservation.getId()));
                    Payment payment = new Payment();
                    payment.setInvoiceId(invoice.getId());
                    payment.setAmount(reservation.getStatus() == StayStatus.ARRIVED
                            ? invoice.getTotal().divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP)
                            : invoice.getTotal());
                    payment.setMethod(PaymentMethod.UPI);
                    payment.setReference("DEMO-PAY-" + reservation.getBookingReference());
                    payment.setPaidAt(DateUtils.nowStamp());
                    payment.setReceivedBy(Session.get().getId());
                    payment.setNote("Demonstration payment");
                    invoices.addPayment(connection, payment);
                    billing.recalc(connection, invoice.getId());
                }
            }
            LodgeCareTask task = new LodgeCareTask();
            task.setLodgeId(roomIds[11]);
            task.setAssigneeId(housekeeper);
            task.setPriority("HIGH");
            task.setStatus("ASSIGNED");
            task.setNotes("Demonstration departure clean");
            task.setInspectionStatus("NONE");
            task.setCreatedAt(DateUtils.nowStamp());
            task.setSample(true);
            housekeeping.insert(connection, task);
            housekeeping.addMaintenance(connection, roomIds[12], "demo", "Demonstration dripping tap", "OPEN",
                    DateUtils.nowStamp(), true);
            long supplier = inventory.addSupplier(connection, "Harbour Supplies", "0484123456", "orders@harbour.example", true);
            String[][] stock = {
                    {"LIN-01", "Bath towel", "Housekeeping supplies", "40", "20"},
                    {"LIN-02", "Bed sheet", "Housekeeping supplies", "8", "15"},
                    {"FB-01", "Mineral water", "Food and beverages", "100", "24"},
                    {"FB-02", "Tea bags", "Food and beverages", "6", "20"},
                    {"MT-01", "LED bulb", "Maintenance supplies", "30", "10"},
                    {"AM-01", "Dental kit", "Guest amenities", "50", "20"},
                    {"OF-01", "A4 paper", "Office supplies", "4", "10"}
            };
            for (String[] row : stock) {
                InventoryItem item = new InventoryItem();
                item.setSku(row[0]);
                item.setName(row[1]);
                item.setCategory(row[2]);
                item.setSupplierId(supplier);
                item.setQuantity(Integer.parseInt(row[3]));
                item.setMinQuantity(Integer.parseInt(row[4]));
                item.setUnit("pcs");
                item.setSample(true);
                item.setResortId(beachId);
                long id = inventory.insert(connection, item);
                inventory.adjust(connection, id, 0, "PURCHASE", "Opening demonstration stock", DateUtils.nowStamp());
            }
            var menu = orders.menu(connection, true);
            ExperienceBooking order = new ExperienceBooking();
            order.setServiceId(menu.get(0).getId());
            order.setDescription(menu.get(0).getName());
            order.setQty(2);
            order.setUnitPrice(menu.get(0).getPrice());
            order.setLineTotal(Money.multiply(menu.get(0).getPrice(), 2));
            order.setOrderType("RESTAURANT");
            order.setStatus("PREPARING");
            order.setCreatedAt(DateUtils.nowStamp());
            order.setSample(true);
            orders.insertOrder(connection, order);
            StayBooking inHouse = stay_bookings.search(connection, null, null, null, null, "ARRIVED").get(0);
            ExperienceBooking roomService = new ExperienceBooking();
            roomService.setServiceId(menu.get(3).getId());
            roomService.setDescription(menu.get(3).getName());
            roomService.setQty(1);
            roomService.setUnitPrice(menu.get(3).getPrice());
            roomService.setLineTotal(menu.get(3).getPrice());
            roomService.setOrderType("ROOM_SERVICE");
            roomService.setStatus("SERVED");
            roomService.setLodgeId(inHouse.getLodgeId());
            roomService.setReservationId(inHouse.getId());
            roomService.setGuestId(inHouse.getGuestId());
            roomService.setCreatedAt(DateUtils.nowStamp());
            roomService.setSample(true);
            orders.insertOrder(connection, roomService);
            billing.attachServedOrders(connection, inHouse.getId());
            for (long lodgeId : roomIds) {
                sync.apply(connection, lodgeId);
            }
            User desk = new User();
            desk.setUsername("beach.desk");
            desk.setPasswordHash(PasswordHasher.hash("Desk@123"));
            desk.setRole(Role.FRONT_DESK);
            desk.setResortId(beachId);
            desk.setMustChangePassword(true);
            desk.setActive(true);
            desk.setCreatedAt(DateUtils.nowStamp());
            new UserDAO().insert(connection, desk);
            new AuditDAO().log(connection, Session.get().getId(), "DEMO_LOAD", "group_settings", "demo", "sample rows");
            return null;
        });
        SettingsService.setDemoLoaded(true);
        Session.dataChanged();
    }

    public void reset() {
        AuthorizationService.require(AppModule.SETTINGS);
        Transactions.in(connection -> {
            exec(connection, "DELETE FROM payments WHERE invoice_id IN (SELECT id FROM invoices WHERE is_sample=1)");
            exec(connection, "DELETE FROM invoice_items WHERE invoice_id IN (SELECT id FROM invoices WHERE is_sample=1)");
            exec(connection, "DELETE FROM experience_bookings WHERE is_sample=1");
            exec(connection, "DELETE FROM arrivals WHERE reservation_id IN (SELECT id FROM stay_bookings WHERE is_sample=1)");
            exec(connection, "DELETE FROM stay_party WHERE reservation_id IN (SELECT id FROM stay_bookings WHERE is_sample=1)");
            exec(connection, "DELETE FROM invoices WHERE is_sample=1");
            exec(connection, "DELETE FROM stay_bookings WHERE is_sample=1");
            exec(connection, "DELETE FROM lodge_care_tasks WHERE is_sample=1");
            exec(connection, "DELETE FROM estate_repairs WHERE is_sample=1");
            exec(connection, "DELETE FROM stock_transactions WHERE item_id IN (SELECT id FROM inventory_items WHERE is_sample=1)");
            exec(connection, "DELETE FROM inventory_items WHERE is_sample=1");
            exec(connection, "DELETE FROM suppliers WHERE is_sample=1");
            exec(connection, "DELETE FROM employee_attendance WHERE employee_id IN (SELECT id FROM employees WHERE is_sample=1)");
            exec(connection, "DELETE FROM lodges WHERE is_sample=1");
            exec(connection, "DELETE FROM guests WHERE is_sample=1");
            exec(connection, "DELETE FROM employees WHERE is_sample=1");
            exec(connection, "DELETE FROM experiences WHERE resort_id IN (SELECT id FROM resorts WHERE is_sample=1)");
            exec(connection, "DELETE FROM users WHERE username='beach.desk'");
            exec(connection, "DELETE FROM resorts WHERE is_sample=1");
            new AuditDAO().log(connection, Session.get().getId(), "DEMO_RESET", "group_settings", "demo", "sample rows removed");
            return null;
        });
        SettingsService.setDemoLoaded(false);
        Session.dataChanged();
    }

    private void exec(java.sql.Connection connection, String sql) throws java.sql.SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
