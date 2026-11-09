rootProject.name = "HouseRules"

// Compile-only copies of BlueMinimap's share event, under its new and old (BlueMapHUD) names, so HouseRules builds without BlueMapHUD. Never shipped.
include("bluemaphud-stub")
project(":bluemaphud-stub").projectDir = file("test/stub")

// Test-only plugins for the live tests in test/live. Never shipped.
include("live-test-helper", "live-test-fakehud", "live-test-fakeminimap")
project(":live-test-helper").projectDir = file("test/live/helper")
project(":live-test-fakehud").projectDir = file("test/live/fakehud")
project(":live-test-fakeminimap").projectDir = file("test/live/fakeminimap")
