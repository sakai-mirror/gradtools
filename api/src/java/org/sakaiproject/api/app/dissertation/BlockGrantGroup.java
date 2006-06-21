/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/BlockGrantGroup.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
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

import java.util.Hashtable;

import org.sakaiproject.entity.api.Entity;


/**
* <p>BlockGrantGroup is the interface for BaseBlockGrantGroup, defined in BaseDissertationService.</p>
* <p>It is the group roll up for Fields of Study in Rackham's database and maps to user parent department site in CHEF. </p>
* 
* @author University of Michigan, CHEF Software Development Team
* @version $Revision$
*/

public interface BlockGrantGroup 
	extends Entity 
{
	/**
	* Access the database id for the BlockGrantGroup object.
	* @return String - the database id.
	*/
	public String getId();
	
	/**
	* Access the database id for the BlockGrantGroup object.
	* @return String - the database id.
	*/
	public String getCode();
	
	/**
	* Access the database name for the BlockGrantGroup object.
	* @return String - the database name.
	*/
	public String getDescription();
	
	/**
	* Access the site for the BlockGrantGroup object.
	* @return String - the site id.
	*/
	public String getSite();
	
	/**
	* Access the database map for the BlockGrantGroup FieldsOfStudy.
	* @return Hashtable - the database FieldsOfStudy (key = FOS code, value = FOS description)
	*/
	public Hashtable getFieldsOfStudy();
}

/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/BlockGrantGroup.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
*
**********************************************************************************/
