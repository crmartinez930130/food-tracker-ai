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
- **Frontend:** `./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest :composeApp:assembleDebug`.

## Review automático (`.github/workflows/claude-review.yml`)

Corre al abrir un PR, al reabrirlo, al sacarlo de draft y en cada push. Los PRs en draft se saltean.

- Usa `anthropics/claude-code-action` con la suscripción Claude Pro del dueño (secret `CLAUDE_CODE_OAUTH_TOKEN`). Consume la misma cuota que el uso interactivo, por eso usa Sonnet y tiene un tope de 45 turnos. El reviewer solo lee el código, no corre builds ni tests: eso lo hace la CI. Si supera el tope, la action marca el job en rojo aunque haya publicado la review.
- Revisa el diff contra los **criterios de aceptación del issue vinculado** y, si el PR trae un change de OpenSpec, contra sus specs y tasks.
- Clasifica los hallazgos en **[Bloqueante]** (criterio no cumplido, bug, faltan tests, seguridad) o **[Sugerencia]**. Solo los bloqueantes piden cambios.
- Publica una review formal: `APPROVE` si no hay bloqueantes, `REQUEST_CHANGES` si los hay.
- **Herramientas:** lee el código con Read, Grep y Glob (no con comandos de shell). Escribe el texto de la review en `.claude-review-body.md` y la publica con `gh pr review --body-file`, sin `$(...)`, heredocs ni pipes, que no coinciden con los permisos de Bash.
- **Comandos denegados:** el paso *Show denied tool calls* muestra en el log solo las llamadas que se le denegaron, con la herramienta y la entrada recortada. No se usa el modo de salida completa de la action porque puede filtrar datos sensibles.
- **Verificación:** el paso *Verify a review was published* exige que el bot haya publicado, para el commit actual, una review `APPROVED` o `CHANGES_REQUESTED`, o una `COMMENTED` con texto (la del caso `needs-human`). Las `COMMENTED` vacías que genera cada comentario en línea no cuentan. Si no hay ninguna, el job queda **en rojo** en lugar de pasar en silencio. En ese caso, revisar las denegaciones en el log y relanzar el job (`gh run rerun <id>`).
- **Límite de 3 rondas:** cuando ya pidió cambios 3 veces en el mismo PR y siguen quedando bloqueantes, deja de pedir cambios. En su lugar agrega la label `needs-human` y deja un comentario para que decida el dueño.

En un PR que agrega o modifica el workflow del reviewer, la action no revisa: se saltea por un error de validación del workflow y el job queda en verde igual, sin publicar ninguna review. Es esperado, porque la action solo confía en el workflow que ya está en `master`. El paso de verificación detecta este caso y no lo marca como error.

## Ciclo de corrección automática

En este setup (app de escritorio de Claude en Linux) no hay panel de PR, así que la app no puede vincular el PR a la sesión ni usar su monitor de auto-fix. Por eso el seguimiento lo hace la propia sesión dev:

1. Después de abrir el PR (o de cada push con correcciones), la sesión deja un **proceso en segundo plano** que consulta las reviews del PR cada 30 s hasta que el bot de Claude publique una review formal (`APPROVED` o `CHANGES_REQUESTED`). También espera a que termine la CI.
2. Cuando ese proceso termina, la sesión se despierta y lee la review y los comentarios inline (`gh api repos/<owner>/<repo>/pulls/<N>/reviews` y `.../comments`).
3. Si hay **[Bloqueante]**s, los corrige, corre los tests, hace push y vuelve al paso 1. Si está aprobado y la CI en verde, avisa al dueño para que haga el merge.

**La sesión dev tiene que seguir abierta mientras el PR esté en review.** Si se cierra, el ciclo se corta: hay que retomarlo pidiéndole a una sesión que lea la última review del PR.

Para contar solo las reviews formales del bot, se ignoran las de tipo `COMMENTED`. La herramienta de comentarios inline de la action crea una de esas vacía por cada comentario.
