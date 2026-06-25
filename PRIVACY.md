# Privacy Policy for TabMates

**Last Updated: June 25, 2026**

We are the developers of **TabMates**, an app for splitting shared expenses with
friends, flatmates and travel groups. This policy explains what personal data the
app handles, why, and what choices you have.

Unlike a purely offline app, TabMates uses a backend server so that the people in
a group can share groups and expenses and stay in sync across their devices. That
means some of the data you enter is sent to and stored on the TabMates server.
This policy describes exactly what that involves.

---

### 1. Information We Collect

We only collect what is needed to run the app. We do **not** collect data for
advertising, profiling, or sale.

**Account data**

* **Email address** — for registered accounts only. It is used to sign you in,
  verify your account, and recover your password.
* **Username** — the display name shown to other members of your groups.
* **Password** — stored on the server only in **hashed and salted** form. We never
  store or have access to your password in plain text.
* **Account type and status** — whether your account is a registered or guest
  account, your email-verification status, and an internal user ID.

If you choose to **continue as a guest** (an anonymous account), you provide only
a username — no email address is collected.

**Content you create**

* **Groups** — group names, optional descriptions, group membership, and invite
  links/tokens you create or use to join.
* **Expenses** — the expenses, income and settlements you record: titles,
  descriptions, amounts, currencies, who paid, how each entry is split between
  members, and the related timestamps.

This content is, by design, shared with the other members of the groups you
belong to, because that is the purpose of the app.

**Device and technical data**

* **Push-notification token** — a Firebase Cloud Messaging (FCM) token used to
  deliver notifications to your device.
* **Device platform** (e.g. Android, iOS, Desktop, Web), **app language/locale**,
  and **app version** — used to deliver localized notifications and to check
  whether an update is available.
* **Server logs** — standard request information (such as IP address and request
  metadata) that any web server records in order to operate the service securely.

---

### 2. How We Use Your Information

We use the data above only to:

* Provide the core service — create and sync your groups and expenses, calculate
  balances and who owes whom, and deliver real-time updates between members.
* Authenticate you and keep your account secure.
* Send you **transactional emails** — account verification and password reset.
  These are not marketing emails.
* Deliver **push notifications** about activity in your groups.
* Check whether a newer version of the app is available.
* Keep the service secure, prevent abuse, and diagnose problems.

We do **not** use your data for advertising, we do **not** build profiles about
you, and we do **not** sell or rent your data to anyone.

---

### 3. Legal Bases for Processing (GDPR)

If you are in the European Economic Area, we process your data on these legal
bases:

* **Performance of a contract** — operating your account and providing the
  expense-sharing service you asked for.
* **Legitimate interests** — keeping the service secure, preventing abuse, and
  maintaining and improving reliability.
* **Consent** — for push notifications, where your device (e.g. Android 13+ and
  iOS) asks for your permission. You can withdraw this at any time in your device
  settings.

---

### 4. Third-Party Services

The app itself does not include analytics, crash reporting, advertising, or
third-party sign-in. To operate, the service relies on a small number of
providers ("sub-processors"):

* **Firebase Cloud Messaging (Google)** — delivers push notifications. Google
  receives your device push token and the notification payload. Firebase
  **Analytics is explicitly disabled** in the app. See Google's privacy policy:
  [https://policies.google.com/privacy](https://policies.google.com/privacy)
* **Infomaniak** — email delivery provider that sends transactional emails
  (verification and password reset). The provider receives the recipient email
  address and message content needed to deliver the email. Infomaniak is based in
  Switzerland. See Infomaniak's privacy policy:
  [https://www.infomaniak.com/en/legal/privacy-policy](https://www.infomaniak.com/en/legal/privacy-policy)
* **Hosting** — the TabMates backend runs on first-party servers located in the
  **European Union (Germany)**.
* **App stores / Google Play in-app updates** — if you installed the app from
  Google Play or another store, that store collects data such as your account
  info and download history under its own privacy policy. On Android, the in-app
  update feature queries Google Play for the latest version. See Google Play:
  [https://policies.google.com/privacy](https://policies.google.com/privacy)
* **GitHub** — if you interact with the project's source code or issues on
  GitHub, GitHub's own privacy practices apply to your activity there.

---

### 5. Data Storage and Security

* **In transit:** all communication with the server uses encrypted HTTPS/WSS
  connections.
* **On your device:** authentication tokens are kept in the operating system's
  secure storage (Android Keystore / iOS Keychain). Your groups and expenses are
  also cached locally so the app works offline.
* **On the server:** your data is stored on servers in the EU; passwords are
  stored hashed and salted.
* We recommend enabling your device's built-in lock screen (PIN, fingerprint or
  face unlock) to protect the data cached on your device.

---

### 6. Data Retention and Deletion

* Your account and the content you create are kept for as long as your account is
  active.
* **Uninstalling the app** removes the local cache from your device, but it does
  **not** delete your data from the server.
* To delete your account and the associated personal data from the server, please
  send a request through the contact channel in Section 9. We will delete your
  personal data within a reasonable period, except where we are required to retain
  certain records by law.
* Note that expenses you added to a shared group may remain visible to other
  members as part of that group's shared history.

---

### 7. Your Rights (GDPR)

Depending on your location, you have the right to:

* **Access** the personal data we hold about you.
* **Rectify** inaccurate data (you can edit much of it directly in the app).
* **Erase** your data ("right to be forgotten").
* **Port** your data to another service.
* **Restrict** or **object** to certain processing.
* **Lodge a complaint** with your local data protection supervisory authority.

To exercise any of these rights, contact us via the channel in Section 9.

---

### 8. Children's Privacy

TabMates is not directed at children under 16, and we do not knowingly collect
personal data from them. If you believe a child has provided personal data,
please contact us so we can delete it.

---

### 9. International Users

The TabMates backend and your data are hosted in the European Union (Germany). If
you use the app from outside the EU, your data is processed on these EU servers.

---

### 10. Changes to This Policy

We may update this Privacy Policy from time to time. Material changes will be
reflected here with an updated "Last Updated" date. We recommend checking this
policy periodically within the app or on the official project repository.

---

### 11. Contact

If you have any questions about this policy or want to exercise your privacy
rights, you can reach out via the official GitHub repository:
[https://github.com/TabMates/app](https://github.com/TabMates/app)
