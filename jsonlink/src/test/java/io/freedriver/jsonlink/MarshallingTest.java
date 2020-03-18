package io.freedriver.jsonlink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.freedriver.jsonlink.jackson.JsonLinkModule;
import io.freedriver.jsonlink.jackson.schema.base.BaseResponse;
import io.freedriver.jsonlink.jackson.schema.base.Version;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class MarshallingTest {

    protected ObjectMapper mapper = JsonLinkModule.getMapper();

    @Test
    public void testMarshallVersion() throws JsonProcessingException {
        String json = "{ \"version\": [1,0,0] }";
        BaseResponse baseResponse = mapper.readValue(json, BaseResponse.class);

        Version expected = new Version(1,0,0);
        Version unexpected = new Version(2,0,0);

        assertEquals(expected, baseResponse.getVersion());
        assertNotEquals(unexpected, baseResponse.getVersion());
    }
}
