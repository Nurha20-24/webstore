package fi.metropolia.nurha.webstore;

import fi.metropolia.nurha.webstore.converter.OrderStatusConverter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Yksikkötesti — ei tarvita Spring-kontekstia
// Testataan pelkkää muunninlogiikkaa
class OrderStatusConverterTest {

    private final OrderStatusConverter converter = new OrderStatusConverter();

    @Test
    void convertToDatabaseColumn_lowercaseInput_returnsUppercase() {
        assertEquals("NEW", converter.convertToDatabaseColumn("new"));
        assertEquals("CANCELLED", converter.convertToDatabaseColumn("cancelled"));
        assertEquals("EXPIRED", converter.convertToDatabaseColumn("expired"));
    }

    @Test
    void convertToDatabaseColumn_alreadyUppercase_returnsUppercase() {
        assertEquals("NEW", converter.convertToDatabaseColumn("NEW"));
    }

    @Test
    void convertToDatabaseColumn_null_returnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumn_withSpaces_trimsAndConverts() {
        assertEquals("NEW", converter.convertToDatabaseColumn("  new  "));
    }

    @Test
    void convertToEntityAttribute_withSpaces_trims() {
        assertEquals("NEW", converter.convertToEntityAttribute("NEW  "));
    }

    @Test
    void convertToEntityAttribute_null_returnsNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }
}