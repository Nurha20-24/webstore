
package fi.metropolia.nurha.webstore.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

@Converter
public class OrderStatusConverter
        implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String status) {
        if (status == null) {
            return null;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    @Override
    public String convertToEntityAttribute(String dbStatus) {
        if (dbStatus == null) {
            return null;
        }
        return dbStatus.trim().toUpperCase(Locale.ROOT);
    }
}
