#!/usr/bin/env python3
"""
Generates an iTunes-format plist XML file from the local beets-indexed music library.

Uses a single batch ``beet ls`` query (no per-file subprocess overhead) and streams
output line-by-line so the full XML string is never accumulated in memory — safe for
the large (~17,000-track) dataset tier.

Usage
-----
    python3 generate-benchmark-xml.py <tier> <output-path>

    tier        ``medium`` or ``large``
    output-path destination XML file (created or overwritten)

Dataset mapping
---------------
    medium  ->  ~/music/Library/Compilations  (~1,200 files)
    large   ->  ~/music/Library               (~17,732 files)

Output paths for benchmark use
-------------------------------
    medium  ->  ~/software/musicott-benchmark-medium.xml
    large   ->  ~/software/musicott-benchmark-large.xml
"""

import subprocess
import sys
import urllib.parse
import datetime
import os
import re

# XML 1.0 forbids most C0 control characters in element content. Some audio tags carry
# stray control bytes (e.g. 0x03); strip everything below 0x20 except tab, LF, and CR so
# the generated plist is well-formed and the importer's XML parser does not abort.
_INVALID_XML_CHARS = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f]")

# "large" is the entire indexed library. A `path:~/music/Library` query does NOT work:
# the library root is a symlink (~/music -> /data/music) and beets canonicalizes an
# existing directory path to its real target, which then mismatches the stored
# /home/kaord/... item paths and returns zero rows. An empty query matches every item.
DATASETS = {
    "medium": "path:~/music/Library/Compilations",
    "large":  "",
}

PLIST_HEADER = """\
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple Computer//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Major Version</key><integer>1</integer>
    <key>Minor Version</key><integer>1</integer>
    <key>Application Version</key><string>12.10</string>
    <key>Date</key><date>{date}</date>
    <key>Library Persistent ID</key><string>4D55534943555041543031</string>
    <key>Tracks</key>
    <dict>
"""

PLIST_FOOTER = """\
    </dict>
    <key>Playlists</key>
    <array>
        <dict>
            <key>Name</key><string>Library</string>
            <key>Playlist Persistent ID</key><string>4D55534943554C494201</string>
            <key>Playlist Items</key>
            <array>
{track_refs}
            </array>
        </dict>
    </array>
</dict>
</plist>
"""


def beet_query(query: str) -> list[str]:
    """
    Runs a single batch ``beet ls`` subprocess with a tab-separated format template
    covering all fields needed for the iTunes plist.

    Returns the raw output lines. Never spawns more than one subprocess regardless
    of dataset size.
    """
    fmt = "$title\t$artist\t$album\t$path\t$year\t$tracknr\t$format\t$bitrate"
    # -f (item format), NOT -af: the -a flag lists one entry per album, which would
    # emit ~album-count rows instead of one row per track (the benchmark needs tracks).
    cmd = ["beet", "ls", "-f", fmt]
    # An empty query matches the entire library (the "large" tier); a non-empty query
    # is appended as a single argument.
    if query.strip():
        cmd.append(query)
    result = subprocess.run(
        cmd,
        capture_output=True,
        text=True,
        check=True,
    )
    return result.stdout.splitlines()


def parse_track_number(raw: str) -> int:
    """
    Parses a track-number value that may be in disc-relative ``"n/total"`` form.

    Splits on ``/`` and uses the first segment so both ``"3/12"`` and ``"3"``
    return ``3``.
    """
    part = raw.split("/")[0].strip()
    try:
        return int(part)
    except ValueError:
        return 0


def url_encode_path(path: str) -> str:
    """Converts a filesystem path to a ``file://`` URL with percent-encoded characters."""
    return "file://" + urllib.parse.quote(path, safe="/:")


def write_track_entry(out, track_id: int, line: str) -> None:
    """Writes a single ``<dict>`` track entry to *out*, streaming directly to disk."""
    fields = line.split("\t")
    if len(fields) < 8:
        return

    title, artist, album, path, year_raw, tracknr_raw, fmt, bitrate_raw = fields[:8]

    try:
        year = int(year_raw.strip()) if year_raw.strip() else 0
    except ValueError:
        year = 0

    track_number = parse_track_number(tracknr_raw)

    try:
        bitrate = int(bitrate_raw.strip()) if bitrate_raw.strip() else 0
    except ValueError:
        bitrate = 0

    location = url_encode_path(path.strip())
    persistent_id = format(track_id, "016X")

    out.write(f"        <key>{track_id}</key>\n")
    out.write("        <dict>\n")
    out.write(f"            <key>Track ID</key><integer>{track_id}</integer>\n")
    out.write(f"            <key>Persistent ID</key><string>{persistent_id}</string>\n")
    out.write(f"            <key>Name</key><string>{_escape(title)}</string>\n")
    out.write(f"            <key>Artist</key><string>{_escape(artist)}</string>\n")
    out.write(f"            <key>Album Artist</key><string>{_escape(artist)}</string>\n")
    out.write(f"            <key>Album</key><string>{_escape(album)}</string>\n")
    if year:
        out.write(f"            <key>Year</key><integer>{year}</integer>\n")
    if track_number:
        out.write(f"            <key>Track Number</key><integer>{track_number}</integer>\n")
    out.write(f"            <key>Total Time</key><integer>300000</integer>\n")
    if bitrate:
        out.write(f"            <key>Bit Rate</key><integer>{bitrate}</integer>\n")
    out.write(f"            <key>Play Count</key><integer>0</integer>\n")
    out.write(f"            <key>Date Added</key><date>2024-01-15T12:00:00Z</date>\n")
    out.write(f"            <key>Location</key><string>{location}</string>\n")
    out.write("        </dict>\n")


def _escape(text: str) -> str:
    """Strips XML-invalid control characters and escapes XML special characters."""
    text = _INVALID_XML_CHARS.sub("", text)
    return (
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace('"', "&quot;")
    )


def generate(tier: str, output_path: str) -> int:
    """
    Generates the iTunes plist XML for the given *tier* and writes it to *output_path*.

    Returns the number of tracks emitted.
    """
    if tier not in DATASETS:
        raise ValueError(f"Unknown tier '{tier}'. Valid values: {list(DATASETS.keys())}")

    query = DATASETS[tier]
    # Expand ~ in the beet query to the real home directory so beet resolves it
    query = query.replace("~", os.path.expanduser("~"))

    lines = beet_query(query)
    # Filter out blank lines
    lines = [ln for ln in lines if ln.strip()]

    now = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")

    with open(output_path, "w", encoding="utf-8") as out:
        out.write(PLIST_HEADER.format(date=now))

        for track_id, line in enumerate(lines, 1):
            write_track_entry(out, track_id, line)

        # Build track ID references for the Library playlist
        track_refs = "\n".join(
            f"                <dict><key>Track ID</key><integer>{i}</integer></dict>"
            for i in range(1, len(lines) + 1)
        )
        out.write(PLIST_FOOTER.format(track_refs=track_refs))

    return len(lines)


def main() -> None:
    if len(sys.argv) != 3:
        print(f"Usage: {sys.argv[0]} <tier> <output-path>", file=sys.stderr)
        print(f"  tier: {list(DATASETS.keys())}", file=sys.stderr)
        sys.exit(1)

    tier = sys.argv[1]
    output_path = sys.argv[2]

    count = generate(tier, output_path)
    print(f"Emitted {count} tracks to {output_path}")


if __name__ == "__main__":
    main()
