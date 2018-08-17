package com.blackducksoftware.integration.hub.detect.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * DetectConfiguration should be the only class that uses this.
 */

public class DetectPropertyMap {
    private final Map<DetectProperty, Object> propertyMap = new HashMap<>();

    public boolean getBooleanProperty(final DetectProperty detectProperty) {
        final Object value = propertyMap.get(detectProperty);
        if (null == value) {
            return false;
        }
        return (boolean) value;
    }

    public Long getLongProperty(final DetectProperty detectProperty) {
        final Object value = propertyMap.get(detectProperty);
        if (null == value) {
            return null;
        }
        return (long) value;
    }

    public Integer getIntegerProperty(final DetectProperty detectProperty) {
        final Object value = propertyMap.get(detectProperty);
        if (null == value) {
            return null;
        }
        return (int) value;
    }

    public String[] getStringArrayProperty(final DetectProperty detectProperty) {
        return (String[]) propertyMap.get(detectProperty);
    }

    public String getProperty(final DetectProperty detectProperty) {
        return (String) propertyMap.get(detectProperty);
    }

    public String getPropertyValueAsString(final DetectProperty detectProperty) {
        final Object objectValue = propertyMap.get(detectProperty);
        String displayValue = "";
        if (DetectPropertyType.STRING == detectProperty.getPropertyType()) {
            displayValue = (String) objectValue;
        } else if (DetectPropertyType.STRING_ARRAY == detectProperty.getPropertyType()) {
            displayValue = StringUtils.join((String[]) objectValue, ",");
        } else if (null != objectValue) {
            displayValue = objectValue.toString();
        }
        return displayValue;
    }

    public void setDetectProperty(final DetectProperty detectProperty, final String stringValue) {
        updatePropertyMap(propertyMap, detectProperty, stringValue);
    }

    public boolean containsDetectProperty(final DetectProperty detectProperty) {
        return propertyMap.containsKey(detectProperty);
    }

    public Map<DetectProperty, Object> getUnderlyingPropertyMap() {
        return propertyMap;
    }

    private void updatePropertyMap(final Map<DetectProperty, Object> propertyMap, final DetectProperty detectProperty, final String stringValue) {
        final Object value;
        if (DetectPropertyType.BOOLEAN == detectProperty.getPropertyType()) {
            value = convertBoolean(stringValue);
        } else if (DetectPropertyType.LONG == detectProperty.getPropertyType()) {
            value = convertLong(stringValue);
        } else if (DetectPropertyType.INTEGER == detectProperty.getPropertyType()) {
            value = convertInt(stringValue);
        } else if (DetectPropertyType.STRING_ARRAY == detectProperty.getPropertyType()) {
            value = convertStringArray(stringValue);
        } else {
            if (null == stringValue) {
                value = "";
            } else {
                value = stringValue;
            }
        }
        propertyMap.put(detectProperty, value);
    }

    private String[] convertStringArray(final String string) {
        if (null == string) {
            return new String[0];
        } else {
            return string.split(",");
        }
    }

    private Integer convertInt(final String integerString) {
        if (null == integerString) {
            return null;
        }
        return NumberUtils.toInt(integerString);
    }

    private Long convertLong(final String longString) {
        if (null == longString) {
            return null;
        }
        try {
            return Long.valueOf(longString);
        } catch (final NumberFormatException e) {
            return 0L;
        }
    }

    private Boolean convertBoolean(final String booleanString) {
        if (null == booleanString) {
            return null;
        }
        return BooleanUtils.toBoolean(booleanString);
    }
}
