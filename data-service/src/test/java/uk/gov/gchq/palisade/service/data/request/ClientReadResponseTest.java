package uk.gov.gchq.palisade.service.data.request;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.data.service.SimpleDataServiceTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class ClientReadResponseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDataServiceTest.class);

    @Mock
    InputStream stream;

    ClientReadResponse clientReadResponse;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        clientReadResponse = new ClientReadResponse(stream);
        LOGGER.info("Simple Data Service created: {}", clientReadResponse);
    }


    @Test
    public void asInputStream() {
        //given

        //when

        //then
        assertEquals(stream, clientReadResponse.asInputStream());

    }

    @Test
    public void writeTo() throws IOException {

        //given
        OutputStream outputStream = Mockito.mock(OutputStream.class);
        when(stream.read(any())).thenReturn(-1);


        //when
        try {
            clientReadResponse.writeTo(outputStream);
        } catch (IOException e) {
            fail();
        }


        //then
        verify(stream, times(1)).read(any());

    }
}