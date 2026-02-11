# Claude Code Orchestrator – Start-Anleitung

## Vorbereitung

1. **Projekt-Setup:**
   ```bash
   cd work-time-tracker
   ```

2. **Agent-Definitionen laden:**
   - Developer Agent: `~/.claude/agents/developer.md`
   - Reviewer Agent: `~/.claude/agents/reviewer.md`
   
   (Falls noch nicht vorhanden: Die Dateien aus diesem Bundle kopieren)

3. **Projektkontext prüfen:**
   - `CLAUDE.md` → Architektur, Konventionen, Befehle
   - `docs/DEPENDENCIES.md` → Abhängigkeitsgraph
   - `docs/features/F*.md` → Feature-Spezifikationen

## Orchestrierung Starten

```bash
claude

> Initialisierung:
>
> 1. Lies docs/DEPENDENCIES.md
> 2. Erstelle einen Task pro Feature mit den Abhängigkeiten aus TASKS.md
> 3. Markiere F01 als READY zum Starten
>
> 4. Starte die Feature-Pipeline:
>    - Für jedes freigeschaltete Feature:
>      a) @developer aufrufen (Spec: docs/features/{feature_id}.md)
>      b) @reviewer aufrufen
>      c) Review-Loop max. 5 Iterationen
>    - Nach Abschluss: APK bauen (builds/v{VERSION}.apk)
>    - Task als DONE markieren
>    - Abhängige Tasks automatisch freischalten
>
> 5. Parallelisieren wo möglich (siehe TASKS.md / Wellen)
```

## Während der Laufzeit

### Status prüfen:
```bash
> Zeige aktuellen Task-Status und nächste freigeschalteten Features
```

### Weitermachen nach Pause:
```bash
> Lies den aktuellen Task-Status aus ~/.claude/tasks/
> Setze die Implementierung fort (nächstens freigeschaltete Features)
```

### Bei Fehlern:
```bash
> Analyisiere die Build-/Test-Fehler und starte @developer erneut
```

## Dokumentation während der Pipeline

Alle Findings, Responses und Testergebnisse werden direkt in den Feature-Dateien dokumentiert:

```
docs/features/
├── F01-project-setup.md
│   ├── [Original-Spec]
│   ├── ## Implementierungszusammenfassung
│   ├── ## Review Findings – Iteration 1
│   └── ## Developer Response – Iteration 2
├── F02-lokale-datenbank.md
│   └── [...]
└── ...
```

## Output pro Feature

Nach jedem abgeschlossenen Feature:

```
✅ {feature_id} abgeschlossen
Version: v{VERSION}
Review-Iterationen: {N}
Tests: {passed}/{total} grün
Nächste Features: [F0X, F0Y, ...] (automatisch freigeschaltet)
Fortschritt: {done}/{total} ({prozent}%)
```

## Git-Workflow

Pro abgeschlossenem Feature:

```bash
git add -A
git commit -m "feat: {feature_id} – {Feature-Name}"
git tag v{VERSION}
```

Bei parallelen Features: Nach jedem Abschluss committen, bevor nächster Agent startet.

## Versionierungsschema

```
v0.1.0 – v0.7.0    Schrittweise Implementierung (16 Features)
v1.0.0             MVP – Integrationstests, Polish
```

APKs werden in `builds/` abgelegt:
```
builds/
├── v0.1.0.apk
├── v0.2.0.apk
└── ...
```

## Abschluss

Nach v1.0.0:
- Alle 16 Features vollständig und approved
- Alle Tests grün
- APK bereit für Release
