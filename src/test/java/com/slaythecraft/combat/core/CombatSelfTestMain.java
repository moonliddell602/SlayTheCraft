package com.slaythecraft.combat.core;

/**
 * Command-line runner for {@link CombatSelfTest}, used to verify the battle rules on a plain JVM
 * (no Minecraft, no Gradle needed):
 *
 * <pre>
 * javac -encoding UTF-8 -d out src/test/java/com/slaythecraft/combat/core/CombatSelfTestMain.java ^
 *       src/main/java/com/slaythecraft/combat/core/*.java
 * java -cp out com.slaythecraft.combat.core.CombatSelfTestMain
 * </pre>
 *
 * The in-game equivalent is {@code /stc combat selftest}.
 */
public final class CombatSelfTestMain {

    private CombatSelfTestMain() {
    }

    public static void main(String[] args) {
        CombatSelfTest.Result result = CombatSelfTest.run();
        result.lines().forEach(System.out::println);
        System.out.println(result.summary());
        if (!result.ok()) {
            System.exit(1);
        }
    }
}
