---
name: release
description: Cut and deploy a tagged schnaq release (vX.Y.Z) for frontend and backend. Use when asked to release, ship, deploy to production, or bump the deployed version.
---

# Releasing schnaq

A release is a semver tag `vX.Y.Z` on `develop`. Frontend and backend always ship
the same version; the footer shows `Version <frontend> · API <backend>`.

| Piece | Triggered by | Result |
|---|---|---|
| Backend image | `backend.yml` on the tag push | `ghcr.io/schnaq/schnaq/backend:vX.Y.Z` |
| Frontend | `deploy-production.yml` on the tag push | Vercel production |
| Backend rollout | PR in `schnaq/charts` | ArgoCD deploys the image tag |

Every build derives its version with `git describe --tags --always`, so builds
between releases read `vX.Y.Z-N-g<sha>`. Sentry releases are
`schnaq-frontend@<version>` and `schnaq-backend@<version>`.

## Steps

1. **Check `develop`.** CI on the head commit must be green
   (`Run Tests`, `Lint Code and check dependencies`). Nothing unmerged that
   should be in the release.
2. **Pick the version.** Read the commits since the last tag
   (`git log $(git describe --tags --abbrev=0)..origin/develop --oneline`).
   Fixes only → patch, new features → minor, breaking API or data changes →
   major. State the version and the reason before tagging.
3. **Tag and push.**
   ```bash
   git fetch origin develop --tags
   git tag -a vX.Y.Z origin/develop -m "vX.Y.Z"
   git push origin vX.Y.Z
   ```
4. **Wait for both workflows** on the tag: `Docker: Build Backend Images`
   (~12 min) and `Vercel: Build Production Deployment` (~4 min). GitHub
   sometimes starts no workflow for a push; if nothing appears within a few
   minutes, start them with `workflow_dispatch` on `ref: vX.Y.Z`.
5. **Verify the image exists** before touching the charts, otherwise the pod
   ends in `ImagePullBackOff`:
   ```bash
   T=$(curl -sS "https://ghcr.io/token?scope=repository:schnaq/schnaq/backend:pull" | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
   curl -sS -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $T" \
     -H "Accept: application/vnd.oci.image.index.v1+json" \
     https://ghcr.io/v2/schnaq/schnaq/backend/manifests/vX.Y.Z   # expect 200
   ```
6. **Roll out the backend.** Open a PR in `schnaq/charts` that sets
   `image.tag: "vX.Y.Z"` in `charts/schnaqbackend/values.yaml` and
   `charts/schnaqstagingbackend/values.yaml`, then merge it. Pushing directly
   to `main` there is blocked as a production deploy; a PR is the way.
7. **Close Sentry issues** fixed in this release (projects `schnaq-backend`,
   `schnaq-frontend`), each with a comment naming the PR and `vX.Y.Z`.
8. **Report** the version, the PRs it contains, and that the footer should
   now read `Version vX.Y.Z · API vX.Y.Z` once ArgoCD has rolled out. The
   cluster is not reachable from a cloud session, so say that the rollout
   itself is unverified.

## Hotfix

Same flow with a patch version from `develop`. There are no release branches;
fix on `develop`, then tag.
