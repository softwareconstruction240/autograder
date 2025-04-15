package edu.byu.cs.canvas.model;


/**
 * Represents a section in Canvas
 *
 * @param id the id of the section stored internally in Canvas, e.g. 29055
 * @param name the name of the section, e.g. CS 240-001: Adv Software Construction
 * @param students the collection of the students in that section
 */
public record CanvasSection (Integer id, String name, Collection<CanvasSectionStudent> students) {
    public record CanvasSectionStudent (String login_id) {}
}
