# Flujo de trabajo

Cada issue se entrega como un Pull Request. El código lo escribe una sesión de Claude, lo revisa otro agente de Claude (en GitHub Actions) y el merge lo hace el dueño del repo.

```
Issue #N → sesión dev → implementa + tests + verificación en emulador → abre PR "Closes #N"
        → CI + Claude reviewer
        → ¿cambios pedidos? → la sesión dev corrige y hace push → CI + reviewer de nuevo → …
        → aprobado + CI verde → merge manual (squash)
```

## Branches y PRs

- **Branch:** `issue-<N>-<slug>` (ej. `issue-2-daily-goals`), creada desde `master`.
- **Título del PR:** en castellano y descriptivo.
- **Body del PR:**
  - `Closes #<N>`: el reviewer lee el issue vinculado y valida sus criterios de aceptación.
  - Resumen de los cambios.
  - Cómo se verificó: tests que se agregaron o corrieron y verificación en el emulador.
- **Verificación en emulador:** la hace la sesión dev **antes** de abrir el PR. No corre en CI.
- **Merge:** squash, siempre manual, lo hace el dueño.

## CI (`.github/workflows/ci.yml`)

Corre en cada PR hacia `master` y en cada push a `master`:

- **Backend:** `npm ci`, `npm run typecheck`, `npm test` (no necesita secrets: los tests usan un cliente de Anthropic falso).
- **Frontend:** `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug`.

## Review automático (`.github/workflows/claude-review.yml`)

Corre al abrir un PR, al reabrirlo, al sacarlo de draft y en cada push. Los PRs en draft se saltean.

- Usa `anthropics/claude-code-action` con la suscripción Claude Pro del dueño (secret `CLAUDE_CODE_OAUTH_TOKEN`). Consume la misma cuota que el uso interactivo, por eso usa Sonnet y tiene tope de turnos.
- Revisa el diff contra los **criterios de aceptación del issue vinculado** y, si el PR trae un change de OpenSpec, contra sus specs y tasks.
- Clasifica los hallazgos en **[Bloqueante]** (criterio no cumplido, bug, faltan tests, seguridad) o **[Sugerencia]**. Solo los bloqueantes piden cambios.
- Publica una review formal: `APPROVE` si no hay bloqueantes, `REQUEST_CHANGES` si los hay.
- **Límite de 3 rondas:** cuando ya pidió cambios 3 veces en el mismo PR y siguen quedando bloqueantes, deja de pedir cambios. En su lugar agrega la label `needs-human` y deja un comentario para que decida el dueño.

La primera vez que se agrega o se modifica el workflow del reviewer, el review de ese mismo PR falla con un error de validación del workflow. Es esperado: la action solo confía en el workflow que ya está en `master`.

## Ciclo de corrección automática

La sesión dev de la app de escritorio de Claude tiene que tener activado el **monitor de PR con auto-fix**. Con eso, la app la despierta cuando llegan comentarios de review o falla la CI, y la sesión corrige y vuelve a subir.
