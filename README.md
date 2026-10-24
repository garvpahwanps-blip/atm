# Secure ATM and Banking Management System

Java 21, MySQL 8.0.16+ (8.4 recommended), Tomcat 10.1, Jakarta Servlet/JSP, and Swing/AWT.
No Maven, Gradle, frameworks, REST APIs, JSTL, or JUnit. The web and desktop interfaces
share the same model, JDBC DAOs, banking service, and security implementation.

## Final layout

```
src/com/atm/
  model/       User, Account, CustomerSummary, BankTransaction, TransactionType
  dao/         Database, UserDAO, AccountDAO, TransactionDAO
  service/     BankingService, Validation, BankingException,
               InsufficientBalanceException, AccountLockedException, AuthenticationException
  security/    PasswordHasher, AdminPasswordTool, AdminAuthenticator
  servlet/     Web, RegistrationServlet, LoginServlet, DashboardServlet,
               DepositServlet, WithdrawServlet, TransferServlet, StatementServlet,
               ChangePinServlet, LogoutServlet, AdminServlet
  filter/      AuthenticationFilter
  desktop/     AdminDashboard, ClockThread, SessionMonitorThread
webapp/
  index.jsp
  assets/style.css
  WEB-INF/web.xml
  WEB-INF/views/  register.jsp, login.jsp, dashboard.jsp, deposit.jsp,
                 withdraw.jsp, transfer.jsp, statement.jsp, change-pin.jsp,
                 admin.jsp, error.jsp, header.jspf, footer.jspf, transactions.jspf
  META-INF/context.xml               # SameSite session cookies
database/schema.sql
resources/database.properties.example
lib/README.md                         # manually add Connector/J here
scripts/                              # javac, jar, tests, desktop launcher
tests/com/atm/tests/                   # main()-based tests, no JUnit
```

JSP views live under WEB-INF so direct requests cannot bypass controllers. Database
configuration and passwords are never included in the WAR or committed to Git.

## 1. IntelliJ and manual libraries

1. Install a JDK **21**, MySQL **8.4**, and Apache Tomcat **10.1** binary distribution
   from their official sites. Verify downloaded archives using publisher checksums.
   Use `java -version` and `javac -version`; a JRE alone is insufficient.
2. Open this directory in IntelliJ as a plain Java project. In **File > Project
   Structure > Project**, select JDK 21 and language level 21. Mark `src` as Sources
   and `tests` as Test Sources. Do not mark `resources` as a deployment resource root.
3. Download MySQL Connector/J **9.4.0** from MySQL's official Connector/J downloads,
   or download the official artifact at
   `https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/9.4.0/mysql-connector-j-9.4.0.jar`.
   This is a manual JAR download, not use of Maven. Put the JAR in `lib/`.
4. In **Project Structure > Modules > Dependencies > + > JARs or directories**, add
   that JAR and `TOMCAT_HOME/lib/servlet-api.jar`. Use **Provided** scope for the
   Servlet API. Tomcat supplies Jakarta Servlet 6; `javax.servlet.*` is incompatible.
5. Set `JAVA_HOME` to your JDK 21 and `TOMCAT_HOME` to the extracted Tomcat directory.
   On Linux/macOS add `$JAVA_HOME/bin` to PATH. The scripts use Bash; on Windows use
   Git Bash, or the equivalent `javac`/`jar` commands with `;` classpath separators.

## 2. MySQL and local configuration

Run `mysql -u root -p < database/schema.sql`. In an administrator MySQL shell,
create a local application login, choosing your own strong password:

```sql
CREATE USER 'atm_app'@'127.0.0.1' IDENTIFIED BY '<choose a strong password>';
GRANT SELECT, INSERT, UPDATE ON secure_atm.* TO 'atm_app'@'127.0.0.1';
```

Use the appropriate host for your installation (MySQL may match localhost instead).
The application user needs no schema administration or DELETE permission.
Copy `resources/database.properties.example` to `resources/database.properties`.
Set `db.user`, `db.password`, and the JDBC host/port. `chmod 600` the local file on
Unix. Do not commit it. The example's TLS-disabled URL is for loopback development
only; remote databases must use `sslMode=VERIFY_IDENTITY` with trusted certificates,
and `allowPublicKeyRetrieval` should be removed. All database timestamps use UTC.

## 3. Build and set the administrator password

```sh
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
export TOMCAT_HOME=/path/to/apache-tomcat-10.1.x
bash scripts/build.sh
java -cp build/classes com.atm.security.AdminPasswordTool
```

The password tool prompts on a real console without echo and emits a PBKDF2 hash
(never the password). Put its output in the local `admin.passwordHash` property.
The administrator login name is `admin`. Use a long, unique password of at least
12 characters. Missing admin configuration disables admin access. Both interfaces
apply an administrator login throttle and idle expiry; web administration also
requires authentication and CSRF tokens.

## 4. Deploy to Tomcat

```sh
export CATALINA_OPTS="-Datm.config=$PWD/resources/database.properties"
cp build/atm.war "$TOMCAT_HOME/webapps/atm.war"
"$TOMCAT_HOME/bin/catalina.sh" run
```

Set the absolute `-Datm.config=...` JVM option in IntelliJ's Tomcat run configuration,
or in a private Tomcat `bin/setenv.sh` for background startup. Do not put credentials
in command arguments. IntelliJ Ultimate can deploy `build/atm.war` under context
`/atm`; Community Edition can use the external Tomcat commands above.
Open the local Tomcat `/atm/` context in your own development environment. Register
a customer, record the generated 12-digit number, log in, deposit, withdraw,
transfer to a second registered customer, and inspect the last ten transactions.
Enable HTTPS and secure cookies before any nonlocal deployment. Do not expose this
development Tomcat or MySQL publicly. Logout uses POST and invalidates the session.

## 5. Swing administrator

```sh
bash scripts/run-admin.sh
```

The launcher reads the same local properties and shared classes as Tomcat. Sign in
as admin. Search by full account number, select a customer, inspect history, and
unlock accounts. Refresh and unlock beep after success. Desktop access requires
a graphical display. Clock and inactivity threads update Swing using the EDT and
are interrupted and joined when the frame closes. Idle expiry returns to login.

## 6. Verification, module by module

1. **Shared security/models:** `bash scripts/test.sh unit` verifies hashing, salt
   uniqueness, invalid PINs/money/names, tampered hashes, and thread shutdown.
2. **JDBC/service:** initialize MySQL, then `bash scripts/test.sh integration`.
   This creates uniquely named test customers in the selected database and leaves
   them for audit. Prefer a dedicated test database configured with
   `ATM_CONFIG=/absolute/path/to/test-database.properties`. Checks cover registration,
   login/lock/unlock, PIN change, deposits, overdrafts, paired transfer entries,
   rollback, and concurrent withdrawal/transfer consistency.
3. **Web:** build/deploy the WAR, then `bash scripts/test.sh web` while Tomcat runs.
   `ATM_BASE_URL` defaults to `http://127.0.0.1:8080/atm`. The Java HTTP test checks
   protected routes, registration, login, session fixation protection, CSRF,
   deposit, withdrawal, transfer, mini statement, PIN changes, and logout.
4. **Desktop:** `bash scripts/test.sh desktop` opens and closes the actual Swing
   frame and tests session expiry; requires a display (or `xvfb-run` on Linux).
   Also manually verify table selection, history, clock updates, unlock, and beep.

`scripts/build.sh` compiles every source with `javac --release 21`, compiles manual
tests, and packages `build/atm.war` with only shared classes and Connector/J. Test
classes and local configuration are excluded. Rebuild and redeploy after edits.
No application source or credentials are generated during environment startup.

## Banking and security behavior

All money uses BigDecimal and decimal SQL columns. Amounts must be positive with
at most two fractional digits and a maximum of 1,000,000,000 per operation. Accounts
start at zero. Registration, authentication state, deposits, withdrawals, PIN
changes, unlocks, and transfers use database transactions. Transfers lock account
rows in ascending ID order with SELECT FOR UPDATE and commit both ledger entries
and balances together. Every ledger row has a unique UUID reference; a transfer's
two references share a UUID and use `-D`/`-C` suffixes for reconciliation.

PINs use PBKDF2WithHmacSHA256, 600,000 iterations, a random 16-byte salt and
constant-time comparison; the versioned hash field includes the salt and cost.
Three wrong PIN attempts lock the account, including wrong current PIN attempts
during PIN changes. Admin unlock resets the counter. Locked customers cannot
perform authenticated operations; inbound transfers can still be received.
SecureRandom generates 12-digit account numbers; a database unique constraint and
bounded retries handle collisions. Session IDs rotate on login; mutation endpoints
require POST and a session CSRF token. JSP output is HTML-escaped in view models.
Session cookies are HttpOnly, cookie-only sessions expire after 10 idle minutes,
and responses prohibit caching and embedding. Database errors are logged on the
server and return generic messages to users.

This implements the requested educational banking workflow. Deposits are simulated
cash credits; real settlement, regulatory controls, distributed admin throttling,
and production operational controls require separate product design.

## Prepared cloud workspace

In the prepared cloud machine, run `source /workspace/.atm-state/env.sh` to activate
the installed JDK 21 and Tomcat paths. Start or restore the local MySQL and Tomcat
services with `bash /workspace/.atm-state/start-services.sh`. MySQL data is stored
under `/workspace/.atm-state/mysql` and survives container recreation. No passwords
are included in source or artifacts. The generated development admin password is
kept in `/workspace/.atm-state/admin-password.txt` with owner-only permissions;
replace it using AdminPasswordTool when desired. The login name is `admin`.

The cloud validation uses a virtual display for desktop tests:
`DISPLAY=:99 ATM_TEST_ADMIN_PASSWORD_FILE=/workspace/.atm-state/admin-password.txt bash scripts/test.sh desktop`.
Authenticated web admin tests use the same password-file variable. Customer test
data is deliberately retained in the development database. Publishing the environment
and validating a newly restored task are separate platform steps.
