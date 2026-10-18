# Campus Find — MES Lost & Found

Full-stack Java mini-project for Pillai HOC College.

## Stack
- React + Vite + HTML/CSS/JavaScript frontend
- Java 21 + Spring Boot 3 backend
- Spring Data JPA + MySQL

## Persistent data
Student accounts, verified-email state, password hashes, and password-reset token hashes are stored in MySQL's `students` table.
Item reports are stored in the `campus_items` table; new reports remain `PENDING` until approved, and only approved items are shown on the public board.
Signed-in students can open **My reports** from the profile menu to track their submissions, delete their own reports, or mark an approved report as resolved. Reports are queried from MySQL using the signed-in account email, and only that account's reports are returned. Resolved reports are removed from the public board. The configured Campus Security account can view reports across all statuses, delete a report with its linked submissions and uploaded photo, and compose a plain-text email to the saved owner address. The recipient is selected by the backend from the report record and cannot be changed in the compose form. These actions are authenticated and restricted to the report owner or security role.
Reports created before owner-email tracking was added cannot be safely attributed automatically; contact Campus Security to verify ownership of a legacy report.
Signed-in students can submit a private ownership claim on an approved `FOUND` listing, or submit an `I found this item` notice on an approved `LOST` listing. Submissions require private verification details; found notices also require the date and location where the item was found. They are stored in `campus_claims` and reviewed in the Campus Security page. Approval sends both parties a handoff email that does not include verification details; rejected submissions remain private and are not published on the board.
Optional item photos are limited to JPEG/PNG files up to 5 MB. The backend stores files in its `uploads/` directory by default and stores their URL in `campus_items.image_url`; set `UPLOAD_DIR` to change the file location. Back up this directory along with the database.
Email is used for account verification, password-reset delivery, and Campus Security report updates through SMTP; messages themselves are sent, not stored in MySQL.

## Requirements
- JDK 21+
- Node.js 20+
- MySQL 8+

## Run the database
Create a database and user in MySQL:
```sql
CREATE DATABASE campus_find;
CREATE USER 'campus_user'@'localhost' IDENTIFIED BY 'change_me';
GRANT ALL PRIVILEGES ON campus_find.* TO 'campus_user'@'localhost';
```
Set environment variables `DB_USERNAME` and `DB_PASSWORD` or update `backend/src/main/resources/application.properties`.

## Run backend
For Gmail email verification, set these environment variables before starting the backend:
`SMTP_HOST=smtp.gmail.com`, `SMTP_PORT=587`, `SMTP_USERNAME` to your Gmail address,
`SMTP_PASSWORD` to a Google App Password, and `SMTP_FROM` to the same Gmail address.
Set `FRONTEND_URL` to the frontend's base URL if it is not `http://localhost:5173`; password-reset links use this value.
Set `CAMPUS_SECURITY_EMAIL` to the verified account that will review and approve reports. For the configured security email to receive email verification, it may be registered even if it is not a MES-domain address.
Keep the app password private. The backend console will show the underlying mail error if sending fails.

```bash
cd backend
mvn spring-boot:run
```
API runs at http://localhost:8080.

## Run frontend
In a second terminal:
```bash
cd frontend
npm install
npm run dev
```
Open the Vite URL shown in the terminal.

## Important authentication note
Registration requires email OTP verification. Password reset links expire after 30 minutes and can be used once.
Campus security actions use expiring bearer sessions and are limited to the configured `CAMPUS_SECURITY_EMAIL`; the moderation page provides constrained report and claim review actions and does not execute arbitrary SQL. Claim and found-notice verification details are available only to Campus Security. Approval emails both parties to coordinate a handoff at the Campus Security desk; those emails do not include the private verification details.
Never use real student data in a demo database.
