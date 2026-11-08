package com.extantape.houserules.rules;

import java.util.Set;
import java.util.UUID;

/** Made-up players for tests. */
final class Players {
    private Players() {
    }

    static Subject player(String name, String world, String... permissions) {
        Set<String> perms = Set.of(permissions);
        return Subject.of(UUID.nameUUIDFromBytes(name.getBytes()), name, world, perms::contains);
    }

    static String circle(String name) {
        return Reach.CIRCLE_PERMISSION + name;
    }
}
