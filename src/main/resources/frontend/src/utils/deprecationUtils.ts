//TODO: Remove these methods between semesters when resetting the database
import type {Rubric, RubricItem} from "@/types/types";

export function getPassoffTests(rubric: Rubric): RubricItem {
    return (rubric.items && rubric.items.PASSOFF_TESTS) || rubric.passoffTests;
}

export function getQuality(rubric: Rubric): RubricItem {
    return (rubric.items && rubric.items.QUALITY) || rubric.quality;
}

export function getUnitTests(rubric: Rubric): RubricItem {
    return (rubric.items && rubric.items.UNIT_TESTS) || rubric.unitTests;
}