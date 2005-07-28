/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/DissertationService.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
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

package org.sakaiproject.api.app.dissertation;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.service.legacy.resource.Resource;
import org.sakaiproject.service.legacy.user.User;
import org.w3c.dom.Element;

public interface DissertationService
{	
	/** This string can be used to find the service in the service manager. */
	public static final String SERVICE_NAME = DissertationService.class.getName();

	/** This string starts the references to resources in this service. */
	public static final String REFERENCE_ROOT = Resource.SEPARATOR + "dissertation";

	/** Security lock for adding a dissertation. */
	public static final String SECURE_ADD_DISSERTATION = "dis.dis.add";

	/** Security lock for accessing a dissertation. */
	public static final String SECURE_ACCESS_DISSERTATION = "dis.dis.read";

	/** Security lock for updating a dissertation. */
	public static final String SECURE_UPDATE_DISSERTATION = "dis.dis.upd";

	/** Security lock for removing a dissertation. */
	public static final String SECURE_REMOVE_DISSERTATION = "dis.dis.del";

	/** Security lock for adding a CandidatePath. */
	public static final String SECURE_ADD_DISSERTATION_CANDIDATEPATH = "dis.path.add";

	/** Security lock for accessing a CandidatePath. */
	public static final String SECURE_ACCESS_DISSERTATION_CANDIDATEPATH = "dis.path.read";

	/** Security lock for updating a CandidatePath. */
	public static final String SECURE_UPDATE_DISSERTATION_CANDIDATEPATH = "dis.path.upd";

	/** Security lock for updating a CandidatePath for a committee member. */
	public static final String SECURE_UPDATE_DISSERTATION_CANDIDATEPATH_COMM = "dis.path.upd.comm";
	
	/** Security lock for removing a CandidatePath. */
	public static final String SECURE_REMOVE_DISSERTATION_CANDIDATEPATH = "dis.path.del";

	/** Security lock for adding a step. */
	public static final String SECURE_ADD_DISSERTATION_STEP = "dis.step.add";

	/** Security lock for accessing a step. */
	public static final String SECURE_ACCESS_DISSERTATION_STEP = "dis.step.read";

	/** Security lock for updating a step. */
	public static final String SECURE_UPDATE_DISSERTATION_STEP = "dis.step.upd";

	/** Security lock for removing a step. */
	public static final String SECURE_REMOVE_DISSERTATION_STEP = "dis.step.del";

	/** Security lock for adding a stepstatus. */
	public static final String SECURE_ADD_DISSERTATION_STEPSTATUS = "dis.status.add";

	/** Security lock for accessing a stepstatus. */
	public static final String SECURE_ACCESS_DISSERTATION_STEPSTATUS = "dis.status.read";

	/** Security lock for updating a stepstatus. */
	public static final String SECURE_UPDATE_DISSERTATION_STEPSTATUS = "dis.status.upd";

	/** Security lock for removing a stepstatus. */
	public static final String SECURE_REMOVE_DISSERTATION_STEPSTATUS = "dis.status.del";

	/** Security lock for adding a candidateinfo. */
	public static final String SECURE_ADD_DISSERTATION_CANDIDATEINFO = "dis.info.add";

	/** Security lock for accessing a candidateinfo. */
	public static final String SECURE_ACCESS_DISSERTATION_CANDIDATEINFO = "dis.info.read";

	/** Security lock for updating a candidateinfo. */
	public static final String SECURE_UPDATE_DISSERTATION_CANDIDATEINFO = "dis.info.upd";

	/** Security lock for removing a candidateinfo. */
	public static final String SECURE_REMOVE_DISSERTATION_CANDIDATEINFO = "dis.info.del";
	
	/** Security lock for accessing a Block Grant Group. */
	public static final String SECURE_ACCESS_DISSERTATION_GROUP = "dis.grp.read";
	
	/** Security lock for adding a Block Grant Group. */
	public static final String SECURE_ADD_DISSERTATION_GROUP = "dis.grp.add";
	
	/** Security lock for removing a Block Grant Group. */
	public static final String SECURE_REMOVE_DISSERTATION_GROUP = "dis.grp.del";
	
	/** Security lock for updating a Block Grant Group. */
	public static final String SECURE_UPDATE_DISSERTATION_GROUP = "dis.grp.upd";
	
	/** Step Status Prerequisites not completed. */
	public static final String STEP_STATUS_PREREQS_NOT_COMPLETED = "Prerequisites not completed.";
	
	/** Step Status Prerequisites completed. */
	public static final String STEP_STATUS_PREREQS_COMPLETED = "Prerequisites completed; step not completed.";
	
	/** Step Status Completed. */
	public static final String STEP_STATUS_COMPLETED = "Step completed.";
	
	/** Checklist section heading */
	public static final String CHECKLIST_SECTION_HEADING1 = "Steps for Candidacy and Forming a Dissertation Committee";
	
	/** Checklist section heading */
	public static final String CHECKLIST_SECTION_HEADING2 = "Steps for Research and Writing of Dissertation";
	
	/** Checklist section heading */
	public static final String CHECKLIST_SECTION_HEADING3 = "Steps for Defense and Evaluation";
	
	/** Checklist section heading */
	public static final String CHECKLIST_SECTION_HEADING4 = "Steps for Degree Conferral";

	/** Rackham data extract file name. */
	public static final String DATAFILE_NAME = "dissertation.upload.filename";
	
	/** Rackham data extract file content type. */
	public static final String DATAFILE_TYPE = "dissertation.upload.filetype";
	
	/** BaseDissertationService.getDataFileBasicProperties number of lines property. */
	public static final String DATAFILE_LINES = "dissertation.upload.lines";
	
	/** Rackham dissertation type for Music Performance. */
	public static final String DISSERTATION_TYPE_MUSIC_PERFORMANCE = "Dissertation Steps: Music Performance";
	
	/** Rackham dissertation type for steps not associated with Music Performance. */
	public static final String DISSERTATION_TYPE_DISSERTATION_STEPS = "Dissertation Steps";
	
	/** The reference of the object that is locked in the db during upload */
	public static final String IS_LOADING_LOCK_REFERENCE = "/dissertation/i/rackham/rackham/dissertation.isloading.lock";
	
	/** The id of the object that is locked in the db during upload */
	public static final String IS_LOADING_LOCK_ID = "dissertation.isloading.lock";
	
	/** The template and properties for creating a static checklist */
	public static final String STATIC_CHECKLIST_TEMPLATE = "static_checklist_template.txt";
	public static final String STATIC_CHECKLIST_NAME = "checklist.html";
	public static final String STATIC_CHECKLIST_DISPLAY_NAME = "Dissertation Checklist";
	public static final String STATIC_CHECKLIST_DESCRIPTION = "Saved copy of the Dissertation Checklist";
	
	/**
	* Check permissions for adding a Block Grant Group.
	* @param site - The site for which permissions are being checked.
	* @return True if the current User is allowed to add a Block Grant Group, false if not.
	*/
	public boolean allowAddBlockGrantGroup(String site);
	
	/**
	* Check permissions for accessing Block Grant Groups.
	* @param blockGrantGroupReference - The BlockGrantGroup's reference.
	* @return True if the current User is allowed to a Block Grant Group, false if not.
	*/
	public boolean allowGetBlockGrantGroup(String blockGrantGroupReference);
	
	/**
	* Check permissions for removing a Block Grant Group.
	* @param blockGrantGroupReference - The BlockGrantGroup's reference.
	* @return True if the current User is allowed to remove a Block Grant Group, false if not.
	*/
	public boolean allowRemoveBlockGrantGroup(String blockGrantGroupReference);
	
	/**
	* Check permissions for updating a BlockGrantGroup.
	* @param blockGrantGroupReference - The BlockGrantGroup's reference.
	* @return True if the current User is allowed to update a Block Grant Group, false if not.
	*/
	public boolean allowUpdateBlockGrantGroup(String blockGrantGroupReference);
	
	/**
	* Check permissions for adding a Dissertation.
	* @param site - The site for which permissions are being checked.
	* @return True if the current User is allowed to add a Dissertation, false if not.
	*/
	public boolean allowAddDissertation(String site);

	/**
	* Check permissions for removing a Dissertation.
	* @param dissertationReference - The Dissertation's reference.
	* @return True if the current User is allowed to remove the Dissertation, false if not.
	*/
	public boolean allowRemoveDissertation(String dissertationReference);
	
	/**
	* Check permissions for accessing a Dissertation.
	* @param dissertationReference - The Dissertation's reference.
	* @return True if the current User is allowed to access the Dissertation, false if not.
	*/
	public boolean allowGetDissertation(String dissertationReference);
	
	/**
	* Check permissions for updating a Dissertation.
	* @param dissertationReference - The Dissertation's reference.
	* @return True if the current User is allowed to update the Dissertation, false if not.
	*/
	public boolean allowUpdateDissertation(String dissertationReference);
	
	/**
	* Check permissions for add CandidatePath
	* @param site - The site for which permissions are being checked.
	* @return True if the current User is allowed to add an CandidatePath, false if not.
	*/
	public boolean allowAddCandidatePath(String site);

	/**
	* Check permissions for removing the CandidatePath
	* @param pathReference - The CandidatePath's reference.
	* @return True if the current User is allowed to remove the CandidatePath, false if not.
	*/
	public boolean allowRemoveCandidatePath(String candidatePathReference);
	
	/**
	* Check permissions for accessing a CandidatePath.
	* @param pathReference - The CandidatePath's reference.
	* @return True if the current User is allowed to get the CandidatePath, false if not.
	*/
	public boolean allowGetCandidatePath(String candidatePathReference);
	
	/**
	* Check permissions for updating CandidatePath.
	* @param pathReference - The CandidatePath's reference.
	* @return True if the current User is allowed to update the CandidatePath, false if not.
	*/
	public boolean allowUpdateCandidatePath(String candidatePathReference);

	/**
	* Check permissions for updating CandidatePath for Committee Members.
	* @param pathReference - The CandidatePath's reference.
	* @return True if the current User is allowed to update the CandidatePath, false if not.
	*/
	public boolean allowUpdateCandidatePathComm(String candidatePathReference);
	
	/**
	* Check permissions for adding a DissertationStep.
	* @param site - The site for which permissions are being checked.
	* @return True if the current User is allowed to add a DissertationStep, false if not.
	*/
	public boolean allowAddDissertationStep(String site);

	/**
	* Check permissions for remove Dissertation step
	* @param stepReference - The DissertationStep reference.
	* @return True if the current User is allowed to remove the DissertationStep, false if not.
	*/
	public boolean allowRemoveDissertationStep(String stepReference);
	
	/**
	* Check permissions for get DissertationStep
	* @param stepReference - The DissertationStep reference.
	* @return True if the current User is allowed to get the DissertationStep, false if not.
	*/
	public boolean allowGetDissertationStep(String stepReference);
	
	/**
	* Check permissions for updating DissertationStep
	* @param stepReference - The DissertationStep reference.
	* @return True if the current User is allowed to update the DissertationStep, false if not.
	*/
	public boolean allowUpdateDissertationStep(String stepReference);	

	/**
	* Check permissions for add StepStatus
	* @param site - The site for which permissions are being checked.
	* @return True if the current User is allowed to add an StepStatus, false if not.
	*/
	public boolean allowAddStepStatus(String site);

	/**
	* Check permissions for removing the StepStatus
	* @param statusReference - The StepStatus's reference.
	* @return True if the current User is allowed to remove the StepStatus, false if not.
	*/
	public boolean allowRemoveStepStatus(String stepStatusReference);
	
	/**
	* Check permissions for accessing a StepStatus.
	* @param statusReference - The StepStatus's reference.
	* @return True if the current User is allowed to get the StepStatus, false if not.
	*/
	public boolean allowGetStepStatus(String stepStatusReference);
	
	/**
	* Check permissions for updating StepStatus.
	* @param statusReference - The StepStatus's reference.
	* @return True if the current User is allowed to update the StepStatus, false if not.
	*/
	public boolean allowUpdateStepStatus(String stepStatusReference);

	/**
	* Check permissions for add CandidateInfo
	* @param site - The site for which permissions are being checked.
	* @return True if the current User is allowed to add an CandidateInfo, false if not.
	*/
	public boolean allowAddCandidateInfo(String site);

	/**
	* Check permissions for removing the CandidateInfo
	* @param infoReference - The CandidateInfo's reference.
	* @return True if the current User is allowed to remove the CandidateInfo, false if not.
	*/
	public boolean allowRemoveCandidateInfo(String infoReference);
	
	/**
	* Check permissions for accessing a CandidateInfo.
	* @param infoReference - The CandidateInfo's reference.
	* @return True if the current User is allowed to get the CandidateInfo, false if not.
	*/
	public boolean allowGetCandidateInfo(String infoReference);
	
	/**
	* Check permissions for updating CandidateInfo.
	* @param infoReference - The CandidateInfo's reference.
	* @return True if the current User is allowed to update the CandidateInfo, false if not.
	*/
	public boolean allowUpdateCandidateInfo(String infoReference);
	
	/**
	* Access the site for the user's Rackham department.
	* @param userId The user's CHEF id.
	* @return The site id of the parent site.
	*/
	public String getParentSiteForUser(String userId);

	/**
	* Access the University id for the user.
	* @param userId The user's CHEF id.
	* @return The university id for the user.
	*/
	public String getEmplidForUser(String userId);
	
	/** 
	* Access the site id of the Rackham site.
	* @return The school site id.
	*/
	public String getSchoolSite();
	
	/** 
	* Access the site id of the Music Performance site.
	* @return The Music Performance site id.
	*/
	public String getMusicPerformanceSite();
	
	/** 
	* Access whether the system is initialized - i.e. whether any CandidateInfo objects exist.
	* @return True if any CandidateInfo objects exist, false if no.
	*/
	public boolean hasCandidateInfos();
	
	/**************** BGG methods ***************************************/
	
	/**
	* Creates and adds a new Block Grant Group to the service.
	* @param site The site context for adding a Block Grant Group.
	* @return The new Block Grant Group object.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public BlockGrantGroupEdit addBlockGrantGroup(String site)
		throws PermissionException;
	
	
	/**
	* Get a locked Block Grant Group object for editing. Must commitEdit() to make official, or cancelEdit() when done!
	* @param reference The Block Grant Group reference string.
	* @return A BlockGrantGroupEdit object for editing.
	* @exception IdUnusedException if not found, or if not a BlockGrantGroupEdit object
	* @exception PermissionException if the current user does not have permission to edit this Block Grant Group.
	* @exception InUseException if the Block Grant Group object is locked by someone else.
	*/
	public BlockGrantGroupEdit editBlockGrantGroup(String blockGrantGroupReference)
		throws IdUnusedException, InUseException, PermissionException;
		
	/**
	* Commit the changes made to a BlockGrantGroupEdit object, and release the lock.
	* The BlockGrantGroupEdit is disabled, and not to be used after this call.
	* @param blockGrantGroupEdit The BlockGrantGroupEdit object to commit.
	*/
	public void commitEdit(BlockGrantGroupEdit blockGrantGroupEdit);
	
	/**
	* Cancel the changes made to a BlockGrantGroupEdit object, and release the lock.
	* The BlockGrantGroupEdit is disabled, and not to be used after this call.
	* @param dissertation The BlockGrantGroupEdit object to cancel.
	*/
	public void cancelEdit(BlockGrantGroupEdit blockGrantGroupEdit);

	/**
	* Creates and adds a new Dissertation to the service.
	* @param site - The site for which permissions are being checked.
	* @return The new Dissertation object.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public DissertationEdit addDissertation(String site)
		throws PermissionException;

	/**
	* Add a new dissertation to the directory, from a definition in XML.
	* Must commitEdit() to make official, or cancelEdit() when done!
	* @param el The XML DOM Element defining the dissertation.
	* @return A locked DissertationEdit object (reserving the id).
	* @exception IdInvalidException if the dissertation id is invalid.
	* @exception IdUsedException if the dissertation id is already used.
	* @exception PermissionException if the current user does not have permission to add an assignnment.
	*/
	public DissertationEdit mergeDissertation(Element el)
		throws IdInvalidException, IdUsedException, PermissionException;

	/** 
	* Removes this Dissertation and all references to it.
	* @param dissertation - The Dissertation to remove.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public void removeDissertation(DissertationEdit dissertation)
		throws PermissionException;

	/**
	* Access the Dissertation with the specified reference.
	* @param dissertationReference - The reference of the Dissertation.
	* @return The Dissertation corresponding to the reference, or null if it does not exist.
	* @throws IdUnusedException if there is no object with this reference.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public Dissertation getDissertation(String dissertationReference)
		throws IdUnusedException, PermissionException;

	/**
	* Get a locked dissertation object for editing. Must commitEdit() to make official, or cancelEdit() when done!
	* @param reference The dissertation reference string.
	* @return A DissertationEdit object for editing.
	* @exception IdUnusedException if not found, or if not a DissertationEdit object
	* @exception PermissionException if the current user does not have permission to edit this dissertation.
	* @exception InUseException if the Dissertation object is locked by someone else.
	*/
	public DissertationEdit editDissertation(String dissertationReference)
		throws IdUnusedException, InUseException, PermissionException;

	/**
	* Commit the changes made to a DissertationEdit object, and release the lock.
	* The DissertationEdit is disabled, and not to be used after this call.
	* @param dissertation The DissertationEdit object to commit.
	*/
	public void commitEdit(DissertationEdit dissertation);

	/**
	* Cancel the changes made to a DissertationEdit object, and release the lock.
	* The DissertationEdit is disabled, and not to be used after this call.
	* @param dissertation The DissertationEdit object to commit.
	*/
	public void cancelEdit(DissertationEdit dissertation);

	/** 
	* Creates and adds a new DissertationStep to the service.
	* @param site - The site for which permissions are being checked.
	* @return DissertationStepEdit The new DissertationStep object.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public DissertationStepEdit addDissertationStep(String site)
		throws PermissionException;
		
	/** 
	* Creates and adds a new DissertationStep to the service.
	* @param site - The site for which permissions are being checked.
	* @param type - The pedagogical type inherent in the steps.
	* @return DissertationStepEdit The new DissertationStep object.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public DissertationStepEdit addDissertationStep(String site, String type)
		throws PermissionException;
	
	/**
	* Add a new DissertationStep to the directory, from a definition in XML.
	* Must commitEdit() to make official, or cancelEdit() when done!
	* @param el The XML DOM Element defining the DissertationStep.
	* @return A locked DissertationStepEdit object (reserving the id).
	* @exception IdInvalidException if the DissertationStep id is invalid.
	* @exception IdUsedException if the DissertationStep id is already used.
	* @exception PermissionException if the current user does not have permission to add an AssignnmentStep.
	*/
	public DissertationStepEdit mergeDissertationStep(Element el)
		throws IdInvalidException, IdUsedException, PermissionException;

	/** 
	* Removes a DissertationStep
	* @param step - the DissertationStep to remove.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public void removeDissertationStep(DissertationStepEdit step)
		throws PermissionException;
	
	/**
	* Access the DissertationStep with the specified reference.
	* @param stepReference - The reference of the DissertationStep.
	* @return The DissertationStep corresponding to the reference, or null if it does not exist.
	* @throws IdUnusedException if there is no object with this reference.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public DissertationStep getDissertationStep(String stepReference)
		throws IdUnusedException, PermissionException;

	/**
	* Get a locked DissertationStep object for editing. Must commitEdit() to make official, or cancelEdit() when done!
	* @param stepReference The step reference string.
	* @return a DissertationStepEdit object for editing.
	* @exception IdUnusedException if not found, or if not a DissertationStepEdit object
	* @exception PermissionException if the current user does not have permission to edit this step.
	* @exception InUseException if the DissertationStep object is locked by someone else.
	*/
	public DissertationStepEdit editDissertationStep(String stepReference)
		throws IdUnusedException, InUseException, PermissionException;
		
	/**
	* Commit the changes made to a DissertationStepEdit object, and release the lock.
	* The DissertationStepEdit is disabled, and not to be used after this call.
	* @param dissertation The DissertationStepEdit object to commit.
	*/
	public void commitEdit(DissertationStepEdit step);

	/**
	* Cancel the changes made to a DissertationStepEdit object, and release the lock.
	* The DissertationStepEdit is disabled, and not to be used after this call.
	* @param dissertation The DissertationStepEdit object to commit.
	*/
	public void cancelEdit(DissertationStepEdit step);
		
	/** 
	* Adds an CandidatePath to the service.
	* @param dissertation - The parent Dissertation.
	* @param site - The site for which permissions are being checked.
	* @return The new CandidatePath.
	* @throws PermissionException if the current User does not have permission to do this.
	*/
	public CandidatePathEdit addCandidatePath(Dissertation dissertation, String site)
		throws PermissionException;
	
	/**
	* Add a new CandidatePath to the directory, from a definition in XML.
	* Must commitEdit() to make official, or cancelEdit() when done!
	* @param el The XML DOM Element defining the CandidatePath.
	* @return A locked CandidatePathEdit object (reserving the id).
	* @exception IdInvalidException if the CandidatePath id is invalid.
	* @exception IdUsedException if the CandidatePath id is already used.
	* @exception PermissionException if the current user does not have permission to add a CandidatePath.
	*/
	public CandidatePathEdit mergeCandidatePath(Element el)
		throws IdInvalidException, IdUsedException, PermissionException;

	/** 
	* Removes an CandidatePath and all references to it
	* @param path - the CandidatePath to remove.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public void removeCandidatePath(CandidatePathEdit path)
		throws PermissionException;
	
	/**
	* Access the CandidatePath with the specified reference.
	* @param pathReference - The reference of the CandidatePath.
	* @return The CandidatePath corresponding to the reference, or null if it does not exist.
	* @throws IdUnusedException if there is no object with this reference.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public CandidatePath getCandidatePath(String candidatePathReference)
		throws IdUnusedException, PermissionException;

	/**
	* Get a locked CandidatePath object for editing. Must commitEdit() to make official, or cancelEdit() when done!
	* @param reference The CandidatePath reference string.
	* @return An CandidatePathEdit object for editing.
	* @exception IdUnusedException if not found, or if not an CandidatePathEdit object
	* @exception PermissionException if the current user does not have permission to edit this CandidatePath.
	* @exception InUseException if the CandidatePath object is locked by someone else.
	*/
	public CandidatePathEdit editCandidatePath(String candidatePathReference)
		throws IdUnusedException, InUseException, PermissionException;
	
	/**
	* Commit the changes made to a CandidatePathEdit object, and release the lock.
	* The CandidatePathEdit is disabled, and not to be used after this call.
	* @param path The CandidatePathEdit object to commit.
	*/
	public void commitEdit(CandidatePathEdit path);

	/**
	* Cancel the changes made to a CandidatePathEdit object, and release the lock.
	* The CandidatePathEdit is disabled, and not to be used after this call.
	* @param path The CandidatePathEdit object to commit.
	*/
	public void cancelEdit(CandidatePathEdit path);
	
	/** 
	* Adds an StepStatus to the service.
	* @param site - The site for which permissions are being checked.
	* @param step - The parent DissertationStep.
	* @param oardStep - Is this step auto-validated from Rackham's database?
	* @return The new StepStatus.
	* @throws PermissionException if the current User does not have permission to do this.
	*/
	public StepStatusEdit addStepStatus(String site, DissertationStep parent, boolean oradStep)
		throws PermissionException;

	/** 
	* Adds an StepStatus to the service.
	* @param site - The site for which permissions are being checked.
	* @return The new StepStatus.
	* @throws PermissionException if the current User does not have permission to do this.
	*/
	public StepStatusEdit addStepStatus(String site)
		throws PermissionException;

	/**
	* Add a new StepStatus to the directory, from a definition in XML.
	* Must commitEdit() to make official, or cancelEdit() when done!
	* @param el The XML DOM Element defining the StepStatus.
	* @return A locked StepStatusEdit object (reserving the id).
	* @exception IdInvalidException if the StepStatus id is invalid.
	* @exception IdUsedException if the StepStatus id is already used.
	* @exception PermissionException if the current user does not have permission to add a StepStatus.
	*/
	public StepStatusEdit mergeStepStatus(Element el)
		throws IdInvalidException, IdUsedException, PermissionException;

	/** 
	* Removes a StepStatus and all references to it
	* @param status - the StepStatus to remove.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public void removeStepStatus(StepStatusEdit status)
		throws PermissionException;
	
	/**
	* Access the StepStatus with the specified reference.
	* @param pathReference - The reference of the StepStatus.
	* @return The StepStatus corresponding to the reference, or null if it does not exist.
	* @throws IdUnusedException if there is no object with this id.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public StepStatus getStepStatus(String stepStatusReference)
		throws IdUnusedException, PermissionException;

	/**
	* Get a locked StepStatus object for editing. Must commitEdit() to make official, or cancelEdit() when done!
	* @param reference The StepStatus reference string.
	* @return An StepStatusEdit object for editing.
	* @exception IdUnusedException if not found, or if not an StepStatusEdit object
	* @exception PermissionException if the current user does not have permission to edit this StepStatus.
	* @exception InUseException if the StepStatus object is locked by someone else.
	*/
	public StepStatusEdit editStepStatus(String stepStatusReference)
		throws IdUnusedException, InUseException, PermissionException;

	/**
	* Commit the changes made to a StepStatusEdit object, and release the lock.
	* The StepStatusEdit is disabled, and not to be used after this call.
	* @param status The StepStatusEdit object to commit.
	*/
	public void commitEdit(StepStatusEdit status);

	/**
	* Cancel the changes made to a StepStatusEdit object, and release the lock.
	* The StepStatusEdit is disabled, and not to be used after this call.
	* @param status The StepStatusEdit object to commit.
	*/
	public void cancelEdit(StepStatusEdit status);
	
	/** 
	* Adds an CandidateInfo to the service.
	* @param site - The site for which permissions are being checked.
	* @return The new CandidateInfo.
	* @throws PermissionException if the current User does not have permission to do this.
	*/
	public CandidateInfoEdit addCandidateInfo(String site)
		throws PermissionException;

	/** 
	* Adds an CandidateInfo to the service.
	* @param site - There is no current site - request comes from DissertationDataListenerService.
	* @return The new CandidateInfo.
	*/
	public CandidateInfoEdit addCandidateInfoFromListener(String site);
	
	/**
	* Add a new CandidateInfo to the directory, from a definition in XML.
	* Must commitEdit() to make official, or cancelEdit() when done!
	* @param el The XML DOM Element defining the CandidateInfo.
	* @return A locked CandidateInfoEdit object (reserving the id).
	* @exception IdInvalidException if the CandidateInfo id is invalid.
	* @exception IdUsedException if the CandidateInfo id is already used.
	* @exception PermissionException if the current user does not have permission to add a CandidateInfo.
	*/
	public CandidateInfoEdit mergeCandidateInfo(Element el)
		throws IdInvalidException, IdUsedException, PermissionException;

	/** 
	* Removes an CandidateInfo and all references to it
	* @param info - the CandidateInfo to remove.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public void removeCandidateInfo(CandidateInfoEdit status)
		throws PermissionException;
	
	/**
	* Access the CandidateInfo with the specified reference.
	* @param infoReference - The reference of the CandidateInfo.
	* @return The CandidateInfo corresponding to the reference, or null if it does not exist.
	* @throws IdUnusedException if there is no object with this reference.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public CandidateInfo getCandidateInfo(String CandidateInfoReference)
		throws IdUnusedException, PermissionException;
		
	/**
	* Access the CandidateInfo for the specified candidate.
	* @param candidate - The Chef id of the candidate.
	* @return The CandidateInfo corresponding to the Chef id, or null if it does not exist.
	* @throws IdUnusedException if there is no object with this reference.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public CandidateInfo getInfoForCandidate(String candidate)
		throws IdUnusedException, PermissionException;

	/**
	* Access the CandidateInfo for the specified user.
	* @param emplid - The emplid of the user.
	* @return The CandidateInfo corresponding to the emplid, or null if it does not exist.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public CandidateInfoEdit getCandidateInfoEditForEmplid(String emplid)
		throws PermissionException;

	/**
	* Get a locked CandidateInfo object for editing. Must commitEdit() to make official, or cancelEdit() when done!
	* @param reference The CandidateInfo reference string.
	* @return An CandidateInfoEdit object for editing.
	* @exception IdUnusedException if not found, or if not an CandidateInfoEdit object
	* @exception PermissionException if the current user does not have permission to edit this CandidateInfo.
	* @exception InUseException if the CandidateInfo object is locked by someone else.
	*/
	public CandidateInfoEdit editCandidateInfo(String CandidateInfoReference)
		throws IdUnusedException, InUseException, PermissionException;

	/**
	* Commit the changes made to a CandidateInfoEdit object, and release the lock.
	* The CandidateInfoEdit is disabled, and not to be used after this call.
	* @param info The CandidateInfoEdit object to commit.
	*/
	public void commitEdit(CandidateInfoEdit info);

	/**
	* Cancel the changes made to a CandidateInfoEdit object, and release the lock.
	* The CandidateInfoEdit is disabled, and not to be used after this call.
	* @param info The CandidateInfoEdit object to commit.
	*/
	public void cancelEdit(CandidateInfoEdit info);
	
	/** 
	* Access the list of Block Grant Groups.
	* @return The list of Block Grant Groups.
	*/
	public List getBlockGrantGroups();
	
	/** 
	* Access the Block Grant Group to which a Field of Study belongs.
	* @param FOSId - The code of the Field of Study for which Block Grant Group is gotten.
	* @return The Block Grant Group.
	*/
	public BlockGrantGroup getBlockGrantGroupForFieldOfStudy(String FOSId)
		throws PermissionException;
	
	/** 
	* Remove the Block Grant Group and all references to it.
	* @param BGGedit - The Block Grant Group edit to remove.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public void removeBlockGrantGroup(BlockGrantGroupEdit BGGedit)
		throws PermissionException;

	/** 
	* Get the List of CandidatePaths assiciated with a parent (department) site.
	* @param site - The site id.
	* @return The List of CandidatePath objects.
	*/
	public List getCandidatePathsForParentSite(String site);

	/**
	* Access the CandidatePath for the specified candidate.
	* @param candidateId The CHEF user id of the candidate.
	* @return The CandidatePath corresponding to the candidate, or null if it does not exist.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public CandidatePath getCandidatePathForCandidate(String candidateId)
		throws PermissionException;

	/**
	* Access the CandidatePath for the specified site.
	* @param siteId The CHEF site id.
	* @return The CandidatePath corresponding to the site, or null if it does not exist.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public CandidatePath getCandidatePathForSite(String siteId)
		throws PermissionException;
	
	/**
	* Access the Dissertation associated with a site.
	* @param site - The site id.
	* @return Dissertation associated with a site, or null if one does not exist.
	*/
	public Dissertation getDissertationForSite(String site)
		throws IdUnusedException, PermissionException;
		
	/**
	* Access the Dissertation associated with a site by type.
	* @param site - The site id.
	* @param type - The pedagogical approach reflected in the steps.
	* @return Dissertation associated with a site, or null if one does not exist.
	*/
	public Dissertation getDissertationForSite(String site, String type)
		throws IdUnusedException, PermissionException;

	/**
	* Access all dissertation objects - known to us (not from external providers).
	* @return A list of dissertation objects.
	*/
	public List getDissertations();

	/**
	* Access all DissertationStep objects - known to us (not from external providers).
	* @return A list of DissertationStep objects.
	*/
	public List getDissertationSteps();
	
	/**
	* Access all CandidatePath objects - known to us (not from external providers).
	* @return A list of CandidatePath objects.
	*/
	public List getCandidatePaths();
	
	/** 
	* Access whether any active CandidatePaths exist.
	* @return True if a CandidateInfo exists, false if not.
	*/
	public boolean getActivePaths();
	
	/** 
	* Access whether a department site has any active CandidatePaths associated with it.
	* @param site The site id.
	* @return True if a CandidateInfo exists for this parent site, false if not.
	*/
	public boolean getActivePathsForSite(String site);

	/** 
	* Access the list of candidates with the parent department site.
	* @param site The site id.
	* @param type The dissertation steps type or null if not the school site.
	* @return The list of users with the parent site.
	*/
	public User[] getAllUsersForSite(String site, String type);
	
	/**
	* Check whether there exists a CandidatePath having candidate attribute (uniqname)
	* for which corresponding sort name’s first letter of the last name matches the letter.
	* @param type - The type of CandidatePath (e.g, “Dissertation Steps”, “Dissertation
	* Steps: Music Performance”)
	* @param letter – A letter of the alphabet, A-Z,a-z.
	* @return True if there exists such a CandidatePath, false otherwise.
	*/
	public boolean isUserOfTypeForLetter(String type, String letter);
	
	/**
	* Check whether there exists a CandidatePath with this parent site
	* for which corresponding sort name’s first letter of the last name matches the letter.
	* @param parent - The parent site for the CandidatePath.
	* @param letter – A letter of the alphabet, A-Z,a-z.
	* @return True if there exists such a CandidatePath, false otherwise.
	*/
	public boolean isUserOfParentForLetter(String parent, String letter);
	
	/**
	* Access in Sort Name order the student User objects for which CandidatePath candidate 
	* attribute (uniqname) has a Sort Name starting with this letter and with this parent department site.
	* @param parentSite - The parent department site from which candidate path inherits steps.
	* @param letter – A letter of the alphabet, A-Z,a-z.
	* @return The List of Users, or null if no such Users exist.
	*/
	public List getSortedUsersOfParentForLetter(String parentSite, String letter);
	
	
	/**
	* Access in Sort Name order the student User objects for which CandidatePath candidate 
	* attribute (uniqname) has a Sort Name starting with this letter.
	* @param type - The CandidatePath type (e.g., “Dissertation Steps”, “Dissertation Steps: Music Performance”.
	* @param letter – A letter of the alphabet, A-Z,a-z.
	* @return The List of Users, or null if no such Users exist.
	*/
	public List getSortedUsersOfTypeForLetter(String type, String letter);
	
	/**
	* Access the CandidatePaths of this type (e.g, “Dissertation Steps”, “Dissertation Steps: Music Performance”)
	* @param type - The CandidatePath type.
	* @return The CandidatePaths of this type, or null if none exist.
	*/
	public List getCandidatePathsOfType(String type);

	
	/**
	* Access Dissertation objects of this type (e.g, “Dissertation Steps”, “Dissertation Steps: Music Performance”)
	* @param type - The Dissertation type.
	* @return The Dissertations of this type, or null if none exist.
	*/
	public List getDissertationsOfType(String type);
	
	/**
	* Check whether a CandidatePath of this type exists.
	* @param type - The CandidatePath type (e.g, “Dissertation Steps”, “Dissertation Steps: Music Performance”)
	* @return True if such a CandidatePath exists, false if not.

	*/
	public boolean isCandidatePathOfType(String type);


	/** 
	* Access whether this user is a candidate.
	* @param userId The user's CHEF id.
	* @return True if a CandidateInfo exists for this user, false if not.
	*/
	public boolean isCandidate(String userId);
	
	/** 
	* Access whether this user is a Music Performance candidate.
	* @param userId The user's CHEF id.
	* @return True if CandidateInfo getProgram() equals Music Performance program, false if not.
	*/
	public boolean isMusicPerformanceCandidate(String userId);
	
	/** 
	* Send in a load of data from the Rackham database.
	* @param data Vector of CandidateInfo objects.
	*/
	public List dumpData(Vector v);
	
	/** 
	* Access whether the db-integration system is initialized.
	* @return True if it's initialized, false if not.
	*/
	public boolean isInitialized();
	
	/** 
	* Access the Rackham program id for Rackham field of study id.
	* @return The program id.
	*/
	public String getProgram(String fos);
	
	/**
	* Get properties of a Rackham data file.
	* @param content - The content for which properties are requested.
	* @return Hashtable of property name(s) and value(s).
	*/
	public Hashtable getDataFileProperties(byte[] content);
	
	/**
	* Load the Rackham database extract files.
	* @param oard - The content of the OARD extract file.
	* @param mp - The content of the MPathways extract file.
	* @return Vector of String messages contaning validation or loading errors.
	*/
	public List loadData(byte[] oard, byte[] mp);
	
	/**
	* See if Rackham extract data loading is in progress.
	* @return boolean - true if in progress, false otherwise.
	*/
	public boolean isLoading();


}	// DissertationService

/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/DissertationService.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
*
**********************************************************************************/
