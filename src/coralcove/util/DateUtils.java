package coralcove.util;

import coralcove.service.SettingsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public final class DateUtils {

    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter ISO_STAMP = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final ZoneId FALLBACK = ZoneId.of("Asia/Kolkata");

    private DateUtils() {
    }

    public static ZoneId zone() {
        try {
            String id = SettingsService.timezoneOrNull();
            if (id == null || id.isBlank()) {
                return FALLBACK;
            }
            return ZoneId.of(id);
        } catch (RuntimeException ex) {
            return FALLBACK;
        }
    }

    public static LocalDate today() {
        return LocalDate.now(zone());
    }

    public static String iso(LocalDate date) {
        return date == null ? null : date.format(ISO_DATE);
    }

    public static LocalDate parse(String iso) {
        if (iso == null || iso.isBlank()) {
            throw new AppException("Enter a date as dd-MM-yyyy.");
        }
        String value = iso.trim();
        try {
            if (value.length() >= 10 && value.charAt(4) == '-') {
                return LocalDate.parse(value.substring(0, 10), ISO_DATE);
            }
            return LocalDate.parse(value, DISPLAY);
        } catch (DateTimeParseException ex) {
            throw new AppException("Enter a date as dd-MM-yyyy.");
        }
    }

    public static String display(String isoOrDisplay) {
        if (isoOrDisplay == null || isoOrDisplay.isBlank()) {
            return "";
        }
        try {
            return parse(isoOrDisplay).format(DISPLAY);
        } catch (AppException ex) {
            return isoOrDisplay;
        }
    }

    public static String display(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY);
    }

    public static String nowStamp() {
        return LocalDateTime.now(zone()).withNano(0).format(ISO_STAMP);
    }

    public static int nights(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new AppException("Check-out must be after check-in.");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > Integer.MAX_VALUE) {
            throw new AppException("Stay is too long.");
        }
        return (int) nights;
    }

    public static boolean overlaps(LocalDate aIn, LocalDate aOut, LocalDate bIn, LocalDate bOut) {
        return aIn.isBefore(bOut) && aOut.isAfter(bIn);
    }
}
