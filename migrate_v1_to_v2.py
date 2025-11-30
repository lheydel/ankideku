#!/usr/bin/env python3
"""
One-time migration script: V1 file-based database → V2 SQLite database

V1 Structure (file-based):
  database/
  ├── settings.json
  ├── decks/*.json
  └── ai-sessions/session-*/
      ├── request.json, state.json, history.json
      └── suggestions/*.json

V2 Structure (SQLite - tables already exist):
  Tables: setting, deck_cache, cached_note, field_value, session, suggestion, history_entry
"""

import json
import sqlite3
import os
from pathlib import Path
from datetime import datetime
from typing import Any

# V1 file-based database (in project directory)
V1_DATABASE_DIR = Path("database")
SETTINGS_FILE = V1_DATABASE_DIR / "settings.json"
DECKS_DIR = V1_DATABASE_DIR / "decks"
AI_SESSIONS_DIR = V1_DATABASE_DIR / "ai-sessions"


def get_v2_database_path() -> Path:
    """Get the platform-specific V2 database path (matches Kotlin getDataPath())."""
    import platform
    home = Path.home()
    system = platform.system().lower()

    if "windows" in system:
        data_dir = home / "AppData" / "Roaming" / "AnkiDeku"
    elif "darwin" in system:  # macOS
        data_dir = home / "Library" / "Application Support" / "AnkiDeku"
    else:  # Linux and others
        data_dir = home / ".config" / "AnkiDeku"

    return data_dir / "ankideku.db"


V2_DATABASE_FILE = get_v2_database_path()

# Track old session ID -> new session ID mapping
session_id_map: dict[str, int] = {}


def parse_timestamp(ts: str | int | None) -> int:
    """Convert various timestamp formats to epoch milliseconds."""
    if ts is None:
        return int(datetime.now().timestamp() * 1000)
    if isinstance(ts, int):
        # If it's already a number, check if it's seconds or milliseconds
        if ts < 10_000_000_000:  # Likely seconds (before year 2286)
            return ts * 1000
        return ts
    if isinstance(ts, str):
        try:
            dt = datetime.fromisoformat(ts.replace("Z", "+00:00"))
            return int(dt.timestamp() * 1000)
        except ValueError:
            return int(datetime.now().timestamp() * 1000)
    return int(datetime.now().timestamp() * 1000)


def read_json_file(path: Path) -> dict | list | None:
    """Read and parse a JSON file, returning None if it doesn't exist or fails."""
    try:
        if path.exists():
            with open(path, "r", encoding="utf-8") as f:
                return json.load(f)
    except (json.JSONDecodeError, IOError) as e:
        print(f"  Warning: Could not read {path}: {e}")
    return None


def migrate_settings(conn: sqlite3.Connection) -> int:
    """Migrate settings.json to setting table."""
    print("\n[1/4] Migrating settings...")

    settings = read_json_file(SETTINGS_FILE)
    if not settings:
        print("  No settings.json found, skipping.")
        return 0

    cursor = conn.cursor()
    now = int(datetime.now().timestamp() * 1000)
    count = 0

    for key, value in settings.items():
        cursor.execute(
            """INSERT INTO setting (key, value, updated_at) VALUES (?, ?, ?)
               ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at""",
            (key, json.dumps(value), now)
        )
        count += 1
        print(f"  Migrated setting: {key}")

    conn.commit()
    print(f"  Done: {count} settings migrated.")
    return count


def migrate_deck_caches(conn: sqlite3.Connection) -> tuple[int, int]:
    """Migrate decks/*.json to deck_cache, cached_note, and field_value tables."""
    print("\n[2/4] Migrating deck caches...")

    if not DECKS_DIR.exists():
        print("  No decks directory found, skipping.")
        return 0, 0

    cursor = conn.cursor()
    deck_count = 0
    note_count = 0
    now = int(datetime.now().timestamp() * 1000)

    for deck_file in DECKS_DIR.glob("*.json"):
        data = read_json_file(deck_file)
        if not data:
            continue

        deck_name = data.get("deckName", deck_file.stem)
        notes = data.get("notes", [])
        last_sync = data.get("lastSyncTimestamp")

        # We need an anki_id for the deck. Use a hash of the deck name as fallback.
        # In v1, decks don't have explicit IDs, so we generate one.
        anki_id = abs(hash(deck_name)) % (2**31)

        # Insert deck cache
        cursor.execute(
            """INSERT INTO deck_cache (anki_id, name, last_sync_timestamp, created_at, updated_at)
               VALUES (?, ?, ?, ?, ?)
               ON CONFLICT(anki_id) DO UPDATE SET
                   name = excluded.name,
                   last_sync_timestamp = excluded.last_sync_timestamp,
                   updated_at = excluded.updated_at""",
            (anki_id, deck_name, last_sync, now, now)
        )
        deck_count += 1

        # Insert notes and their fields
        for note in notes:
            note_id = note.get("noteId")
            if not note_id:
                continue

            note_deck_name = note.get("deckName", deck_name)

            # Get deck_id for this note's deck (may differ from parent deck)
            note_deck_id = abs(hash(note_deck_name)) % (2**31)

            # Estimate tokens from fields
            fields = note.get("fields", {})
            estimated_tokens = sum(
                len(str(f.get("value", ""))) // 4
                for f in fields.values()
            ) if fields else None

            cursor.execute(
                """INSERT INTO cached_note (id, deck_id, deck_name, model_name, tags, mod, estimated_tokens, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                   ON CONFLICT(id) DO UPDATE SET
                       deck_id = excluded.deck_id,
                       deck_name = excluded.deck_name,
                       model_name = excluded.model_name,
                       tags = excluded.tags,
                       mod = excluded.mod,
                       estimated_tokens = excluded.estimated_tokens,
                       updated_at = excluded.updated_at""",
                (
                    note_id,
                    note_deck_id,
                    note_deck_name,
                    note.get("modelName", ""),
                    json.dumps(note.get("tags", [])),
                    note.get("mod", 0),
                    estimated_tokens,
                    now,
                    now
                )
            )

            # Insert fields into field_value table
            for field_name, field_data in fields.items():
                field_value = field_data.get("value", "") if isinstance(field_data, dict) else str(field_data)
                field_order = field_data.get("order", 0) if isinstance(field_data, dict) else 0

                cursor.execute(
                    """INSERT INTO field_value (note_id, suggestion_id, history_id, context, field_name, field_value, field_order)
                       VALUES (?, NULL, NULL, 'current', ?, ?, ?)""",
                    (note_id, field_name, field_value, field_order)
                )

            note_count += 1

        print(f"  Migrated deck: {deck_name} ({len(notes)} notes)")

    conn.commit()
    print(f"  Done: {deck_count} decks, {note_count} notes migrated.")
    return deck_count, note_count


def migrate_sessions(conn: sqlite3.Connection) -> int:
    """Migrate ai-sessions/*/request.json and state.json to session table."""
    print("\n[3/4] Migrating sessions...")

    if not AI_SESSIONS_DIR.exists():
        print("  No ai-sessions directory found, skipping.")
        return 0

    cursor = conn.cursor()
    count = 0

    for session_dir in sorted(AI_SESSIONS_DIR.iterdir()):
        if not session_dir.is_dir():
            continue

        request = read_json_file(session_dir / "request.json")
        state = read_json_file(session_dir / "state.json")

        if not request:
            continue

        old_session_id = request.get("sessionId", session_dir.name)
        deck_name = request.get("deckName", "")
        deck_id = abs(hash(deck_name)) % (2**31) if deck_name else 0

        # Map v1 state to v2 state
        v1_state = state.get("state", "completed") if state else "unknown"
        state_map = {
            "pending": "pending",
            "running": "running",
            "completed": "completed",
            "failed": "failed",
            "cancelled": "cancelled",
            "unknown": "incomplete"
        }
        v2_state = state_map.get(v1_state, "incomplete")

        created_at = parse_timestamp(request.get("timestamp"))
        updated_at = parse_timestamp(state.get("timestamp") if state else None)

        cursor.execute(
            """INSERT INTO session
               (deck_id, deck_name, prompt, state, state_message, exit_code,
                progress_processed_cards, progress_total_cards,
                progress_processed_batches, progress_total_batches,
                progress_suggestions_count, progress_input_tokens, progress_output_tokens,
                progress_failed_batches, created_at, updated_at)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            (
                deck_id,
                deck_name,
                request.get("prompt", ""),
                v2_state,
                state.get("message") if state else None,
                state.get("exitCode") if state else None,
                request.get("totalCards", 0),  # Use totalCards as processed (completed sessions)
                request.get("totalCards", 0),
                0, 0, 0, 0, 0, 0,  # batch/token progress not tracked in v1
                created_at,
                updated_at
            )
        )

        # Map old string ID to new integer ID
        new_session_id = cursor.lastrowid
        session_id_map[old_session_id] = new_session_id

        count += 1
        print(f"  Migrated session: {old_session_id} -> ID {new_session_id}")

    conn.commit()
    print(f"  Done: {count} sessions migrated.")
    return count


def migrate_suggestions_and_history(conn: sqlite3.Connection) -> tuple[int, int]:
    """Migrate suggestions/*.json and history.json from each session."""
    print("\n[4/4] Migrating suggestions and history...")

    if not AI_SESSIONS_DIR.exists():
        print("  No ai-sessions directory found, skipping.")
        return 0, 0

    cursor = conn.cursor()
    suggestion_count = 0
    history_count = 0
    now = int(datetime.now().timestamp() * 1000)

    for session_dir in AI_SESSIONS_DIR.iterdir():
        if not session_dir.is_dir():
            continue

        old_session_id = session_dir.name
        new_session_id = session_id_map.get(old_session_id)

        if not new_session_id:
            print(f"  Warning: No session mapping for {old_session_id}, skipping")
            continue

        # Migrate suggestions
        suggestions_dir = session_dir / "suggestions"
        if suggestions_dir.exists():
            for suggestion_file in suggestions_dir.glob("*.json"):
                suggestion = read_json_file(suggestion_file)
                if not suggestion:
                    continue

                note_id = suggestion.get("noteId")
                if not note_id:
                    continue

                # Map v1 accepted (bool/null) to v2 status (string)
                accepted = suggestion.get("accepted")
                if accepted is True:
                    status = "accepted"
                elif accepted is False:
                    status = "rejected"
                else:
                    status = "pending"

                try:
                    cursor.execute(
                        """INSERT INTO suggestion (session_id, note_id, reasoning, status, created_at, decided_at)
                           VALUES (?, ?, ?, ?, ?, ?)""",
                        (
                            new_session_id,
                            note_id,
                            suggestion.get("reasoning", ""),
                            status,
                            now,
                            now if status != "pending" else None
                        )
                    )
                    suggestion_id = cursor.lastrowid

                    # Insert original fields into field_value
                    original = suggestion.get("original", {})
                    original_fields = original.get("fields", {})
                    for field_name, field_data in original_fields.items():
                        field_value = field_data.get("value", "") if isinstance(field_data, dict) else str(field_data)
                        field_order = field_data.get("order", 0) if isinstance(field_data, dict) else 0
                        cursor.execute(
                            """INSERT INTO field_value (note_id, suggestion_id, history_id, context, field_name, field_value, field_order)
                               VALUES (NULL, ?, NULL, 'original', ?, ?, ?)""",
                            (suggestion_id, field_name, field_value, field_order)
                        )

                    # Insert changes into field_value
                    changes = suggestion.get("changes", {})
                    order = 0
                    for field_name, field_value in changes.items():
                        cursor.execute(
                            """INSERT INTO field_value (note_id, suggestion_id, history_id, context, field_name, field_value, field_order)
                               VALUES (NULL, ?, NULL, 'changes', ?, ?, ?)""",
                            (suggestion_id, field_name, str(field_value), order)
                        )
                        order += 1

                    suggestion_count += 1

                except sqlite3.IntegrityError as e:
                    print(f"  Warning: Duplicate suggestion for note {note_id}: {e}")

        # Migrate history
        history = read_json_file(session_dir / "history.json")
        if history and isinstance(history, list):
            for entry in history:
                note_id = entry.get("noteId")
                deck_name = entry.get("deckName", "")
                deck_id = abs(hash(deck_name)) % (2**31) if deck_name else 0

                cursor.execute(
                    """INSERT INTO history_entry (session_id, note_id, deck_id, deck_name, action, reasoning, timestamp)
                       VALUES (?, ?, ?, ?, ?, ?, ?)""",
                    (
                        new_session_id,
                        note_id,
                        deck_id,
                        deck_name,
                        entry.get("action", "unknown"),
                        entry.get("reasoning"),
                        parse_timestamp(entry.get("timestamp"))
                    )
                )
                history_id = cursor.lastrowid

                # Insert original fields
                original = entry.get("original", {})
                order = 0
                for field_name, field_value in original.items():
                    cursor.execute(
                        """INSERT INTO field_value (note_id, suggestion_id, history_id, context, field_name, field_value, field_order)
                           VALUES (NULL, NULL, ?, 'original', ?, ?, ?)""",
                        (history_id, field_name, str(field_value), order)
                    )
                    order += 1

                # Insert ai_changes
                changes = entry.get("changes", {})
                order = 0
                for field_name, field_value in changes.items():
                    cursor.execute(
                        """INSERT INTO field_value (note_id, suggestion_id, history_id, context, field_name, field_value, field_order)
                           VALUES (NULL, NULL, ?, 'ai_changes', ?, ?, ?)""",
                        (history_id, field_name, str(field_value), order)
                    )
                    order += 1

                # Insert applied_changes if present
                applied = entry.get("appliedChanges")
                if applied:
                    order = 0
                    for field_name, field_value in applied.items():
                        cursor.execute(
                            """INSERT INTO field_value (note_id, suggestion_id, history_id, context, field_name, field_value, field_order)
                               VALUES (NULL, NULL, ?, 'applied', ?, ?, ?)""",
                            (history_id, field_name, str(field_value), order)
                        )
                        order += 1

                # Insert user_edits if present
                user_edits = entry.get("userEdits")
                if user_edits:
                    order = 0
                    for field_name, field_value in user_edits.items():
                        cursor.execute(
                            """INSERT INTO field_value (note_id, suggestion_id, history_id, context, field_name, field_value, field_order)
                               VALUES (NULL, NULL, ?, 'user_edits', ?, ?, ?)""",
                            (history_id, field_name, str(field_value), order)
                        )
                        order += 1

                history_count += 1

    conn.commit()
    print(f"  Done: {suggestion_count} suggestions, {history_count} history entries migrated.")
    return suggestion_count, history_count


def main():
    print("=" * 60)
    print("AnkiDeku Database Migration: V1 (files) → V2 (SQLite)")
    print("=" * 60)

    # Check if V1 data exists
    if not V1_DATABASE_DIR.exists():
        print(f"\nError: V1 database directory not found: {V1_DATABASE_DIR}")
        return 1

    # Check if V2 database exists (required - tables should be initialized)
    if not V2_DATABASE_FILE.exists():
        print(f"\nError: V2 database not found: {V2_DATABASE_FILE}")
        print("Please run the application first to initialize the database.")
        return 1

    print(f"\nUsing existing database: {V2_DATABASE_FILE}")

    # Connect to V2 database
    conn = sqlite3.connect(V2_DATABASE_FILE)
    conn.execute("PRAGMA foreign_keys = ON")

    # Run migrations
    settings_count = migrate_settings(conn)
    deck_count, note_count = migrate_deck_caches(conn)
    session_count = migrate_sessions(conn)
    suggestion_count, history_count = migrate_suggestions_and_history(conn)

    conn.close()

    # Summary
    print("\n" + "=" * 60)
    print("Migration Complete!")
    print("=" * 60)
    print(f"  Settings:    {settings_count}")
    print(f"  Decks:       {deck_count}")
    print(f"  Notes:       {note_count}")
    print(f"  Sessions:    {session_count}")
    print(f"  Suggestions: {suggestion_count}")
    print(f"  History:     {history_count}")
    print(f"\nData migrated to: {V2_DATABASE_FILE}")
    print("\nYou can now safely delete/archive the old V1 files:")
    print(f"  - {V1_DATABASE_DIR}/  (entire directory)")

    return 0


if __name__ == "__main__":
    exit(main())
