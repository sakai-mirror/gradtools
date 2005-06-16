/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/StepStatus.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
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
import java.util.List;

import org.sakaiproject.service.legacy.resource.Resource;
import org.sakaiproject.service.legacy.time.Time;

/**
* <p>StepStatus is the interface for BaseStepStatus, defined in BaseDissertationService.</p>
* <p>It contains the instructions, validation type, and prerequisistes for a single step, as 
* well as methods to connect with the parent step object.  CandidatePath objects contain collections of StepStatus.</p>
* @author University of Michigan, CHEF Software Development Team
* @version $Revision$
*/
public interface StepStatus
	extends Resource, Comparable
{
	/**
	* Access the reference of the parent DissertationStep.
	* @return The reference of the parent DissertationStep, if it exists, -1 otherwise.
	*/
	public String getParentStepReference();
	
	/**
	* Access additional text to display with step status.
	* @return List of String lines of text, or null there are none.
	*/
	public List getAuxiliaryText();

	/**
	* Access the time the step was marked as completed.
	* @return The time the step was marked as completed, or null if it is not completed.
	*/
	public Time getTimeCompleted();
	
	/**
	* Access the text to append to the time the step was marked as completed.
	* @return The text to append to the time the step was marked as completed, or null if it is not set.
	*/
	public String getTimeCompletedText();
	
	/**
	* Access the recommended deadline for completion of this step.
	* @return The Time recommended by the creator for completion.
	*/
	public Time getRecommendedDeadline();
	
	/**
	* Access the absolute deadline for completion of the step.
	* @return The absolute deadline set by the creator for completion.
	*/
	public Time getHardDeadline();
	
	/**
	 * Has this step been completed by the candidate ?
	 * @return Whether this step has been completed.
	 */
	public boolean getCompleted();

	/**
	* Access whether this step is auto-validated by the Rackham database.
	* @return True if the step is auto-validated by the Rackham database, false otherwise.
	*/
	public boolean getOardValidated();
	
	/**
	* Access the validation type of this step.
	* @return the type of permissions required to mark a step as completed.  See the DissertationStep interface.
	*/
	public String getValidationType();

	/**
	* Access the instructions for the StepStatus with html links for user-entered links.
	* @return the step's instructions with html links for user-entered links.
	*/
	public String getInstructions();
	
	/**
	* Access the instructions as entered by the user.
	* @return the instructions as entered by the user.
	*/
	public String getInstructionsText();
	
	/**
	* Access the instructions as entered by the user, with a maximum length of 80 characters for display in selects.
	* @return the instructions as entered by the user, with a maximum length of 80 characters for display in selects.
	*/
	public String getShortInstructionsText();

	/**
	* Access the site id for the StepStatus's site.
	* @return the site id.
	*/
	public String getSite();

	/**
	* Access the creator of this StepStatus.
	* @return The user id of the creator.
	*/
	public String getCreator();
	
	/**
	* Access the prerequisites for this step.
	* @return A List containing the references of the prerequisites for this step.
	*/
	public List getPrereqs();
	
	/**
	* See whether this step contains this prerequisite.
	* @param statusReference The reference of the prerequisite.
	* @return true if this step has this prerequisite, false otherwise.
	*/
	public boolean hasPrerequisite(String statusReference);
	
	/**
	* Access the user who marked the step as completed.
	* @return The user id of the user who marked the step as completed.
	*/
	public String getValidator();
	
	/**
	* Access the id for auto-validation by the Rackham database.
	* @return the id used for auto-validation by the Rackham database,
	* or -1 if the step is not auto-validated.
	*/
	public String getAutoValidationId();
}

/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/StepStatus.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
*
**********************************************************************************/
