package com.slaythecraft.question;

import java.util.List;

/**
 * 一个问号事件（v0.0.2 V11；v0.5.0 增加事件描述）。
 *
 * @param id          稳定 id，例如 {@code armorer}
 * @param name        展示名，例如 {@code 盔甲商}
 * @param description 事件描述（打开界面时逐段折行显示；可以为空）
 * @param options     选项（1..3 个，界面按顺序排列）
 */
public record QuestionEvent(String id, String name, List<String> description, List<QuestionOption> options) {

    public QuestionEvent {
        description = description == null ? List.of() : List.copyOf(description);
        options = List.copyOf(options);
    }

    /** 没有描述的便捷构造（v0.0.2 的老事件）。 */
    public QuestionEvent(String id, String name, List<QuestionOption> options) {
        this(id, name, List.of(), options);
    }

    public int optionCount() {
        return options.size();
    }

    /** 越界返回 {@code null}（服务端收到客户端的选择时会用它做校验）。 */
    public QuestionOption option(int index) {
        return index >= 0 && index < options.size() ? options.get(index) : null;
    }
}
