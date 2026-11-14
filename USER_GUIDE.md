# CoralCove user guide

CoralCove is the front-desk and back-office window for a group of resorts. The sidebar shows only the modules your role can open. The top bar shows the property you are working in.

## Start

1. Run the project from NetBeans, or run `dist/CoralCove.jar`.
2. Sign in. The first account is `admin` / `Admin@123`. The app asks for a new password before the desk opens.
3. The top bar opens on CoralCove Beach. An administrator can switch to another active resort, or to **All resorts**. Everyone else sees a fixed label for their own resort.

Remember username stores the name only, in `data/preferences.properties`. The password is never stored there.

## What each screen is for

| Screen | Use it to |
| --- | --- |
| Dashboard | See lodge counts, arrivals, departures, and charts for the resort in the top bar. |
| Stays | Book a lodge. Dates cannot overlap another stay on that lodge unless the other stay is cancelled. |
| Lodges | Add units. A unit code is unique inside one resort, not across the group. Zones are Beach, Lagoon, Garden, and Cliff. |
| Guests | One guest book for the whole group. A guest can stay at more than one property. |
| Arrivals | Arrive a guest when today sits inside the stay, then depart them. Departure sends the lodge to turn-down. |
| Billing | Take cash, card, UPI, or a bank transfer. A refund is a negative payment. Checkout is allowed with a balance still open. |
| Lodge care | Turn-down and inspection. A lodge becomes Ready only after a completed task is passed and no hold covers today. |
| Experiences | Spa, water, dining, laundry, and minibar items for the current resort. Served in-lodge orders attach to the bill on departure. |
| Employees | Staff for the current resort. Departments stay Reception, Housekeeping, Maintenance, Restaurant, Security, and Management. |
| Inventory | Stock for the current resort. A SKU is unique inside that resort. |
| Reports | Revenue, occupancy, stay trends, cancellations, guests, balances, lodge performance, stock, and attendance. |
| Settings | Administrators only. Add or edit resorts, load demonstration data, and create users. |

When the top bar says **All resorts**, lists add the property name and totals skip inactive resorts. Saving a lodge, stay, employee, stock line, or experience still needs one property. The lodge dialog asks which resort. Other saves use the first active resort if you have not picked one. Switch to that resort first when you want the row stored there.

New staff accounts that are not administrators are assigned to the resort selected in the top bar. If the bar says All resorts, they are assigned to the first active resort.

## Status words

Lodge: Ready, In stay, Held, Turn-down, Repair, Closed.

Stay: Pending, Confirmed, Arrived, Departed, Cancelled, No-show.

A no-show still blocks the dates. Only a cancelled stay frees them.

Meal plans on a stay are EP, CP, MAP, or AP. Transfers are None, Airport, or Jetty.

## Demonstration data

Settings, **Load demonstration data**, adds sample rows and does not replace real ones.

- CoralCove Beach (already seeded, not a sample) receives lodges B01–B10 (beach) and L01–L06 (lagoon).
- CoralCove Cliff (Varkala) is added as a sample resort with G01–G08 (garden) and C01–C06 (cliff).
- That is 30 lodges, 15 shared guests, 10 staff split across the two properties, and 10 stays whose references start with `CC-`.
- Payments are UPI, some partial and some full. One turn-down is high priority. One estate repair is open. Some stock is low. One dining order is preparing, and one served in-lodge experience sits on an arrived stay.
- User `beach.desk` / `Desk@123` is front desk for CoralCove Beach, so you can sign out and see that a Beach account cannot book a Cliff lodge.

**Reset demonstration data** deletes rows marked sample, the Cliff property, the copied Cliff catalog, and `beach.desk`. CoralCove Beach and its original catalog stay. After a reset the lodge list is empty until you load the sample again or add real units.

## Occupancy

Occupancy is sold lodge-nights divided by lodges that are not Closed, times the number of days in the range. It is not a count of lodges whose status is In stay today. Pending, cancelled, and no-show stays are left out of the numerator. Confirmed, arrived, and departed stays count.

## Money and dates

Amounts are rupees with two decimal places. The screen shows ₹. Printed pages use `Rs.` because the built-in PDF font has no rupee glyph. Dates on screen are `dd-MM-yyyy`. The database stores `yyyy-MM-dd`. "Today" follows the selected resort's time zone, or Asia/Kolkata when All resorts is selected and the first active resort has no zone. The date spinner itself uses the computer's calendar.

Tax is the percent stored on the resort (12 on the seeded Beach property).
