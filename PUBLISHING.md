# Publishing policy — Recipe Hunter

Current public target: **0.4.4** (Minecraft 26.1.2, Fabric, Java 25).

The owner explicitly authorized the 0.4.4 public GitHub release on 2026-10-31. The release is based on the successfully tested source from the 0.4.3 Test Build 2 candidate, with the Gradle project version and Fabric metadata updated consistently to 0.4.4.

The regular `.github/workflows/build.yml` remains build/test-only and never publishes releases. The dedicated `.github/workflows/release.yml` is restricted to the exact `release/0.4.4` branch and the one-time `Release Recipe Hunter v0.4.4` commit. It rebuilds the tagged release candidate, runs catalogue, regression and native UI checks, then publishes the JAR and SHA-256 checksum using the repository's GitHub Actions token.

Do not publish to Modrinth or other distribution platforms without separate explicit owner approval. Preserve project and NEU-data licences. No authenticated Hypixel API, user session tokens, or paid AI requests are used by this release.
