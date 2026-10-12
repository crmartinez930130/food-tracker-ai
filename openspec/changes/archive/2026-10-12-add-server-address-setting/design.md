## Context

- `FoodTrackerApiClient` reads `BackendConfig.baseUrl` lazily on every call, so changing that variable affects the very next request. Nothing sets it today; it starts at `defaultBackendBaseUrl()` (`http://10.0.2.2:3000` on Android).
- Options preferences (`AppPreferences`) are JSON in `SharedPreferences`, owned by `MainActivity` (#34). The reminder receiver runs in a possibly fresh process and builds its own client.
- The backend binds `0.0.0.0:3000`, and the manifest allows cleartext HTTP.

## Goals / Non-Goals

**Goals:**
- One source of truth for the address, applied everywhere, including from the receiver.
- A wrong address is caught before saving (format) and diagnosable after saving (connection test).

**Non-Goals:**
- Automatic discovery (mDNS or scanning).
- Auth or TLS setup.
- Different addresses per network.
- iOS.

## Decisions

### D1. Stored with the Options preferences
- `AppPreferences.serverUrl: String? = null`. Null means "use the platform default", so a later change to the default reaches users who never customized it.
- `effectiveServerUrl(prefs) = prefs.serverUrl ?: defaultBackendBaseUrl()`.
- Older stored JSON without the field decodes to null, because the decoder already ignores unknown keys and fills defaults.

### D2. Pure normalization and validation
`normalizeServerUrl(input): String?`:
- trims the input;
- requires an `http://` or `https://` prefix, case-insensitive, and lowercases the scheme;
- requires a non-empty host made of letters, digits, `.`, `-` or `_`, or an IPv4 address;
- accepts an optional `:port` in 1..65535;
- accepts no path, query or fragment, except a single trailing `/`, which is dropped.

It returns the canonical form (`http://192.168.1.50:3000`) or null. Unit-tested for each rule.
- **Alternative:** accept anything and let requests fail. Rejected: a typo would only show up as confusing errors on every screen.

### D3. Applying the address
- **`MainActivity`:** sets `BackendConfig.baseUrl = effectiveServerUrl(preferences)` before `setContent`, and again in `onPreferencesChange`.
- **The receiver:** builds `FoodTrackerApiClient(httpClient) { effectiveServerUrl(PreferencesStore.load(context)) }`. It never relies on the process-global value, which a fresh process wouldn't have set.

### D4. Options UI
- **Section "Server":**
  - an `OutlinedTextField` holding the effective address, with a URL keyboard;
  - buttons "Save", "Test connection" and "Restore default".
- **Save:** validates with D2. On failure it shows "Enter an address like http://192.168.1.50:3000". On success it stores the canonical form and shows "Saved".
- **Test connection:** calls `apiClient.getGoals()` against the address typed in the field (validated first), not necessarily the saved one, under `withTimeoutOrNull(5 s)`. It shows "Connected" or "Couldn't connect: <detail>".
  - The client is built per test with a `baseUrlProvider` pointing at that address. Testing a candidate address is the useful case before saving.
- **Restore default:** stores null and shows the default in the field.
- `SettingsScreen` builds this test client itself, with `createHttpClient()` and the candidate URL, and closes it after the test.

## Risks / Trade-offs

- **The PC's LAN IP changes, for example after a DHCP lease change.** → The user updates it in Options, and "Test connection" makes the cause obvious. A DHCP reservation on the router is the user's long-term fix.
- **Cleartext HTTP over the LAN.** → This matches the current single-user, home-network design, and the manifest already allows it.
- **The firewall on the PC blocks port 3000.** → "Test connection" surfaces it. It is out of scope for the app.

## Migration Plan

None. A missing field means the default address, which is today's behavior.
