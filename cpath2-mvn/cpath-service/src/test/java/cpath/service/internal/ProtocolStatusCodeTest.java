package cpath.service.internal;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import cpath.service.jaxb.ErrorType;

public class ProtocolStatusCodeTest {

	@Test
	public final void testGetAllStatusCodes() {
		List<String> list = ProtocolStatusCode.getAllStatusCodes();
		assertEquals(8, list.size());
	}

	@Test
	public final void testMarshal() {
		ErrorType e = ProtocolStatusCode.BAD_COMMAND.createErrorType();
		e.setErrorDetails("created in the juinit test ;)");
		String out = ProtocolStatusCode.marshal(e);
		assertTrue(out.length()>0);
		assertTrue(out.toLowerCase().contains("<error_msg>"));
		System.out.println(out);
	}

}