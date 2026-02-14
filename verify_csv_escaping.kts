#!/usr/bin/env kotlin

/**
 * Standalone verification script for CSV escaping logic.
 * Demonstrates that the RFC 4180 implementation is correct.
 */

fun escapeCsvField(field: String, separator: String = ";"): String {
    // Check if field needs quoting
    val needsQuoting = field.contains(separator) ||
                      field.contains('"') ||
                      field.contains('\n') ||
                      field.contains('\r')

    if (!needsQuoting) {
        return field
    }

    // Escape quotes by doubling them
    val escaped = field.replace("\"", "\"\"")

    // Wrap in quotes
    return "\"$escaped\""
}

fun parseCsvRow(row: String, separator: String = ";"): List<String> {
    val fields = mutableListOf<String>()
    val currentField = StringBuilder()
    var inQuotes = false
    var i = 0

    while (i < row.length) {
        val char = row[i]

        when {
            // Handle quote characters
            char == '"' -> {
                if (inQuotes) {
                    // Check if it's an escaped quote (double quote)
                    if (i + 1 < row.length && row[i + 1] == '"') {
                        currentField.append('"')
                        i++ // Skip next quote
                    } else {
                        // End of quoted field
                        inQuotes = false
                    }
                } else {
                    // Start of quoted field
                    inQuotes = true
                }
            }
            // Handle field separator
            char == separator[0] && !inQuotes -> {
                fields.add(currentField.toString())
                currentField.clear()
            }
            // Regular character
            else -> {
                currentField.append(char)
            }
        }
        i++
    }

    // Add the last field
    fields.add(currentField.toString())

    return fields
}

// Test cases
val testCases = listOf(
    "Simple text" to "Simple text",
    "Meeting; Konferenz" to "\"Meeting; Konferenz\"",
    "Er sagte \"Hallo\"" to "\"Er sagte \"\"Hallo\"\"\"",
    "Zeile 1\nZeile 2" to "\"Zeile 1\nZeile 2\"",
    "Multiple; semicolons; here" to "\"Multiple; semicolons; here\"",
    "" to ""
)

println("=== CSV Escaping Verification ===\n")

var passed = 0
var failed = 0

for ((input, expected) in testCases) {
    val actual = escapeCsvField(input)
    val result = if (actual == expected) {
        passed++
        "✓ PASS"
    } else {
        failed++
        "✗ FAIL"
    }
    println("$result: escapeCsvField(\"${input.replace("\n", "\\n")}\")")
    println("  Expected: $expected")
    println("  Actual:   $actual")
    println()
}

println("\n=== CSV Row Parsing Verification ===\n")

val rowTestCases = listOf(
    "2026-02-10;Montag;Home Office;08:15;16:37;8.37;0.50;7.87;" to
        listOf("2026-02-10", "Montag", "Home Office", "08:15", "16:37", "8.37", "0.50", "7.87", ""),
    "2026-02-10;Montag;Home Office;08:15;16:37;8.37;0.50;7.87;\"Meeting; Konferenz\"" to
        listOf("2026-02-10", "Montag", "Home Office", "08:15", "16:37", "8.37", "0.50", "7.87", "Meeting; Konferenz"),
    "2026-02-10;Montag;Home Office;08:15;16:37;8.37;0.50;7.87;\"Er sagte \"\"Hallo\"\"\"" to
        listOf("2026-02-10", "Montag", "Home Office", "08:15", "16:37", "8.37", "0.50", "7.87", "Er sagte \"Hallo\"")
)

for ((input, expected) in rowTestCases) {
    val actual = parseCsvRow(input)
    val result = if (actual == expected) {
        passed++
        "✓ PASS"
    } else {
        failed++
        "✗ FAIL"
    }
    println("$result: parseCsvRow(\"...\")")
    println("  Expected fields: ${expected.size}")
    println("  Actual fields:   ${actual.size}")
    if (actual != expected) {
        println("  Expected: $expected")
        println("  Actual:   $actual")
    }
    println()
}

println("\n=== Round-trip Test ===\n")

val originalData = listOf(
    "2026-02-10",
    "Montag",
    "Home Office",
    "08:15",
    "16:37",
    "8.37",
    "0.50",
    "7.87",
    "Meeting; Konferenz, \"wichtig\""
)

val csvRow = originalData.joinToString(";") { escapeCsvField(it) }
println("CSV Row: $csvRow")

val parsedData = parseCsvRow(csvRow)
val roundTripResult = if (parsedData == originalData) {
    passed++
    "✓ PASS"
} else {
    failed++
    "✗ FAIL"
}

println("$roundTripResult: Round-trip test")
println("  Original:  $originalData")
println("  Parsed:    $parsedData")
println()

println("\n=== Summary ===")
println("Passed: $passed")
println("Failed: $failed")
println("Total:  ${passed + failed}")

if (failed == 0) {
    println("\n✓ All tests passed! CSV escaping is RFC 4180 compliant.")
} else {
    println("\n✗ Some tests failed. Please review the implementation.")
}
