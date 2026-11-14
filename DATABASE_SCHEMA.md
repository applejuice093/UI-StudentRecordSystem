# CoralCove database

SQLite file: `data/coralcove.db`. Override with `CORALCOVE_DB`.

`PRAGMA foreign_keys=ON` is set on every connection. Money columns are TEXT with two decimal places. Dates are ISO-8601. Schema version lives in `schema_version` and starts at 1. Later changes should add columns. Do not delete the file to "migrate" it. The old hotel file, if it is still in `data/`, is not opened.

## Who owns a row

Group-wide, shared by every property:

| Table | Why it is shared |
| --- | --- |
| `group_settings` | `demo_loaded` only |
| `lodge_categories` | Pool Villa, Beach Cottage, Garden Suite, Cliff Cabin, Family Lodge, Overwater Suite, Wellness Villa |
| `guests` | One guest book |
| `suppliers` | Shared vendors |
| `users` | Administrators have a null `resort_id`. Other roles must have one. |
| `audit_logs` | Group history |
| `schema_version` | One version for the file |

Owned by one resort (`resort_id` is required):

| Table | Notes |
| --- | --- |
| `resorts` | Name is unique. `active=0` drops the property from the top bar and from All-resorts totals. History stays. |
| `lodges` | Unique `(resort_id, unit_code)`. Zone is BEACH, LAGOON, GARDEN, or CLIFF. View is SEA, GARDEN, or POOL. |
| `employees` | |
| `experiences` | Catalog. The first launch fills eight items on the first resort. They are not samples. |
| `inventory_items` | Unique `(resort_id, sku)` |
| `stay_bookings` | `resort_id` must match the lodge. Meal plan EP, CP, MAP, AP. Transfer NONE, AIRPORT, JETTY. |

Reached through a lodge or a stay, with no extra `resort_id`:

`stay_party`, `arrivals`, `invoices`, `invoice_items`, `payments`, `experience_bookings`, `lodge_care_tasks`, `estate_repairs`, `stock_transactions`, `employee_attendance`.

`experience_bookings.invoice_item_id` is stored and is not a declared foreign key. `experience_bookings.slot_at` is an optional ISO timestamp.

## Seed

On an empty file the app inserts:

- `group_settings.demo_loaded = 0`
- the seven lodge categories
- resort **CoralCove Beach**, Kochi, Malabar coast, `Asia/Kolkata`, tax `12`, active, not a sample
- user `admin`, role ADMIN, `must_change_password=1`, no resort
- eight experiences on Beach: Ayurveda hour, Kayak session, Sunset cruise, Beach barbecue, Breakfast platter, Dinner thali, Laundry press, Still water

## Lodge status

`LodgeStatusSync` skips a missing lodge and any lodge already `CLOSED` (that status sticks). Otherwise, in order:

1. An `ARRIVED` stay sets `IN_STAY`.
2. An open lodge-care task (not `COMPLETED`) sets `TURNDOWN`.
3. An open estate repair sets `REPAIR`.
4. A `PENDING` or `CONFIRMED` stay that covers today sets `HELD`.
5. Otherwise the lodge is `READY`.

Manual `IN_STAY` is rejected. Guests, lodges, and stays that already have history are not deleted.

## Stay overlap

A new stay conflicts when `newArrival < existingDeparture AND newDeparture > existingArrival`. `CANCELLED` is ignored. `NO_SHOW` still blocks the dates.

Arrival is allowed only when today is inside `[check_in, check_out)`, the stay is `PENDING` or `CONFIRMED`, and no other `ARRIVED` stay is on that lodge. Departure sets `DEPARTED`, attaches served experience lines, opens a lodge-care task, and the sync moves the lodge to `TURNDOWN`.

## Occupancy

- Numerator: lodge-nights of `CONFIRMED`, `ARRIVED`, and `DEPARTED` stays that overlap the inclusive range.
- Denominator: lodges whose status is not `CLOSED`, multiplied by the number of days. All resorts counts lodges on active resorts only.

The rate is not the number of lodges whose status is `IN_STAY` today.

## Passwords

`pbkdf2$iterations$salt$hash`, PBKDF2-HMAC-SHA256, 210000 iterations.

## Demonstration reset

Reset deletes `is_sample=1` rows, experiences that belong to a sample resort, the user `beach.desk`, and sample resorts. It does not delete CoralCove Beach.
