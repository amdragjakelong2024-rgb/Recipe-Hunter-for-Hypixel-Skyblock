# Publishing policy — Recipe Hunter

Public target version: **0.4.1** (Minecraft 26.1.2, Fabric, Java 25).

**Release freeze:** The GitHub Actions build workflow is build/test-only. It may upload temporary CI artifacts but MUST NOT publish or overwrite a GitHub Release, move a release tag, or publish to Modrinth on pushes to `main` or a Work branch. Do not restore the old automatic `gh release upload --clobber` step.

Work on the Recipe Book UI and material-unit/progress corrections separately. Publishing is allowed only after the owner explicitly approves the tested final 0.4.1 build. Until then the public 0.4.1 release is an existing, potentially defective build and should not be described as verified.

Release cleanup: GitHub has older tags/releases including 0.4.2 and 0.4.3, but 0.4.1 is intentionally designated Latest. The owner wishes to remove confusing/duplicate newer-numbered releases and consolidate work under 0.4.1. Clean up those historical releases only through an authorized release-management workflow, after verifying assets and retaining a recoverable source commit; do not delete development history.

No Modrinth or Hypixel approval is claimed. Preserve project and NEU-data licences. ChatGPT Plus billing is separate from API charges. AI requests require consent and privately stored credentials; no telemetry or automatic paid generation.
