/**********************************************************************************
 * $URL$
 * $Id$
 **********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005 The Regents of the University of Michigan, Trustees of Indiana University,
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

package org.sakaiproject.component.app.dissertation;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.sakaiproject.api.app.dissertation.CandidateInfoEdit;
import org.sakaiproject.api.app.dissertation.CandidatePath;
import org.sakaiproject.api.app.dissertation.CandidatePathEdit;
import org.sakaiproject.api.app.dissertation.Dissertation;
import org.sakaiproject.api.app.dissertation.DissertationEdit;
import org.sakaiproject.api.app.dissertation.DissertationStep;
import org.sakaiproject.api.app.dissertation.DissertationStepEdit;
import org.sakaiproject.api.app.dissertation.cover.DissertationService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.util.Web;

/**
 * <p>
 * Make a DissertationStep change in the current Dissertation and 
 * all Dissertations and CandidatePaths derived from the current 
 * Dissertation. Step changes are: New, Revise, Move, Delete.
 * This is set up to use RAM store so object serialization is
 * not a concern, and non-stateful so concerrent execution is not
 * handled through Quartz but rather outside Quartz through a lock
 * in the Sakai locks table.
 * </p>
 * 
 * @author University of Michigan, Sakai Software Development Team
 * @version $Revision: $
 */
public class StepChangeJobImpl implements StepChangeJob 
{
	private static final Log m_logger = LogFactory.getLog(StepChangeJobImpl.class);
	private String m_jobType = null;
	private String jobName = null;
	private StringBuffer buf = new StringBuffer();
	private JobDetail jobDetail = null;
	private JobDataMap dataMap = null;
	private JobAnnouncement announcement = null;
	private Calendar cal = Calendar.getInstance();
	private SimpleDateFormat formatter = new SimpleDateFormat(DissertationService.STEP_JOB_DATE_FORMAT);
	private static final String NEWLINE = Web.escapeHtmlFormattedText("<br/>");
	
	//execution parameters
	private Boolean retroactive = null;
	private boolean m_retro = false;
	private String m_dissRef = null;
	private String m_currentSite = null;
	private String m_currentUser = null;
	private String m_schoolSite = null;
	private String m_location = null;
	private String m_instructionsText = null;
	private String m_validType = null;
	private String m_autoValid = null;
	private String m_section = null;
	private String m_stepRef = null;
	private String[] m_stepRefs;
	private DissertationStep m_before = null;
	private List m_previousStepRefs;
	
	//counter
	int numUpdated = 0;
	int thisStep = 0;
	
	//school site
	boolean school = false;
	
	//the root checklist being changed
	Dissertation dissertation = null;

	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	public void execute(JobExecutionContext context) throws JobExecutionException 
	{
		//execute() is the main method of a Quartz job
		try
		{
			//Spring injection of Logger was getting lost when Quartz instantiated job
			//m_logger = org.sakaiproject.service.framework.log.cover.Logger.getInstance();
			if(m_logger == null)
				System.out.println(this + ".execute() couldn't get a logger");
			
			//TODO set user to current user
			Session s = SessionManager.getCurrentSession();
			if (s != null)
				s.setUserId("admin");
			else
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".execute() could not setUserId to admin");
			
			//get the job detail
			jobDetail = context.getJobDetail();
			jobName = jobDetail.getName();
			dataMap = jobDetail.getJobDataMap();
			if(jobDetail != null)
			{
				//get the job type
				m_jobType = (String)dataMap.get("JOB_TYPE");
				if(m_jobType == null || m_jobType.equals(""))
					if(m_logger.isWarnEnabled())
						m_logger.warn(this + ".execute() job type not set");
			}
			else
			{
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".execute() jobDetail is null");
			}
			
			//make sure we can report on the job's execution
			announcement = new JobAnnouncement(jobDetail);
			if(announcement == null)
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".execute() could not instantiate JobAnnouncement");
			
			//set job execution parameters
			if(!setJobExecutionParameters(context))
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".execute() could not get all required job execution parameters");
			
			//get the root checklist being changed
			dissertation = DissertationService.getDissertation(m_dissRef);
			
			//this may be a school or department change
			if(dissertation.getSite().equals(DissertationService.getSchoolSite()))
				school = true;
			
			//note job is starting
			buf.append(getTime() + " JOB NAME: " + jobName + " - START" + NEWLINE);
			if(m_logger.isInfoEnabled())
				m_logger.info(getTime() + " " + jobName + " - START");
			
			//dispatch based on job type
			if(m_jobType.equals("New"))
				doNew();
			else if(m_jobType.equals("Revise"))
				doRevise();
			else if(m_jobType.equals("Move"))
				doMove();
			else if(m_jobType.equals("Delete"))
				doDelete();
			else
			{
				if(m_logger.isWarnEnabled())
				{
					m_logger.warn(this + ".execute() job type unrecognized");
					if(m_jobType != null)
						m_logger.warn(this + ".execute() job type unrecognized");
				}
			}
		}
		catch(Exception e)
		{
			//note exception in job execution report
			buf.append(getTime() + " JOB NAME: " + jobName + " exception - " + NEWLINE + e  + NEWLINE);
			
			if(m_logger.isWarnEnabled())
				m_logger.warn(getTime() + " " + jobName + " exception - " + e  + NEWLINE);
		}
		finally
		{
			//note that job is done
			buf.append(getTime() + " JOB NAME: " + jobName + " - DONE" + NEWLINE);
			
			if(m_logger.isInfoEnabled())
				m_logger.info(getTime() + " " + jobName + " - DONE");
			
			//send report of job execution to Announcements
			String announce = buf.toString();
			try
			{
				if(announce != null && !announce.equals(""))
					announcement.addAnnouncementMessage(announce);
			}
			catch(Exception e)
			{
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".execute() addAnnouncementMessage " + e);
			}
			
			//clean up db lock at the end
			try
			{
				CandidateInfoEdit lock = DissertationService.editCandidateInfo(DissertationService.IS_CHANGING_STEP_LOCK_REFERENCE);
				if(lock != null && lock.isActiveEdit())
					DissertationService.removeCandidateInfo(lock);
			}
			catch(Exception e)
			{
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".execute() removeCandidateInfo(lock) " + e);
				
				buf.append(getTime() + " JOB NAME: " + jobName + " exception removing lock " + e + NEWLINE);
			}
		}
	}//execute
	
	public void doNew()
	{
		Dissertation dissertation = null;
		DissertationEdit dissEdit = null;
		CandidatePath path = null;
		CandidatePathEdit pathEdit = null;
		DissertationStepEdit stepEdit = null;
		List allDissertations = null;
		List allPaths = null;
		try
		{
			//note job has started removing prerequisite references
			buf.append(getTime() + " USER: " + m_currentUser + NEWLINE);
			
			//get the step to add
			stepEdit = DissertationService.editDissertationStep(m_stepRef);
			
			//identify the step being added
			buf.append(getTime() + " ADDED STEP: '" + stepEdit.getInstructionsText() + "'" + NEWLINE);
			
			dissertation = DissertationService.getDissertation(m_dissRef);
			
			//school step needs to be added to other dissertations of this type
			if(school)
			{
				allDissertations = DissertationService.getDissertationsOfType(dissertation.getType());
				try
				{
					//remove the current Dissertation, to which step was added earlier
					if(allDissertations.contains(dissertation))
						allDissertations.remove(dissertation);
				}
				catch(Exception e)
				{
					//add exception to job report
					buf.append(getTime() + " JOB NAME: " + jobName + " remove current dissertation from Collection - " +
							NEWLINE + e + NEWLINE);
					if(dissertation != null)
						buf.append("Dissertation : " + dissertation.getReference() + NEWLINE);
					
					if(m_logger.isWarnEnabled())
						m_logger.warn(this + ".doNew() " + dissertation.getReference() + e.toString());
				}
				
				//for all Dissertations of this type
				for(int x = 0; x < allDissertations.size(); x++)
				{
					dissertation = (Dissertation)allDissertations.get(x);
					try
					{
						dissEdit = DissertationService.editDissertation(dissertation.getReference());
						dissEdit.addStep(stepEdit, m_location);
						DissertationService.commitEdit(dissEdit);
					}
					catch(Exception e)
					{
						if(dissEdit != null && dissEdit.isActiveEdit())
							DissertationService.cancelEdit(dissEdit);
						
						//add exception to job report
						buf.append(getTime() + " JOB NAME: " + jobName + " School change - " + NEWLINE + e + NEWLINE);
						if(dissEdit != null)
							buf.append("Dissertation Edit Reference: " + dissEdit.getReference() + NEWLINE);
						
						if(m_logger.isWarnEnabled())
							m_logger.warn(this + ".doNew() School change - dissertation id " + dissEdit.getId() + " " + e);
						
						//keep going
						continue;
					}
				}
			}//school
			
			//if the change is retroactive, add the step to appropriate paths
			if(m_retro)
			{
				//get the paths that inherit steps from the current dissertation
				if(school)
					allPaths = DissertationService.getCandidatePathsOfType(dissertation.getType());
				else
					allPaths = DissertationService.getCandidatePathsForParentSite(dissertation.getSite());
				
				//note that job is adding step to paths
				buf.append(getTime() + " NUMBER: " + "adding step to " + allPaths.size() + " candidate paths" + NEWLINE);
				
				if(m_logger.isInfoEnabled())
					m_logger.info(this + ".doNew() adding step to " + allPaths.size() + " candidate paths");
				
				numUpdated = 0;
				
				//add step to all paths of the same type as current dissertation
				for(int y = 0; y < allPaths.size(); y++)
				{
					//apply to each path
					path = (CandidatePath)allPaths.get(y);
					try
					{
						pathEdit = DissertationService.editCandidatePath(path.getReference());
						if(pathEdit != null)
						{
							pathEdit.liveAddStep(stepEdit, m_previousStepRefs, m_currentSite);
							DissertationService.commitEdit(pathEdit);
							allPaths.set(y, null);
							numUpdated++;
						}
						else
						{
							if(m_logger.isWarnEnabled())
								m_logger.warn(this + ".doNew() exception - path was null " + y);
							
							//note exception in job report
							buf.append(getTime() + " EXCEPTION: " + "- candidate path was null " + y  + NEWLINE);
							
							//keep going
							continue;
						}
						
						//reset the current session.inactive time every 100 paths
						//TODO remove?
						if((numUpdated % 100) == 0)
						{
							Session session = SessionManager.getCurrentSession();
							session.setActive();
						}
						
						//log progress updating paths every 500 paths
						if((numUpdated % 500) == 0)
							if(m_logger.isInfoEnabled())
								m_logger.info(this + ".doNew() " + numUpdated + " paths retroactively updated");
					}
					catch (Exception e)
					{	
						if(pathEdit != null && pathEdit.isActiveEdit())
							DissertationService.cancelEdit(pathEdit);
						
						//note exception in job report
						buf.append(getTime() + " JOB NAME: " + jobName + " exception - path edit "+ NEWLINE + e + NEWLINE);
						if(pathEdit != null)
							buf.append("path edit " + pathEdit.getId() + NEWLINE);
						
						if(m_logger.isWarnEnabled())
							m_logger.warn(this + ".doNew() exception - path id " + pathEdit.getId() + " " + e);
						
						//keep going
						continue;
					}
				}//for all paths of the same type
				
				//note number of paths updated in job report
				buf.append(getTime() + " AT END: " + "step added to  " + numUpdated + " candidate paths" + NEWLINE);
				
				if(m_logger.isInfoEnabled())
					m_logger.info(this + ".doNew() step added to " + numUpdated + " candidate paths.");
				
			}//m_retro
		
		}
		catch(Exception e)
		{
			//note job exception in report
			buf.append(getTime() + " JOB NAME: " + jobName + " EXCEPTION: " + NEWLINE + e  + NEWLINE);
			
			if(m_logger.isWarnEnabled())
				m_logger.warn(getTime() + " " + jobName + " - " + NEWLINE + e  + NEWLINE);
		}
		finally
		{
			if(stepEdit!=null && stepEdit.isActiveEdit())
				DissertationService.cancelEdit(stepEdit);
		}
		
	}//doNew
	
	public void doRevise()
	{
		//initialize
		CandidatePath tempPath = null;
		CandidatePathEdit pathEdit = null;
		DissertationStepEdit stepEdit = null;
		List allPaths = null;
		Dissertation dissertation = null;
		
		//TODO get disstype from current dissertation instead
		try
		{
			//no need to change dissertations, which contain reference to step
			
			//if the change is retroactive, revise step in candidate paths
			if(m_retro)
			{
				numUpdated = 0;
				
				//note job has started removing prerequisite references
				buf.append(getTime() + " USER: " + m_currentUser + NEWLINE);
				
				
				//get the revised step from the service
				stepEdit = DissertationService.editDissertationStep(m_stepRef);
				
				//identify the step being revised
				buf.append(getTime() + " REVISED STEP: '" + stepEdit.getInstructionsText() + "'" + NEWLINE);
				
				//get the type/site for the current dissertation
				dissertation = DissertationService.getDissertation(m_dissRef);
				
				//get the paths that inherit steps from the current dissertation
				if(school)
					allPaths = DissertationService.getCandidatePathsOfType(dissertation.getType());
				else
					allPaths = DissertationService.getCandidatePathsForParentSite(dissertation.getSite());
				
				//note number of paths being updated
				buf.append(getTime() + " NUMBER: revising step in " + allPaths.size() + " paths"  + NEWLINE);
				
				if(m_logger.isInfoEnabled())
					m_logger.info(this + ".doRevise() revising step in " + allPaths.size() + " paths");
	
				//revise step in all paths of the same type as current dissertation
				for(int y =  0; y < allPaths.size(); y++)
				{
					//revise in each path
					tempPath = (CandidatePath)allPaths.get(y);
					try
					{
						pathEdit = DissertationService.editCandidatePath(tempPath.getReference());
						pathEdit.liveUpdateStep(m_before, stepEdit);
						DissertationService.commitEdit(pathEdit);
						numUpdated++;
						
						//reset the current session.inactive time periodically
						if((numUpdated % 100) == 0)
						{
							Session session = SessionManager.getCurrentSession();
							session.setActive();
						}
						
						//log progress updating paths every 500 paths
						if((numUpdated % 500) == 0)
							if(m_logger.isInfoEnabled())
								m_logger.info(this + ".doRevise() " + numUpdated + 
										" paths updated so far");
					}
					catch (Exception e)
					{
						if(pathEdit != null && pathEdit.isActiveEdit())
							DissertationService.cancelEdit(pathEdit);
						
						//note exception in job report
						buf.append(getTime() + " " + jobName + " - error - dissertation id " + NEWLINE + e  + NEWLINE); 
						if(pathEdit != null)
							buf.append("path edit "+ pathEdit.getId() + NEWLINE);
						
						if(m_logger.isWarnEnabled())
							m_logger.warn(this + ".doRevise() dissertation id " + pathEdit.getId() + " " + e);
						
						//keep going
						continue;
					}
				}
				
				//note number of paths updated in job report
				buf.append(getTime() + " AT END: " + "step revised in  " + numUpdated + " candidate paths" + NEWLINE);
			}
		}
		catch(Exception e)
		{
			//note exception in job report
			buf.append(getTime() + " " + jobName + " - exception while revising step - " + NEWLINE + e + NEWLINE);
			if(m_logger.isWarnEnabled())
				m_logger.warn(this + ".doRevise() exception while revising step - " + e);
			
		}
		finally
		{
			if(stepEdit != null && stepEdit.isActiveEdit())
				DissertationService.cancelEdit(stepEdit);
		}
		
	}//doRevise
	
	public void doMove()
	{
		//initialize
		
		String previousStepPosition = null;
		Dissertation tempDissertation = null;
		DissertationEdit dissEdit = null;
		CandidatePath tempPath = null;
		CandidatePathEdit pathEdit = null;
		DissertationStep step = null;
		DissertationStepEdit stepEdit = null;
		List allDissertations = null;
		List allPaths = null;
		try
		{
			//identify the user
			buf.append(getTime() + " USER: " + m_currentUser + NEWLINE);
			
			//identify the step
			buf.append(getTime() + " MOVED STEP: '" + DissertationService.getDissertationStep(m_stepRef).getInstructionsText() + "'" + NEWLINE);
			
			//school steps have an m_section, department steps do not
			if(school)
			{
				try
				{
					//get the step being moved
					stepEdit = DissertationService.editDissertationStep(m_stepRef);
					
					//edit the m_section of the step being moved
					stepEdit.setSection(DissertationService.getSectionId(m_section));
					DissertationService.commitEdit(stepEdit);
				}
				catch(Exception e)
				{
					if(stepEdit != null && stepEdit.isActiveEdit())
						DissertationService.cancelEdit(stepEdit);
					if(m_logger.isWarnEnabled())
						m_logger.warn(this + ".doMove() setSection() " + e);
					
					//note exception in job report
					buf.append(getTime() + " JOB NAME: " + jobName + " exception - step id " + e + NEWLINE); 
					if(stepEdit != null)
						buf.append("Step Edit Reference: " + stepEdit.getReference() + NEWLINE);
				}
			}
			
			//remove current dissertation, then move step in all other dissertations of this type
			Dissertation dissertation = DissertationService.getDissertation(m_dissRef);
			if(school)
			{
				allDissertations = DissertationService.getDissertationsOfType(dissertation.getType());
				
				//remove current dissertation, which was changed earlier
				if(allDissertations.contains(dissertation))
					allDissertations.remove(dissertation);
				
				//for all others
				for(int x = 0; x < allDissertations.size(); x++)
				{
					tempDissertation = (Dissertation)allDissertations.get(x);
					try
					{
						//move step in this dissertation
						dissEdit = DissertationService.editDissertation(tempDissertation.getReference());
						dissEdit.moveStep(m_stepRef, m_location);
						DissertationService.commitEdit(dissEdit);
					}
					catch (Exception e)
					{
						if(dissEdit != null && dissEdit.isActiveEdit())
							DissertationService.cancelEdit(dissEdit);
						
						//note exception in job report
						buf.append(getTime() + " JOB NAME: " + jobName + " exception - dissertation id " + NEWLINE + e + NEWLINE);
						if(dissEdit != null)
							buf.append("dissertation edit " + dissEdit.getId() + NEWLINE);
						
						if(m_logger.isWarnEnabled())
							m_logger.warn(this + ".doMove() " + e);
						
						//keep going
						continue;
					}
				}//all dissertations of this type
			}//school
			
			//move step in candidate paths (i.e., make a retroactive change)
			if(m_retro)
			{
				//get all paths that inherit steps from the current dissertation
				if(school)
					allPaths = DissertationService.getCandidatePathsOfType(dissertation.getType());
				else
					allPaths = DissertationService.getCandidatePathsForParentSite(dissertation.getSite());
				
				//note number of paths being updated
				buf.append(getTime() + " NUMBER: revising step in " + allPaths.size() + " paths"  + NEWLINE);
				
				if(m_logger.isInfoEnabled())
					m_logger.info(this + ".doMove() applying Move Step retroactivley to " + 
							allPaths.size() + " paths");
				
				numUpdated = 0;
				
				//get the step to move
				try
				{
					step = DissertationService.getDissertationStep(m_stepRef);
				}
				catch(Exception e)
				{
					//note exception in job report
					buf.append(getTime() + " JOB NAME: " + jobName + " exception - get step " +  NEWLINE + e + NEWLINE);
					if(step != null)
						buf.append("step " + step.getId() +  NEWLINE);
					
					if(m_logger.isWarnEnabled())
						m_logger.warn(this + ".doMove() "  + jobName + " " + e);
					
					//if we can't get the step
					throw new JobExecutionException(e);
				}
	
				//move step in all paths that inherit steps from the current dissertation
				for(int y = 0; y < allPaths.size(); y++)
				{
					//move step in this path 
					tempPath = (CandidatePath)allPaths.get(y);
					try
					{
						dissertation = DissertationService.getDissertation(m_dissRef);
						previousStepPosition = dissertation.getOrderForStep(m_location);
						pathEdit = DissertationService.editCandidatePath(tempPath.getReference());
						pathEdit.liveMoveStep(step, m_location, previousStepPosition);
						DissertationService.commitEdit(pathEdit);
						numUpdated++;
						
						//reset the current session.inactive time every 100 paths
						//TODO remove?
						if((numUpdated % 100) == 0)
						{
							Session session = SessionManager.getCurrentSession();
							session.setActive();
						}
						
						//log progress updating paths every 500 paths
						if((numUpdated % 500) == 0)
							if(m_logger.isInfoEnabled())
								m_logger.info(this + ".doMove() " + numUpdated 
										+ " paths updated so far");
					}
					catch (Exception e)
					{
						if(pathEdit != null && pathEdit.isActiveEdit())
							DissertationService.cancelEdit(pathEdit);
						
						//note exception in job report
						buf.append(getTime() + " EXCEPTION: " + NEWLINE + e + NEWLINE); 
						if(pathEdit != null)
							buf.append("Path Edit Reference: " + pathEdit.getReference() + NEWLINE);
						
						if(m_logger.isWarnEnabled())
							m_logger.warn(this + ".doMove() - path edit " + pathEdit.getId()  + " " + e.toString());
						
						//keep going
						continue;
					}
				}//for all paths
				
				//note number of candidate paths updated in report job
				buf.append(getTime() + " AT END: step moved in "+ numUpdated + " candidate paths." + NEWLINE);
				
				if(m_logger.isInfoEnabled())
					m_logger.info(this + ".doMove() step moved in " + numUpdated + " candidate paths.");
				
			}//m_retro
		}
		catch(Exception e)
		{
			//note exception in job report
			buf.append(getTime() + " JOB NAME: " + jobName + " - " + NEWLINE + e + NEWLINE);
			
			if(m_logger.isInfoEnabled())
				m_logger.info(getTime() + " " + jobName + " - " + e);
		}
		
	}//doMove
	
	public void doDelete()
	{
		DissertationEdit dissEdit = null;
		CandidatePath tmpPath = null;
		CandidatePathEdit pathEdit = null;
		DissertationStep aStep = null;
		DissertationStep tmpStep = null;
		DissertationStepEdit stepEdit = null;
		Dissertation currentDissertation = null;
		Dissertation dissertation = null;
		List allDissertations = null;
		List allPaths = null;
		try
		{
			//first, cycle through all steps and remove prerequisite references to these steps
			//TODO just get steps that HAVE prerequisites
			
			List allSteps = DissertationService.getDissertationSteps();
			
			//delete from prerequisite references
			List prereqs = null;
			
			//note job has started removing prerequisite references
			buf.append(getTime() + " USER: " + m_currentUser + NEWLINE);
			
			//for each step being deleted 
			for(int counter = 0; counter < m_stepRefs.length; counter++)
			{
				//for all steps
				for(int z = 0; z < allSteps.size(); z++)
				{
					aStep = (DissertationStep)allSteps.get(z);
					prereqs = aStep.getPrerequisiteStepReferences();
					
					//for each prerequsite of a step
					for(int x = 0; x < prereqs.size(); x++)
					{
						//if step has step being deleted as a prereq
						if(prereqs.get(x).equals(m_stepRefs[counter]))
						{
							try
							{
								stepEdit = DissertationService.editDissertationStep(m_stepRefs[counter]);
								
								//remove the to-be-deleted step as prereq
								stepEdit.removePrerequisiteStep(m_stepRefs[counter]);
								DissertationService.commitEdit(stepEdit);
							}
							catch(Exception e)
							{
								if(stepEdit != null && stepEdit.isActiveEdit())
									DissertationService.cancelEdit(stepEdit);
								
								//note exception in job report
								buf.append(getTime() + " JOB NAME: " + jobName + " - exception - step id " + NEWLINE + e + NEWLINE);
								if(stepEdit != null)
									buf.append("step edit "+ stepEdit.getId() + NEWLINE);
								
								if(m_logger.isWarnEnabled())
									m_logger.warn(this + ".doDelete() exception - step id " + stepEdit.getId() + " " + e);
								
								//keep going
								continue;
							}
						}
					}
				}
			}
			
			//delete from dissertations
			
			//for each step being deleted
			for(int x = 0; x < m_stepRefs.length; x++)
			{
				try
				{
					//a more intuitive number for messages
					thisStep = x + 1;
					
					//if more than one step is being deleted, note which one
					if(m_stepRefs.length > 1)
						buf.append(getTime() + " deleting step "  + thisStep  + NEWLINE);
					
					//identify the step being deleted
					buf.append(getTime() + " DELETED STEP: '" + DissertationService.getDissertationStep(m_stepRefs[x]).getInstructionsText() + "'" + NEWLINE);
					
					if(m_logger.isInfoEnabled())
						m_logger.info(this + ".doDelete() deleting step " + x);
					
					/** if this is the school, delete step from all department checklists 
					 *  of the same type. */
					currentDissertation = DissertationService.getDissertation(m_dissRef);
					if(currentDissertation.getSite().equals(DissertationService.getSchoolSite()))
					{
						try
						{
							//school - removing from all checklists of this type
							allDissertations = DissertationService.getDissertationsOfType(currentDissertation.getType());
							
							//step(s) removed from current Dissertation earlier
							if(allDissertations.contains(currentDissertation))
								allDissertations.remove(currentDissertation);
							
							//for all Dissertations of this type
							for(int y = 0; y < allDissertations.size(); y++)
							{
								dissertation = (Dissertation)allDissertations.get(y);
								dissEdit = DissertationService.editDissertation(dissertation.getReference());
								dissEdit.removeStep(m_stepRefs[x]);
								DissertationService.commitEdit(dissEdit);
							}
						}
						catch(Exception e)
						{
							if(dissEdit != null && dissEdit.isActiveEdit())
								DissertationService.cancelEdit(dissEdit);
							
							//note exception in job report
							buf.append(getTime() + " JOB NAME: " + jobName + " - error - dissertation id " + NEWLINE + e + NEWLINE);
							if(dissEdit != null)
								buf.append("Dissertation Edit Reference: "+ dissEdit.getReference() + NEWLINE);
							
							if(m_logger.isWarnEnabled())
								m_logger.warn(this + ".doDelete()  error - dissertation id " + dissEdit.getId() + " " + e);
							
							//keep going
							continue;
						}
						
						//get all candidate paths of this type
						if(m_retro)
							allPaths = DissertationService.getCandidatePathsOfType(currentDissertation.getType());
						
					}//school, delete step from all department checklist definitions
					else
					{
						//was removed from current Dissertation earlier
						
						//get all candidate paths under department
						if(m_retro)
							allPaths = DissertationService.getCandidatePathsForParentSite(currentDissertation.getSite());
					}//department, delete from current dissertation only
				
					/** delete from candidate paths */
					
					//delete step from all candidate paths having step (i.e., make a retroactive change)
					if(m_retro)
					{
						//note that job is deleting step from paths
						buf.append(getTime() + " NUMBER: deleting step from " + allPaths.size() + " candidate paths." + NEWLINE);
						
						if(m_logger.isInfoEnabled())
							m_logger.info(this + ".doDelete() deleting step from " + allPaths.size() + " paths");
						
						//for each path containing step to delete
						for(int v = 0; v < allPaths.size(); v++)
						{
							try
							{
								//remove step from path
								tmpPath = (CandidatePath)allPaths.get(v);
								tmpStep = DissertationService.getDissertationStep(m_stepRefs[x]);
								pathEdit = DissertationService.editCandidatePath(tmpPath.getReference());
								if(tmpStep != null && pathEdit != null)
								{
									pathEdit.liveRemoveStep(tmpStep);
									DissertationService.commitEdit(pathEdit);
									numUpdated++;
								}
								else
								{
									if(pathEdit == null)
									{
										if(m_logger.isWarnEnabled())
											m_logger.warn(this + ".doDelete() exception - path was null " + v);
										
										//note exception in job report
										buf.append(getTime() + " - exception - path was null " + v  + NEWLINE);
										
										//keep going
										continue;
									}
									else
									{
										if(m_logger.isWarnEnabled())
											m_logger.warn(this + ".doDelete() exception - step was null " + v);
										
										//note exception in job report
										buf.append(getTime() + " JOB NAME: " + jobName + " - exception - step was null " + v  + NEWLINE);
										
										//keep going
										continue;
									}
								}//path or step was null
								
								//reset the current session.inactive time periodically
								if((numUpdated % 100) == 0)
								{
									Session session = SessionManager.getCurrentSession();
									session.setActive();
								}
								
								//log progress periodically
								if((numUpdated % 500) == 0)
								{
									if(m_logger.isInfoEnabled())
										m_logger.info(this + ".doDelete() step(s) deleted from " + numUpdated + " candidate paths.");
								}
							}
							catch(Exception e)
							{
								if(pathEdit != null && pathEdit.isActiveEdit())
									DissertationService.cancelEdit(pathEdit);
								
								//note exception in job report
								buf.append(getTime() + " JOB NAME: " + jobName + " - exception - path id " + NEWLINE + e + NEWLINE);
								if(pathEdit != null)
									buf.append("Path Edit Reference: " + pathEdit.getReference() + NEWLINE);
								
								if(m_logger.isWarnEnabled())
									m_logger.warn(this + ".doDelete() exception - path id " + pathEdit.getId() + " " + e);
								
								//keep going
								continue;
							}
						}//for each path containing step to delete
						
						//note number of paths updated in report job
						buf.append(getTime() + " AT END: step deleted from " + numUpdated + " candidate paths." + NEWLINE);
						if(m_logger.isInfoEnabled())
							m_logger.info(this + ".doDelete() step deleted from " + numUpdated + " candidate paths.");
					}//retro
					
					stepEdit = DissertationService.editDissertationStep(m_stepRefs[x]);
					DissertationService.removeDissertationStep(stepEdit);
				}
				catch(Exception e)
				{
					//note exception in job report
					buf.append(getTime() + " JOB NAME: " + jobName + " - exception while deleting step - " + thisStep + " - " + NEWLINE + e + NEWLINE);
					if(m_logger.isWarnEnabled())
						m_logger.warn(this + ".doDelete() exception while deleting step " + x + " - " + e);
					
					//keep going
					continue;
				}
			}//for each step being deleted
		}
		catch(Exception e)
		{
			//note exception in job report
			buf.append(getTime() + " JOB NAME: " + jobName + " - exception while deleting step - " + NEWLINE + e + NEWLINE);
			if(m_logger.isWarnEnabled())
				m_logger.warn(this + ".doDelete() exception while deleting step - " + e);
		}
		
	}//doDelete
	
	protected boolean setJobExecutionParameters(JobExecutionContext context)
	{
		m_schoolSite = DissertationService.getSchoolSite();
		
		//get common job execution parameters from job data map
		retroactive = (Boolean)dataMap.get("RETROACTIVE");
		m_dissRef = (String)dataMap.get("DISSERTATION_REF");
		m_currentSite = (String)dataMap.get("CURRENT_SITE");
		m_currentUser = (String)dataMap.get("CURRENT_USER");
		m_retro =  retroactive.booleanValue();
		m_stepRef = (String)dataMap.get("STEP_REF");
		
		//if step ref was included in map, get it's step attributes
		if(m_stepRef != null)
		{
			try
			{
				DissertationStep step = DissertationService.getDissertationStep(m_stepRef);
				m_instructionsText = step.getInstructionsText();
				m_validType = step.getValidationType();
				m_autoValid = step.getAutoValidationId();
			}
			catch(Exception e)
			{
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".setJobExecutionParameters() " + jobName +
						" - set step attributes " + e);
				return false;
			}
		}
		
		//check that NOT NULL parameters are not null
		if(retroactive == null || m_dissRef == null ||
				m_currentSite == null)
		{
			if(m_logger.isWarnEnabled())
				m_logger.warn(this + ".setJobExecutionParameters() " + jobName +
					" - one or more required job execution parameters null");
			return false;
		}
		
		//set global flags
		m_retro = retroactive.booleanValue();
		school = m_currentSite.equals(m_schoolSite);
		
		//add other job execution parameters based on job type
		if(m_jobType.equals("New"))
		{
			m_location = (String)dataMap.get("LOCATION");
			m_previousStepRefs = (List)dataMap.get("PREVIOUS_STEP_REFS");

			//check that not-null parameters are not null
			if(m_schoolSite == null || m_location == null || m_autoValid == null
					|| m_stepRef == null || m_instructionsText == null 
					|| m_validType == null)
			{
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".setJobExecutionParameters() " + jobName + 
						" - one or more required job execution parameters null");
				return false;
			}
			return true;
		}
		else if(m_jobType.equals("Revise"))
		{
			m_section = (String)dataMap.get("SECTION");

			m_before = (DissertationStep)dataMap.get("BEFORE");
			if(m_before == null)
			{
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".setJobExecutionParameters() " + jobName + 
						" - (DissertationStep) m_before was null.");
				return false;
			}
			return true;
		}
		else if(m_jobType.equals("Move"))
		{
			m_location = (String)dataMap.get("LOCATION");
			m_section = (String)dataMap.get("SECTION");
			
			//check that not-null parameters are not null
			if(m_location == null || m_stepRef == null)
			{
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".setJobExecutionParameters() " + jobName + 
						" - one or more required job execution parameters null");
				return false;
			}
			return true;
		}
		else if(m_jobType.equals("Delete"))
		{
			m_stepRefs = (String[])dataMap.get("STEP_REFS");
			
			//check that NOT NULL parameters are not null
			if(m_stepRefs == null || m_stepRefs.length == 0)
			{
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".setJobExecutionParameters() " + jobName + 
						" - Step references null or zero length array");
				return false;
			}
			return true;
		}
		else
		{
			if(m_logger.isWarnEnabled())
				m_logger.warn(this + ".setJobExecutionParameters() " + jobName + 
					" - Job type setting not recognized");
			return false;
		}
		
	}//setJobExecutionParameters
	
	protected String getTime()
	{
		String now = null;
		long millis = System.currentTimeMillis();
		cal.setTimeInMillis(millis);
		now = formatter.format(cal.getTime());
		return now;
	}

	/* (non-Javadoc)
	 * @see org.sakaiproject.component.app.dissertation.StepChangeJob#init()
	 */
	public void init() 
	{
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.sakaiproject.component.app.dissertation.StepChangeJob#destroy()
	 */
	public void destroy() 
	{
		// TODO Auto-generated method stub
		
	}

}
