package org.jaudiotagger.tag.aiff;

/*
 *   Enum for AIFF fields that don't have obvious matches in FieldKey 
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public enum AiffTagFieldKey {
    TIMESTAMP("TIMESTAMP");

    private final String fieldName;

    AiffTagFieldKey(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
