package edu.byu.cs.canvas.model;

import java.util.Collection;

public record CanvasSection (Integer id, String name, Collection<CanvasSectionStudent> students) {
    public record CanvasSectionStudent (String login_id) {}
}
