/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/CandidatePath.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
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
import java.util.List;

import org.sakaiproject.entity.api.Entity;

/**
* <p>CandidatePath is the interface for BaseCandidatePath, defined in BaseDissertationService.</p>
* <p>It contains the steps in the path to candidacy and methods to handle orders from the parent Dissertation object. </p>
* 
* @author University of Michigan, CHEF Software Development Team
* @version $Revision$
*/
public interface CandidatePath
	extends Entity, Comparable
{
	/**
	* Access the advisor of the candidate.
	* @return String - the user id of the candidate's advisor
	*/
	public String getAdvisor();

	/**
	* Access the candidate that owns this.
	* @return String - the user id of the candidate
	*/
	public String getCandidate();
	
	/**
	* Access the prerequisites set for Rackham School Steps within a sub-department's Dissertation.
	* @return Hashtable - the prerequsites keyed by step reference.
	*/
	public Hashtable getSchoolPrereqs();
	
	/**
	* Access the completion status of a StepStatus.
	* @return String - describes the status of the StepStatus.
	*/
	public String getStatusStatus(StepStatus status);
	
	/**
	* Access the type of dissertation set at the time of creation.
	* @return String - the type String.
	*/
	public String getType();
	
	/**
	* Access the alphabetical letter set at creation used to search and select CandidatePath by display name.
	* @return String - the letter used by the alphabetical candidate chooser.
	*/
	public String getSortLetter();
	

	/**
	* Access the StepStatus object corresponding to the parent DissertationStep.
	* @return String - the reference for the matching StepStatus, or null if there is no match.
	*/
	public String matchStepWithStatus(DissertationStep step);
	
	/**
	* Access the site set at the time of creation, or updated at first candidate access.
	* @return String - the site id
	*/
	public String getSite();
	
	/**
	* Access the parent department site set at the time of creation.
	* @return String - the site id
	*/
	public String getParentSite();
	
	/**
	* Access the prerequisites for a step in comma-delimited display form.
	* @return String - the display string of the prequisites
	*/
	public String getPrerequisiteStepsDisplayString(StepStatus status);
	
	/**
	* The ordered table of StepStatus objects.
	* @return Hashtable - the StepStatus references keyed by display order number.
	*/
	public Hashtable getOrderedStatus();
	
	/**
	* Access the reference of a StepStatus object by display order.
	* @param order - the order number in string form.
	* @return String - the StepStatus reference.
	*/
	public String getStatusReferenceByOrder(String order);
	
	/**
	* Access the order number of a StepStatus.
	* @param statusReference - the reference of the StepStatus.
	* @return int - the order number for the StepStatus.
	*/
	public int getOrderForStatus(String statusReference);

	/**
	* Access the prequisites added for Rackham School Steps within a department's Dissertation.
	* @param stepReference - the reference of the StepStatus
	* @return List - the references for the prerequisites
	*/
	public List getSchoolPrereqs(String stepReference);
}

/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/CandidatePath.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
*
**********************************************************************************/
