package com.kevinguanchedarias.owgejava.util;

import org.apache.log4j.Logger;

import com.kevinguanchedarias.owgejava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;

public final class RequirementUtil {
	
	private static Logger logger = Logger.getLogger(RequirementUtil.class); 
	
	private RequirementUtil(){
		throw new AssertionError();
	}
	
	/**
	 * Will check if the requirement type is valid
	 * @param input - The value that has been input
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public static Boolean validRequirementType(String input){
		try{
			RequirementTypeEnum.valueOf(input);
			return true;
		}catch(IllegalArgumentException e){
			logger.warn(e);
			return false;
		}
	}
	
	/**
	 * Will check if the target object type is valid
	 * @param input - The input value to test
	 * @author Kevin Guanche Darias
	 */
	public static Boolean validObjectType(String input){
		try{
			RequirementTargetObject.valueOf(input);
			return true;
		}catch(IllegalArgumentException e){
			logger.warn(e);
			return false;
		}
	}
	
}
