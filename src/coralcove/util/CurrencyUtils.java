package coralcove.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Indian rupee display. PDF text uses "Rs." because the built-in PDF font is Helvetica. */
public final class CurrencyUtils {

    private static final DecimalFormat FORMAT;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        symbols.setGroupingSeparator(',');
        FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    private CurrencyUtils() {
    }

    public static String format(BigDecimal amount) {
        synchronized (FORMAT) {
            return "\u20B9" + FORMAT.format(Money.parse(Money.store(amount)));
        }
    }

    public static String formatPlain(BigDecimal amount) {
        synchronized (FORMAT) {
            return "Rs. " + FORMAT.format(Money.parse(Money.store(amount)));
        }
    }
}
