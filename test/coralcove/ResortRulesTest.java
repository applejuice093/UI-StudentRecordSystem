package coralcove;

import coralcove.model.BookingSource;
import coralcove.model.Guest;
import coralcove.model.PaymentMethod;
import coralcove.model.StayBooking;
import coralcove.model.StayStatus;
import coralcove.model.Role;
import coralcove.model.Lodge;
import coralcove.model.LodgeStatus;
import coralcove.model.LodgeCategory;
import coralcove.model.Resort;
import coralcove.model.User;
import coralcove.service.BillingService;
import coralcove.service.ResortService;
import coralcove.service.ArrivalService;
import coralcove.service.DashboardService;
import coralcove.service.GuestService;
import coralcove.service.LodgeCareService;
import coralcove.service.StayService;
import coralcove.service.LodgeService;
import coralcove.service.Session;
import coralcove.util.AppException;
import coralcove.util.DateUtils;
import coralcove.util.Money;
import java.math.BigDecimal;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ResortRulesTest extends DbFixture {
    private final LodgeService lodges = new LodgeService();
    private final GuestService guests = new GuestService();
    private final StayService stay_bookings = new StayService();
    private final ArrivalService stays = new ArrivalService();
    private final BillingService billing = new BillingService();
    private final LodgeCareService housekeeping = new LodgeCareService();

    @Test
    public void rejectsDuplicateRoomNumberAndUnsafeDelete() {
        Lodge room = saveRoom("101", money("4000"));
        try {
            saveRoom("101", money("4500"));
            Assert.fail("duplicate room");
        } catch (AppException ex) {
            Assert.assertTrue(ex.getMessage().contains("already exists"));
        }
        Guest guest = saveGuest("Asha");
        StayBooking stay = reservation(guest, room, 0, 2, StayStatus.CONFIRMED);
        stay_bookings.create(stay);
        try {
            lodges.delete(room.getId());
            Assert.fail("deleted room with history");
        } catch (AppException ex) {
            Assert.assertTrue(ex.getMessage().contains("history"));
        }
    }

    @Test
    public void guestSearchFindsCreatedGuest() {
        saveGuest("Priya Nair");
        List<Guest> found = guests.search("Priya");
        Assert.assertEquals(1, found.size());
    }

    @Test
    public void overlapRejectsAndCancelledDoesNotBlock() {
        Lodge room = saveRoom("202", money("5000"));
        Guest guest = saveGuest("Rahul");
        stay_bookings.create(reservation(guest, room, 1, 4, StayStatus.CONFIRMED));
        try {
            stay_bookings.create(reservation(guest, room, 3, 6, StayStatus.CONFIRMED));
            Assert.fail("overlap accepted");
        } catch (AppException ex) {
            Assert.assertTrue(ex.getMessage().contains("overlapping"));
        }
        StayBooking cancelled = stay_bookings.create(reservation(guest, room, 10, 12, StayStatus.CONFIRMED));
        stay_bookings.cancel(cancelled.getId());
        StayBooking again = stay_bookings.create(reservation(guest, room, 10, 12, StayStatus.PENDING));
        Assert.assertEquals(StayStatus.PENDING, again.getStatus());
    }

    @Test
    public void checkoutBeforeCheckInIsRejected() {
        StayBooking draft = reservation(saveGuest("Neha"), saveRoom("303", money("8000")), 0, 2, StayStatus.CONFIRMED);
        draft.setCheckOut(draft.getCheckIn());
        try {
            stay_bookings.create(draft);
            Assert.fail("zero-night stay");
        } catch (AppException ex) {
            Assert.assertTrue(ex.getMessage().toLowerCase().contains("check-out"));
        }
    }

    @Test
    public void checkInAndCheckOutFollowStayRules() {
        Lodge room = saveRoom("404", money("6000"));
        Guest guest = saveGuest("Vikram");
        StayBooking booked = stay_bookings.create(reservation(guest, room, 0, 2, StayStatus.CONFIRMED));
        stay_bookings.cancel(booked.getId());
        try {
            stays.checkIn(booked.getId());
            Assert.fail("checked in cancelled");
        } catch (AppException ex) {
            Assert.assertTrue(ex.getMessage().contains("pending or confirmed"));
        }
        StayBooking live = stay_bookings.create(reservation(guest, room, 0, 2, StayStatus.CONFIRMED));
        try {
            stays.checkOut(live.getId());
            Assert.fail("checked out early");
        } catch (AppException ex) {
            Assert.assertTrue(ex.getMessage().contains("checked in"));
        }
        stays.checkIn(live.getId());
        var invoice = billing.findForReservation(live.getId());
        Assert.assertTrue(invoice.getTotal().signum() > 0);
        billing.takePayment(invoice.getId(), invoice.getTotal().divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP),
                PaymentMethod.CASH, "PAY-1", false);
        try {
            billing.takePayment(invoice.getId(), invoice.getTotal(), PaymentMethod.CARD, "PAY-2", false);
            Assert.fail("overpayment");
        } catch (AppException ex) {
            Assert.assertTrue(ex.getMessage().contains("exceeds"));
        }
        invoice = billing.findForReservation(live.getId());
        billing.takePayment(invoice.getId(), invoice.getBalance(), PaymentMethod.UPI, "PAY-3", false);
        invoice = billing.findForReservation(live.getId());
        Assert.assertEquals(coralcove.model.InvoiceStatus.PAID, invoice.getStatus());
        stays.checkOut(live.getId());
        Lodge after = lodges.search("404", null, null, null).get(0);
        Assert.assertEquals(LodgeStatus.TURNDOWN, after.getStatus());
    }

    @Test
    public void housekeepingReleasesRoomUnlessTodayIsReserved() {
        Lodge clean = saveRoom("501", money("3500"));
        housekeeping.create(clean.getId(), null, "NORMAL", "checkout");
        HousekeepingTaskId id = openTask(clean.getUnitCode());
        housekeeping.update(id.id, null, "NORMAL", "COMPLETED", "PASSED", "done");
        Assert.assertEquals(LodgeStatus.READY, lodges.search("501", null, null, null).get(0).getStatus());

        Lodge held = saveRoom("502", money("3500"));
        Guest guest = saveGuest("Meera");
        stay_bookings.create(reservation(guest, held, 0, 3, StayStatus.CONFIRMED));
        housekeeping.create(held.getId(), null, "HIGH", "still booked");
        HousekeepingTaskId heldTask = openTask(held.getUnitCode());
        housekeeping.update(heldTask.id, null, "HIGH", "COMPLETED", "PASSED", "done");
        Assert.assertEquals(LodgeStatus.HELD, lodges.search("502", null, null, null).get(0).getStatus());
    }

    @Test
    public void dashboardLoadsWhenReservationsJoinRooms() {
        new DashboardService().load();
    }

    @Test
    public void beachDeskCannotBookCliffLodgeAndAdminSeesBoth() {
        User admin = Session.get();
        Lodge beach = saveRoom("B01", money("2000"));
        long beachId = beach.getResortId();
        Resort cliff = new Resort();
        cliff.setName("CoralCove Cliff");
        cliff.setLocation("Varkala");
        cliff.setCoastline("Malabar coast");
        cliff.setTimezone("Asia/Kolkata");
        cliff.setTaxPercent("12");
        cliff.setActive(true);
        cliff = new ResortService().save(cliff);
        Session.setResortFilter(cliff.getId());
        Lodge cliffLodge = saveRoom("C01", money("3000"));
        Guest guest = saveGuest("Ada");
        User desk = new User();
        desk.setId(admin.getId());
        desk.setRole(Role.FRONT_DESK);
        desk.setResortId(beachId);
        desk.setActive(true);
        Session.set(desk);
        Session.setResortFilter(beachId);
        try {
            stay_bookings.create(reservation(guest, cliffLodge, 1, 3, StayStatus.CONFIRMED));
            Assert.fail("cross resort booking");
        } catch (AppException ex) {
            Assert.assertTrue(ex.getMessage().contains("another resort"));
        }
        Session.set(admin);
        Session.setResortFilter(null);
        Assert.assertEquals(2, lodges.search(null, null, null, null).size());
    }

    @Test
    public void housekeepingRoleCannotCreateReservation() {
        Guest guest = saveGuest("X");
        Lodge room = saveRoom("601", money("1000"));
        User user = new User();
        user.setId(Session.get().getId());
        user.setRole(Role.ATTENDANT);
        user.setActive(true);
        Session.set(user);
        try {
            stay_bookings.create(reservation(guest, room, 0, 1, StayStatus.CONFIRMED));
            Assert.fail("housekeeping booked");
        } catch (AppException ex) {
            Assert.assertTrue(ex.getMessage().contains("Stays"));
        }
    }

    private Lodge saveRoom(String number, BigDecimal price) {
        LodgeCategory type = lodges.types().get(0);
        Lodge room = new Lodge();
        room.setUnitCode(number);
        room.setZone("BEACH");
        room.setViewAspect("SEA");
        room.setLodgeCategoryId(type.getId());
        room.setBedConfig("1 King");
        room.setMaxGuests(2);
        room.setPricePerNight(price);
        room.setStatus(LodgeStatus.READY);
        return lodges.save(room, true);
    }

    private Guest saveGuest(String name) {
        Guest guest = new Guest();
        guest.setFullName(name);
        guest.setPhone("900000" + name.length() + name.charAt(0));
        guest.setNationality("India");
        return guests.save(guest, true);
    }

    private StayBooking reservation(Guest guest, Lodge room, int startOffset, int endOffset, StayStatus status) {
        StayBooking reservation = new StayBooking();
        reservation.setGuestId(guest.getId());
        reservation.setLodgeId(room.getId());
        reservation.setCheckIn(DateUtils.today().plusDays(startOffset));
        reservation.setCheckOut(DateUtils.today().plusDays(endOffset));
        reservation.setAdults(1);
        reservation.setChildren(0);
        reservation.setDiscount(Money.zero());
        reservation.setSource(BookingSource.WALK_IN);
        reservation.setStatus(status);
        return reservation;
    }

    private HousekeepingTaskId openTask(String unitCode) {
        var tasks = housekeeping.search(null, null);
        for (var task : tasks) {
            if (unitCode.equals(task.getUnitCode()) && !"COMPLETED".equals(task.getStatus())) {
                return new HousekeepingTaskId(task.getId());
            }
        }
        throw new AssertionError("missing task");
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private record HousekeepingTaskId(long id) {
    }
}
