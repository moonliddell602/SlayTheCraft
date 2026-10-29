package com.slaythecraft.combat.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/** A draw pile / discard pile: cards enter at the bottom, are drawn from the top, and can be reshuffled. */
public final class Pile {
    private final Deque<Card> cards = new ArrayDeque<>();

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public void add(Card card) {
        cards.addLast(card);
    }

    /**
     * 把一张牌插到**抽牌堆顶**（v0.4.7.4）。
     *
     * <p>{@link #draw()} 取的是 {@code pollFirst()}，所以"顶"就是第一个元素。</p>
     */
    public void addFirst(Card card) {
        cards.addFirst(card);
    }

    public void addAll(Collection<Card> incoming) {
        for (Card card : incoming) {
            cards.addLast(card);
        }
    }

    /** Removes and returns the top card, or {@code null} when empty. */
    public Card draw() {
        return cards.pollFirst();
    }

    /** Removes every card, returning them in pile order. */
    /**
     * 取走第 {@code index} 张（v0.4.7.6 选牌页用）。
     *
     * <p>{@code cards} 是 {@link ArrayDeque}，**没有下标访问**，所以先倒进列表、改完再倒回去
     * （顺序不变：原来的第一个仍然是第一个 = 抽牌堆顶）。</p>
     *
     * @return 被取走的那张；下标越界返回 {@code null}
     */
    public Card removeAt(int index) {
        if (index < 0 || index >= cards.size()) {
            return null;
        }
        List<Card> list = new ArrayList<>(cards);
        Card removed = list.remove(index);
        cards.clear();
        cards.addAll(list);
        return removed;
    }

    /** 把第 {@code index} 张换成另一张（v0.4.7.6 应急储备"变化"）。 */
    public boolean setAt(int index, Card card) {
        if (card == null || index < 0 || index >= cards.size()) {
            return false;
        }
        List<Card> list = new ArrayList<>(cards);
        list.set(index, card);
        cards.clear();
        cards.addAll(list);
        return true;
    }

    public List<Card> drainAll() {
        List<Card> drained = new ArrayList<>(cards);
        cards.clear();
        return drained;
    }

    /** 取出所有满足条件的牌（保持堆内顺序），用于"固有"关键字开局的定向抽取。 */
    public List<Card> extractMatching(java.util.function.Predicate<Card> predicate) {
        List<Card> extracted = new ArrayList<>();
        java.util.Iterator<Card> iterator = cards.iterator();
        while (iterator.hasNext()) {
            Card card = iterator.next();
            if (predicate.test(card)) {
                extracted.add(card);
                iterator.remove();
            }
        }
        return extracted;
    }

    /** Read-only snapshot in pile order (top first). */
    public List<Card> view() {
        return Collections.unmodifiableList(new ArrayList<>(cards));
    }

    public void shuffle(Random random) {
        List<Card> list = new ArrayList<>(cards);
        Collections.shuffle(list, random);
        cards.clear();
        cards.addAll(list);
    }

    public void clear() {
        cards.clear();
    }

    @Override
    public String toString() {
        return cards.size() + " 张";
    }
}
