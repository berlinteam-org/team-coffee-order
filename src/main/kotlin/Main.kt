import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.http.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.net.URL
import java.net.URLEncoder
import kotlin.random.Random
import java.time.Instant
import java.time.Duration
import jakarta.mail.*
import jakarta.mail.internet.*
import java.util.Properties

data class LoginCode(val code: String, val expiresAt: Instant)

private val loginCodes = mutableMapOf<String, LoginCode>()

fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            staticResources("/assets", "assets")

            get("/") {
                val mailingOn = System.getenv("MAILING_ON") ?: "ON"
                val isOff = mailingOn.uppercase() == "OFF"

                call.respondText(
                    """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>Welcome</title>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <link rel="icon" type="image/svg+xml" href="/assets/berlin_team.svg" />
                        <style>
                            * {
                                box-sizing: border-box;
                                margin: 0;
                                padding: 0;
                                font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                            }
                            body {
                                min-height: 100vh;
                                display: flex;
                                align-items: center;
                                justify-content: center;
                                background: radial-gradient(circle at top left, #66b3ff 0%, #0b63ce 40%, #021b79 100%);
                                color: #0b1a33;
                            }
                            .card {
                                background: rgba(255, 255, 255, 0.96);
                                border-radius: 18px;
                                box-shadow:
                                    0 24px 60px rgba(0, 26, 77, 0.45),
                                    0 0 0 1px rgba(255, 255, 255, 0.6);
                                padding: 40px 36px 32px;
                                width: 100%;
                                max-width: 380px;
                                backdrop-filter: blur(18px);
                                text-align: center;
                            }
                            .logo {
                                width: 60px;
                                height: 60px;
                                display: flex;
                                align-items: center;
                                justify-content: center;
                                margin: 0 auto 20px;
                            }
                            .logo img {
                                width: 100%;
                                height: 100%;
                                object-fit: contain;
                                display: block;
                            }
                            h1 {
                                font-size: 24px;
                                margin-bottom: 6px;
                                color: #0b1a33;
                                text-align: center;
                            }
                            p.subtitle {
                                font-size: 14px;
                                color: #4a5b7c;
                                margin-bottom: 26px;
                                text-align: center;
                            }
                            .field {
                                display: flex;
                                flex-direction: column;
                                gap: 6px;
                                margin-bottom: 16px;
                                text-align: left;
                            }
                            label {
                                font-size: 13px;
                                color: #2f3c59;
                                font-weight: 500;
                            }
                            input {
                                border-radius: 999px;
                                border: 1px solid rgba(15, 35, 95, 0.14);
                                padding: 10px 14px;
                                font-size: 14px;
                                outline: none;
                                transition: border-color 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease;
                                background: rgba(248, 250, 255, 0.95);
                                width: 100%;
                            }
                            input:focus {
                                border-color: #2f7bff;
                                box-shadow: 0 0 0 1px rgba(47, 123, 255, 0.45), 0 10px 30px rgba(32, 85, 182, 0.25);
                                background: #ffffff;
                            }
                            .actions {
                                margin-top: 18px;
                                display: flex;
                                flex-direction: column;
                                gap: 12px;
                            }
                            button {
                                border: none;
                                border-radius: 999px;
                                padding: 11px 16px;
                                font-size: 15px;
                                font-weight: 600;
                                letter-spacing: 0.02em;
                                cursor: pointer;
                                background: linear-gradient(135deg, #1d6bff 0%, #23a6ff 45%, #38f9d7 100%);
                                color: #ffffff;
                                box-shadow: 0 14px 35px rgba(15, 76, 163, 0.45);
                                transition: transform 0.12s ease, box-shadow 0.12s ease, filter 0.12s ease;
                                width: 100%;
                            }
                            button:hover {
                                transform: translateY(-1px);
                                box-shadow: 0 18px 40px rgba(10, 63, 140, 0.55);
                                filter: brightness(1.03);
                            }
                            button:active {
                                transform: translateY(0);
                                box-shadow: 0 10px 26px rgba(5, 38, 95, 0.55);
                                filter: brightness(0.98);
                            }
                            .meta, .hint {
                                font-size: 12px;
                                color: #627099;
                                text-align: center;
                            }
                        </style>
                    </head>
                    <body>
                        <main class="card">
                            <div class="logo">
                                <img src="/assets/berlin_team.svg" alt="Logo" />
                            </div>
                            ${if (isOff) """
                                <h1>Order Team Coffee</h1>
                                <p class="subtitle">Quick pre order for team.</p>
                                <form method="post" action="/coffee">
                                    <input type="hidden" name="email" value="guest@digital-consultants.de" />
                                    <input type="hidden" name="skipAuth" value="true" />
                                    <div class="actions">
                                        <button type="submit">Order coffee now</button>
                                    </div>
                                </form>
                            """ else """
                                <h1>Welcome back</h1>
                                <p class="subtitle">Log in without a password using only your email.</p>
                                <form method="post" action="/login/request-code">
                                    <div class="field">
                                        <label for="email">Email Address</label>
                                        <input id="email" name="email" type="email" placeholder="you@example.com" autocomplete="email" required />
                                    </div>
                                    <div class="actions">
                                        <button type="submit">Request Login Link</button>
                                        <p class="meta">We'll send a one-time code to your email.</p>
                                    </div>
                                </form>
                            """}
                        </main>
                    </body>
                    </html>
                    """.trimIndent(),
                    ContentType.Text.Html
                )
            }

            post("/login/request-code") {
                val params = call.receiveParameters()
                val email = params["email"]?.trim().orEmpty()

                if (email.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Email must not be empty.")
                    return@post
                }

                // Test bypass: skip code check for the test address (only in DEV_MODE)
                if (System.getenv("DEV_MODE") == "true" && email.lowercase() == "a@b.de") {
                    call.respondText(
                        """
                        <!DOCTYPE html>
                        <html><head><meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        </head>
                        <body>
                        <form id="f" method="post" action="/coffee">
                            <input type="hidden" name="email" value="${email}" />
                            <input type="hidden" name="skipAuth" value="true" />
                        </form>
                        <script>document.getElementById('f').submit();</script>
                        </body></html>
                        """.trimIndent(),
                        ContentType.Text.Html
                    )
                    return@post
                }

                val code = (100000..999999).random().toString()
                val expiresAt = Instant.now().plus(Duration.ofMinutes(5))
                loginCodes[email.lowercase()] = LoginCode(code, expiresAt)

                // Always print to logs for debugging/fallback
                println("Login code for $email: $code (valid for 5 minutes)")

                val devMode = System.getenv("DEV_MODE") == "true"
                if (!devMode) {
                    // Production: send via Gmail SMTP
                    try {
                        val smtpHost = System.getenv("MAIL_SMTP_HOST") ?: "smtp.gmail.com"
                        val smtpPort = System.getenv("MAIL_SMTP_PORT") ?: "587"
                        val mailUser = System.getenv("MAIL_USERNAME") ?: ""
                        val mailPass = System.getenv("MAIL_PASSWORD") ?: ""
                        val mailFrom = System.getenv("MAIL_FROM") ?: mailUser

                        val props = Properties().apply {
                            put("mail.smtp.host", smtpHost)
                            put("mail.smtp.port", smtpPort)
                            put("mail.smtp.auth", "true")
                            
                            // Essential for cloud providers / SSL vs STARTTLS
                            if (smtpPort == "465") {
                                put("mail.smtp.ssl.enable", "true")
                                put("mail.smtp.socketFactory.port", "465")
                                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                            } else {
                                put("mail.smtp.starttls.enable", "true")
                                put("mail.smtp.starttls.required", "true")
                            }

                            // Timeouts to prevent hanging
                            put("mail.smtp.connectiontimeout", "10000")
                            put("mail.smtp.timeout", "10000")
                            put("mail.smtp.writetimeout", "10000")
                        }
                        val session = Session.getInstance(props, object : Authenticator() {
                            override fun getPasswordAuthentication() =
                                PasswordAuthentication(mailUser, mailPass)
                        })
                        val message = MimeMessage(session).apply {
                            setFrom(InternetAddress(mailFrom, "Team Coffee"))
                            setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                            subject = "Your login code"
                            setText("Your login code is: $code\n\nThe code is valid for 5 minutes.")
                        }
                        Transport.send(message)
                        println("Login code email sent to $email")
                    } catch (e: Exception) {
                        println("Failed to send email to $email: ${e.message}")
                    }
                }

                call.respondText(
                    """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>Code Sent</title>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            body {
                                min-height: 100vh;
                                display: flex;
                                align-items: center;
                                justify-content: center;
                                background: radial-gradient(circle at top left, #66b3ff 0%, #0b63ce 40%, #021b79 100%);
                                margin: 0;
                                font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                            }
                            .card {
                                background: rgba(255, 255, 255, 0.96);
                                border-radius: 18px;
                                box-shadow: 0 24px 60px rgba(0, 26, 77, 0.45);
                                padding: 32px 30px 28px;
                                width: 100%;
                                max-width: 380px;
                            }
                            h1 {
                                font-size: 22px;
                                margin: 0 0 10px;
                            }
                            p {
                                font-size: 14px;
                                color: #4a5b7c;
                                margin: 0 0 20px;
                            }
                            .field {
                                display: flex;
                                flex-direction: column;
                                gap: 6px;
                                margin-bottom: 16px;
                            }
                            label {
                                font-size: 13px;
                                color: #2f3c59;
                                font-weight: 500;
                            }
                            input {
                                border-radius: 999px;
                                border: 1px solid rgba(15, 35, 95, 0.14);
                                padding: 10px 14px;
                                font-size: 14px;
                                outline: none;
                                background: rgba(248, 250, 255, 0.95);
                            }
                            button {
                                border: none;
                                border-radius: 999px;
                                padding: 11px 16px;
                                font-size: 15px;
                                font-weight: 600;
                                cursor: pointer;
                                background: linear-gradient(135deg, #1d6bff 0%, #23a6ff 45%, #38f9d7 100%);
                                color: #ffffff;
                                width: 100%;
                                margin-top: 8px;
                            }
                            .hint {
                                margin-top: 10px;
                            }
                        </style>
                    </head>
                    <body>
                        <main class="card">
                            <h1>Code Sent</h1>
                            <p>We've sent a one-time login code to <strong>${email}</strong>.</p>
                            <form method="post" action="/coffee">
                                <input type="hidden" name="email" value="${email}" />
                                <div class="field">
                                    <label for="code">Login Code</label>
                                    <input id="code" name="code" type="text" inputmode="numeric" pattern="[0-9]{6}" placeholder="123456" required />
                                </div>
                                <button type="submit">Verify Code</button>
                                <p class="hint">The code is valid for 5 minutes.</p>
                            </form>
                        </main>
                    </body>
                    </html>
                    """.trimIndent(),
                    ContentType.Text.Html
                )
            }

            post("/coffee") {
                val params = call.receiveParameters()
                val email = params["email"]?.trim().orEmpty().lowercase()
                val code = params["code"]?.trim().orEmpty()
                val skipAuth = params["skipAuth"] == "true"

                val stored = loginCodes[email]
                val now = Instant.now()

                val success = skipAuth || (stored != null &&
                    stored.code == code &&
                    stored.expiresAt.isAfter(now))

                if (success) {
                    loginCodes.remove(email)
                    // Nach erfolgreichem Login direkt in den Team-Coffee-Flow
                    call.respondText(
                        """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <title>Team Coffee</title>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <link rel="icon" type="image/svg+xml" href="/assets/berlin_team.svg" />
                            <style>
                                * { box-sizing: border-box; margin: 0; padding: 0; font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
                                body {
                                    min-height: 100vh;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    background: radial-gradient(circle at top left, #66b3ff 0%, #0b63ce 40%, #021b79 100%);
                                    padding: 24px;
                                }
                                .card {
                                    background: rgba(255,255,255,0.98);
                                    border-radius: 22px;
                                    box-shadow: 0 26px 70px rgba(0,26,77,0.5);
                                    padding: 32px 28px 26px;
                                    width: 100%;
                                    max-width: 460px;
                                }
                                .logo-row {
                                    display: flex;
                                    flex-direction: column;
                                    align-items: center;
                                    text-align: center;
                                    margin-bottom: 18px;
                                }
                                .logo {
                                    width: 90px;
                                    height: 90px;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    margin-bottom: 12px;
                                }
                                .logo img {
                                    width: 100%;
                                    height: 100%;
                                    object-fit: contain;
                                    display: block;
                                }
                                h1 {
                                    font-size: 24px;
                                    color: #0b1a33;
                                    text-align: center;
                                }
                                .tagline {
                                    font-size: 13px;
                                    color: #4a5b7c;
                                    margin-bottom: 18px;
                                    text-align: center;
                                }
                                .step-label {
                                    font-size: 12px;
                                    font-weight: 600;
                                    text-transform: uppercase;
                                    letter-spacing: 0.08em;
                                    color: #6173ff;
                                    margin-bottom: 6px;
                                }
                                .field {
                                    display: flex;
                                    flex-direction: column;
                                    gap: 6px;
                                    margin-bottom: 14px;
                                }
                                label {
                                    font-size: 13px;
                                    color: #2f3c59;
                                    font-weight: 500;
                                }
                                input, select {
                                    border-radius: 999px;
                                    border: 1px solid rgba(15,35,95,0.16);
                                    padding: 9px 14px;
                                    font-size: 14px;
                                    outline: none;
                                    background: rgba(248,250,255,0.98);
                                }
                                input:focus, select:focus {
                                    border-color: #2f7bff;
                                    box-shadow: 0 0 0 1px rgba(47,123,255,0.45), 0 10px 30px rgba(32,85,182,0.25);
                                    background: #ffffff;
                                }
                                .inline {
                                    display: grid;
                                    grid-template-columns: 1fr 1fr;
                                    gap: 10px;
                                }
                                .actions {
                                    display: flex;
                                    justify-content: flex-end;
                                    margin-top: 16px;
                                }
                                .send-row {
                                    display: flex;
                                    justify-content: flex-end;
                                    margin-top: 12px;
                                }
                                button {
                                    border: none;
                                    border-radius: 999px;
                                    padding: 10px 18px;
                                    font-size: 14px;
                                    font-weight: 600;
                                    cursor: pointer;
                                    background: linear-gradient(135deg, #1d6bff 0%, #23a6ff 45%, #38f9d7 100%);
                                    color: #ffffff;
                                    box-shadow: 0 14px 35px rgba(15,76,163,0.45);
                                    transition: transform 0.12s ease, box-shadow 0.12s ease, filter 0.12s ease;
                                }
                                button:disabled {
                                    opacity: 1;
                                    cursor: not-allowed;
                                    background: #e2e8f0;
                                    color: #94a3b8;
                                    border: 1px solid #cbd5e1;
                                    transform: none;
                                    box-shadow: none;
                                }
                                button.secondary {
                                    background: transparent;
                                    color: #1d6bff;
                                    box-shadow: none;
                                }
                                button.secondary:disabled {
                                    opacity: 0.5;
                                    color: #627099;
                                }
                                ul {
                                    list-style: none;
                                    margin-top: 12px;
                                    margin-bottom: 4px;
                                    padding: 12px;
                                    border: 1px solid rgba(15,35,95,0.16);
                                    border-radius: 12px;
                                    background: rgba(248,250,255,0.5);
                                    min-height: 44px;
                                }
                                li {
                                    font-size: 13px;
                                    color: #2f3c59;
                                    padding: 4px 0;
                                }
                                .hint {
                                    font-size: 12px;
                                    color: #627099;
                                    margin-top: 4px;
                                }
                                .summary-title {
                                    margin-top: 18px;
                                    font-size: 14px;
                                    font-weight: 600;
                                    color: #0b1a33;
                                }
                                .radio-group {
                                    display: flex;
                                    flex-direction: column;
                                    gap: 8px;
                                }
                                .radio-option {
                                    display: flex;
                                    align-items: center;
                                    gap: 10px;
                                    padding: 10px 14px;
                                    border: 1px solid rgba(15,35,95,0.16);
                                    border-radius: 12px;
                                    background: rgba(248,250,255,0.98);
                                    cursor: pointer;
                                    transition: all 0.2s ease;
                                }
                                .radio-option:has(input:checked) {
                                    border-color: #2f7bff;
                                    background: #ffffff;
                                    box-shadow: 0 4px 12px rgba(47,123,255,0.15);
                                }
                                .radio-option input[type="radio"] {
                                    margin: 0;
                                    width: 18px;
                                    height: 18px;
                                    accent-color: #2f7bff;
                                    cursor: pointer;
                                }
                                .radio-label {
                                    font-size: 14px;
                                    color: #2f3c59;
                                    font-weight: 500;
                                    cursor: pointer;
                                }
                            </style>
                        </head>
                        <body>
                            <main class="card">
                                <div class="logo-row">
                                    <div class="logo">
                                        <img src="/assets/berlin_team.svg" alt="Logo" />
                                    </div>
                                    <h1>Team Coffee</h1>
                                    <p class="tagline">Order coffee for you and your friends - you can pick it up after the service - keep your team card ready so we can stamp it</p>
                                </div>
                                <form id="coffeeForm" method="post" action="/team-coffee/send">
                                    <div class="step-label">Step 1 · Name</div>
                                    <div class="field">
                                        <input id="friendName" type="text" placeholder="e.g. Anna" autocomplete="off" />
                                    </div>
                                    <div class="step-label">Step 2 · Coffee</div>
                                    <div class="field">
                                        <div class="radio-group" id="drinkGroup">
                                            <label class="radio-option"><input type="radio" name="drink" value="Flat White" checked> <span class="radio-label">Flat White</span></label>
                                            <label class="radio-option"><input type="radio" name="drink" value="Cappuccino"> <span class="radio-label">Cappuccino</span></label>
                                            <label class="radio-option"><input type="radio" name="drink" value="Americano"> <span class="radio-label">Americano</span></label>
                                            <label class="radio-option"><input type="radio" name="drink" value="Espresso"> <span class="radio-label">Espresso</span></label>
                                            <label class="radio-option"><input type="radio" name="drink" value="Espresso Doppio"> <span class="radio-label">Espresso Doppio</span></label>
                                            <label class="radio-option"><input type="radio" name="drink" value="Chai Latte"> <span class="radio-label">Chai Latte</span></label>
                                            <label class="radio-option"><input type="radio" name="drink" value="Hot Chocolate"> <span class="radio-label">Hot Chocolate</span></label>
                                        </div>
                                    </div>
                                    <div class="step-label">Step 3 · milk</div>
                                    <div class="field">
                                        <div class="radio-group" id="milkGroup">
                                            <label class="radio-option"><input type="radio" name="milk" value="Oat" checked> <span class="radio-label">Oat</span></label>
                                            <label class="radio-option"><input type="radio" name="milk" value="Cow"> <span class="radio-label">Cow</span></label>
                                            <label class="radio-option"><input type="radio" name="milk" value="None"> <span class="radio-label">None</span></label>
                                        </div>
                                    </div>

                                    <div class="actions">
                                        <button type="button" class="secondary" id="addBtn">Add Coffee</button>
                                    </div>

                                    <p class="summary-title">Your order list</p>
                                    <ul id="cartList"></ul>
                                    <p class="hint">When you are done, klick ready and send us your order.</p>
                                    <div class="send-row">
                                        <button type="submit" id="sendBtn" disabled>Senden</button>
                                    </div>

                                    <input type="hidden" name="summary" id="summaryInput" />
                                    <input type="hidden" name="email" value="${email}" />
                                </form>
                            </main>
                            <script>
                                const cart = [];
                                const cartList = document.getElementById('cartList');
                                const friendInput = document.getElementById('friendName');
                                const summaryInput = document.getElementById('summaryInput');
                                const sendBtn = document.getElementById('sendBtn');

                                function renderCart() {
                                    cartList.innerHTML = '';
                                    if (cart.length === 0) {
                                        cartList.innerHTML = '<li style="text-align:center; color:#627099; padding: 4px 0;">Pls add a coffee</li>';
                                    }
                                    cart.forEach((item, idx) => {
                                        const li = document.createElement('li');
                                        li.textContent = (idx + 1) + '. ' + item.name + ' – ' + item.drink + ' (' + item.milk + ')';
                                        cartList.appendChild(li);
                                    });
                                    if (cart.length > 0) {
                                        sendBtn.disabled = false;
                                        const lines = cart.map((item, idx) =>
                                            '👉 ' + item.name + ' – ' + item.drink + ' (' + item.milk + ')'
                                        );
                                        const dateObj = new Date();
                                        const rawHours = dateObj.getHours();
                                        const ampm = rawHours >= 12 ? 'pm' : 'am';
                                        const hours12 = rawHours % 12 || 12;
                                        const timeStr = hours12 + ':' + dateObj.getMinutes().toString().padStart(2, '0') + ' ' + ampm;
                                        summaryInput.value = 'Team Coffee Order - ' + timeStr + ' - for:\n' + lines.join('\n');
                                    } else {
                                        sendBtn.disabled = true;
                                        summaryInput.value = '';
                                    }
                                }

                                document.getElementById('addBtn').addEventListener('click', () => {
                                    const name = friendInput.value.trim();
                                    if (!name) {
                                        alert('Please enter a name.');
                                        return;
                                    }
                                    const drinkChecked = document.querySelector('input[name="drink"]:checked');
                                    const milkChecked = document.querySelector('input[name="milk"]:checked');
                                    cart.push({
                                        name,
                                        drink: drinkChecked ? drinkChecked.value : '',
                                        milk: milkChecked ? milkChecked.value : ''
                                    });
                                    friendInput.value = '';
                                    renderCart();
                                });

                                document.getElementById('coffeeForm').addEventListener('submit', (e) => {
                                    if (cart.length === 0) {
                                        e.preventDefault();
                                        alert('Please add at least one coffee to the list.');
                                    }
                                });

                                // Initial render to show empty placeholder
                                renderCart();
                            </script>
                        </body>
                        </html>
                        """.trimIndent(),
                        ContentType.Text.Html
                    )
                } else {
                val message = "The code is invalid or has expired."
                    call.respondText(
                        """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <title>Login Failed</title>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        </head>
                        <body>
                            <p>$message</p>
                            <p><a href="/">Back to login page</a></p>
                        </body>
                        </html>
                        """.trimIndent(),
                        ContentType.Text.Html
                    )
                }
            }

            post("/team-coffee/send") {
                val params = call.receiveParameters()
                val summary = params["summary"]?.trim().orEmpty()
                val email = params["email"]?.trim().orEmpty()

                if (summary.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "No order data received.")
                    return@post
                }

                val botToken = System.getenv("TELEGRAM_BOT_TOKEN")
                val chatId = System.getenv("TELEGRAM_CHAT_ID")

                val telegramOk = if (botToken.isNullOrBlank() || chatId.isNullOrBlank()) {
                    false
                } else {
                    try {
                        val url = URL("https://api.telegram.org/bot$botToken/sendMessage")
                        val text = summary
                        val data = "chat_id=" + URLEncoder.encode(chatId, "UTF-8") +
                                "&text=" + URLEncoder.encode(text, "UTF-8")
                        val conn = url.openConnection()
                        conn.doOutput = true
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                        conn.getOutputStream().use { os ->
                            os.write(data.toByteArray(Charsets.UTF_8))
                        }
                        val codeResp = (conn as java.net.HttpURLConnection).responseCode
                        codeResp in 200..299
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }

                val statusText = if (telegramOk) {
                    "Your team coffee order has been sent to the service team!"
                } else {
                    "Order created, but Telegram could not be reached. Please check the bot token and chat ID."
                }

                call.respondText(
                    """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>Team Coffee – Sent</title>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <link rel="icon" type="image/svg+xml" href="/assets/berlin_team.svg" />
                        <style>
                            * { box-sizing: border-box; margin: 0; padding: 0; font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
                            body {
                                min-height: 100vh;
                                display: flex;
                                align-items: center;
                                justify-content: center;
                                background: radial-gradient(circle at top left, #66b3ff 0%, #0b63ce 40%, #021b79 100%);
                                padding: 24px;
                            }
                            .card {
                                background: rgba(255,255,255,0.98);
                                border-radius: 22px;
                                box-shadow: 0 26px 70px rgba(0,26,77,0.5);
                                padding: 40px 32px 36px;
                                width: 100%;
                                max-width: 460px;
                                text-align: center;
                            }
                            .emoji-hero {
                                font-size: 64px;
                                margin-bottom: 12px;
                            }
                            h1 {
                                font-size: 24px;
                                color: #0b1a33;
                                margin-bottom: 8px;
                            }
                            p.status {
                                font-size: 14px;
                                color: #4a5b7c;
                                margin-bottom: 24px;
                            }
                            pre {
                                text-align: left;
                                background: rgba(248,250,255,0.5);
                                padding: 16px;
                                border-radius: 12px;
                                border: 1px solid rgba(15,35,95,0.16);
                                font-size: 13px;
                                color: #2f3c59;
                                margin-bottom: 28px;
                                white-space: pre-wrap;
                                font-family: inherit;
                                line-height: 1.5;
                            }
                            .button {
                                display: inline-block;
                                text-decoration: none;
                                border: none;
                                border-radius: 999px;
                                padding: 12px 24px;
                                font-size: 15px;
                                font-weight: 600;
                                cursor: pointer;
                                background: linear-gradient(135deg, #1d6bff 0%, #23a6ff 45%, #38f9d7 100%);
                                color: #ffffff;
                                box-shadow: 0 14px 35px rgba(15,76,163,0.45);
                                transition: transform 0.12s ease, box-shadow 0.12s ease, filter 0.12s ease;
                                width: 100%;
                            }
                            .button:hover {
                                transform: translateY(-1px);
                                box-shadow: 0 18px 40px rgba(10,63,140,0.55);
                                filter: brightness(1.03);
                            }
                            .button:active {
                                transform: translateY(0);
                                box-shadow: 0 10px 26px rgba(5,38,95,0.55);
                                filter: brightness(0.98);
                            }
                        </style>
                    </head>
                    <body>
                        <main class="card">
                            <div class="emoji-hero">🎉</div>
                            <h1>Order Sent!</h1>
                            <p class="status">$statusText</p>
                            <pre>${summary.replace("<", "&lt;")}</pre>
                            <form method="post" action="/coffee">
                                <input type="hidden" name="email" value="${email}" />
                                <input type="hidden" name="skipAuth" value="true" />
                                <button type="submit" class="button">New Order</button>
                            </form>
                        </main>
                    </body>
                    </html>
                    """.trimIndent(),
                    ContentType.Text.Html
                )
            }
        }
    }.start(wait = true)
}

