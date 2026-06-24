package fr.neplex.stratum;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;

public final class StratumPositionContext {
    private static final ThreadLocal<Deque<Integer>> ACTIVE_Y = ThreadLocal.withInitial(ArrayDeque::new);

    private StratumPositionContext() {
    }

    public static void push(int y) {
        ACTIVE_Y.get().push(y);
    }

    public static void push(BlockPos pos) {
        if (pos != null) {
            push(pos.getY());
        }
    }

    public static void pop() {
        Deque<Integer> stack = ACTIVE_Y.get();
        if (stack.isEmpty()) {
            return;
        }

        stack.pop();

        if (stack.isEmpty()) {
            ACTIVE_Y.remove();
        }
    }

    public static Integer get() {
        Deque<Integer> stack = ACTIVE_Y.get();
        return stack.isEmpty() ? null : stack.peek();
    }
}
