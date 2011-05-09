/**
 ** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance,
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/

package cpath.webservice.args;

public enum GraphType
{
	NEIGHBORHOOD,
	PATHSBETWEEN,
	COMMONSTREAM
	;

	// I know this looks very idiotic but there was no solution for using those enums in
	// @RequestMapping. It requires a constant string and if we use enum values while creating the
	// constant string it won't accept. So I defined below strings representing enum values. If
	// you change this just make sure that string value is the same with enum (enum should be
	// capital letters and string is case insensitive).

	public static final String NEIGHBORHOOD_STR = "neighborhood";
	public static final String PATHSBETWEEN_STR = "pathsbetween";
	public static final String COMMONSTREAM_STR = "commonstream";

	public String getFullName()
	{
		switch (this)
		{
			case NEIGHBORHOOD: return NEIGHBORHOOD_STR;
			case PATHSBETWEEN: return PATHSBETWEEN_STR;
			case COMMONSTREAM: return COMMONSTREAM_STR;
		}
		return null;
	}
}