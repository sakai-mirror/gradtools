/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/Dissertation.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
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
import java.util.Hashtable;
import java.util.List;

import org.sakaiproject.service.legacy.resource.Entity;
import org.sakaiproject.service.legacy.time.Time;

/**
* <p>Dissertation is the interface for BaseDissertation, defined in BaseDissertationService.</p>
* <p>It contains the steps in the path to candidacy defined by a Graduate School Department. </p>
* 
* @author University of Michigan, CHEF Software Development Team
* @version $Revision$
*/
public interface Dissertation
	extends Entity
{
	/**
	* Access the reference of the DissertationStep by display order.
	* @param order The order number as a String.
	* @return The reference of the DissertationStep, or null if there is no match.
	*/
	public String getStepReferenceByOrder(String order);

	/**
	* Access the prerequisites added by department administrators to School steps for their department.
	* @return the Hashtable of FlexStringArrays containing the references of prerequisite steps, keyed by DissertationStep reference.
	*/
	public Hashtable getSchoolStepPrereqs();

	/**
	* Access the Hashtable containing the DissertationSteps.
	* @return The DissertationSteps keyed by display order.
	*/
	public Hashtable getOrderedSteps();

	/**
	* Access the display order for a DissertationStep.
	* @param stepReference The reference of the DissertationStep.
	* @return The display order number as a String.
	*/
	public String getOrderForStep(String stepReference);
	
	/**
	* Access the references of the prerequisite steps for a School step.
	* @param schoolStepReference The reference of the step.
	* @return List containing the prerequisite step references.
	*/
	public List getSchoolPrereqs(String schoolStepReference);
	
	/**
	* Access the order numbers of the prerequisites in a comma-delimited string for display.
	* @param step The DissertationStep.
	* @return The prerequisite steps display string.
	*/
	public String getPrerequisiteStepsDisplayString(DissertationStep step);
	
	/**
	* Access the site id for the Dissertation's site.
	* @return the site id.
	*/
	public String getSite();
	
	/**
	* A snapshot of this Dissertation's structure is used to create the steps in the CandidatePath.
	* This method is used in the creation of new CandidatePath objects.  This Dissertation becomes
	* the CandidatePath's parent Dissertation.
	* @param candidatePath The CandidatePath to initialize.
	* @param siteString The site id of the CandidatePath.
	*/
	public void initializeCandidatePath(CandidatePathEdit candidatePath, String siteString);
	
	/**
	* Access the Time that this Dissertation was last modified.
	* @return the Time last modified.
	*/
	public Time getTimeLastModified();
	
	/**
	* Access the pedagogical type of the Dissertation.
	* @return The type of the Dissertation, or null if not set.
	*/
	public String getType();

	
	/**
	* Access the creator of this Dissertation.
	* @return The user id of the creator.
	*/
	public String getCreator();
	
	/**
	* Access the user who last modified this Disseration.
	* @return the user id the the author who last modified the Dissertation.
	*/
	public String getAuthorLastModified();
}

/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/Dissertation.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
*
**********************************************************************************/
