package coralcove.database;

import coralcove.config.AppConfig;
import coralcove.util.PasswordHasher;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void createSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            for (String sql : DDL) {
                statement.execute(sql);
            }
        }
    }

    public static void seed(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT OR IGNORE INTO group_settings(key, value) VALUES
                    ('demo_loaded', '0')
                    """);
            statement.executeUpdate("""
                    INSERT OR IGNORE INTO lodge_categories(name, description) VALUES
                    ('Pool Villa', 'Private pool and an outdoor shower'),
                    ('Beach Cottage', 'Steps from the sand, one bedroom'),
                    ('Garden Suite', 'Courtyard suite under the palms'),
                    ('Cliff Cabin', 'Elevated cabin with a sea wind'),
                    ('Family Lodge', 'Two bedrooms for a longer holiday'),
                    ('Overwater Suite', 'Deck over the lagoon'),
                    ('Wellness Villa', 'Treatment room attached to the lodge')
                    """);
            statement.executeUpdate("""
                    INSERT INTO resorts(name, location, coastline, timezone, tax_percent, active, is_sample)
                    SELECT 'CoralCove Beach', 'Kochi', 'Malabar coast', 'Asia/Kolkata', '12', 1, 0
                    WHERE NOT EXISTS (SELECT 1 FROM resorts)
                    """);
        }
        try (var count = connection.prepareStatement("SELECT COUNT(*) FROM users");
                var rs = count.executeQuery()) {
            rs.next();
            if (rs.getInt(1) == 0) {
                try (var insert = connection.prepareStatement("""
                        INSERT INTO users(username, password_hash, role, must_change_password, active, created_at)
                        VALUES (?, ?, 'ADMIN', 1, 1, datetime('now'))
                        """)) {
                    insert.setString(1, AppConfig.DEFAULT_ADMIN);
                    insert.setString(2, PasswordHasher.hash(AppConfig.DEFAULT_PASSWORD));
                    insert.executeUpdate();
                }
            }
        }
        try (var count = connection.prepareStatement("SELECT COUNT(*) FROM experiences");
                var rs = count.executeQuery()) {
            rs.next();
            if (rs.getInt(1) == 0) {
                long resortId;
                try (var resort = connection.prepareStatement("SELECT id FROM resorts ORDER BY id LIMIT 1");
                        var found = resort.executeQuery()) {
                    found.next();
                    resortId = found.getLong(1);
                }
                try (var insert = connection.prepareStatement(
                        "INSERT INTO experiences(resort_id, name, category, price, active) VALUES (?,?,?,?,1)")) {
                    Object[][] menu = {
                            {"Ayurveda hour", "Spa", "2800.00"},
                            {"Kayak session", "Water", "1500.00"},
                            {"Sunset cruise", "Excursion", "4500.00"},
                            {"Beach barbecue", "Dining", "1800.00"},
                            {"Breakfast platter", "Dining", "450.00"},
                            {"Dinner thali", "Dining", "650.00"},
                            {"Laundry press", "Laundry", "150.00"},
                            {"Still water", "Minibar", "60.00"}
                    };
                    for (Object[] row : menu) {
                        insert.setLong(1, resortId);
                        insert.setString(2, (String) row[0]);
                        insert.setString(3, (String) row[1]);
                        insert.setString(4, (String) row[2]);
                        insert.executeUpdate();
                    }
                }
            }
        }
    }

    private static final String[] DDL = {
            """
            CREATE TABLE IF NOT EXISTS group_settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS suppliers (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                phone TEXT,
                email TEXT,
                notes TEXT,
                is_sample INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS resorts (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL UNIQUE,
                location TEXT NOT NULL,
                coastline TEXT,
                timezone TEXT NOT NULL,
                tax_percent TEXT NOT NULL,
                active INTEGER NOT NULL DEFAULT 1,
                is_sample INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS employees (
                id INTEGER PRIMARY KEY,
                resort_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                phone TEXT,
                email TEXT,
                designation TEXT,
                department TEXT NOT NULL,
                salary TEXT NOT NULL DEFAULT '0.00',
                joining_date TEXT,
                status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
                is_sample INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (resort_id) REFERENCES resorts(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY,
                username TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL CHECK (role IN ('ADMIN','MANAGER','FRONT_DESK','ATTENDANT')),
                employee_id INTEGER,
                resort_id INTEGER,
                must_change_password INTEGER NOT NULL DEFAULT 0,
                active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT NOT NULL,
                FOREIGN KEY (employee_id) REFERENCES employees(id),
                FOREIGN KEY (resort_id) REFERENCES resorts(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS guests (
                id INTEGER PRIMARY KEY,
                full_name TEXT NOT NULL,
                phone TEXT,
                email TEXT,
                gender TEXT,
                nationality TEXT,
                address TEXT,
                id_doc_type TEXT,
                id_doc_number TEXT,
                preferences TEXT,
                vip TEXT NOT NULL DEFAULT 'STANDARD' CHECK (vip IN ('STANDARD','VIP')),
                registered_on TEXT NOT NULL,
                is_sample INTEGER NOT NULL DEFAULT 0
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_guests_phone ON guests(phone)",
            """
            CREATE TABLE IF NOT EXISTS lodge_categories (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL UNIQUE,
                description TEXT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS lodges (
                id INTEGER PRIMARY KEY,
                resort_id INTEGER NOT NULL,
                unit_code TEXT NOT NULL,
                zone TEXT NOT NULL CHECK (zone IN ('BEACH','LAGOON','GARDEN','CLIFF')),
                view_aspect TEXT NOT NULL CHECK (view_aspect IN ('SEA','GARDEN','POOL')),
                lodge_category_id INTEGER NOT NULL,
                bed_config TEXT,
                max_guests INTEGER NOT NULL,
                price_per_night TEXT NOT NULL,
                description TEXT,
                amenities TEXT,
                status TEXT NOT NULL CHECK (status IN ('READY','IN_STAY','HELD','TURNDOWN','REPAIR','CLOSED')),
                is_sample INTEGER NOT NULL DEFAULT 0,
                UNIQUE (resort_id, unit_code),
                FOREIGN KEY (resort_id) REFERENCES resorts(id),
                FOREIGN KEY (lodge_category_id) REFERENCES lodge_categories(id)
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_rooms_number ON lodges(unit_code)",
            "CREATE INDEX IF NOT EXISTS idx_rooms_status ON lodges(status)",
            """
            CREATE TABLE IF NOT EXISTS stay_bookings (
                id INTEGER PRIMARY KEY,
                booking_reference TEXT NOT NULL UNIQUE,
                resort_id INTEGER NOT NULL,
                guest_id INTEGER NOT NULL,
                lodge_id INTEGER NOT NULL,
                check_in TEXT NOT NULL,
                check_out TEXT NOT NULL,
                adults INTEGER NOT NULL,
                children INTEGER NOT NULL DEFAULT 0,
                nights INTEGER NOT NULL,
                room_price TEXT NOT NULL,
                discount TEXT NOT NULL,
                tax TEXT NOT NULL,
                total TEXT NOT NULL,
                source TEXT NOT NULL CHECK (source IN ('WALK_IN','WEBSITE','PHONE','TRAVEL_AGENT')),
                meal_plan TEXT NOT NULL CHECK (meal_plan IN ('EP','CP','MAP','AP')),
                transfer TEXT NOT NULL CHECK (transfer IN ('NONE','AIRPORT','JETTY')),
                status TEXT NOT NULL CHECK (status IN ('PENDING','CONFIRMED','ARRIVED','DEPARTED','CANCELLED','NO_SHOW')),
                special_requests TEXT,
                created_at TEXT NOT NULL,
                is_sample INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (resort_id) REFERENCES resorts(id),
                FOREIGN KEY (guest_id) REFERENCES guests(id),
                FOREIGN KEY (lodge_id) REFERENCES lodges(id)
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_res_ref ON stay_bookings(booking_reference)",
            "CREATE INDEX IF NOT EXISTS idx_res_guest ON stay_bookings(guest_id)",
            "CREATE INDEX IF NOT EXISTS idx_res_room ON stay_bookings(lodge_id)",
            "CREATE INDEX IF NOT EXISTS idx_res_in ON stay_bookings(check_in)",
            "CREATE INDEX IF NOT EXISTS idx_res_out ON stay_bookings(check_out)",
            "CREATE INDEX IF NOT EXISTS idx_res_status ON stay_bookings(status)",
            """
            CREATE TABLE IF NOT EXISTS stay_party (
                reservation_id INTEGER NOT NULL,
                guest_id INTEGER NOT NULL,
                is_primary INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (reservation_id, guest_id),
                FOREIGN KEY (reservation_id) REFERENCES stay_bookings(id),
                FOREIGN KEY (guest_id) REFERENCES guests(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS arrivals (
                id INTEGER PRIMARY KEY,
                reservation_id INTEGER NOT NULL UNIQUE,
                actual_arrival TEXT NOT NULL,
                actual_departure TEXT,
                performed_by INTEGER,
                FOREIGN KEY (reservation_id) REFERENCES stay_bookings(id),
                FOREIGN KEY (performed_by) REFERENCES users(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS invoices (
                id INTEGER PRIMARY KEY,
                invoice_number TEXT NOT NULL UNIQUE,
                reservation_id INTEGER NOT NULL,
                guest_id INTEGER NOT NULL,
                subtotal TEXT NOT NULL,
                discount TEXT NOT NULL,
                tax TEXT NOT NULL,
                total TEXT NOT NULL,
                amount_paid TEXT NOT NULL,
                balance TEXT NOT NULL,
                status TEXT NOT NULL CHECK (status IN ('UNPAID','PARTIALLY_PAID','PAID','REFUNDED')),
                issued_at TEXT NOT NULL,
                is_sample INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (reservation_id) REFERENCES stay_bookings(id),
                FOREIGN KEY (guest_id) REFERENCES guests(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS invoice_items (
                id INTEGER PRIMARY KEY,
                invoice_id INTEGER NOT NULL,
                kind TEXT NOT NULL CHECK (kind IN ('ROOM','RESTAURANT','LAUNDRY','MINIBAR','MISC')),
                description TEXT NOT NULL,
                qty INTEGER NOT NULL,
                unit_price TEXT NOT NULL,
                line_total TEXT NOT NULL,
                FOREIGN KEY (invoice_id) REFERENCES invoices(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS payments (
                id INTEGER PRIMARY KEY,
                invoice_id INTEGER NOT NULL,
                amount TEXT NOT NULL,
                method TEXT NOT NULL CHECK (method IN ('CASH','CARD','UPI','BANK_TRANSFER')),
                reference TEXT NOT NULL UNIQUE,
                paid_at TEXT NOT NULL,
                received_by INTEGER,
                note TEXT,
                FOREIGN KEY (invoice_id) REFERENCES invoices(id),
                FOREIGN KEY (received_by) REFERENCES users(id)
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_pay_ref ON payments(reference)",
            """
            CREATE TABLE IF NOT EXISTS experiences (
                id INTEGER PRIMARY KEY,
                resort_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                price TEXT NOT NULL,
                active INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY (resort_id) REFERENCES resorts(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS experience_bookings (
                id INTEGER PRIMARY KEY,
                experience_id INTEGER,
                reservation_id INTEGER,
                guest_id INTEGER,
                lodge_id INTEGER,
                order_type TEXT NOT NULL CHECK (order_type IN ('RESTAURANT','ROOM_SERVICE')),
                description TEXT NOT NULL,
                qty INTEGER NOT NULL,
                unit_price TEXT NOT NULL,
                line_total TEXT NOT NULL,
                status TEXT NOT NULL CHECK (status IN ('NEW','PREPARING','READY','SERVED','CANCELLED')),
                slot_at TEXT,
                invoice_item_id INTEGER,
                created_at TEXT NOT NULL,
                is_sample INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (experience_id) REFERENCES experiences(id),
                FOREIGN KEY (reservation_id) REFERENCES stay_bookings(id),
                FOREIGN KEY (guest_id) REFERENCES guests(id),
                FOREIGN KEY (lodge_id) REFERENCES lodges(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS lodge_care_tasks (
                id INTEGER PRIMARY KEY,
                lodge_id INTEGER NOT NULL,
                assignee_employee_id INTEGER,
                priority TEXT NOT NULL CHECK (priority IN ('LOW','NORMAL','HIGH')),
                status TEXT NOT NULL CHECK (status IN ('PENDING','ASSIGNED','IN_PROGRESS','COMPLETED','INSPECTION_REQUIRED')),
                notes TEXT,
                inspection_status TEXT NOT NULL DEFAULT 'NONE' CHECK (inspection_status IN ('NONE','PASSED','FAILED')),
                created_at TEXT NOT NULL,
                completed_at TEXT,
                is_sample INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (lodge_id) REFERENCES lodges(id),
                FOREIGN KEY (assignee_employee_id) REFERENCES employees(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS estate_repairs (
                id INTEGER PRIMARY KEY,
                lodge_id INTEGER NOT NULL,
                reported_by TEXT,
                description TEXT NOT NULL,
                status TEXT NOT NULL CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED')),
                created_at TEXT NOT NULL,
                is_sample INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (lodge_id) REFERENCES lodges(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS inventory_items (
                id INTEGER PRIMARY KEY,
                resort_id INTEGER NOT NULL,
                sku TEXT NOT NULL,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                supplier_id INTEGER,
                quantity INTEGER NOT NULL,
                min_quantity INTEGER NOT NULL,
                unit TEXT,
                is_sample INTEGER NOT NULL DEFAULT 0,
                UNIQUE (resort_id, sku),
                FOREIGN KEY (resort_id) REFERENCES resorts(id),
                FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS stock_transactions (
                id INTEGER PRIMARY KEY,
                item_id INTEGER NOT NULL,
                qty_delta INTEGER NOT NULL,
                reason TEXT NOT NULL CHECK (reason IN ('PURCHASE','ADJUSTMENT','CONSUMPTION')),
                note TEXT,
                created_at TEXT NOT NULL,
                FOREIGN KEY (item_id) REFERENCES inventory_items(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS employee_attendance (
                id INTEGER PRIMARY KEY,
                employee_id INTEGER NOT NULL,
                work_date TEXT NOT NULL,
                status TEXT NOT NULL CHECK (status IN ('PRESENT','ABSENT','LEAVE')),
                note TEXT,
                UNIQUE (employee_id, work_date),
                FOREIGN KEY (employee_id) REFERENCES employees(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS audit_logs (
                id INTEGER PRIMARY KEY,
                user_id INTEGER,
                action TEXT NOT NULL,
                entity TEXT NOT NULL,
                entity_id TEXT,
                detail TEXT,
                created_at TEXT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """
    };
}
