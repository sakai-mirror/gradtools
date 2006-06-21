/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/DissertationStep.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
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

import java.util.List;

import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.time.api.Time;

/**
* <p>DissertationStep is the interface for BaseDissertationStep, defined in BaseDissertationService.</p>
* <p>It contains the instructions, validation type, and prerequisistes for a single step. 
* Dissertation objects contain collections of DissertationSteps.</p>
* @author University of Michigan, CHEF Software Development Team
* @version $Revision$
*/
public interface DissertationStep
	extends Entity, Comparable
{
	/** Validation type student.*/
	public static final String STEP_VALIDATION_TYPE_STUDENT = "1";

	/** Validation type chair.*/
	public static final String STEP_VALIDATION_TYPE_CHAIR = "2";

	/** Validation type committee.*/
	public static final String STEP_VALIDATION_TYPE_COMMITTEE = "3";

	/** Validation type department.*/
	public static final String STEP_VALIDATION_TYPE_DEPARTMENT = "4";
	
	/** Validation type school.*/
	public static final String STEP_VALIDATION_TYPE_SCHOOL = "5";

	/** Validation type student-chair.*/
	public static final String STEP_VALIDATION_TYPE_STUDENT_CHAIR = "6";

	/** Validation type student-committee.*/
	public static final String STEP_VALIDATION_TYPE_STUDENT_COMMITTEE = "7";

	/** Validation type student-department.*/
	public static final String STEP_VALIDATION_TYPE_STUDENT_DEPARTMENT = "8";
	
	/** Validation type student-school.*/
	public static final String STEP_VALIDATION_TYPE_STUDENT_SCHOOL = "9";


	/** Validation type description strings */
	public static final String STUDENT_VALIDATION_STRING = "Student";
	public static final String CHAIR_VALIDATION_STRING = "Chair";
	public static final String COMMITTEE_VALIDATION_STRING = "Committee";
	public static final String DEPARTMENT_VALIDATION_STRING = "Department";
	public static final String SCHOOL_VALIDATION_STRING = "Rackham";
	public static final String STUDENT_CHAIR_VALIDATION_STRING = "Student / Chair";
	public static final String STUDENT_COMMITTEE_VALIDATION_STRING = "Student / Committee";
	public static final String STUDENT_DEPARTMENT_VALIDATION_STRING = "Student / Department";
	public static final String STUDENT_SCHOOL_VALIDATION_STRING = "Student / Rackham";
	
	/**
	* Access the creator of this DissertationStep.
	* @return The user id of the creator.
	*/
	public String getCreator();
	
	/**
	* Access the user who last modified this DisserationStep.
	* @return the user id the the author who last modified the DissertationStep.
	*/
	public String getAuthorLastModified();
	
	/**
	* Access the Time that this DissertationStep was last modified.
	* @return the Time last modified.
	*/
	public Time getTimeLastModified();
	
	/**
	* Access the site id for the DissertationStep's site.
	* @return the site id.
	*/
	public String getSite();
	
	/**
	* Access the instructions for the DissertationStep with html links for user-entered links.
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
	* Access the prerequisites for this step.
	* @return A List containing the references of the prerequisites for this step.
	*/
	public List getPrerequisiteStepReferences();
	
	/**
	* Access the prerequisites for this step.
	* @return A List containing the prerequisite DissertationStep objects.
	*/
	public List getPrerequisiteSteps();
	
	/**
	* See whether this step contains this prerequisite.
	* @return true if this step has this prerequisite, false otherwise.
	*/
	public boolean hasPrerequisiteStep(String stepReference);
	
	/**
	* See whether this step has prerequisites.
	* @return true if the step has prerequisites, false otherwise.
	*/
	public boolean hasPrerequisites();

	/**
	* Access the validation type of this step.
	* @return the type of permissions required to mark a step as completed.  See the DissertationStep interface.
	*/
	public String getValidationType();
	
	/**
	* Access the validation type as a human-readable string.
	* @return The human-readable string corresonding to this step's validation type.
	*/
	public String getValidationTypeString();
	
	/**
	* Access the id used for Rackham auto-validation.
	* @return the auto-validation id.
	*/
	public String getAutoValidationId();
	
	/**
	* Access the step's checklist section identifier.
	* @return the section identifier.
	*/
	public String getSection();
	
}

/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/DissertationStep.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
*
**********************************************************************************/
