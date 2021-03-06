package org.kie.server.api.marshalling;

import java.util.Set;

import org.kie.server.api.marshalling.jaxb.JaxbMarshaller;
import org.kie.server.api.marshalling.json.JSONMarshaller;
import org.kie.server.api.marshalling.xstream.XStreamMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarshallerFactory {

    private static final Logger logger = LoggerFactory.getLogger( MarshallerFactory.class );

    public static Marshaller getMarshaller(MarshallingFormat format, ClassLoader classLoader) {
        return getMarshaller(null, format, classLoader);
    }

    public static Marshaller getMarshaller(Set<Class<?>> classes, MarshallingFormat format, ClassLoader classLoader) {
        switch ( format ) {
            case XSTREAM:
                return new XStreamMarshaller( classLoader );
            case JAXB:
                return new JaxbMarshaller(classes, classLoader); // has to be implemented
            case JSON:
                return new JSONMarshaller(classes, classLoader); // has to be implemented
            default:
                logger.error( "Unsupported marshalling format: " + format );
        }
        return null;
    }
}
