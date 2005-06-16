/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/DissertationEdit.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
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

//import
import java.util.Hashtable;
import java.util.List;

import org.sakaiproject.service.legacy.resource.Edit;

/**
* <p>DissertationEdit is the interface for BaseDissertationEdit, defined in BaseDissertationService.</p>
* <p>It contains the steps in the path to candidacy defined by a Graduate School Department. </p>
* 
* @author University of Michigan, CHEF Software Development Team
* @version $Revision$
*/
public interface DissertationEdit
	extends Edit, Dissertation
{
	/**
	* Add a DissertationStep to this Dissertation.
	* @param step The DissertationStep to be added.
	* @param previousStepReference The reference of the DissertationStep after which the step will be added.
	*/
	public void addStep(DissertationStep step, String previousStepReference);

	/**
	* Remove a DissertationStep from this Dissertation.
	* @param stepToRemoveReference The reference of the DissertationStep to be removed.
	*/
	public void removeStep(String stepToRemoveReference);
	
	/**
	* Move a DissertationStep within this Dissertation.
	* @param stepToMoveRef The reference of the DissertationStep to be removed.
	* @param location The reference of the step after which the DissertationStep is to be moved.
	*/
	public void moveStep(String stepToMoveRef, String location);
	
	/**
	* Set the DissertationSteps for this Dissertation.
	* @param table The Hashtable of DissertationStep references, keyed by display order as a String.
	*/
	public void setOrderedSteps(Hashtable table);
	
	/**
	* Add a prerequisite to a School step.
	* @param schoolStepReference The reference of the School step to which an prerequisite is to be added.
	* @param prereqStepReference The reference of the prerequisite step to be added.
	*/
	public void addSchoolPrereq(String schoolStepReference, String prereqStepReference);

	/**
	* Remove a prerequisite from a School step.
	* @param schoolStepReference The reference of the School step from which an prerequisite is to be removed.
	* @param prereqStepReference The reference of the prerequisite step to be removed.
	*/
	public void removeSchoolPrereq(String schoolStepReference, String prereqStepReference);
	
	/**
	* Set the site for this Dissertation.
	* @param site The site id.
	*/
	public void setSite(String site);
	
	/**
	* Set the pedagogical type for this Dissertation.
	* @param type The type.
	*/
	public void setType(String type);
	
	
	/**
	* Set the prerequisites for a School step.
	* @param schoolStepReference The reference of the school step.
	* @param prereqs The List of DissertationStep references.
	*/
	public void setSchoolPrereqs(String schoolStepReference, List prereqs);
}

/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/DissertationEdit.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
*
**********************************************************************************/
