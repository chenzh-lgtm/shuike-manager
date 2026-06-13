# Phase 2: Requirements Analysis

## 1. Functional Requirements

### FR-1: Regex Pattern Matching
- The tool SHALL accept a regex pattern as a required argument.
- The pattern SHALL be applied to filenames (not full paths) within a target directory.
- Only files whose names match the pattern SHALL be candidates for renaming.

### FR-2: Regex Replacement
- The tool SHALL accept a replacement string as a required argument.
- The replacement SHALL support backreferences (`\1`, `\2`, `\g<name>`) referencing capture groups in the pattern.
- The replacement SHALL support standard regex substitution semantics (as implemented by `re.sub`).

### FR-3: Target Directory Specification
- The tool SHALL accept a target directory path as an optional argument.
- If no directory is specified, the tool SHALL default to the current working directory.

### FR-4: Dry-Run Mode
- The tool SHALL support a `--dry-run` flag.
- In dry-run mode, the tool SHALL display all renames that WOULD occur without actually renaming any files.
- The dry-run output SHALL show the original filename and the new filename for each affected file.

### FR-5: Recursive Mode
- The tool SHALL support a `--recursive` (or `-r`) flag.
- In recursive mode, the tool SHALL apply the rename operation to all files in the target directory and all its subdirectories.
- In non-recursive mode (default), the tool SHALL only process files directly in the target directory (no subdirectories).

### FR-6: File Filtering
- The tool SHALL support a `--filter` option to specify a glob pattern for selecting which files to consider.
- Only files matching the glob SHALL be evaluated against the regex pattern.
- Default behavior (no filter) SHALL consider all files.

### FR-7: Confirmation Prompt
- The tool SHALL prompt the user for confirmation before executing any rename operations.
- The prompt SHALL display the total count of files to be renamed.
- The tool SHALL support a `--yes` (or `-y`) flag to skip the confirmation prompt.

### FR-8: Collision Detection
- The tool SHALL detect when renaming would cause two files to have the same name.
- In dry-run mode, collisions SHALL be reported as warnings.
- In live mode, the tool SHALL refuse to proceed if collisions are detected.

### FR-9: Verbose Output
- The tool SHALL support a `--verbose` (or `-v`) flag.
- In verbose mode, the tool SHALL log each rename operation as it occurs.

### FR-10: Quiet Mode
- The tool SHALL support a `--quiet` (or `-q`) flag.
- In quiet mode, the tool SHALL suppress all output except errors.

### FR-11: Error Handling
- The tool SHALL catch and report `PermissionError` with the specific file path.
- The tool SHALL catch and report invalid regex patterns before processing files.
- The tool SHALL handle non-existent directories gracefully.

### FR-12: Help and Usage
- The tool SHALL provide a `--help` flag that displays usage information.
- The help text SHALL include usage examples.

## 2. Non-Functional Requirements

### NFR-1: Zero External Dependencies
- The tool SHALL require no third-party packages beyond the Python standard library.

### NFR-2: Cross-Platform Compatibility
- The tool SHALL run on Linux, macOS, and Windows without modification.

### NFR-3: Performance
- The tool SHALL process 10,000 files in under 5 seconds (excluding I/O latency).

### NFR-4: Idempotency
- Running the same rename command twice SHALL NOT produce unexpected results (the second run should rename nothing since patterns no longer match).

### NFR-5: Usability
- The tool SHALL exit with code 0 on success.
- The tool SHALL exit with non-zero codes for errors (1 for user errors, 2 for system errors).

## 3. Use Cases

### UC-1: Replace Spaces with Underscores
```
batch-rename "\s+" "_" --filter "*.txt"
```
Renames all `.txt` files: "my document.txt" -> "my_document.txt"

### UC-2: Add Prefix Using Capture Groups
```
batch-rename "(.*)" "backup_\1" --dry-run
```
Preview: "notes.txt" -> "backup_notes.txt"

### UC-3: Reorder Date Components
```
batch-rename "(\d{4})-(\d{2})-(\d{2})" "\3-\2-\1" --filter "report_*"
```
Renames: "report_2024-01-15.pdf" -> "report_15-01-2024.pdf"

### UC-4: Recursive Cleanup in Project
```
batch-rename "\.jpeg$" ".jpg" --recursive -v
```
Recursively renames all `.jpeg` extensions to `.jpg` with verbose output.

## 4. Constraints

- Python 3.8 or later required.
- Tool operates on filenames only (cannot rename directories).
- Regex is applied to the basename, not the full path.
- Symbolic links are followed (the file they point to is renamed, not the link target's name).