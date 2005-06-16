/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/StepStatusEdit.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
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

import org.sakaiproject.service.legacy.resource.Edit;
import org.sakaiproject.service.legacy.time.Time;

/**
* <p>StepStatusEdit is the interface for BaseStepStatusEdit, defined in BaseDissertationService.</p>
* <p>It contains the instructions, validation type, and prerequisistes for a single step, as 
* well as methods to connect with the parent step object.  CandidatePath objects contain collections of StepStatus.</p>
* @author University of Michigan, CHEF Software Development Team
* @version $Revision$
*/
public interface StepStatusEdit
	extends Edit, StepStatus
{
	/**
	* Fill the properties of the StepStatus based on the parent DissertationStep.
	* @param site The site id of the current site at the time of creation.
	* @param step The parent DissertationStep from which this StepStatus is spawned.
	* @param oard Signifies whether this step is auto-validated by the Rackham database.
	*/
	public void initialize(String site, DissertationStep step, boolean oard);
	
	/**
	* Set the text to append to the time the step was marked as completed.
	* @param text The text to append to the time the step was marked as completed, or null if it is not set.
	*/
	public void setTimeCompletedText(String text);
	
	/**
	* Set the time the step was marked as completed.
	* @param time The time the step was marked as completed, or null if it is not completed.
	*/
	public void setTimeCompleted(Time time);
	
	/**
	* Set the recommended deadline for completion of this step.
	* @param deadline The Time recommended by the creator for completion.
	*/
	public void setRecommendedDeadline(Time deadline);
	
	/**
	* Set the absolute deadline for completion of the step.
	* @param deadline The absolute deadline set by the creator for completion.
	*/
	public void setHardDeadline(Time deadline);
	
	/**
	* Has this step been marked completed by the candidate yet ?
	* @param completed True if the step has been marked as completed, false otherwise.
	*/
	public void setCompleted(boolean completed);
	
	/**
	* Set additional lines of text to display with step status.
	* @param List of String lines of text to display.
	*/
	public void setAuxiliaryText(List text);

	/**
	* Set the validation type for this step.
	* @param type The type of permissions required to mark this step as completed.  See the DissertationStep interface for values.
	*/
	public void setValidationType(String type);

	/**
	* Set the instructions for this StepStatusEdit.
	* @param instructions The instructions.
	*/
	public void setInstructions(String instructions);
	
	/**
	* Set the instructions for this StepStatusEdit.
	* @param instructions The instructions.
	*/
	public void setInstructionsHtml(String instructions);

	/**
	* Set the site for this StepStatusEdit.
	* @param site The site id.
	*/
	public void setSite(String site);
	
	/**
	* Add a prerequisite to this step.
	* @param statusReference The reference for the prerequisite StepStatus to be added.
	*/
	public void addPrerequisiteStatus(String statusReference);
	
	/**
	* Remove a prerequisite to this step.
	* @param statusReference The reference for the prerequisite StepStatus to be removed.
	*/
	public void removePrerequisiteStatus(String statusReference);
	
	/**
	* Set the collection of prerequisites to this step.
	* @param prereqs List containing the references to the prerequisite StepStatus objects.
	*/
	public void setPrereqs(List prereqs);
	
	/**
	* Set the id for auto-validation by the Rackham database.
	* @param validId the id used for auto-validation by the Rackham database,
	* or -1 if the step is not auto-validated.
	*/
	public void setAutoValidationId(String validId);
}

/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/StepStatusEdit.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
*
**********************************************************************************/
