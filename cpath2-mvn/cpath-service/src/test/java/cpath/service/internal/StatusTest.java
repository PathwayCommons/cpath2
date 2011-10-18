package cpath.service.internal;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import cpath.service.Status;
import cpath.service.jaxb.ErrorResponse;

@Deprecated
@Ignore //tested in the cpath-api
public class StatusTest {

	@Test
	public final void testGetAllStatusCodes() {
		List<String> list = Status.getAllStatusCodes();
		assertEquals(5, list.size());
	}

	@Test
	public final void testMarshalErrorResponse() {
		ErrorResponse e = Status.BAD_COMMAND.errorResponse(null);
		e.setErrorDetails("created in the juinit test ;)");
		String out = Status.marshal(e);
		assertTrue(out.length()>0);
		assertTrue(out.toLowerCase().contains("<error_msg>"));
		System.out.println(out);
	}

}
