# chordsdb — Android Lyrics & Chords Viewer

## Project Background
This is a fork of the SDB (Song Database) Viewer Android app.
It fetches a song file from a user-configured URL and displays
lyrics with guitar chords for live performance use.

## Tech Stack
- Java (Android SDK)
- SQLite FTS4 for search (DatabaseAccess.java)
- GsonXml for XML parsing (SDBFetcher.java)
- NestedScrollView in song detail layout

## Key Files
- `Song.java` — data model
- `SDBFetcher.java` — network fetch + XML parse
- `SongParser.java` — parses lyrics string into typed elements
- `DatabaseAccess.java` — SQLite FTS4 cache
- `SongDetailFragment.java` — main display fragment
- `SongDetailActivity.java` — hosts the fragment
- `activity_song_detail.xml` — layout with NestedScrollView

## Coding Conventions
- Do not use Kotlin. Stay in Java.
- Do not introduce new third-party libraries unless absolutely necessary.
- Preserve the existing fetch-from-URL and offline caching behaviour.
- Do not touch ACRA crash reporting code yet — leave it as is.
- Do not touch QR scanner code yet — leave it as is.

## Known Issues / Context
- The lyrics field uses two chord notation styles currently:
  Style A (preferred): Chords on a separate line above lyrics
  Style B (legacy):    Inline chords like (Dm)lyrics text
- The SongParser chord detection uses a >=50% spaces heuristic.
  This works for Style A only.
- CAPO is currently just free text inside the lyrics field
  e.g. "Capo 1st" as the first line. There is no dedicated field.
- Transition chord pairs like "DmEm" mean two chords played
  in quick succession and should be treated as two separate chords
  for transposition purposes.

## Planned Features (implement in this order)
### Phase 1 — Auto Scroll
Add a play button to SongDetailActivity toolbar.
When tapped, begin smooth continuous scrolling of the NestedScrollView.
Add +, -, Pause, and Stop buttons to the toolbar during scroll mode.
+ increases scroll speed, - decreases it.
Pause pauses the scroll (retaining position).
Stop cancels scroll and returns to top.
Scroll speed should be adjustable from very slow to fast
via a repeating Handler that nudges scrollY by a small pixel increment.
The speed level should persist across songs within a session.

### Phase 2 — JSON Migration
Replace the XML song format with JSON.
Replace GsonXml with plain Gson.
New JSON schema per song:
{
  "uuid": "...",
  "title": "...",
  "composer": "...",
  "language": "...",
  "publisher": "...",
  "capo": 0,
  "chordSequence": ["C", "G", "Am", "D"],
  "lyrics": "..."
}
capo is an integer (fret number). 0 means no capo.
chordSequence is a JSON array of strings, not a space-separated string.
The lyrics field format stays the same as Style A.
Update SDBFetcher to fetch a JSON array of song objects.
Update Song.java model to match new fields.
Update DatabaseAccess to handle the new schema.

### Phase 3 — Chord Transposition + CAPO
Add a transpose control in SongDetailFragment (semitone up/down buttons).
Add CAPO indicator display when song.capo > 0.
The transposition engine must handle:
- Standard chords: C, Dm, G#m, F#m, Bb, A# etc.
- Suspended chords: split root + suffix (e.g. Dsus2 → root D, suffix sus2)
- Slash chords: Am/G → transpose both sides independently
- Transition pairs: DmEm → split on capital letters, transpose each
- Enharmonic normalisation: prefer flats in minor keys, sharps in major
CAPO logic: if song.capo = N and user has no capo,
transpose all displayed chords up by N semitones.
