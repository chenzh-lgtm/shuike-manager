#!/usr/bin/env python3
"""
batch-rename -- A command-line tool for batch renaming files using regex.

Usage examples:
  Replace spaces with underscores:
    batch-rename '\s+' '_'

  Add prefix using capture groups:
    batch-rename '(.*)' 'backup_\1' -f '*.txt'

  Recursively fix file extensions:
    batch-rename '\.jpeg$' '.jpg' -r -v
"""

import argparse
import fnmatch
import os
import re
import sys
from pathlib import Path

__version__ = "1.0.0"


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    """Parse command-line arguments and return a namespace object."""
    parser = argparse.ArgumentParser(
        prog="batch-rename",
        description="Batch rename files using regular expressions.",
        epilog=(
            "Examples:\n"
            "  Replace spaces with underscores:\n"
            "    batch-rename '\\s+' '_'\n\n"
            "  Add a prefix to all .txt files:\n"
            "    batch-rename '(.*)' 'backup_\\1' -f '*.txt'\n\n"
            "  Reorder date parts (2024-01-15 -> 15-01-2024):\n"
            "    batch-rename '(\\d{4})-(\\d{2})-(\\d{2})' '\\3-\\2-\\1' --dry-run\n\n"
            "  Recursively fix file extensions:\n"
            "    batch-rename '\\.jpeg$' '.jpg' -r -v"
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "pattern",
        type=str,
        help="Regex pattern to match against filenames",
    )
    parser.add_argument(
        "replacement",
        type=str,
        help="Replacement string (supports backreferences: \\1, \\2, etc.)",
    )
    parser.add_argument(
        "-d", "--directory",
        type=str,
        default=".",
        help="Target directory (default: current directory)",
    )
    parser.add_argument(
        "-r", "--recursive",
        action="store_true",
        help="Process subdirectories recursively",
    )
    parser.add_argument(
        "-f", "--filter",
        type=str,
        default=None,
        dest="filter_glob",
        metavar="GLOB",
        help='Only rename files matching this glob pattern (e.g. "*.txt")',
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Preview renames without making any changes",
    )
    parser.add_argument(
        "-y", "--yes",
        action="store_true",
        help="Skip confirmation prompt",
    )
    parser.add_argument(
        "-v", "--verbose",
        action="store_true",
        help="Print each rename as it occurs",
    )
    parser.add_argument(
        "-q", "--quiet",
        action="store_true",
        help="Suppress all output except errors",
    )

    return parser.parse_args(argv)


def collect_files(directory: Path, recursive: bool) -> list[Path]:
    """Collect file paths from the target directory.

    Args:
        directory: Target directory path.
        recursive: If True, recurse into subdirectories.

    Returns:
        Sorted list of file paths.

    Raises:
        SystemExit: If the directory does not exist.
    """
    directory = directory.resolve()

    if not directory.exists():
        print(f"Error: Directory '{directory}' does not exist.", file=sys.stderr)
        raise SystemExit(1)

    if not directory.is_dir():
        print(f"Error: '{directory}' is not a directory.", file=sys.stderr)
        raise SystemExit(1)

    files: list[Path] = []

    try:
        if recursive:
            for entry in directory.rglob("*"):
                try:
                    if entry.is_file():
                        files.append(entry)
                except PermissionError:
                    print(f"Warning: Permission denied accessing '{entry}'.", file=sys.stderr)
        else:
            for entry in directory.iterdir():
                try:
                    if entry.is_file():
                        files.append(entry)
                except PermissionError:
                    print(f"Warning: Permission denied accessing '{entry}'.", file=sys.stderr)
    except PermissionError:
        print(f"Error: Permission denied reading directory '{directory}'.", file=sys.stderr)
        raise SystemExit(1)

    return sorted(files)


def filter_files(paths: list[Path], glob_pattern: str | None) -> list[Path]:
    """Apply an optional glob filter to the file list.

    Args:
        paths: List of file paths.
        glob_pattern: Glob pattern to match filenames against, or None.

    Returns:
        Filtered list of paths whose filenames match the glob.
    """
    if not glob_pattern:
        return paths

    return [p for p in paths if fnmatch.fnmatch(p.name, glob_pattern)]


def compute_rename_plan(
    paths: list[Path], pattern: str, replacement: str
) -> list[tuple[Path, str]]:
    """Compute the rename plan: (original_path, new_name) for each file.

    Args:
        paths: List of file paths to consider.
        pattern: Regex pattern to match against filenames.
        replacement: Replacement string with backreference support.

    Returns:
        List of (Path, new_name) tuples for files whose name would change.

    Raises:
        SystemExit: If the regex pattern is invalid.
        ValueError: If a replacement produces an empty filename.
    """
    try:
        compiled = re.compile(pattern)
    except re.error as e:
        print(f"Error: Invalid regex pattern: {e}", file=sys.stderr)
        raise SystemExit(1)

    plan: list[tuple[Path, str]] = []

    for path in paths:
        filename = path.name
        new_name = compiled.sub(replacement, filename)

        if new_name != filename:
            if not new_name:
                raise ValueError(
                    f"Error: Replacement produced an empty filename for '{filename}'. Aborting."
                )
            # Guard against path separators in replacement (security/sanity)
            if os.sep in new_name or "/" in new_name:
                print(
                    f"Warning: Replacement contains path separator for '{filename}' "
                    f"-> '{new_name}'. Skipping.",
                    file=sys.stderr,
                )
                continue
            plan.append((path, new_name))

    return plan


def detect_collisions(
    plan: list[tuple[Path, str]], directory: Path
) -> list[str]:
    """Detect naming collisions within the rename plan and with existing files.

    Args:
        plan: The rename plan (list of (path, new_name) tuples).
        directory: The target directory (used for pre-existing name checks).

    Returns:
        List of collision warning/error messages. Empty if no collisions.
    """
    collisions: list[str] = []

    # Internal collision: two files in the plan map to the same new name
    name_map: dict[str, list[Path]] = {}
    for path, new_name in plan:
        name_map.setdefault(new_name, []).append(path)

    for new_name, originals in name_map.items():
        if len(originals) > 1:
            original_strs = [str(p.name) for p in originals]
            collisions.append(
                f"Collision: {', '.join(original_strs)} all map to '{new_name}'"
            )

    # Pre-existing name conflict: a target name already exists outside the plan
    planned_originals = {p.name for p, _ in plan}
    parent_dirs: set[Path] = set()

    for p, _ in plan:
        parent_dirs.add(p.parent)

    # Collect all existing filenames in the plan's parent directories
    # that are NOT among the files being renamed
    existing_names: set[str] = set()
    planned_parents: dict[Path, set[str]] = {}
    for p, _ in plan:
        planned_parents.setdefault(p.parent, set()).add(p.name)

    for parent in parent_dirs:
        try:
            for entry in parent.iterdir():
                if entry.is_file():
                    # Exclude files that are in the plan (being renamed away)
                    planned_in_dir = planned_parents.get(parent, set())
                    if entry.name not in planned_in_dir:
                        existing_names.add(entry.name)
        except PermissionError:
            # Can't read this directory; skip conflict check for it
            pass

    target_names = {new_name for _, new_name in plan}
    conflicts = target_names & existing_names

    for conflict in sorted(conflicts):
        collisions.append(
            f"Conflict: Target name '{conflict}' already exists and would be overwritten."
        )

    return collisions


def print_dry_run(plan: list[tuple[Path, str]], collisions: list[str]) -> None:
    """Display the planned renames without executing them.

    Args:
        plan: The rename plan.
        collisions: List of collision messages.
    """
    if not plan:
        print("No files to rename.")
        return

    print("DRY RUN -- The following renames would be performed:")
    print("-" * 60)
    # Determine max width for alignment
    max_orig_len = max(len(p.name) for p, _ in plan)
    for path, new_name in plan:
        print(f"  {path.name:<{max_orig_len}}  -->  {new_name}")
    print("-" * 60)

    if collisions:
        print("\nWARNINGS:")
        for c in collisions:
            print(f"  {c}")
        print()

    print(f"{len(plan)} file(s) would be renamed.")


def execute_renames(plan: list[tuple[Path, str]], verbose: bool = False) -> int:
    """Execute the rename plan on disk.

    Args:
        plan: The rename plan.
        verbose: If True, print each rename as it occurs.

    Returns:
        0 on full success, 2 if any errors occurred.
    """
    renamed_count = 0
    error_count = 0

    for path, new_name in plan:
        target = path.parent / new_name
        try:
            path.rename(target)
            renamed_count += 1
            if verbose:
                print(f"Renamed: {path.name} --> {new_name}")
        except PermissionError as e:
            print(f"Error: Permission denied renaming '{path.name}': {e}", file=sys.stderr)
            error_count += 1
        except FileExistsError:
            print(
                f"Error: Cannot rename '{path.name}' to '{new_name}' -- target already exists.",
                file=sys.stderr,
            )
            error_count += 1
        except OSError as e:
            print(f"Error: Failed to rename '{path.name}': {e}", file=sys.stderr)
            error_count += 1

    if not verbose and not error_count:
        pass  # Only print summary in non-quiet mode (handled in main)
    elif error_count:
        pass  # Summary handled in main

    print(f"{renamed_count} file(s) renamed, {error_count} error(s).")
    return 2 if error_count > 0 else 0


def main() -> int:
    """Orchestrate the full rename pipeline.

    Returns:
        Exit code (0 for success, 1 for user error, 2 for system error).
    """
    args = parse_args()

    directory = Path(args.directory)

    # Step 1: Collect files
    files = collect_files(directory, args.recursive)

    # Step 2: Apply glob filter
    files = filter_files(files, args.filter_glob)

    if not files:
        if not args.quiet:
            print("No files found matching the filter criteria.")
        return 0

    # Step 3: Compute rename plan
    try:
        plan = compute_rename_plan(files, args.pattern, args.replacement)
    except ValueError as e:
        print(str(e), file=sys.stderr)
        return 1

    if not plan:
        if not args.quiet:
            print("No files matched the pattern. Nothing to rename.")
        return 0

    # Step 4: Detect collisions
    collisions = detect_collisions(plan, directory)

    # Step 5: Dry-run mode
    if args.dry_run:
        print_dry_run(plan, collisions)
        return 0

    # Step 6: Handle collisions (block in live mode)
    if collisions:
        for c in collisions:
            print(c, file=sys.stderr)
        print("Aborted due to name collisions. Use --dry-run to preview.", file=sys.stderr)
        return 1

    # Step 7: Confirmation prompt
    if not args.yes:
        try:
            response = input(f"Rename {len(plan)} file(s)? [y/N]: ").strip().lower()
        except (EOFError, KeyboardInterrupt):
            print("\nAborted.")
            return 0
        if response not in ("y", "yes"):
            print("Aborted.")
            return 0

    # Step 8: Execute renames
    return execute_renames(plan, verbose=args.verbose)


if __name__ == "__main__":
    sys.exit(main())