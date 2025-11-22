# Card Processor - Claude Code Integration

This directory contains the **prompt-based** card processing system that prepares tasks for Claude Code to analyze Anki cards and generate suggestions.

## How It Works

When a user clicks "Generate Suggestions" in the UI:

1. **Backend** creates a session folder in `database/ai-sessions/session-{timestamp}/`
2. **Backend** writes `request.json` containing the user's prompt and deck information
3. **Backend** spawns `claudeProcessor.ts` which:
   - Loads the card data from the deck cache
   - Generates a comprehensive prompt for Claude Code
   - Writes `claude-task.md` (or batched task files) to the session folder
   - Outputs instructions for invoking Claude Code
4. **Claude Code (you!)** reads the task file and:
   - Analyzes each card based on the user's request
   - Uses the Write tool to create `suggestion-{noteId}.json` files for cards needing changes
5. **Backend** file watcher detects new suggestion files as you write them
6. **Backend** streams suggestions to frontend via WebSocket in real-time
7. **Frontend** queue updates as suggestions arrive

## Architecture

This is a **prompt-based** system where:
- The backend generates prompts with structured instructions
- Claude Code (you) does the actual analysis work
- Output is written as individual JSON files
- Real-time streaming happens as files are created

## Using This System

### When a Session is Created

The backend will output instructions like this:

```
================================================================================
READY FOR CLAUDE CODE PROCESSING
================================================================================

Session: session-2025-01-22-143022
Task file: database/ai-sessions/session-2025-01-22-143022/claude-task.md

Claude Code will analyze 50 cards and write suggestion files.
================================================================================
```

### The Task File Format

Each `claude-task.md` contains:
- The user's original prompt
- Instructions on what to do
- All card data formatted for easy reading
- The exact JSON format expected for output

Example output format:
```json
{
  "noteId": 12345,
  "original": {
    "noteId": 12345,
    "modelName": "Basic",
    "fields": {
      "Front": { "value": "こんいちは", "order": 0 },
      "Back": { "value": "Hello", "order": 1 }
    },
    "tags": [],
    "cards": [1234567890],
    "mod": 1234567890
  },
  "changes": {
    "Front": "こんにちは"
  },
  "reasoning": "Fixed typo: いち → に in Japanese greeting"
}
```

## How to Process Cards with Claude Code

You (Claude Code) should:

1. **Read the task file** (`claude-task.md`) to get:
   - The user's prompt
   - The path to the deck file
   - Output format instructions

2. **Read the deck file** from the path specified (e.g., `database/decks/DeckName.json`)
   - Parse the JSON to get the `notes` array
   - Each note contains: noteId, modelName, fields, tags, cards, mod

3. **Analyze each card** according to the user's prompt
   - Iterate through all notes
   - Apply the user's request (e.g., "Fix spelling errors")
   - Identify which cards need changes

4. **For each card needing changes:**
   - Use the Write tool to create `session-{timestamp}/suggestion-{noteId}.json`
   - Include the full original card data in "original"
   - Specify only the changed fields in "changes"
   - Provide clear reasoning for the changes

5. **Skip cards** that don't need any changes
   - Don't create suggestion files for cards that are already correct

The backend is watching the session folder and will stream each suggestion to the frontend as soon as you write the file!

## Files

- `promptGenerator.ts` - Generates the prompts for Claude Code
- `claudeProcessor.ts` - Main processor that creates sessions and task files
- `README.md` - This file

## Example Session Workflow

```bash
# 1. User clicks "Generate Suggestions" in UI

# 2. Backend creates session
database/ai-sessions/session-2025-01-22-143022/
  ├── request.json (user's prompt + deck info)
  └── claude-task.md (generated prompt)

# 3. You (Claude Code) process the task
# Read: claude-task.md
# Analyze cards based on user's prompt
# Write suggestion files for cards needing changes

# 4. As you write files:
database/ai-sessions/session-2025-01-22-143022/
  ├── request.json
  ├── claude-task.md
  ├── suggestion-123.json  ← Backend detects this
  ├── suggestion-456.json  ← Streams to frontend
  └── suggestion-789.json  ← Queue updates live!

# 5. Frontend shows suggestions in real-time as they arrive
```

## Future Enhancements

- **Automated invocation**: Have backend automatically invoke Claude Code CLI
- **Slash command**: Create `/process-anki-session` command for easier invocation
- **Progress tracking**: Update a metadata file with progress
- **Resume capability**: Handle interruptions and resume processing
