package cpath.client;

import static org.junit.Assert.*;

import org.biopax.paxtools.model.Model;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Ozgun Babur
 */
public class PathwayCommonsIOHandlerTest
{
	@Test
	@Ignore
	// Ignoring this test because it depends on the PC service.
	public void testGetNeighbors() throws IOException
	{
		PathwayCommonsIOHandler ioHandler = new PathwayCommonsIOHandler();
		ioHandler.setInputIdType(PathwayCommonsIOHandler.ID_TYPE.ENTREZ_GENE);
		Model result = ioHandler.getNeighbors("367");
		assertTrue(result.getObjects().size() > 0);
	}
}
