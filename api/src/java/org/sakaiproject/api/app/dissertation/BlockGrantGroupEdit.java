/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/BlockGrantGroupEdit.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
*
***********************************************************************************

*
* Copyright (c) 2003, 2004 The Regents of the University of Michigan, Trustees of Indiana University,
*                  Board of Trustees of the Leland Stanford, Jr., University, and The MIT Corporation
* 
* Licensed under the Educational Community License Version 1.0 (the "License");
* By obtaining, using and/or copying this Original Work, you agree that you have read,
* understand, and will comply with the terms and conditions of the Educational Community License.
* You may obtain a copy of the License at:
* 
*      http://cvs.sakaiproject.org/licenses/license_1_0.html
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
* INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
* AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
* DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*
**********************************************************************************/

//package
package org.sakaiproject.api.app.dissertation;

//imports
import org.sakaiproject.service.legacy.resource.Edit;


/**
* <p>BlockGrantGroupEdit is the interface for BaseBlockGrantGroupEdit, defined in BaseDissertationService.</p>
* <p>It is the edit for group roll up for Fields of Study in Rackham's database and maps to user parent department site in CHEF. </p>
* 
* @author University of Michigan, CHEF Software Development Team
* @version $Revision$
*/
public interface BlockGrantGroupEdit 
	extends Edit, BlockGrantGroup 
{
	/**
	* Set the database id for the BlockGrantGroup object.
	* @param code - String BGGC.
	*/
	public void setCode(String code);
	
	/**
	* Set the database description for the BlockGrantGroup object.
	* @param description - String BGGD.
	*/
	public void setDescription(String description);
	
	/**
	* Add a Field of Study to a BlockGrantGroup object.
	* @param code - String BGGC.
	* @param description - String BGGD.
	*/
	public void addFieldOfStudy(String code, String description);
	
	/**
	* Remove a Field of Study from a BlockGrantGroup object.
	* @param code - String BGGC.
	*/
	public void removeFieldOfStudy(String code);
}

/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/BlockGrantGroupEdit.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
*
***********************************************************************************/
