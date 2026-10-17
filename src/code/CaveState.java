package code;

import java.util.Arrays;

/**
 * One snapshot of the cave: explorer position, remaining resources,
 * whether a key is carried, and which doors/keys are already done.
 * Immutable, so it is safe to use as a HashMap key.
 */
public final class CaveState {
    private final int x;                    // column of the explorer
    private final int y;                    // row of the explorer
    private final int energy;               // energy remaining
    private final int rope;                 // rope remaining (meters)
    private final int lives;                // lives remaining
    private final boolean hasKey;           // carrying a key (max one)
    private final boolean[] doorsOpen;      // doorsOpen[i] = door i is unlocked
    private final boolean[] keysCollected;  // keysCollected[i] = key i was picked up

    public CaveState(int x, int y, int energy, int rope, int lives,
            boolean hasKey, boolean[] doorsOpen, boolean[] keysCollected) {
        this.x = x;
        this.y = y;
        this.energy = energy;
        this.rope = rope;
        this.lives = lives;
        this.hasKey = hasKey;
        // Copy the arrays so no one outside can change this state.
        this.doorsOpen = doorsOpen.clone();
        this.keysCollected = keysCollected.clone();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getEnergy() {
        return energy;
    }

    public int getRope() {
        return rope;
    }

    public int getLives() {
        return lives;
    }

    public boolean hasKey() {
        return hasKey;
    }

    public boolean isDoorOpen(int index) {
        return doorsOpen[index];
    }

    public boolean isKeyCollected(int index) {
        return keysCollected[index];
    }

    // Two states are equal if every field matches (used to detect repeated states).
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CaveState)) {
            return false;
        }

        CaveState state = (CaveState) other;

        return x == state.x
            && y == state.y
            && energy == state.energy
            && rope == state.rope
            && lives == state.lives
            && hasKey == state.hasKey
            && Arrays.equals(doorsOpen, state.doorsOpen)
            && Arrays.equals(keysCollected, state.keysCollected);
    }

    // Must agree with equals so HashMap lookups work.
    @Override
    public int hashCode() {
        int result = Integer.hashCode(x);
        result = 31 * result + Integer.hashCode(y);
        result = 31 * result + Integer.hashCode(energy);
        result = 31 * result + Integer.hashCode(rope);
        result = 31 * result + Integer.hashCode(lives);
        result = 31 * result + Boolean.hashCode(hasKey);
        result = 31 * result + Arrays.hashCode(doorsOpen);
        result = 31 * result + Arrays.hashCode(keysCollected);
        return result;
    }
}
