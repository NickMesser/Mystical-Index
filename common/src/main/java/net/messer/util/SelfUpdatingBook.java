package net.messer.util;

/**
 * Marks a book that rewrites its own custom data while the player is holding it.
 *
 * <p>The vanilla client re-plays the equip animation whenever the held stack stops comparing equal
 * to the one from last frame, which is the right behaviour for swapping items and the wrong one for
 * a book that ticks: a Book of Farming updating its clock once a second makes the hand re-raise
 * once a second. Both loaders expose a hook to veto that, but each in its own interface injected
 * into {@code Item}, so shared code cannot implement either - hence a marker here that the two
 * loader-side mixins can both test for.
 *
 * <p>Implement it on any book whose contents or counters change on their own while held.
 */
public interface SelfUpdatingBook {
}
