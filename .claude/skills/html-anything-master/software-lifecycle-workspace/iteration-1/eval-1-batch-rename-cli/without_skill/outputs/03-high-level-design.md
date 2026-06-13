# Phase 3: High-Level Design

## 1. Architecture Overview

The tool follows a **single-module, pipeline-based architecture**. It reads as a CLI script that pipes data through a sequence of transformations: collect files -> filter -> match regex -> compute new name -> detect collisions -> execute rename.

```
                        +---------------------+
                        |     CLI Parser       |
                        |   (argparse)         |
                        +----------+----------+
                                   |
                        +----------v----------+
                        |   File Collector     |
                        |  (list dir / walk)   |
                        +----------+----------+
                                   |
                        +----------v----------+
                        |   File Filter        |
                        |  (glob matching)     |
                        +----------+----------+
                                   |
                        +----------v----------+
                        |   Regex Matcher      |
                        |  (re.search)         |
                        +----------+----------+
                                   |
                        +----------v----------+
                        |   Name Generator     |
                        |  (re.sub)            |
                        +----------+----------+
                                   |
                        +----------v----------+
                        | Collision Detector   |
                        +----------+----------+
                                   |
                       +-------+--------+
                       |                |
               +-------v------+  +-----v-------+
               |   Dry Run    |  |  Live Mode  |
               | (print plan) |  | (execute)   |
               +--------------+  +-------------+
```

## 2. Module Decomposition

### 2.1 Single File Structure

The entire tool lives in one file: `batch_rename.py`. This keeps distribution trivial (copy one file) and avoids the complexity of a package for a tool of this scope.

```
batch_rename.py
  |
  +-- main()                  # Entry point: parse args, orchestrate pipeline
  +-- parse_args()            # Build argparse.ArgumentParser, return Namespace
  +-- collect_files()         # Return list of file paths from target directory
  +-- filter_files()          # Apply glob filter to file list
  +-- compute_rename_plan()   # Match regex and compute old->new name mapping
  +-- detect_collisions()     # Check for duplicate target names
  +-- execute_renames()       # Perform actual os.rename() calls
  +-- print_dry_run()         # Display planned renames without executing
```

### 2.2 Data Flow

```
CLI Args
  |
  v
Namespace { pattern, replacement, directory, recursive, filter, dry_run, yes, verbose, quiet }
  |
  v
collect_files(directory, recursive) --> List[Path]
  |
  v
filter_files(paths, glob_pattern) --> List[Path]
  |
  v
compute_rename_plan(paths, pattern, replacement) --> List[(Path, str)]   # (original_path, new_name)
  |
  v
detect_collisions(plan) --> List[str]   # collision warnings / errors
  |
  v
  +---- dry_run? ----YES---> print_dry_run(plan, collisions)
  |                                |
  NO                               v
  |                           exit(0)
  v
  +---- collisions exist? ----YES---> print errors, exit(1)
  |
  NO
  |
  v
  +---- confirmation? ----NOT -y---> prompt user
  |
  v
execute_renames(plan, verbose) --> rename files on disk
  |
  v
exit(0)
```

## 3. Key Design Decisions

### 3.1 `pathlib.Path` over `os.path`
**Decision:** Use `pathlib.Path` throughout.
**Rationale:** More readable API, better cross-platform path handling, native glob support.

### 3.2 Plan-Then-Execute Pattern
**Decision:** Compute the full rename plan before touching any files.
**Rationale:** Enables collision detection, dry-run preview, and confirmation prompt. No partial state.

### 3.3 File-Scope Functions (No Class)
**Decision:** Use plain functions, not a class.
**Rationale:** The tool is a pipeline of stateless transformations. A class adds ceremony without benefit. Each function is independently testable.

### 3.4 Exit Codes
**Decision:** Follow Unix convention: 0=success, 1=user error, 2=system error.
**Rationale:** Enables scripting and chaining in shell pipelines.

## 4. Interface Specification

### 4.1 Command-Line Interface

```
usage: batch-rename [-h] [-d DIRECTORY] [-r] [-f FILTER] [--dry-run]
                    [-y] [-v] [-q]
                    PATTERN REPLACEMENT

Positional arguments:
  PATTERN               Regular expression to match against filenames
  REPLACEMENT           Replacement string (supports backreferences: \1, \2, etc.)

Optional arguments:
  -h, --help            Show this help message and exit
  -d, --directory DIR   Target directory (default: current directory)
  -r, --recursive       Process subdirectories recursively
  -f, --filter GLOB     Only consider files matching this glob pattern
  --dry-run             Show what would be renamed without making changes
  -y, --yes             Skip confirmation prompt
  -v, --verbose         Print each rename operation as it happens
  -q, --quiet           Suppress all output except errors
```

### 4.2 Functional Signatures

```python
def parse_args(argv: list[str] | None = None) -> argparse.Namespace: ...
def collect_files(directory: Path, recursive: bool) -> list[Path]: ...
def filter_files(paths: list[Path], glob_pattern: str | None) -> list[Path]: ...
def compute_rename_plan(paths: list[Path], pattern: str, replacement: str) -> list[tuple[Path, str]]: ...
def detect_collisions(plan: list[tuple[Path, str]]) -> list[str]: ...
def print_dry_run(plan: list[tuple[Path, str]], collisions: list[str]) -> None: ...
def execute_renames(plan: list[tuple[Path, str]], verbose: bool) -> int: ...
def main() -> int: ...
```

## 5. Error Handling Strategy

| Error Condition | Detection Point | Response |
|----------------|----------------|---------|
| Invalid regex | `compute_rename_plan` | Print `re.error` message, exit(1) |
| Directory not found | `collect_files` | Print error, exit(1) |
| Permission denied (read) | `collect_files` | Print warning, skip directory, continue |
| Permission denied (rename) | `execute_renames` | Print error per file, continue, exit(2) |
| Collision detected | `detect_collisions` | Print collision list, exit(1) or warn in dry-run |
| No files matched | `compute_rename_plan` | Print informational message, exit(0) |