package cpath.converter;

import java.io.InputStream;
import java.io.OutputStream;

import org.biopax.paxtools.converter.psi.PsiToBiopax3Converter;

public final class PsimiToComplexesConverter extends BaseConverter {

	public void convert(InputStream is, OutputStream os) {
		try {				
			PsiToBiopax3Converter psimiConverter = new PsiToBiopax3Converter(xmlBase);
			//generate Complexes instead of MolecularInteractions
			psimiConverter.convert(is, os, true);
			os.close();
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert PSI-MI to BioPAX", e);
		}
	}

}
