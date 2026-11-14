rootProject.name = "Mayor"

// A compile-only copy of BlueMapMini's share event, so Mayor builds without BlueMapMini. Never shipped.
include("bluemapmini-stub")
project(":bluemapmini-stub").projectDir = file("test/stub")

// Test-only plugins for the live tests in test/live. Never shipped.
include("live-test-helper", "live-test-fakeminimap")
project(":live-test-helper").projectDir = file("test/live/helper")
project(":live-test-fakeminimap").projectDir = file("test/live/fakeminimap")
