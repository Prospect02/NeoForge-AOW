package net.prospect.otherworlds.bending.ability;

import java.util.ArrayList;
import java.util.List;
import net.prospect.otherworlds.bending.ability.Click;
/** A sequence of left and right clicks. */
public record ComboPattern(List<Click> clicks) {
    public static final ComboPattern NONE = new ComboPattern(List.of());

    /** "LRL" means left, right, left. */
    public static ComboPattern of(String pattern) {
        List<Click> list = new ArrayList<>();
        for (char c : pattern.toUpperCase().toCharArray()) {
            if (c == 'L') list.add(Click.LEFT);
            else if (c == 'R') list.add(Click.RIGHT);
            else throw new IllegalArgumentException("Combo may only contain L and R: " + pattern);
        }
        return new ComboPattern(List.copyOf(list));
    }

    public int length() { return clicks.size(); }
    public Click get(int index) { return clicks.get(index); }
}