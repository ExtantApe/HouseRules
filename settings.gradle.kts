rootProject.name = "HouseRules"

// A compile-only copy of BlueMapHUD's share event, so HouseRules builds without BlueMapHUD. Never shipped.
include("bluemaphud-stub")
project(":bluemaphud-stub").projectDir = file("test/stub")

// Test-only plugins for the live tests in test/live. Never shipped.
include("live-test-helper", "live-test-fakehud")
project(":live-test-helper").projectDir = file("test/live/helper")
project(":live-test-fakehud").projectDir = file("test/live/fakehud")
