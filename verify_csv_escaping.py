#!/usr/bin/env python3

"""
Standalone verification script for CSV escaping logic.
Demonstrates that the RFC 4180 implementation is correct.
"""

def escape_csv_field(field, separator=";"):
    """Escape a CSV field according to RFC 4180."""
    # Check if field needs quoting
    needs_quoting = (separator in field or
                    '"' in field or
                    '\n' in field or
                    '\r' in field)

    if not needs_quoting:
        return field

    # Escape quotes by doubling them
    escaped = field.replace('"', '""')

    # Wrap in quotes
    return f'"{escaped}"'

def parse_csv_row(row, separator=";"):
    """Parse a CSV row according to RFC 4180."""
    fields = []
    current_field = []
    in_quotes = False
    i = 0

    while i < len(row):
        char = row[i]

        if char == '"':
            if in_quotes:
                # Check if it's an escaped quote (double quote)
                if i + 1 < len(row) and row[i + 1] == '"':
                    current_field.append('"')
                    i += 1  # Skip next quote
                else:
                    # End of quoted field
                    in_quotes = False
            else:
                # Start of quoted field
                in_quotes = True
        elif char == separator and not in_quotes:
            # Field separator
            fields.append(''.join(current_field))
            current_field = []
        else:
            # Regular character
            current_field.append(char)

        i += 1

    # Add the last field
    fields.append(''.join(current_field))

    return fields

# Test cases
test_cases = [
    ("Simple text", "Simple text"),
    ("Meeting; Konferenz", '"Meeting; Konferenz"'),
    ('Er sagte "Hallo"', '"Er sagte ""Hallo"""'),
    ("Zeile 1\nZeile 2", '"Zeile 1\nZeile 2"'),
    ("Multiple; semicolons; here", '"Multiple; semicolons; here"'),
    ("", ""),
]

print("=== CSV Escaping Verification ===\n")

passed = 0
failed = 0

for input_val, expected in test_cases:
    actual = escape_csv_field(input_val)
    result = "✓ PASS" if actual == expected else "✗ FAIL"
    if actual == expected:
        passed += 1
    else:
        failed += 1

    print(f"{result}: escape_csv_field({repr(input_val)})")
    print(f"  Expected: {expected}")
    print(f"  Actual:   {actual}")
    print()

print("\n=== CSV Row Parsing Verification ===\n")

row_test_cases = [
    ("2026-02-10;Montag;Home Office;08:15;16:37;8.37;0.50;7.87;",
     ["2026-02-10", "Montag", "Home Office", "08:15", "16:37", "8.37", "0.50", "7.87", ""]),
    ('2026-02-10;Montag;Home Office;08:15;16:37;8.37;0.50;7.87;"Meeting; Konferenz"',
     ["2026-02-10", "Montag", "Home Office", "08:15", "16:37", "8.37", "0.50", "7.87", "Meeting; Konferenz"]),
    ('2026-02-10;Montag;Home Office;08:15;16:37;8.37;0.50;7.87;"Er sagte ""Hallo"""',
     ["2026-02-10", "Montag", "Home Office", "08:15", "16:37", "8.37", "0.50", "7.87", 'Er sagte "Hallo"']),
]

for input_row, expected in row_test_cases:
    actual = parse_csv_row(input_row)
    result = "✓ PASS" if actual == expected else "✗ FAIL"
    if actual == expected:
        passed += 1
    else:
        failed += 1

    print(f"{result}: parse_csv_row(...)")
    print(f"  Expected fields: {len(expected)}")
    print(f"  Actual fields:   {len(actual)}")
    if actual != expected:
        print(f"  Expected: {expected}")
        print(f"  Actual:   {actual}")
    print()

print("\n=== Round-trip Test ===\n")

original_data = [
    "2026-02-10",
    "Montag",
    "Home Office",
    "08:15",
    "16:37",
    "8.37",
    "0.50",
    "7.87",
    'Meeting; Konferenz, "wichtig"'
]

csv_row = ";".join(escape_csv_field(field) for field in original_data)
print(f"CSV Row: {csv_row}")

parsed_data = parse_csv_row(csv_row)
round_trip_result = "✓ PASS" if parsed_data == original_data else "✗ FAIL"
if parsed_data == original_data:
    passed += 1
else:
    failed += 1

print(f"{round_trip_result}: Round-trip test")
print(f"  Original:  {original_data}")
print(f"  Parsed:    {parsed_data}")
print()

print("\n=== Summary ===")
print(f"Passed: {passed}")
print(f"Failed: {failed}")
print(f"Total:  {passed + failed}")

if failed == 0:
    print("\n✓ All tests passed! CSV escaping is RFC 4180 compliant.")
else:
    print("\n✗ Some tests failed. Please review the implementation.")
    exit(1)
