/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/DissertationStepEdit.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
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

import org.sakaiproject.service.legacy.resource.Edit;

/**
* <p>DissertationStepEdit is the interface for BaseDissertationStepEdit, defined in BaseDissertationService.</p>
* <p>It contains the instructions, validation type, and prerequisistes for a single step. 
* Dissertation objects contain collections of DissertationSteps.</p>
* @author University of Michigan, CHEF Software Development Team
* @version $Revision$
*/
public interface DissertationStepEdit
	extends Edit, DissertationStep
{
	/**
	* Set the instructions for this DissertationStepEdit.
	* @param instructionsText The instructions.
	*/
	public void setInstructionsText(String instructionsText);
	
	/**
	* Add a prerequisite to this step.
	* @param stepReference The reference for the prerequisite DissertationStep to be added.
	*/
	public void addPrerequisiteStep(String stepReference);
	
	/**
	* Remove a prerequisite to this step.
	* @param stepReference The reference for the prerequisite DissertationStep to be removed.
	*/
	public void removePrerequisiteStep(String stepReference);
	
	/**
	* Set the validation type for this step.
	* @param type The type of permissions required to mark this step as completed.  See the DissertationStep interface for values.
	*/
	public void setValidationType(String type);
	
	/**
	* Set the site for this DissertationStepEdit.
	* @param site The site id.
	*/
	public void setSite(String site);
	
	/**
	* Set the auto-validation id for this step.
	* @param validId The id for auto-validation from the Rackham database.
	*/
	public void setAutoValidationId(String validId);
	
	/**
	* Set the step's checklist section identifier.
	* @param section The section identifier.
	*/
	public void setSection(String section);
	
}

/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/DissertationStepEdit.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
*
**********************************************************************************/
