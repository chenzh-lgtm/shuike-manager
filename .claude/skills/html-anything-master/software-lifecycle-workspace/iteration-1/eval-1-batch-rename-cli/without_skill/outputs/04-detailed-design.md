# Phase 4: Detailed Design

## 1. Function-Level Design

### 1.1 `parse_args(argv=None) -> argparse.Namespace`

**Purpose:** Parse command-line arguments and return a namespace object.

**Algorithm:**
1. Create `argparse.ArgumentParser` with description and epilog containing examples.
2. Add positional argument `PATTERN` (type=str, help="Regex pattern to match").
3. Add positional argument `REPLACEMENT` (type=str, help="Replacement string, supports \\1 backreferences").
4. Add `-d`/`--directory` (type=str, default=".", help="Target directory").
5. Add `-r`/`--recursive` (action="store_true", help="Recurse into subdirectories").
6. Add `-f`/`--filter` (type=str, default=None, help="Glob pattern for file filtering").
7. Add `--dry-run` (action="store_true", help="Preview renames without executing").
8. Add `-y`/`--yes` (action="store_true", help="Skip confirmation prompt").
9. Add `-v`/`--verbose` (action="store_true", help="Verbose output").
10. Add `-q`/`--quiet` (action="store_true", help="Suppress non-error output").
11. Return `parser.parse_args(argv)`.

**Edge Cases:**
- If `--verbose` and `--quiet` are both set, `--quiet` takes precedence (print nothing).

---

### 1.2 `collect_files(directory: Path, recursive: bool) -> list[Path]`

**Purpose:** Collect file paths from the target directory.

**Algorithm:**
1. Resolve `directory` to an absolute path.
2. Check if directory exists; if not, print error and raise `SystemExit(1)`.
3. If `recursive` is True:
   - Use `directory.rglob("*")` and filter to files only with `p.is_file()`.
   - Catch `PermissionError` per-subdirectory and print warning, then continue.
4. If `recursive` is False:
   - Iterate `directory.iterdir()`, keep entries where `p.is_file()`.
5. Sort the returned list for deterministic order.
6. Return `list[Path]`.

**Edge Cases:**
- Empty directory: return empty list (handled upstream with a "no files" message).
- Symlinks: `is_file()` follows symlinks, so they are included.
- Unreadable directory: catch `PermissionError`, print warning, return empty list.

---

### 1.3 `filter_files(paths: list[Path], glob_pattern: str | None) -> list[Path]`

**Purpose:** Apply optional glob filtering to the file list.

**Algorithm:**
1. If `glob_pattern` is None or empty, return `paths` unchanged.
2. Use `fnmatch.fnmatch(p.name, glob_pattern)` to test each path.
3. Return only paths whose filename matches the glob.

**Edge Cases:**
- Glob does not match any files: return empty list.
- Glob pattern is case-sensitive on Linux, case-insensitive on macOS/Windows (consistent with `fnmatch` OS behavior).

---

### 1.4 `compute_rename_plan(paths: list[Path], pattern: str, replacement: str) -> list[tuple[Path, str]]`

**Purpose:** Compute the mapping from old files to new names.

**Algorithm:**
1. Compile `pattern` with `re.compile(pattern)`. If compilation fails (raises `re.error`), catch it, print descriptive error, and `sys.exit(1)`.
2. Initialize empty result list.
3. For each path:
   a. Extract `filename = path.name`.
   b. Compute `new_name = compiled_pattern.sub(replacement, filename)`.
   c. If `new_name != filename` (i.e., substitution actually changed something):
      - Append `(path, new_name)` to result list.
   d. If `new_name == filename`: skip (no change).
4. Return result list.

**Key detail:** Use `re.sub()`, not `re.subn()`. We only care about whether the result differs from the original. The `count=0` default (replace all occurrences) is the intended behavior.

**Edge Cases:**
- Invalid regex: caught at compile time.
- Empty replacement string: legitimate use case (remove matched text).
- Pattern matches entire filename but replacement produces empty string: treat as an error (empty filename is invalid). Detect and raise `ValueError`.
- No files match: return empty list.

---

### 1.5 `detect_collisions(plan: list[tuple[Path, str]]) -> list[str]`

**Purpose:** Detect when two or more files would be renamed to the same target name.

**Algorithm:**
1. Build a `dict[str, list[Path]]` mapping `new_name -> list[Path]` (original paths that want this name).
2. Filter to entries where `len(original_paths) > 1`.
3. For each collision, construct a warning message: `"Collision: {path1} and {path2} both would become '{name}'"`.
4. Return list of all collision messages.

**Edge Cases:**
- A file is renamed to a name that already exists in the directory (but isn't in the plan): This is a naming conflict, not a collision within the plan. We will also check for this by building a set of pre-existing filenames in the directory.
- A file is renamed to its own current name: handled upstream (not included in the plan).

**Extended collision check (pre-existing names):**
After the internal collision check, also verify that no target name already exists as a file that is NOT being renamed. This prevents silent overwrites.

---

### 1.6 `print_dry_run(plan: list[tuple[Path, str]], collisions: list[str]) -> None`

**Purpose:** Display planned renames without executing them.

**Algorithm:**
1. If plan is empty: print "No files to rename." and return.
2. Print header: "DRY RUN -- The following renames would be performed:"
3. For each `(path, new_name)` in plan:
   - Print `"  {path.name}  -->  {new_name}"`
4. If collisions is non-empty:
   - Print "WARNING: Collisions detected:"
   - Print each collision message.
5. Print summary: `"{N} file(s) would be renamed."`

---

### 1.7 `execute_renames(plan: list[tuple[Path, str]], verbose: bool) -> int`

**Purpose:** Perform the actual file rename operations.

**Algorithm:**
1. Initialize `renamed_count = 0` and `error_count = 0`.
2. For each `(path, new_name)` in plan:
   a. Compute `target = path.parent / new_name`.
   b. Try `path.rename(target)`.
   c. On success: increment `renamed_count`. If `verbose`, print `"Renamed: {path.name} -> {new_name}"`.
   d. On `PermissionError`: print error to stderr, increment `error_count`.
   e. On `FileExistsError`: print error to stderr, increment `error_count`.
   f. On any other `OSError`: print error to stderr, increment `error_count`.
3. Print summary: `"{renamed_count} file(s) renamed, {error_count} error(s)."`
4. Return `2` if `error_count > 0`, else `0`.

**Edge Cases:**
- Target file unexpectedly exists (race condition): caught by `FileExistsError`.
- Filesystem full: caught by `OSError`.
- Atomicity: `path.rename()` is atomic on most filesystems (POSIX), but not guaranteed on all. Accept this limitation.

---

### 1.8 `main() -> int`

**Purpose:** Orchestrate the full pipeline.

**Algorithm:**
```
1. args = parse_args()
2. Resolve directory to absolute Path.
3. files = collect_files(directory, args.recursive)
4. files = filter_files(files, args.filter)
5. plan = compute_rename_plan(files, args.pattern, args.replacement)
6. If plan is empty: print "No files matched the pattern.", return 0.
7. Detect pre-existing name conflicts (target names that already exist outside the plan).
8. collisions = detect_collisions(plan) + pre_existing_conflicts
9. If args.dry_run:
     print_dry_run(plan, collisions)
     return 0
10. If collisions:
      print collision errors to stderr
      return 1
11. If not args.yes:
      prompt user with "Rename {N} file(s)? [y/N]: "
      if response is not 'y' or 'Y': print "Aborted.", return 0
12. return execute_renames(plan, args.verbose)
```

**SysExit integration:** The `if __name__ == "__main__"` block should call `sys.exit(main())`.

---

## 2. Pre-Existing Name Conflict Detection

This is a critical safety feature. Before renaming, we must ensure no target name already exists on disk as a file that is NOT part of the rename plan.

**Algorithm:**
1. Build a set of all parent directories from the plan (unique parent paths).
2. For each parent directory, list existing filenames.
3. Build a set `existing_names` of all current filenames in relevant directories.
4. Build a set `planned_originals` of the basenames of files in the plan (these are going away).
5. Build a set `target_names` of all new names from the plan.
6. For each target name, check if it is in `existing_names` but NOT in `planned_originals`.
7. If so, that is a conflict -- the target name would overwrite an existing file.
8. Return list of conflict messages.

---

## 3. Test Strategy

### 3.1 Test File: `test_batch_rename.py`

All tests use `unittest` and `tempfile.TemporaryDirectory` for isolated filesystem operations.

### 3.2 Test Cases

| Test ID | Test Name | Description |
|---------|-----------|-------------|
| T01 | `test_collect_files_non_recursive` | Collects only files, no subdirectories |
| T02 | `test_collect_files_recursive` | Collects files from nested subdirectories |
| T03 | `test_collect_files_empty_dir` | Returns empty list for empty directory |
| T04 | `test_collect_files_nonexistent_dir` | Raises SystemExit for missing directory |
| T05 | `test_filter_files_with_glob` | Filters by glob pattern correctly |
| T06 | `test_filter_files_no_filter` | Returns all files when filter is None |
| T07 | `test_filter_files_no_match` | Returns empty list when glob matches nothing |
| T08 | `test_compute_rename_plan_basic` | Simple substitution works |
| T09 | `test_compute_rename_plan_groups` | Backreference \1 works |
| T10 | `test_compute_rename_plan_no_match` | Returns empty plan when no files match |
| T11 | `test_compute_rename_plan_invalid_regex` | SystemExit on bad regex |
| T12 | `test_compute_rename_plan_empty_name` | ValueError on empty target name |
| T13 | `test_detect_collisions` | Detects when two files map to same name |
| T14 | `test_detect_collisions_none` | Returns empty when no collisions |
| T15 | `test_execute_renames_success` | Files are actually renamed on disk |
| T16 | `test_execute_renames_count` | Returns correct count of renamed files |
| T17 | `test_end_to_end_dry_run` | Full pipeline in dry-run produces expected output |
| T18 | `test_end_to_end_verbose` | Verbose flag produces per-file output |

---

## 4. File Output Structure

```
outputs/
  +-- 01-feasibility-analysis.md
  +-- 02-requirements-analysis.md
  +-- 03-high-level-design.md
  +-- 04-detailed-design.md       (this file)
  +-- batch_rename.py             (source code)
  +-- test_batch_rename.py        (unit tests)
```

## 5. Sample Help Output

```
usage: batch-rename [-h] [-d DIRECTORY] [-r] [-f FILTER] [--dry-run]
                    [-y] [-v] [-q]
                    PATTERN REPLACEMENT

Batch rename files using regular expressions.

Positional arguments:
  PATTERN               Regex pattern to match against filenames
  REPLACEMENT           Replacement string (supports \1, \2 backreferences)

Options:
  -h, --help            Show this help message and exit
  -d, --directory DIR   Target directory (default: current directory)
  -r, --recursive       Process subdirectories recursively
  -f, --filter GLOB     Only rename files matching this glob (e.g. "*.txt")
  --dry-run             Preview renames without making any changes
  -y, --yes             Skip confirmation prompt
  -v, --verbose         Print each rename as it occurs
  -q, --quiet           Suppress all output except errors

Examples:
  Replace spaces with underscores:
    batch-rename '\s+' '_'

  Add a prefix to all .txt files:
    batch-rename '(.*)' 'backup_\1' -f '*.txt'

  Reorder date parts in filenames (2024-01-15 -> 15-01-2024):
    batch-rename '(\d{4})-(\d{2})-(\d{2})' '\3-\2-\1' --dry-run

  Recursively fix file extensions:
    batch-rename '\.jpeg$' '.jpg' -r -v
```