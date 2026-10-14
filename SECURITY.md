# Security policy

Food Tracker is a personal project: a backend meant to run on your home network and an Android app that talks to it. The [Security section of the README](README.md#security) explains what the backend protects and how to run it safely.

## Supported versions

Only the latest commit on `master` gets fixes. There are no releases or backports.

## Reporting a vulnerability

Please **don't open a public issue**. Report it privately through GitHub instead:

1. Go to the repository's **Security** tab.
2. Click **Report a vulnerability**.
3. Describe the problem, how to reproduce it and what it affects.

You should get an answer within a week. This is a hobby project, so fixes depend on available time, but a confirmed vulnerability takes priority over other work. Once it's fixed, the report is published as a security advisory, crediting you if you want.

## Out of scope

- Running the backend exposed to the internet, or over plain HTTP on an untrusted network. The README advises against both.
- Anything that requires the access token, the Anthropic API key or physical access to an unlocked phone.
