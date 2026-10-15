package code;

import java.util.Arrays;

public final class CaveState {
    private final int x;
    private final int y;
    private final int energy;
    private final int rope;
    private final int lives;
    private final boolean hasKey;
    private final boolean[] doorsOpen;
    private final boolean[] keysCollected;

    public CaveState(int x, int y, int energy, int rope, int lives,
            boolean hasKey, boolean[] doorsOpen, boolean[] keysCollected) {
        this.x = x;
        this.y = y;
        this.energy = energy;
        this.rope = rope;
        this.lives = lives;
        this.hasKey = hasKey;
        // Keep each search state independent from changes to the caller's arrays.
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
